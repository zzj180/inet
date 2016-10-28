/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.lzl.laucher.model;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
@SuppressLint("UseSparseArrays")
@SuppressWarnings("unused")
public class LauncherModel extends BroadcastReceiver {
    static final boolean DEBUG_LOADERS = true;
    static final String TAG = "Launcher.Model";

    private static final int ITEMS_CHUNK = 6; // batch size for the workspace icons
    private final boolean mAppsCanBeOnExternalStorage;
    private int mBatchSize; // 0 is all apps at once
    private int mAllAppsLoadDelay; // milliseconds between batches

    private final Context mApp;
    private final Object mLock = new Object();
    private DeferredHandler mHandler = new DeferredHandler();
    private LoaderTask mLoaderTask;
    private boolean mIsLoaderTaskRunning;
    private volatile boolean mFlushingWorkerThread;

    // Specific runnable types that are run on the main thread deferred handler, this allows us to
    // clear all queued binding runnables when the Launcher activity is destroyed.
    private static final int MAIN_THREAD_NORMAL_RUNNABLE = 0;
    private static final int MAIN_THREAD_BINDING_RUNNABLE = 1;


    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    // We start off with everything not loaded.  After that, we assume that
    // our monitoring of the package manager provides all updates and we never
    // need to do a requery.  These are only ever touched from the loader thread.
    private boolean mWorkspaceLoaded;
    private boolean mAllAppsLoaded;

    // When we are loading pages synchronously, we can't just post the binding of items on the side
    // pages as this delays the rotation process.  Instead, we wait for a callback from the first
    // draw (in Workspace) to initiate the binding of the remaining side pages.  Any time we start
    // a normal load, we also clear this set of Runnables.
    static final ArrayList<Runnable> mDeferredBindRunnables = new ArrayList<Runnable>();

    private WeakReference<Callbacks> mCallbacks;

    // < only access in worker thread >
    private AllAppsList mBgAllAppsList;

    // The lock that must be acquired before referencing any static bg data structures.  Unlike
    // other locks, this one can generally be held long-term because we never expect any of these
    // static data structures to be referenced outside of the worker thread except on the first
    // load after configuration change.
    static final Object sBgLock = new Object();

    // sBgItemsIdMap maps *all* the ItemInfos (shortcuts, folders, and widgets) created by
    // LauncherModel to their ids
    static final HashMap<Long, ItemInfo> sBgItemsIdMap = new HashMap<Long, ItemInfo>();

    // sBgWorkspaceItems is passed to bindItems, which expects a list of all folders and shortcuts
    //       created by LauncherModel that are directly on the home screen (however, no widgets or
    //       shortcuts within folders).
    static final ArrayList<ItemInfo> sBgWorkspaceItems = new ArrayList<ItemInfo>();

    // sBgDbIconCache is the set of ItemInfos that need to have their icons updated in the database
    static final HashMap<Object, byte[]> sBgDbIconCache = new HashMap<Object, byte[]>();
    // </ only access in worker thread >

    private IconCache mIconCache;
    private Bitmap mDefaultIcon;
    protected int mPreviousConfigMcc;

