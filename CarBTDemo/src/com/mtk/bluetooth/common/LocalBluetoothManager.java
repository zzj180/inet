/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mtk.bluetooth.common;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//import com.mtk.bluetooth.R;

// TODO: have some notion of shutting down.  Maybe a minute after they leave BT settings?
/**
 * LocalBluetoothManager provides a simplified interface on top of a subset of
 * the Bluetooth API.
 */
public class LocalBluetoothManager {
    private static final String TAG = "LocalBluetoothManager";
    static final boolean V = true;
    static final boolean D = true;

    private static final String SHARED_PREFERENCES_NAME = "bluetooth_settings";

    public static final ParcelUuid HSAG = ParcelUuid
            .fromString("00001112-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid HFAG = ParcelUuid
            .fromString("0000111F-0000-1000-8000-00805F9B34FB");

    /** Singleton instance. */
    // private static LocalBluetoothManager INSTANCE;
    /** Singleton instance. */
    private static LocalBluetoothManager sInstance;
    private boolean mInitialized;

    private Context mContext;

    private BluetoothAdapter mAdapter;

    private final LocalBluetoothAdapter mLocalAdapter;

    private final CachedBluetoothDeviceManager mCachedDeviceManager;

    /** The Bluetooth profile manager. */
    private final LocalBluetoothProfileManager mProfileManager;

    /** The broadcast receiver event manager. */
    private final BluetoothEventManager mEventManager;
    private BluetoothA2dp mBluetoothA2dp;

    private int mState = BluetoothAdapter.ERROR;

    private List<Callback> mCallbacks = new ArrayList<Callback>();

    private static final int SCAN_EXPIRATION_MS = 2 * 60 * 1000; // 5 mins

    // If a device was picked from the device picker or was in discoverable mode
    // in the last 60 seconds, show the pairing dialogs in foreground instead
    // of raising notifications
    private static long GRACE_PERIOD_TO_SHOW_DIALOGS_IN_FOREGROUND = 60 * 1000;
    /* Android2.2 Added : Begin */
    public static final String SHARED_PREFERENCES_KEY_DISCOVERING_TIMESTAMP = "last_discovering_time";
    /* Android2.2 Added : End */
    private static final String SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE = "last_selected_device";

    private static final String SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE_TIME = "last_selected_device_time";

    private static final String SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT = "auto_connect_to_dock";

    private long mLastScan;

    public static synchronized LocalBluetoothManager getInstance(Context context) {
        if (sInstance == null) {
            LocalBluetoothAdapter adapter = LocalBluetoothAdapter.getInstance();
            if (adapter == null) {
                return null;
            }
            // This will be around as long as this process is
            Context appContext = context.getApplicationContext();
            sInstance = new LocalBluetoothManager(adapter, appContext);
        }

        return sInstance;
    }

    private LocalBluetoothManager(LocalBluetoothAdapter adapter, Context context) {
        mContext = context;
        mLocalAdapter = adapter;

        mCachedDeviceManager = new CachedBluetoothDeviceManager(context);
        mEventManager = new BluetoothEventManager(mLocalAdapter, mCachedDeviceManager, context);
        mProfileManager = new LocalBluetoothProfileManager(context, mLocalAdapter,
                mCachedDeviceManager, mEventManager);
    }

    // public BluetoothAdapter getBluetoothAdapter() {
    // return mAdapter;
    // }
    public LocalBluetoothAdapter getBluetoothAdapter() {
        return mLocalAdapter;
    }

    public Context getContext() {
        return mContext;
    }

    public SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public CachedBluetoothDeviceManager getCachedDeviceManager() {
        return mCachedDeviceManager;
    }

    public BluetoothEventManager getEventManager() {
        return mEventManager;
    }

    List<Callback> getCallbacks() {
        return mCallbacks;
    }

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    public void startScanning(boolean force) {
        if (mAdapter.isDiscovering()) {
            /*
             * Already discovering, but give the callback that information.
             * Note: we only call the callbacks, not the same path as if the
             * scanning state had really changed (in that case the device
             * manager would clear its list of unpaired scanned devices).
             */
            dispatchScanningStateChanged(true);
        } else {
            if (!force) {
                // Don't scan more than frequently than SCAN_EXPIRATION_MS,
                // unless forced
                if (mLastScan + SCAN_EXPIRATION_MS > System.currentTimeMillis()) {
                    return;
                }
            }

            if (mAdapter.startDiscovery()) {
                mLastScan = System.currentTimeMillis();
            }
        }
    }

    public void stopScanning() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
    }

    public int getBluetoothState() {

        if (mState == BluetoothAdapter.ERROR) {
            syncBluetoothState();
        }

        return mState;
    }

    void setBluetoothStateInt(int state) {
        mState = state;
        if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF) {
            mCachedDeviceManager.onBluetoothStateChanged(/* state == */
            BluetoothAdapter.STATE_ON); // TBD
        }
    }

    private void syncBluetoothState() {
        int bluetoothState;

        if (mAdapter != null) {
            bluetoothState = mAdapter.isEnabled() ? BluetoothAdapter.STATE_ON
                    : BluetoothAdapter.STATE_OFF;
        } else {
            bluetoothState = BluetoothAdapter.ERROR;
        }

        setBluetoothStateInt(bluetoothState);
    }

    public void setBluetoothEnabled(boolean enabled) {
        boolean wasSetStateSuccessful = enabled ? mAdapter.enable() : mAdapter.disable();

        if (wasSetStateSuccessful) {
            setBluetoothStateInt(enabled ? BluetoothAdapter.STATE_TURNING_ON
                    : BluetoothAdapter.STATE_TURNING_OFF);
        } else {
            if (V) {
                Log.v(TAG, "setBluetoothEnabled call, manager didn't return success for enabled: "
                        + enabled);
            }

            syncBluetoothState();
        }
    }

    /**
     * @param started
     *            True if scanning started, false if scanning finished.
     */
    void onScanningStateChanged(boolean started) {
        // TODO: have it be a callback (once we switch bluetooth state changed
        // to callback)
        mCachedDeviceManager.onScanningStateChanged(started);
        dispatchScanningStateChanged(started);
    }

    private void dispatchScanningStateChanged(boolean started) {
        synchronized (mCallbacks) {
            for (Callback callback : mCallbacks) {
                callback.onScanningStateChanged(started);
            }
        }
    }

    public interface Callback {
        void onScanningStateChanged(boolean started);

        void onDeviceAdded(CachedBluetoothDevice cachedDevice);

        void onDeviceDeleted(CachedBluetoothDevice cachedDevice);
    }

    void persistSelectedDeviceInPicker(String deviceAddress) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(LocalBluetoothManager.SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE,
                deviceAddress);
        editor.putLong(LocalBluetoothManager.SHARED_PREFERENCES_KEY_LAST_SELECTED_DEVICE_TIME,
                System.currentTimeMillis());
        editor.apply();
    }

    public boolean hasDockAutoConnectSetting(String addr) {
        return getSharedPreferences().contains(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr);
    }

    public boolean getDockAutoConnectSetting(String addr) {
        return getSharedPreferences().getBoolean(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr,
                false);
    }

    public void saveDockAutoConnectSetting(String addr, boolean autoConnect) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr, autoConnect);
        editor.apply();
    }

    public void removeDockAutoConnectSetting(String addr) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.remove(SHARED_PREFERENCES_KEY_DOCK_AUTO_CONNECT + addr);
        editor.apply();
    }

    public LocalBluetoothProfileManager getProfileManager() {
        return mProfileManager;
    }
}
