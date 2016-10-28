package com.mtk.bluetooth.common;

import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.mtk.bluetooth.PhoneCallActivity;
import com.mtk.bluetooth.DialFragment;
import com.mtk.bluetooth.MainActivity;

import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.telephony.TelephonyManager;

public class InComingCallRequest extends BroadcastReceiver {

    private static final String TAG = "InComingCallRequest";
    private static final String BACK_CAR = "back_car_state";  

    private LocalBluetoothProfileManager mManager;

    private void hanleReceivedIntent(final Context context, final String cllNumber,
        final int callFrom) {

        Runnable r = new Runnable() {
            @Override
            public void run() {
            
                Intent it = new Intent(context, PhoneCallActivity.class);
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                it.putExtra("callfrom", callFrom);
                it.putExtra("PhoneName", "");
		        it.putExtra("PhoneNumber", cllNumber);
		        it.putExtra(BluetoothDevice.EXTRA_DEVICE, MainActivity.mConnectedDevice);
                context.startActivity(it);
            }

        };

        new Thread(r).start();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive action : " + action);

            BluetoothHeadsetClientCall call = 
              (BluetoothHeadsetClientCall)intent.getParcelableExtra(BluetoothHeadsetClient.EXTRA_CALL);
    					  
    			Log.d(TAG, "ACTION_CALL_CHANGED BluetoothHeadsetClientCall state " + call.getState()
                         +"mLastCallState =="+ DialFragment.mLastCallState);
    			// incoming call
    			if(call.getState()== BluetoothHeadsetClientCall.CALL_STATE_INCOMING){
    				if (DialFragment.mLastCallState != BluetoothHeadsetClientCall.CALL_STATE_INCOMING ){
    					DialFragment.mLastCallState = BluetoothHeadsetClientCall.CALL_STATE_INCOMING;
    					 Log.d(TAG, "CALL_STATE_INCOMING invokePhoneCallActivity  " + call.getNumber());
    					 int parkingState = android.provider.Settings.System.getInt(
    							 context.getContentResolver(), BACK_CAR, 0);
    					 Log.d(TAG, "=======parkingState==========" + parkingState);
    					 
    					 if(parkingState != 1){
                             Intent it = new Intent("android.intent.action.BLUETOOTH_PHONE_STATE");               
                             it.putExtra("state", TelephonyManager.CALL_STATE_RINGING);
                             it.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER, call.getNumber());
                            // context.sendBroadcast(it);
                             hanleReceivedIntent(context, call.getNumber(),1); 
    					 }

    				}
    			}else if(call.getState()== BluetoothHeadsetClientCall.CALL_STATE_DIALING ||
            			call.getState()== BluetoothHeadsetClientCall.CALL_STATE_ALERTING ) {
            	    if(DialFragment.mLastCallState == BluetoothHeadsetClientCall.CALL_STATE_INCOMING){
                         return;
                    }
        		    if (DialFragment.mLastCallState != call.getState() ){
    					 DialFragment.mLastCallState = call.getState();
    					 Log.d(TAG, "CALL_STATE_DIALING CALL_STATE_ALERTING  " + call.getNumber());
    					 hanleReceivedIntent(context, call.getNumber(),0);
    			    }
                }else if(call.getState()== BluetoothHeadsetClientCall.CALL_STATE_ACTIVE) {
                    if (DialFragment.mLastCallState != call.getState() ){
    					DialFragment.mLastCallState = call.getState();
                         Log.d(TAG, "CALL_STATE_ACTIVE invokePhoneCallActivity  " + call.getNumber());
                         if(!TextUtils.isEmpty(call.getNumber())){
                            Intent it = new Intent("android.intent.action.BLUETOOTH_PHONE_STATE");               
                             it.putExtra("state", TelephonyManager.CALL_STATE_OFFHOOK);
                             context.sendBroadcast(it);
        					hanleReceivedIntent(context, call.getNumber(),2);
                        }
    			    }
                }
       
        
    }

}
