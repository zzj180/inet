package com.mtk.bluetooth.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.mtk.bluetooth.service.CarBtService;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import com.mtk.bluetooth.MainActivity;

public class BootBroadcastReceiver extends BroadcastReceiver {

	public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
        Log.i("cxs","====bt===BootBroadcastReceiver=============");
		if (TextUtils.equals(action, ACTION_BOOT_COMPLETED)) {
			context.startService(new Intent(context, CarBtService.class));
		}else if(TextUtils.equals(action, BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)){
			 int new_state = intent.getIntExtra(
					BluetoothProfile.EXTRA_STATE, 
					BluetoothProfile.STATE_DISCONNECTED);          
                
                if(new_state == BluetoothProfile.STATE_CONNECTED){
                  	MainActivity.mConnectedDevice = (BluetoothDevice)intent.getParcelableExtra(
						BluetoothDevice.EXTRA_DEVICE);
                }
		}
	}
}
