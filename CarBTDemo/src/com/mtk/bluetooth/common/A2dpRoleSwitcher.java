package com.mtk.bluetooth.common;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.mtk.bluetooth.A2dpRoleChangeActivity;


public class A2dpRoleSwitcher {

    private static final String TAG = "A2dpRoleSwitcher";

    private static A2dpRoleSwitcher sInstance;

    private LocalBluetoothProfile mWaitToConnectProfile;

    private BluetoothDevice mWaitToConnectDevice;

    public static final int CONFIRM_CONNECT_ACCEPT = 1;
    public static final int CONFIRM_CONNECT_REJECT = 0;

    public static final int CONNECT_ACTION = 100;
    public static final int CONNECT_CONFIRM_ACTION = 101;

    private static final String REQUEST_ROLE = "request_role";
    private static final String DEVICE_ADDRESS = "device_address_to_connect";
    private static final String DEVICE_ADDRESS_DISCONNECT = "device_address_to_disconnect";

    private static final String CONNECT_OR_CONFIRM_ACTION = "action";

    private static final String REQUEST_ROLE_SOURCE = "source";
    private static final String REQUEST_ROLE_SINK = "sink";

    private int mConnectOrConfirm = -1;

    private LocalBluetoothProfileManager mProfileManager;
    private CachedBluetoothDeviceManager mDeviceManager;
    private A2dpProfile mA2dpSourceProfile;
    private A2dpSinkProfile mA2dpSinkProfile;
    private LocalBluetoothProfile mDisconnectProfile;

    private List<BluetoothDevice> mDisconnectDevices;
    private List<BluetoothDevice> mReturnDisconnectDevices;
    private Context mContext;

    private A2dpRoleSwitcher(Context context) {
        mContext = context;
        mProfileManager = LocalBluetoothManager.getInstance(
              mContext).getProfileManager();
        mDeviceManager = LocalBluetoothManager.getInstance(
              mContext).getCachedDeviceManager();
        mA2dpSourceProfile = mProfileManager.getA2dpProfile();
        mA2dpSinkProfile = mProfileManager.getA2dpSinkProfile();
    }

