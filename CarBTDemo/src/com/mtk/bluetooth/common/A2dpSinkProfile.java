package com.mtk.bluetooth.common;


import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothA2dpSink;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import com.mtk.bluetooth.R;
import com.mtk.bluetooth.util.Utils;


public class A2dpSinkProfile implements LocalBluetoothProfile {
    
    private static final String TAG = "[BT][A2DP-setting][A2dpSinkProfile]";
//    private static final String TAG_SURPLUS = "[surplus][A2dpSinkProfile]";
    
    private static final int ORDINAL = 7;
    
    public static final String NAME = "A2DPSINK";
    
    private Context mContext;
    
    static final ParcelUuid[] SINK_UUIDS = {
        BluetoothUuid.AudioSource,
//        BluetoothUuid.AdvAudioDist,
    };
    
    private final CachedBluetoothDeviceManager mDeviceManager;
    private final LocalBluetoothProfileManager mProfileManager;
    private final LocalBluetoothAdapter mLocalAdapter;
    private BluetoothA2dpSink mA2dpSink;
    private boolean mIsProfileReady;
    
    A2dpSinkProfile(Context context, LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager,
            LocalBluetoothProfileManager profileManager) {
        mContext = context;
        mDeviceManager = deviceManager;
        mProfileManager = profileManager;
        mLocalAdapter = adapter;
        mLocalAdapter.getProfileProxy(context, new A2dpSinkServiceListener(),
                BluetoothProfile.A2DP_SINK);
    }
    
    private final class A2dpSinkServiceListener
                implements BluetoothProfile.ServiceListener {

        @Override
        public void onServiceConnected(int arg0, BluetoothProfile arg1) {
            Log.d(TAG, "[onServiceConnected] A2dp Sink Service Connected");
            mA2dpSink = (BluetoothA2dpSink)arg1;
         // We just bound to the service, so refresh the UI for any connected A2DP devices.
            List<BluetoothDevice> deviceList = mA2dpSink.getConnectedDevices();
            
            Log.d(TAG, "[onServiceConnected] deviceList size : " + deviceList.size());
            
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = mDeviceManager.findDevice(nextDevice);
                // we may add a new device here, but generally this should not happen
                if (device == null) {
                    Log.w(TAG, "A2dpSinkProfile found new device: " + nextDevice);
                    device = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(A2dpSinkProfile.this, BluetoothProfile.STATE_CONNECTED);
                device.refresh();
            }
            mIsProfileReady=true;
        }

        @Override
        public void onServiceDisconnected(int arg0) {
            Log.d(TAG, "[onServiceDisconnected] A2dp Sink Service Disconnected");
            mIsProfileReady = false;
        }
        
    }
    
    @Override
    public boolean isConnectable() {
        return true;
    }

    @Override
    public boolean isAutoConnectable() {
        return false;
    }

    @Override
    public boolean connect(BluetoothDevice device) {
        if (mA2dpSink == null) {
            Log.e(TAG, "[connect] mA2dpSink is null");
            return false;
        }
        List<BluetoothDevice> sinks = getConnectedDevices();
        Log.d(TAG, "[connect] connected devices size : " + sinks.size());
        
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                mA2dpSink.disconnect(sink);
            }
        }
        return mA2dpSink.connect(device);
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        if (mA2dpSink == null) {
            Log.e(TAG, "[disconnect] mA2dpSink is null");
            return false;
        }
        return mA2dpSink.disconnect(device);
    }

    @Override
    public int getConnectionStatus(BluetoothDevice device) {
        if (mA2dpSink == null) {
            Log.e(TAG, "[getConnectionStatus] mA2dpSink is null");
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        return mA2dpSink.getConnectionState(device);
    }
    
    public List<BluetoothDevice> getConnectedDevices() {
        if (mA2dpSink == null) {
            Log.e(TAG, "[getConnectedDevices] mA2dpSink is null");
            return new ArrayList<BluetoothDevice>();
        }
        return mA2dpSink.getConnectedDevices();
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (mA2dpSink == null) {
            Log.e(TAG, "[getDevicesMatchingConnectionStates] mA2dpSink is null");
            return new ArrayList<BluetoothDevice>();
        }
        return mA2dpSink.getDevicesMatchingConnectionStates(states);
    }
    
    @Override
    public boolean isPreferred(BluetoothDevice device) {
        return getConnectionStatus(device) == BluetoothProfile.STATE_CONNECTED;
    }

    @Override
    public int getPreferred(BluetoothDevice device) {
        return -1;
    }

    @Override
    public void setPreferred(BluetoothDevice device, boolean preferred) {
        // ignore, the A2dpSink is not auto-connectable
    }

    @Override
    public boolean isProfileReady() {
        return mIsProfileReady;
    }

    @Override
    public int getOrdinal() {
        return ORDINAL;
    }

    @Override
    public int getNameResource(BluetoothDevice device) {
        return R.string.bluetooth_profile_a2dp_sink;
    }

    @Override
    public int getSummaryResourceForDevice(BluetoothDevice device) {
        int state = getConnectionStatus(device);
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return R.string.bluetooth_a2dp_sink_profile_summary_use_for;

            case BluetoothProfile.STATE_CONNECTED:
                return R.string.bluetooth_a2dp_sink_profile_summary_connected;

            default:
                return Utils.getConnectionStateSummary(state);
        }
    }

    public String toString() {
        return NAME;
    }
    
    @Override
    public int getDrawableResource(BluetoothClass btClass) {
        // TODO need planner to define a icon to identify the A2dpSink
        return R.drawable.ic_bt_headphones_a2dp;
    }
    
    protected void finalize() {
        Log.d(TAG, "finalize()");
        if (mA2dpSink != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.A2DP_SINK,
                        mA2dpSink);
                mA2dpSink = null;
            }catch (Throwable t) {
                Log.w(TAG, "Error cleaning up A2DP proxy", t);
            }
        }
    }
    
    public void connectConfirm(BluetoothDevice device, int acceptOrReject) {
        Log.d(TAG, "[connectConfirm] acceptOrReject : " + acceptOrReject);
//        if (mA2dpSink == null) {
//            Log.e(TAG, "[connectConfirm] mService is null");
//        } else {
//            mA2dpSink.connectConfirm(acceptOrReject);
//        }
        Intent intent = new Intent();
        intent.setAction("android.bluetooth.a2dp_sink.connect_confirm");
        intent.putExtra("confirmed", acceptOrReject);
        intent.putExtra("address", device.getAddress());
        mContext.sendBroadcast(intent);
    }

}
