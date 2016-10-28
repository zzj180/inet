/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.mtk.bluetooth.common;

import android.content.Context;
import android.content.Intent;


import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothA2dpSink;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothMap;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

import com.mediatek.bluetooth.BluetoothProfileManager;
/**
 * LocalBluetoothProfileManager provides access to the LocalBluetoothProfile
 * objects for the available Bluetooth profiles.
 */
public  class LocalBluetoothProfileManager {
    private static final String TAG = "Car_LocalBluetoothProfileManager";
    private static final boolean DEBUG = true;

    public static final String ACTION_PROFILE_UPDATE ="com.mtk.bluetooth.common.profiles_updater";
    /** Singleton instance. */
	private static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;
	// private static Set<Profile> mConenctedProfileList;
	private static LocalBluetoothManager mLocalManager;
	/** Used when obtaining a reference to the singleton instance. */
	private static Object INSTANCE_LOCK = new Object();

    /**
     * An interface for notifying BluetoothHeadset IPC clients when they have
     * been connected to the BluetoothHeadset service. Only used by
     * {@link DockService}.
     */
    public interface ServiceListener {
        /**
         * Called to notify the client when this proxy object has been connected
         * to the BluetoothHeadset service. Clients must wait for this callback
         * before making IPC calls on the BluetoothHeadset service.
         */
        void onServiceConnected();

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the BluetoothHeadset service. Clients must not make
         * IPC calls on the BluetoothHeadset service after this callback. This
         * callback will currently only occur if the application hosting the
         * BluetoothHeadset service, but may be called more often in future.
         */
        void onServiceDisconnected();
    }

    private final Context mContext;
	private static BluetoothProfileManager mService;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final BluetoothEventManager mEventManager;

    private A2dpProfile mA2dpProfile;
	/**
     * Add for A2dpSink
     */
    private A2dpSinkProfile mA2dpSinkProfile;
    private HeadsetProfile mHeadsetProfile;
    private HeadsetClientProfile mHeadsetClientProfile;
    //private OppProfile mOppProfile;
    private PbapServerProfile mPbapProfile;

    /**
     * Mapping from profile name, e.g. "HEADSET" to profile object.
     */
    private final Map<String, LocalBluetoothProfile> mProfileNameMap = new HashMap<String, LocalBluetoothProfile>();

    LocalBluetoothProfileManager(Context context, LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager, BluetoothEventManager eventManager) {
        mContext = context;

        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mEventManager = eventManager;
        // pass this reference to adapter and event manager (circular
        // dependency)
        mLocalAdapter.setProfileManager(this);
        mEventManager.setProfileManager(this);

        ParcelUuid[] uuids = adapter.getUuids();

        // uuids may be null if Bluetooth is turned off
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }

        // Create PBAP server profile, but do not add it to list of profiles
        // as we do not need to monitor the profile as part of profile list
        mPbapProfile = new PbapServerProfile(context);