    public static A2dpRoleSwitcher getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is NULL");
        }
        if (sInstance == null) {
            sInstance = new A2dpRoleSwitcher(context);
        }
        return sInstance;
    }

    public void processA2dpConnect(LocalBluetoothProfile profile,
            BluetoothDevice device) {
        if (profile == null || device == null) {
            Log.d(TAG, "Invalid paramater, profile : " + profile + " device : " + device);
            return;
        }
        Log.d(TAG, "processA2dpConnect() profile : " + profile.toString()
                + ", device : " + device.getAddress());

        List<BluetoothDevice> devices = getConnectedDevices(profile);
        if (devices == null || devices.size() == 0) {
            Log.d(TAG, "profile : " + profile.toString()
                    + " connected device is 0 , connect directly");
            profile.connect(device);
            return;
        }
        Log.d(TAG, "profile : " + profile.toString()
                + " connected device size : " + devices.size());

        showA2dpRoleChangeConfirm(profile, getNeedToDisconnectDevices().get(0), device);
    }

    private void showA2dpRoleChangeConfirm(final LocalBluetoothProfile profile,
            final BluetoothDevice deviceToDisconnect,
            final BluetoothDevice deviceToConnect) {

        Runnable r = new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(mContext,
                        A2dpRoleChangeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                String str = profile instanceof A2dpProfile ? 
                        REQUEST_ROLE_SOURCE : REQUEST_ROLE_SINK;
                intent.putExtra(REQUEST_ROLE, str);
                intent.putExtra(DEVICE_ADDRESS_DISCONNECT,
                        deviceToDisconnect.getAddress());
                intent.putExtra(DEVICE_ADDRESS, deviceToConnect.getAddress());
                intent.putExtra(CONNECT_OR_CONFIRM_ACTION, CONNECT_ACTION);
                mContext.startActivity(intent);
            }

        };
        new Thread(r).start();
    }

    public void confirmConnect(int acceptOrReject, LocalBluetoothProfile profileToConnect,
            BluetoothDevice deviceToConnect, int connectOrConfirm) {

        Log.d(TAG, "acceptOrReject : " + acceptOrReject + ", connectOrConfirm : " + connectOrConfirm);
        Log.d(TAG, "profileToConnect : " + profileToConnect.toString() + ", deviceToConnect : "
                        + deviceToConnect.getAddress());

        if (acceptOrReject == CONFIRM_CONNECT_ACCEPT) {
            Log.d(TAG, "Connect accept, disconnect the connected device firstly");
            mConnectOrConfirm = connectOrConfirm;

            List<BluetoothDevice> devices = getConnectedDevices(profileToConnect);
            if (devices == null || devices.size() == 0) {
                Log.d(TAG, "[confirmConnect] devices is null or empty");
                profileToConnect.connect(deviceToConnect);
                return;
            }

            mWaitToConnectDevice = deviceToConnect;
            mWaitToConnectProfile = profileToConnect;

            CachedBluetoothDevice cacheDevice =
                    mDeviceManager.findDevice(mWaitToConnectDevice);
            if (cacheDevice == null) {
                cacheDevice = mDeviceManager.addDevice(
                                LocalBluetoothAdapter.getInstance(),
                                mProfileManager,
                                mWaitToConnectDevice);
            }
            cacheDevice.onProfileStateChanged(mWaitToConnectProfile,
                    BluetoothProfile.STATE_CONNECTING);

            mDisconnectDevices = new ArrayList<BluetoothDevice>(devices);
            for (BluetoothDevice device : mDisconnectDevices) {
                Log.d(TAG, "[Disconnect] profile : " + mDisconnectProfile.toString() + " device : " + device);
                mDisconnectProfile.disconnect(device);
            }

        } else if (acceptOrReject == CONFIRM_CONNECT_REJECT) {
            if (connectOrConfirm == CONNECT_ACTION) {
                Log.d(TAG, "[confirmConnect] connect reject");
            } else if (connectOrConfirm == CONNECT_CONFIRM_ACTION) {
                Log.d(TAG, "[confirmConnect] connect reject, send the confirm reject");
                if (profileToConnect instanceof A2dpProfile) {
                    ((A2dpProfile) profileToConnect).connectConfirm(deviceToConnect, 0);
                } else if (profileToConnect instanceof A2dpSinkProfile) {
                    ((A2dpSinkProfile) profileToConnect).connectConfirm(deviceToConnect, 0);
                }
            }
        }
    }

    public List<BluetoothDevice> getConnectedDevices(LocalBluetoothProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile is null");
        }
        Log.d(TAG, "[getConnectedDevices] profile : " + profile.toString());
        List<BluetoothDevice> devices;
        if (profile instanceof A2dpSinkProfile) {
            devices = mA2dpSourceProfile.getConnectedDevices();
            mDisconnectProfile = mA2dpSourceProfile;
            mReturnDisconnectDevices = new ArrayList<BluetoothDevice>(devices);
            Log.d(TAG, "source connected devices size : "+ devices.size());
            return devices;
        } else if (profile instanceof A2dpProfile) {
            devices = mA2dpSinkProfile.getConnectedDevices();
            mDisconnectProfile = mA2dpSinkProfile;
            mReturnDisconnectDevices = new ArrayList<BluetoothDevice>(devices);
            Log.d(TAG, "sink connected devices size : " + devices.size());
            return devices;
        } else {
            Log.e(TAG, "[getConnectedDevices] not A2dp Sink & Source Profile");
            return null;
        }
    }

    List<BluetoothDevice> getNeedToDisconnectDevices() {
        return new ArrayList<BluetoothDevice>(mReturnDisconnectDevices);
    }

    public void changeProfileState(LocalBluetoothProfile profile,
            BluetoothDevice device, int state) {
        Log.d(TAG, "[changeProfileState] profile : " + profile.toString()
                + ", device : " + device.getAddress() + ", state : " + state);

        if (state == BluetoothProfile.STATE_DISCONNECTED) {
            if (mDisconnectDevices != null) {
                if (mDisconnectDevices.contains(device)) {
                    mDisconnectDevices.remove(device);
                }
                if (mDisconnectDevices.size() == 0) {
                    if (mConnectOrConfirm == CONNECT_ACTION) {
                        mWaitToConnectProfile.connect(mWaitToConnectDevice);
                    } else if (mConnectOrConfirm == CONNECT_CONFIRM_ACTION) {
                        if (mWaitToConnectProfile instanceof A2dpProfile) {
                            ((A2dpProfile) mWaitToConnectProfile).connectConfirm(mWaitToConnectDevice, 1);
                        } else if (mWaitToConnectProfile instanceof A2dpSinkProfile) {
                            ((A2dpSinkProfile) mWaitToConnectProfile).connectConfirm(mWaitToConnectDevice, 1);
                        }
                    }
                    mDisconnectDevices.clear();
                    mDisconnectDevices = null;
                    mWaitToConnectDevice = null;
                    mWaitToConnectProfile = null;
                }
            }
            return;
        }
    }
}
