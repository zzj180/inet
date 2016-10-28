package com.mtk.bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mtk.bluetooth.R;
import com.mtk.bluetooth.common.*;

public class A2dpRoleChangeActivity extends Activity {

    private static final String TAG = "A2dpRoleChangeActivity";

    private static final String REQUEST_ROLE = "request_role";
    private static final String DEVICE_ADDRESS_CONNECT = "device_address_to_connect";
    private static final String DEVICE_ADDRESS_DISCONNECT = "device_address_to_disconnect";
    private static final String CONNECT_OR_CONFIRM_ACTION = "action";

    private static final String REQUEST_ROLE_SOURCE = "source";
    private static final String REQUEST_ROLE_SINK = "sink";
    private static final int ROLE_SOURCE = 1;
    private static final int ROLE_SINK = 2;

    private Resources mRes;
    private TextView mTextView;
    private Button mOkButton;
    private Button mCancelButton;

    private LocalBluetoothProfile mProfileToConnect = null;
    private BluetoothDevice mDeviceToConnect;
    private Context mContext;

    private int mConnectOrConfirmAction;

    private static final String ROLE_CHANGE_CANCEL_ACTION =
                     "android.bluetooth.a2dp.role_change_canceled";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ROLE_CHANGE_CANCEL_ACTION.equals(action)) {
                String address = intent.getStringExtra(DEVICE_ADDRESS_CONNECT);
                String requestRole = intent.getStringExtra(REQUEST_ROLE);
                Log.d(TAG, "[Role_change_cancle] address : " + address + " requestRole : " + requestRole);
      //          Toast.makeText(mContext, mContext.getString(R.string.role_change_cancel),
      //                   Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        String connectAddress = intent.getStringExtra(DEVICE_ADDRESS_CONNECT);
        String requestRole = intent.getStringExtra(REQUEST_ROLE);
        String disconnectAddress = intent.getStringExtra(DEVICE_ADDRESS_DISCONNECT);
        mConnectOrConfirmAction = intent.getIntExtra(CONNECT_OR_CONFIRM_ACTION, 1);

        Log.d(TAG, "connectAddress : " + connectAddress + " disconnectAddress : " + disconnectAddress);

        if (connectAddress == null) {
            this.finish();
            return;
        }

        int request;
        if (requestRole.equals(REQUEST_ROLE_SOURCE)) {
            mProfileToConnect = LocalBluetoothManager.getInstance(this)
                    .getProfileManager().getA2dpProfile();
            request = ROLE_SOURCE;
        } else if (requestRole.equals(REQUEST_ROLE_SINK)) {
            mProfileToConnect = LocalBluetoothManager.getInstance(this)
                    .getProfileManager().getA2dpSinkProfile();
            request = ROLE_SINK;
        } else {
            this.finish();
            return;
        }

        setContentView(R.layout.a2dp_role_change_activity_layout);

        mRes = getResources();
        mTextView = (TextView) findViewById(R.id.role_change_information);
        mOkButton = (Button) findViewById(R.id.role_change_accept_button);
        mCancelButton = (Button) findViewById(R.id.role_change_reject_button);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mDeviceToConnect = adapter.getRemoteDevice(connectAddress);
        mContext = this;
        Log.d(TAG,"mConnectOrConfirmAction = "+mConnectOrConfirmAction);

        if (mConnectOrConfirmAction == A2dpRoleSwitcher.CONNECT_ACTION ||
            mConnectOrConfirmAction == A2dpRoleSwitcher.CONNECT_CONFIRM_ACTION) {
            Log.d(TAG,"disconnectAddress ="+disconnectAddress);
            if (disconnectAddress != null ) {
                BluetoothDevice deviceToDisconnect = adapter.getRemoteDevice(disconnectAddress);
                initView(deviceToDisconnect, request);
            } else {
                initView(request);
            }
        }

        mOkButton.setOnClickListener(mButtonClickListener);
        mCancelButton.setOnClickListener(mButtonClickListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ROLE_CHANGE_CANCEL_ACTION);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        mDeviceToConnect = null;
        mProfileToConnect = null;
    }

    private void initView(BluetoothDevice device, int requestRole) {
       String result = mRes.getString(R.string.bluetooth_disconnect_prompt);

       String deviceName = TextUtils.isEmpty(device.getName()) ? 
                  device.getAddress() : device.getName();
       Log.d(TAG,"deviceName = "+deviceName);
       String roleName = (requestRole == ROLE_SOURCE) ?
                      mRes.getString(R.string.bluetooth_profile_a2dp_sink)
                        :  mRes.getString(R.string.bluetooth_profile_a2dp);
       Log.d(TAG,"roleName = "+roleName);
       result = String.format(result, roleName, deviceName);
       mTextView.setText(result);
    }

    private void initView(int requestRole) {
       String result = mRes.getString(R.string.bluetooth_disconnect_confirm_prompt);
       String roleName = (requestRole == ROLE_SOURCE) ?
                   mRes.getString(R.string.bluetooth_profile_a2dp_sink)
                        :  mRes.getString(R.string.bluetooth_profile_a2dp);
       Log.d(TAG,"initView() roleName ="+roleName+" result ="+result);
       result = String.format(result, roleName);
       Log.d(TAG,"result ="+result);
       mTextView.setText(result);
    }

    private View.OnClickListener mButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == mOkButton.getId()) {
                Log.d(TAG, "[onClick] BUTTON_POSITIVE clicked");
                Log.d(TAG,"mConnectOrConfirmAction ="+mConnectOrConfirmAction);
                if (mConnectOrConfirmAction == A2dpRoleSwitcher.CONNECT_ACTION) {
                    A2dpRoleSwitcher.getInstance(mContext).confirmConnect(
                            A2dpRoleSwitcher.CONFIRM_CONNECT_ACCEPT,
                            mProfileToConnect, mDeviceToConnect,
                            A2dpRoleSwitcher.CONNECT_ACTION);
                } else if (mConnectOrConfirmAction == A2dpRoleSwitcher.CONNECT_CONFIRM_ACTION) {
                    A2dpRoleSwitcher.getInstance(mContext).confirmConnect(
                            A2dpRoleSwitcher.CONFIRM_CONNECT_ACCEPT,
                            mProfileToConnect, mDeviceToConnect,
                            A2dpRoleSwitcher.CONNECT_CONFIRM_ACTION);
                }
                A2dpRoleChangeActivity.this.finish();
            } else if (v.getId() == mCancelButton.getId()) {
                Log.d(TAG, "[onClick] BUTTON_NEGATIVE clicked");
                if (mConnectOrConfirmAction == A2dpRoleSwitcher.CONNECT_ACTION) {
                    A2dpRoleSwitcher.getInstance(mContext).confirmConnect(
                            A2dpRoleSwitcher.CONFIRM_CONNECT_REJECT,
                            mProfileToConnect, mDeviceToConnect,
                            A2dpRoleSwitcher.CONNECT_ACTION);
                } else if (mConnectOrConfirmAction == A2dpRoleSwitcher.CONNECT_CONFIRM_ACTION) {
                    A2dpRoleSwitcher.getInstance(mContext).confirmConnect(
                            A2dpRoleSwitcher.CONFIRM_CONNECT_REJECT,
                            mProfileToConnect, mDeviceToConnect,
                            A2dpRoleSwitcher.CONNECT_CONFIRM_ACTION);
                }
                A2dpRoleChangeActivity.this.finish();
            }
        }
    };

}