        if (DEBUG)
            Log.d(TAG, "LocalBluetoothProfileManager construction complete");
    }

    /**
     * Initialize or update the local profile objects. If a UUID was previously
     * present but has been removed, we print a warning but don't remove the
     * profile object as it might be referenced elsewhere, or the UUID might
     * come back and we don't want multiple copies of the profile objects.
     * 
     * @param uuids
     */
	  void updateLocalProfiles(ParcelUuid[] uuids) {
		  Log.d(TAG, "updateLocalProfiles");
		  // A2DP
		  if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSource))
		  	{
			  if (mA2dpProfile == null) {
				  Log.d(TAG, "Adding local A2DP profile");
				  mA2dpProfile = new A2dpProfile(mContext, mLocalAdapter, mDeviceManager, this);
				  addProfile(mA2dpProfile, A2dpProfile.NAME,
						  BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
			  }
		  } else if (mA2dpProfile != null) {
			  Log.w(TAG, "Warning: A2DP profile was previously added but the UUID is now missing.");
		  }
		  /**
		  * Add for A2dpSink
		  */
		  if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.AudioSink)) {
		      if (mA2dpSinkProfile == null) {
		          Log.d(TAG, "Adding local A2dpSink profile");
		          mA2dpSinkProfile = new A2dpSinkProfile(mContext, mLocalAdapter, mDeviceManager, this);
		          addProfile(mA2dpSinkProfile, A2dpSinkProfile.NAME,
		              BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
		      }
		  } else if (mA2dpSinkProfile != null) {
		      Log.w(TAG, "Warning : A2dpSink profile was previously added but the UUID is now missing.");
		  }
	
		  // Headset / Handsfree
		  if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree_AG) ||
			  BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP_AG)) {
			  if (mHeadsetProfile == null) {
				  Log.d(TAG, "Adding local HEADSET profile");
				  mHeadsetProfile = new HeadsetProfile(mContext, mLocalAdapter,
				  		  mDeviceManager, this);
				  addProfile(mHeadsetProfile, HeadsetProfile.NAME,
						  BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);

			  }
		  } else if (mHeadsetProfile != null) {
			  Log.w(TAG, "Warning: HEADSET profile was previously added but the UUID is now missing.");
		  }

          ///M: Add for Headset Client
          if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree) ||
                BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP)) {
                if (mHeadsetClientProfile == null) {
                    if (DEBUG) Log.d(TAG, "Adding local HEADSET CLIENT profile");
                    mHeadsetClientProfile = new HeadsetClientProfile(mContext, mLocalAdapter,
                            mDeviceManager, this);
                    addProfile(mHeadsetClientProfile, HeadsetClientProfile.NAME,
                            BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED);
                }
            } else if (mHeadsetClientProfile != null) {
                Log.w(TAG, "Warning: HEADSET CLIENT profile was previously added but the UUID is now missing.");
            }
          
		  // OPP
		  
		  mEventManager.registerProfileIntentReceiver();
	
		  // There is no local SDP record for HID and Settings app doesn't control PBAP
	  }

    private final Collection<ServiceListener> mServiceListeners = new ArrayList<ServiceListener>();

    private void addProfile(LocalBluetoothProfile profile, String profileName,
            String stateChangedAction) {
        mEventManager.addProfileHandler(stateChangedAction, new StateChangedHandler(profile));
        mProfileNameMap.put(profileName, profile);
    }

    LocalBluetoothProfile getProfileByName(String name) {
        return mProfileNameMap.get(name);
    }

    // Called from LocalBluetoothAdapter when state changes to ON
    void setBluetoothStateOn() {
        if (mPbapProfile == null) {
            mPbapProfile = new PbapServerProfile(mContext);
        }
        ParcelUuid[] uuids = mLocalAdapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }
        mEventManager.readPairedDevices();
    }

    /**
     * Generic handler for connection state change events for the specified
     * profile.
     */
    private class StateChangedHandler implements BluetoothEventManager.Handler {
        final LocalBluetoothProfile mProfile;

        StateChangedHandler(LocalBluetoothProfile profile) {
            mProfile = profile;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            Log.d(TAG,"StateChangedHandler cachedDevice =="+cachedDevice);
            if (cachedDevice == null) {
                Log.w(TAG, "StateChangedHandler found new device: " + device);
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter,
                        LocalBluetoothProfileManager.this, device);
            }
            int newState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
            int oldState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0);
            if (newState == BluetoothProfile.STATE_DISCONNECTED
                    && oldState == BluetoothProfile.STATE_CONNECTING) {
                Log.i(TAG, "Failed to connect " + mProfile + " device");
            }

            cachedDevice.onProfileStateChanged(mProfile, newState);
            cachedDevice.refresh();
        }
    }

    // called from DockService
    void addServiceListener(ServiceListener l) {
        mServiceListeners.add(l);
    }

    // called from DockService
    void removeServiceListener(ServiceListener l) {
        mServiceListeners.remove(l);
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceConnectedListeners() {
        for (ServiceListener l : mServiceListeners) {
            l.onServiceConnected();
        }
    }

    // not synchronized: use only from UI thread! (TODO: verify)
    void callServiceDisconnectedListeners() {
        for (ServiceListener listener : mServiceListeners) {
            listener.onServiceDisconnected();
        }
    }

    // This is called by DockService, so check Headset and A2DP.
    public synchronized boolean isManagerReady() {
        // Getting just the headset profile is fine for now. Will need to deal
        // with A2DP
        // and others if they aren't always in a ready state.
        LocalBluetoothProfile profile = mHeadsetProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        profile = mA2dpProfile;
        if (profile != null) {
            return profile.isProfileReady();
        }
        return false;
    }

    public A2dpProfile getA2dpProfile() {
        return mA2dpProfile;
    }

	/**
     * Add for A2dpSink
     */
    public A2dpSinkProfile getA2dpSinkProfile() {
        return mA2dpSinkProfile;
    }

    public HeadsetProfile getHeadsetProfile() {
        return mHeadsetProfile;
    }

	public HeadsetClientProfile getHeadsetClientProfile() {
        return mHeadsetClientProfile;
    }
    public PbapServerProfile getPbapProfile() {
        return mPbapProfile;
    }

    /**
     * Fill in a list of LocalBluetoothProfile objects that are supported by the
     * local device and the remote device.
     * 
     * @param uuids
     *            of the remote device
     * @param localUuids
     *            UUIDs of the local device
     * @param profiles
     *            The list of profiles to fill
     * @param removedProfiles
     *            list of profiles that were removed
     */
	 synchronized void updateProfiles(ParcelUuid[] uuids, ParcelUuid[] localUuids,
            Collection<LocalBluetoothProfile> profiles,
            Collection<LocalBluetoothProfile> removedProfiles,
            boolean isPanNapConnected, BluetoothDevice device) {
		profiles.clear();

		if (uuids == null) {
			return;
		}
		Log.d(TAG, "updateProfiles start"); 
		Log.d(TAG, "updateProfiles mHeadsetProfile :" +mHeadsetProfile + ",mA2dpProfile: " + mA2dpProfile);
		Log.d(TAG, "updateProfiles mPbapProfile :" + mPbapProfile + ",mA2dpSinkProfile = "+ mA2dpSinkProfile ); 

		Log.v(TAG, "localUuids:");
		for (ParcelUuid uuid : localUuids) {
		    Log.v(TAG, "  " + uuid);
		}

		Log.v(TAG, "UUID:");
		for (ParcelUuid uuid : uuids) {
		    Log.v(TAG, "  " + uuid);
		}
    		
        if (mHeadsetProfile != null) {
            if ((BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.HSP_AG) &&
                 BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP)) ||
                 (BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.Handsfree_AG) &&
                  BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree))) { 
                Log.d(TAG, "Add HeadsetProfile to connectable profile list");
                profiles.add(mHeadsetProfile);
                removedProfiles.remove(mHeadsetProfile);
             }
        }
        ///M: Add for Headset Client
        if (mHeadsetClientProfile != null) {
            if ((BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.HSP) &&
                 BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.HSP_AG)) ||
                 (BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.Handsfree) &&
                  BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Handsfree_AG))) { 
                Log.d(TAG, "Add HeadsetClientProfile to connectable profile list");
                profiles.add(mHeadsetClientProfile);
                removedProfiles.remove(mHeadsetClientProfile);
             }
        }        
       		
        if (BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.SINK_UUIDS) &&
            mA2dpProfile != null) {
            Log.d(TAG, "Add A2dpProfile to connectable profile list");
            profiles.add(mA2dpProfile);
            removedProfiles.remove(mA2dpProfile);
        }

         
          /// Add for A2dpSink               
        if (BluetoothUuid.containsAnyUuid(uuids, A2dpSinkProfile.SINK_UUIDS)
                && mA2dpSinkProfile != null) {
            Log.d(TAG, "[updateProfiles] remote device support source, add the a2dp sink profile");
            Log.d(TAG, "[updateProfiles] contains the SINK UUID");
            profiles.add(mA2dpSinkProfile);
            removedProfiles.remove(mA2dpSinkProfile);
        }
       
        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.PBAP_PSE) &&
            mPbapProfile != null) {
            Log.d(TAG, "Add PbapProfile to connectable profile list");
            profiles.add(mPbapProfile);
            removedProfiles.remove(mPbapProfile);
        }
	     Log.d(TAG, "updateProfiles end profiles" + profiles );
         Intent intent = new Intent();
	     intent.setAction(ACTION_PROFILE_UPDATE);
	     mContext.sendBroadcast(intent);
	}

	private void log(String info) {
		if (true) {
			Log.v(TAG, "[BT][profile manager]" + info);
		}
	}
	/* MTK Added : End */
}