    public interface Callbacks {
        public boolean setLoadOnResume();
        public int getCurrentWorkspaceScreen();
        public void startBinding();
        public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end);
        public void finishBindingItems();
        public void bindAllApplications(ArrayList<ApplicationInfo> apps);
        public void bindAppsAdded(ArrayList<ApplicationInfo> apps);
        public void bindAppsUpdated(ArrayList<ApplicationInfo> apps);
        public void bindComponentsRemoved(ArrayList<String> packageNames,
                        ArrayList<ApplicationInfo> appInfos,
                        boolean matchPackageNamesOnly);
        public void bindPackagesUpdated(ArrayList<Object> widgetsAndShortcuts);
        public boolean isAllAppsVisible();
        public boolean isAllAppsButtonRank(int rank);
        public void bindSearchablesChanged();
        public void onPageBoundSynchronously(int page);
    }

    public LauncherModel(Context app, IconCache iconCache) {
        mAppsCanBeOnExternalStorage = !Environment.isExternalStorageEmulated();
        mApp = app;
        mBgAllAppsList = new AllAppsList(iconCache);
        mIconCache = iconCache;

        mDefaultIcon = Utilities.createIconBitmap(
                mIconCache.getFullResDefaultActivityIcon(), app);

        final Resources res = app.getResources();
        mAllAppsLoadDelay = 100;
        mBatchSize = 200;
        Configuration config = res.getConfiguration();
        mPreviousConfigMcc = config.mcc;
    }

    /** Runs the specified runnable immediately if called from the main thread, otherwise it is
     * posted on the main thread handler. */
    private void runOnMainThread(Runnable r) {
        runOnMainThread(r, 0);
    }
    private void runOnMainThread(Runnable r, int type) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    /** Runs the specified runnable immediately if called from the worker thread, otherwise it is
     * posted on the worker thread handler. */
    private static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }

    public Bitmap getFallbackIcon() {
        return Bitmap.createBitmap(mDefaultIcon);
    }

    public void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the " +
                    "main thread");
        }

        // Clear any deferred bind runnables
        mDeferredBindRunnables.clear();
        // Remove any queued bind runnables
        mHandler.cancelAllRunnablesOfType(MAIN_THREAD_BINDING_RUNNABLE);
    }

    public void flushWorkerThread() {
        mFlushingWorkerThread = true;
        Runnable waiter = new Runnable() {
                public void run() {
                    synchronized (this) {
                        notifyAll();
                        mFlushingWorkerThread = false;
                    }
                }
            };

        synchronized(waiter) {
            runOnWorkerThread(waiter);
            if (mLoaderTask != null) {
                synchronized(mLoaderTask) {
                    mLoaderTask.notify();
                }
            }
            boolean success = false;
            while (!success) {
                try {
                    waiter.wait();
                    success = true;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(Callbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<Callbacks>(callbacks);
        }
    }

    /**
     * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED and
     * ACTION_PACKAGE_CHANGED.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG_LOADERS) Log.d(TAG, "onReceive intent=" + intent);

        final String action = intent.getAction();

        if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            final String packageName = intent.getData().getSchemeSpecificPart();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            int op = PackageUpdatedTask.OP_NONE;

            if (packageName == null || packageName.length() == 0) {
                // they sent us a bad intent
                return;
            }

            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                op = PackageUpdatedTask.OP_UPDATE;
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_REMOVE;
                }
                // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                // later, we will update the package at this time
            } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                if (!replacing) {
                    op = PackageUpdatedTask.OP_ADD;
                } else {
                    op = PackageUpdatedTask.OP_UPDATE;
                }
            }

            if (op != PackageUpdatedTask.OP_NONE) {
                enqueuePackageUpdated(new PackageUpdatedTask(op, new String[] { packageName }));
            }

        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
            // First, schedule to add these apps back in.
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            enqueuePackageUpdated(new PackageUpdatedTask(PackageUpdatedTask.OP_ADD, packages));
            // Then, rebind everything.
            startLoaderFromBackground();
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
            String[] packages = intent.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            enqueuePackageUpdated(new PackageUpdatedTask(
                        PackageUpdatedTask.OP_UNAVAILABLE, packages));
        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // If we have changed locale we need to clear out the labels in all apps/workspace.
            forceReload();
        } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action)) {
             // Check if configuration change was an mcc/mnc change which would affect app resources
             // and we would need to clear out the labels in all apps/workspace. Same handling as
             // above for ACTION_LOCALE_CHANGED
             Configuration currentConfig = context.getResources().getConfiguration();
             if (mPreviousConfigMcc != currentConfig.mcc) {
                   Log.d(TAG, "Reload apps on config change. curr_mcc:"
                       + currentConfig.mcc + " prevmcc:" + mPreviousConfigMcc);
                   forceReload();
             }
             // Update previousConfig
             mPreviousConfigMcc = currentConfig.mcc;
        } else if (SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED.equals(action) ||
                   SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED.equals(action)) {
            if (mCallbacks != null) {
                Callbacks callbacks = mCallbacks.get();
                if (callbacks != null) {
                    callbacks.bindSearchablesChanged();
                }
            }
        }
    }

    private void forceReload() {
        resetLoadedState(true, true);

        // Do this here because if the launcher activity is running it will be restarted.
        // If it's not running startLoaderFromBackground will merely tell it that it needs
        // to reload.
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (mLock) {
            // Stop any existing loaders first, so they don't set mAllAppsLoaded or
            // mWorkspaceLoaded to true later
            stopLoaderLocked();
            if (resetAllAppsLoaded) mAllAppsLoaded = false;
            if (resetWorkspaceLoaded) mWorkspaceLoaded = false;
        }
    }

    /**
     * When the launcher is in the background, it's possible for it to miss paired
     * configuration changes.  So whenever we trigger the loader from the background
     * tell the launcher that it needs to re-run the loader when it comes back instead
     * of doing it now.
     */
    public void startLoaderFromBackground() {
        boolean runLoader = false;
        if (mCallbacks != null) {
            Callbacks callbacks = mCallbacks.get();
            if (callbacks != null) {
                // Only actually run the loader if they're not paused.
                if (!callbacks.setLoadOnResume()) {
                    runLoader = true;
                }
            }
        }
        if (runLoader) {
            startLoader(false, -1);
        }
    }

    // If there is already a loader task running, tell it to stop.
    // returns true if isLaunching() was true on the old task
    private boolean stopLoaderLocked() {
        boolean isLaunching = false;
        LoaderTask oldTask = mLoaderTask;
        if (oldTask != null) {
            if (oldTask.isLaunching()) {
                isLaunching = true;
            }
            oldTask.stopLocked();
        }
        return isLaunching;
    }

    public void startLoader(boolean isLaunching, int synchronousBindPage) {
        synchronized (mLock) {
            if (DEBUG_LOADERS) {
                Log.d(TAG, "startLoader isLaunching=" + isLaunching);
            }

            // Clear any deferred bind-runnables from the synchronized load process
            // We must do this before any loading/binding is scheduled below.
            mDeferredBindRunnables.clear();

            // Don't bother to start the thread if we know it's not going to do anything
            if (mCallbacks != null && mCallbacks.get() != null) {
                // If there is already one running, tell it to stop.
                // also, don't downgrade isLaunching if we're already running
                isLaunching = isLaunching || stopLoaderLocked();
                mLoaderTask = new LoaderTask(mApp, isLaunching);
                if (synchronousBindPage > -1 && mAllAppsLoaded && mWorkspaceLoaded) {
                    mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                } else {
                    sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                    sWorker.post(mLoaderTask);
                }
            }
        }
    }


    public void stopLoader() {
        synchronized (mLock) {
            if (mLoaderTask != null) {
                mLoaderTask.stopLocked();
            }
        }
    }

    public boolean isAllAppsLoaded() {
        return mAllAppsLoaded;
    }

    /**
     * Runnable for the thread that loads the contents of the launcher:
     *   - workspace icons
     *   - widgets
     *   - all apps icons
     */
    private class LoaderTask implements Runnable {
        private Context mContext;
        private boolean mIsLaunching;
        private boolean mStopped;
        private boolean mLoadAndBindStepFinished;

        private HashMap<Object, CharSequence> mLabelCache;

        LoaderTask(Context context, boolean isLaunching) {
            mContext = context;
            mIsLaunching = isLaunching;
            mLabelCache = new HashMap<Object, CharSequence>();
        }

        boolean isLaunching() {
            return mIsLaunching;
        }

        private void waitForIdle() {
            // Wait until the either we're stopped or the other threads are done.
            // This way we don't start loading all apps until the workspace has settled
            // down.
            synchronized (LoaderTask.this) {
                final long workspaceWaitTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

                mHandler.postIdle(new Runnable() {
                        public void run() {
                            synchronized (LoaderTask.this) {
                                mLoadAndBindStepFinished = true;
                                if (DEBUG_LOADERS) {
                                    Log.d(TAG, "done with previous binding step");
                                }
                                LoaderTask.this.notify();
                            }
                        }
                    });

                while (!mStopped && !mLoadAndBindStepFinished && !mFlushingWorkerThread) {
                    try {
                        // Just in case mFlushingWorkerThread changes but we aren't woken up,
                        // wait no longer than 1sec at a time
                        this.wait(1000);
                    } catch (InterruptedException ex) {
                        // Ignore
                    }
                }
                if (DEBUG_LOADERS) {
                    Log.d(TAG, "waited "
                            + (SystemClock.uptimeMillis()-workspaceWaitTime)
                            + "ms for previous step to finish binding");
                }
            }
        }

        void runBindSynchronousPage(int synchronousBindPage) {
            if (synchronousBindPage < 0) {
                // Ensure that we have a valid page index to load synchronously
                throw new RuntimeException("Should not call runBindSynchronousPage() without " +
                        "valid page index");
            }
            if (!mAllAppsLoaded || !mWorkspaceLoaded) {
                // Ensure that we don't try and bind a specified page when the pages have not been
                // loaded already (we should load everything asynchronously in that case)
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
            synchronized (mLock) {
                if (mIsLoaderTaskRunning) {
                    // Ensure that we are never running the background loading at this point since
                    // we also touch the background collections
                    throw new RuntimeException("Error! Background loading is already running");
                }
            }

            // XXX: Throw an exception if we are already loading (since we touch the worker thread
            //      data structures, we can't allow any other thread to touch that data, but because
            //      this call is synchronous, we can get away with not locking).

            // The LauncherModel is static in the LauncherApplication and mHandler may have queued
            // operations from the previous activity.  We need to ensure that all queued operations
            // are executed before any synchronous binding work is done.
            mHandler.flush();

            // Divide the set of loaded items into those that we are binding synchronously, and
            // everything else that is to be bound normally (asynchronously).
            onlyBindAllApps();
        }

        public void run() {
            synchronized (mLock) {
                mIsLoaderTaskRunning = true;
            }
            // Optimize for end-user experience: if the Launcher is up and // running with the
            // All Apps interface in the foreground, load All Apps first. Otherwise, load the
            // workspace first (default).
            final Callbacks cbk = mCallbacks.get();

            keep_running: {
                // Elevate priority when Home launches for the first time to avoid
                // starving at boot time. Staring at a blank home is not cool.
                synchronized (mLock) {
                    if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to " +
                            (mIsLaunching ? "DEFAULT" : "BACKGROUND"));
                    android.os.Process.setThreadPriority(mIsLaunching
                            ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }
                loadAndBindAllApps();

                if (mStopped) {
                    break keep_running;
                }

                // Whew! Hard work done.  Slow us down, and wait until the UI thread has
                // settled down.
                synchronized (mLock) {
                    if (mIsLaunching) {
                        if (DEBUG_LOADERS) Log.d(TAG, "Setting thread priority to BACKGROUND");
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    }
                }
                waitForIdle();

                loadAndBindAllApps();

                // Restore the default thread priority after we are done loading items
                synchronized (mLock) {
                    android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                }
            }

            // Clear out this reference, otherwise we end up holding it until all of the
            // callback runnables are done.
            mContext = null;

            synchronized (mLock) {
                // If we are still the last one to be scheduled, remove ourselves.
                if (mLoaderTask == this) {
                    mLoaderTask = null;
                }
                mIsLoaderTaskRunning = false;
            }
        }

        public void stopLocked() {
            synchronized (LoaderTask.this) {
                mStopped = true;
                this.notify();
            }
        }

        /**
         * Gets the callbacks object.  If we've been stopped, or if the launcher object
         * has somehow been garbage collected, return null instead.  Pass in the Callbacks
         * object that was around when the deferred message was scheduled, and if there's
         * a new Callbacks object around then also return null.  This will save us from
         * calling onto it with data that will be ignored.
         */
        Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
            synchronized (mLock) {
                if (mStopped) {
                    return null;
                }

                if (mCallbacks == null) {
                    return null;
                }

                final Callbacks callbacks = mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks == null) {
                    Log.w(TAG, "no mCallbacks");
                    return null;
                }

                return callbacks;
            }
        }

        /**
         * Binds all loaded data to actual views on the main thread.
         */

        private void loadAndBindAllApps() {
            if (DEBUG_LOADERS) {
                Log.d(TAG, "loadAndBindAllApps mAllAppsLoaded=" + mAllAppsLoaded);
            }
            if (!mAllAppsLoaded) {
                loadAllAppsByBatch();
                synchronized (LoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mAllAppsLoaded = true;
                }
            } else {
                onlyBindAllApps();
            }
        }

        private void onlyBindAllApps() {
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (onlyBindAllApps)");
                return;
            }

            // shallow copy
            @SuppressWarnings("unchecked")
            final ArrayList<ApplicationInfo> list
                    = (ArrayList<ApplicationInfo>) mBgAllAppsList.data.clone();
            Runnable r = new Runnable() {
                public void run() {
                    final long t = SystemClock.uptimeMillis();
                    final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(list);
                    }
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "bound all " + list.size() + " apps from cache in "
                                + (SystemClock.uptimeMillis()-t) + "ms");
                    }
                }
            };
            boolean isRunningOnMainThread = !(sWorkerThread.getThreadId() == Process.myTid());
            if (oldCallbacks.isAllAppsVisible() && isRunningOnMainThread) {
                r.run();
            } else {
                mHandler.post(r);
            }
        }

        private void loadAllAppsByBatch() {
            final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = mCallbacks.get();
            if (oldCallbacks == null) {
                // This launcher has exited and nobody bothered to tell us.  Just bail.
                Log.w(TAG, "LoaderTask running with no launcher (loadAllAppsByBatch)");
                return;
            }

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final PackageManager packageManager = mContext.getPackageManager();
            List<ResolveInfo> apps = null;

            int N = Integer.MAX_VALUE;

            int startIndex;
            int i=0;
            int batchSize = -1;
            while (i < N && !mStopped) {
                if (i == 0) {
                    mBgAllAppsList.clear();
                    final long qiaTime = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;
                    apps = packageManager.queryIntentActivities(mainIntent, 0);
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "queryIntentActivities took "
                                + (SystemClock.uptimeMillis()-qiaTime) + "ms");
                    }
                    if (apps == null) {
                        return;
                    }
                    N = apps.size();
                    if (DEBUG_LOADERS) {
                        Log.d(TAG, "queryIntentActivities got " + N + " apps");
                    }
                    if (N == 0) {
                        // There are no apps?!?
                        return;
                    }
                    if (mBatchSize == 0) {
                        batchSize = N;
                    } else {
                        batchSize = mBatchSize;
                    }
                }

                final long t2 = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

                startIndex = i;
                for (int j=0; i<N && j<batchSize; j++) {
                    // This builds the icon bitmaps.
                    mBgAllAppsList.add(new ApplicationInfo(packageManager, apps.get(i),
                            mIconCache, mLabelCache));
                    i++;
                }

                final boolean first = i <= batchSize;
                final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                final ArrayList<ApplicationInfo> added = mBgAllAppsList.added;
                mBgAllAppsList.added = new ArrayList<ApplicationInfo>();

                mHandler.post(new Runnable() {
                    public void run() {
                        final long t = SystemClock.uptimeMillis();
                        if (callbacks != null) {
                            if (first) {
                                callbacks.bindAllApplications(added);
                            } else {
                                callbacks.bindAppsAdded(added);
                            }
                            if (DEBUG_LOADERS) {
                                Log.d(TAG, "bound " + added.size() + " apps in "
                                    + (SystemClock.uptimeMillis() - t) + "ms");
                            }
                        } else {
                            Log.i(TAG, "not binding apps: no Launcher activity");
                        }
                    }
                });

                if (DEBUG_LOADERS) {
                    Log.d(TAG, "batch of " + (i-startIndex) + " icons processed in "
                            + (SystemClock.uptimeMillis()-t2) + "ms");
                }

                if (mAllAppsLoadDelay > 0 && i < N) {
                    try {
                        if (DEBUG_LOADERS) {
                            Log.d(TAG, "sleeping for " + mAllAppsLoadDelay + "ms");
                        }
                        Thread.sleep(mAllAppsLoadDelay);
                    } catch (InterruptedException exc) { }
                }
            }

            if (DEBUG_LOADERS) {
                Log.d(TAG, "cached all " + N + " apps in "
                        + (SystemClock.uptimeMillis()-t) + "ms"
                        + (mAllAppsLoadDelay > 0 ? " (including delay)" : ""));
            }
        }

        public void dumpState() {
            synchronized (sBgLock) {
                Log.d(TAG, "mLoaderTask.mContext=" + mContext);
                Log.d(TAG, "mLoaderTask.mIsLaunching=" + mIsLaunching);
                Log.d(TAG, "mLoaderTask.mStopped=" + mStopped);
                Log.d(TAG, "mLoaderTask.mLoadAndBindStepFinished=" + mLoadAndBindStepFinished);
                Log.d(TAG, "mItems size=" + sBgWorkspaceItems.size());
            }
        }
    }

    void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;

        public static final int OP_NONE = 0;
        public static final int OP_ADD = 1;
        public static final int OP_UPDATE = 2;
        public static final int OP_REMOVE = 3; // uninstlled
        public static final int OP_UNAVAILABLE = 4; // external media unmounted


        public PackageUpdatedTask(int op, String[] packages) {
            mOp = op;
            mPackages = packages;
        }

        public void run() {
            final Context context = mApp;

            final String[] packages = mPackages;
            final int N = packages.length;
            switch (mOp) {
                case OP_ADD:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.addPackage " + packages[i]);
                        mBgAllAppsList.addPackage(context, packages[i]);
                    }
                    break;
                case OP_UPDATE:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.updatePackage " + packages[i]);
                        mBgAllAppsList.updatePackage(context, packages[i]);
                    }
                    break;
                case OP_REMOVE:
                case OP_UNAVAILABLE:
                    for (int i=0; i<N; i++) {
                        if (DEBUG_LOADERS) Log.d(TAG, "mAllAppsList.removePackage " + packages[i]);
                        mBgAllAppsList.removePackage(packages[i]);
                    }
                    break;
            }

            ArrayList<ApplicationInfo> added = null;
            ArrayList<ApplicationInfo> modified = null;
            final ArrayList<ApplicationInfo> removedApps = new ArrayList<ApplicationInfo>();

            if (mBgAllAppsList.added.size() > 0) {
                added = new ArrayList<ApplicationInfo>(mBgAllAppsList.added);
                mBgAllAppsList.added.clear();
            }
            if (mBgAllAppsList.modified.size() > 0) {
                modified = new ArrayList<ApplicationInfo>(mBgAllAppsList.modified);
                mBgAllAppsList.modified.clear();
            }
            if (mBgAllAppsList.removed.size() > 0) {
                removedApps.addAll(mBgAllAppsList.removed);
                mBgAllAppsList.removed.clear();
            }

            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
            if (callbacks == null) {
                Log.w(TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                return;
            }

            if (added != null) {
                final ArrayList<ApplicationInfo> addedFinal = added;
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsAdded(addedFinal);
                        }
                    }
                });
            }
            
            if (modified != null) {
                final ArrayList<ApplicationInfo> modifiedFinal = modified;
                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsUpdated(modifiedFinal);
                        }
                    }
                });
            }
            // If a package has been removed, or an app has been removed as a result of
            // an update (for example), make the removed callback.
            if (mOp == OP_REMOVE || !removedApps.isEmpty()) {
                final boolean permanent = (mOp == OP_REMOVE);
                final ArrayList<String> removedPackageNames =
                        new ArrayList<String>(Arrays.asList(packages));

                mHandler.post(new Runnable() {
                    public void run() {
                        Callbacks cb = mCallbacks != null ? mCallbacks.get() : null;
                        if (callbacks == cb && cb != null) {
                            callbacks.bindComponentsRemoved(removedPackageNames,
                                    removedApps, permanent);
                        }
                    }
                });
            }

        }
    }
    
    static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        if (info.activityInfo != null) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        } else {
            return new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
        }
    }

}
