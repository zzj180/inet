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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbap;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import com.mtk.bluetooth.R;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientConstants;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientManager;
import java.util.HashMap;
import java.util.List;

/**
 * PBAPServer Profile
 */
final class PbapServerProfile implements LocalBluetoothProfile {
    private static final String TAG = "PbapServerProfile";
    private static boolean V = true;

    private BluetoothPbap mService;
    private boolean mIsProfileReady;

    static final String NAME = "PBAP Server";

    // Order of this profile in device profiles list
    private static final int ORDINAL = 6;

    // These callbacks run on the main thread.
    private final class PbapServiceListener
            implements BluetoothPbap.ServiceListener {

        public void onServiceConnected(BluetoothPbap proxy) {
            if (V) Log.d(TAG,"Bluetooth service connected");
            mService = (BluetoothPbap) proxy;
            mIsProfileReady=true;
        }

        public void onServiceDisconnected() {
            if (V) Log.d(TAG,"Bluetooth service disconnected");
            mIsProfileReady=false;
        }
    }

    public boolean isProfileReady() {
        return mIsProfileReady;
    }

    PbapServerProfile(Context context) {
        BluetoothPbap pbap = new BluetoothPbap(context, new PbapServiceListener());
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean connect(BluetoothDevice device) {//connect pbap client

        clientConnect(device);
        return true;

    }

    private void clientConnect(BluetoothDevice device){
        BluetoothPbapClientManager mManager =  
            BluetoothPbapClientManager.getInstance();
        int state = mManager.getConnectState();
        if((state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED)||
               (state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTING||
               !mManager.getDevice().getAddress().equals(device.getAddress()))){

               mManager.initConnect(device);
               mManager.connectDevice();

        }
      
    }
  
    public boolean disconnect(BluetoothDevice device) {
        BluetoothPbapClientManager mManager =  
            BluetoothPbapClientManager.getInstance();
        if(mManager != null){
           mManager.disconnectDevice();
        }
        if (mService == null) return false;
        return mService.disconnect();
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (mService == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        if (mService.isConnected(device))
            return BluetoothProfile.STATE_CONNECTED;
        else
            return BluetoothProfile.STATE_DISCONNECTED;
    }

    public boolean isPreferred(BluetoothDevice device) {
        return false;
    }

    public int getPreferred(BluetoothDevice device) {
        return -1;
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        // ignore: isPreferred is always true for PBAP
    }

    public String toString() {
        return NAME;
    }

    public int getOrdinal() {
        return ORDINAL;
    }

    public int getNameResource(BluetoothDevice device) {
        return R.string.bluetooth_profile_pbap;
    }

    public int getSummaryResourceForDevice(BluetoothDevice device) {
        return /*R.string.bluetooth_profile_pbap_summary*/R.string.bluetooth_profile_pbap; //TBD
    }

    public int getDrawableResource(BluetoothClass btClass) {
        return R.drawable.ic_bt_cellphone;
    }

    protected void finalize() {
        if (V) Log.d(TAG, "finalize()");
        if (mService != null) {
            try {
                mService.close();
                mService = null;
            }catch (Throwable t) {
                Log.w(TAG, "Error cleaning up PBAP proxy", t);
            }
        }
    }
}
