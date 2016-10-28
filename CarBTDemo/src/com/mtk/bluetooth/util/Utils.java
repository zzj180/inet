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

package com.mtk.bluetooth.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.Toast;
import android.widget.PopupWindow;
import android.util.Log;
import android.bluetooth.BluetoothDevice;
import android.app.KeyguardManager;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import com.mtk.bluetooth.R;
import com.mtk.bluetooth.pbapclient.*;
import com.mtk.bluetooth.common.*;
import android.os.ParcelUuid;
import android.bluetooth.BluetoothUuid;
import android.content.DialogInterface;
import android.app.AlertDialog;

/**
 * Utils is a helper class that contains constants for various Android resource
 * IDs, debug logging flags, and static methods for creating dialogs.
 */
public class Utils {
    public static final boolean V = true; // verbose logging
    public static final boolean D = true; // regular logging

    private static final String KEY_ERROR = "errorMessage";
    private static final String TAG = "Bluetooth.Utils";
	static final int BD_ADDR_LEN = 6; // bytes
	static final int BD_UUID_LEN = 16; // bytes	
    static PopupWindow mwindow;
    private static AlertDialog dialog;

    private static final String KEY_INPUT_VIEW_X = "KEY_INPUT_VIEW_X";
    private static final String KEY_INPUT_VIEW_Y = "KEY_INPUT_VIEW_Y";
     private static final String SP_NAME = "float_window_settings";
     private static final String CHECK_ITEM = "check_item";

    private Utils() {
    }

	public static String getAddressStringFromByte(BluetoothDevice device) {
		byte[] address = getByteAddress(device);
		Log.d(TAG,"getAddressStringFromByte "+address);
        if (address == null || address.length != BD_ADDR_LEN) {
            return null;
        }

        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                address[5], address[4], address[3], address[2], address[1],
                address[0]);
    }

	public static byte[] getByteAddress(BluetoothDevice device) {
        return getBytesFromAddress(device.getAddress());
    }

    public static byte[] getBytesFromAddress(String address) {
		Log.d(TAG,"getBytesFromAddress ="+address);
        int i, j = 0;
        byte[] output = new byte[BD_ADDR_LEN];

        for (i = 0; i < address.length(); i++) {
            if (address.charAt(i) != ':') {
                output[j] = (byte) Integer.parseInt(address.substring(i, i + 2), BD_UUID_LEN);
                j++;
                i++;
            }
        }

        return output;
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
        case BluetoothProfile.STATE_CONNECTED:
            return R.string.bluetooth_connected;
        case BluetoothProfile.STATE_CONNECTING:
            return R.string.bluetooth_connecting;
        case BluetoothProfile.STATE_DISCONNECTED:
            return R.string.bluetooth_disconnected;
        case BluetoothProfile.STATE_DISCONNECTING:
            return R.string.bluetooth_disconnecting;
        default:
            return 0;
        }
    }

    public static void showPopupWindow(Context context,View parent,int screenW,int screenH){        
        if(mwindow == null){
            mwindow = new PopupWindow(LayoutInflater.from(context).inflate(R.layout.pro_popuwindow,null));
            mwindow.setOutsideTouchable(false);
            mwindow.setHeight(screenH);
            mwindow.setWidth(screenW);
            mwindow.showAtLocation(parent,Gravity.CENTER,0,0);
        }else{
            mwindow.showAtLocation(parent,Gravity.CENTER,0,0);
        }
    }
    
    public static void dismissPopupWindow(){
        Log.d(TAG,"dismissDialog");
        if(mwindow!=null && mwindow.isShowing()){
           try {
                         mwindow.dismiss();
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
        }

    }

    // Create (or recycle existing) and show disconnect dialog.
    static AlertDialog showDisconnectDialog(Context context, AlertDialog dialog,
            DialogInterface.OnClickListener disconnectListener, CharSequence title,
            CharSequence message) {
        if (dialog == null) {
            dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(android.R.string.ok, disconnectListener)
                    .setNegativeButton(android.R.string.cancel, null).create();
        } else {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            // use disconnectListener for the correct profile(s)
            CharSequence okText = context.getText(android.R.string.ok);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, okText, disconnectListener);
        }
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
        return dialog;
    }

    // TODO: wire this up to show connection errors...
    static void showConnectingError(Context context, String name) {
        // if (!mIsConnectingErrorPossible) {
        // return;
        // }
        // mIsConnectingErrorPossible = false;

        showError(context, name, R.string.bluetooth_connecting_error_message);
    }

    public static void showError(Context context, String name, int messageResId) {
        String message = context.getString(messageResId, name);
        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
        if (manager == null) {
            return;
        }
		/*
        // / M: Use DialogFragment instead of AlertDialog @{
        Log.d(TAG, "show ErrorDialogFragment, message is " + message);
        ErrorDialogFragment dialog = new ErrorDialogFragment();
        final Bundle args = new Bundle();
        args.putString(KEY_ERROR, message);
        dialog.setArguments(args);
        dialog.show(((Activity)context).getFragmentManager(), "Error");
        // / @}*/
		
//	Toast.makeText(context, message, Toast.LENGTH_SHORT).show(); //TBD
    }

    /**
     * Update the search Index for a specific class name and resources.
     */
    /*
     * public static void updateSearchIndex(Context context, String className,
     * String title, String screenTitle, int iconResId, boolean enabled) {
     * SearchIndexableRaw data = new SearchIndexableRaw(context); data.className
     * = className; data.title = title; data.screenTitle = screenTitle;
     * data.iconResId = iconResId; data.enabled = enabled;
     * 
     * //Index.getInstance(context).updateFromSearchIndexableData(data); }
     */
    public static class ErrorDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final String message = getArguments().getString(KEY_ERROR);

            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.bluetooth_error_title).setMessage(message)
                    .setPositiveButton(android.R.string.ok, null).show();
        }
    }

	public static void showShortToast(Context context,String content){
//		Toast.makeText(context,content,Toast.LENGTH_SHORT).show();
	}

	public static void showShortToast(Context context,int content){
//		Toast.makeText(context,content,Toast.LENGTH_SHORT).show();
	}


	public static void wakeUpAndUnlock(Context context){
        KeyguardManager km= (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);  
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock"); 
        
        kl.disableKeyguard();
        PowerManager pm=(PowerManager) context.getSystemService(Context.POWER_SERVICE); 
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK,"bright"); 
         
        wl.acquire();
        wl.release();
    } 

    public static void disconnectBT(Context context,CachedBluetoothDevice device){

        Log.d(TAG,"device =="+device);
        if(device == null){
            Log.d(TAG,"cacheDevice == Null");
            return;
        }
        device.disconnect();
    }

    public static void disconnectPbap(){
        BluetoothPbapClientManager mPbapManager = BluetoothPbapClientManager.getInstance();
        if(mPbapManager != null){
           Log.d(TAG,"disconnect pbap");
           mPbapManager.disconnectDevice();
        }

    }
    public static void connectPBAPClient(Context context,BluetoothDevice mConnectedDevice){
        if(mConnectedDevice == null){
            showShortToast(context,"connected device is null");
            return;
        }
        BluetoothPbapClientManager mPbapclientManager = 
             BluetoothPbapClientManager.getInstance();
        if(mPbapclientManager != null ){
           int state = mPbapclientManager.getConnectState();
           Log.d(TAG,"mPbap state ==" +state);
           if((state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED)||
               (state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTING||
               !mPbapclientManager.getDevice().getAddress().equals(mConnectedDevice.getAddress()))){
                 Log.d(TAG,"mConnectedDevice =="+mConnectedDevice);             
                 mPbapclientManager.initConnect(mConnectedDevice);
                 mPbapclientManager.connectDevice();
           }
          }else{
             Log.d(TAG,"mPbapClientManager == null");

          }

    }

     public static boolean isA2dpSinkSupport(LocalBluetoothAdapter adapter) {
        boolean isA2dpSinkSupport = false;
        ParcelUuid[] localUuids = adapter.getUuids();
        if (localUuids != null) {
            isA2dpSinkSupport = BluetoothUuid.isUuidPresent(localUuids, BluetoothUuid.AudioSink);
        }
        Log.d(TAG, "localUuids : " + localUuids + " isA2dpSinkSupport : " + isA2dpSinkSupport);
        return isA2dpSinkSupport;
    }



    public static void showPbapConnectDialog(final Context context,final BluetoothDevice device){
        if(dialog != null){
            dialog.dismiss();
        }
        dialog = new AlertDialog.Builder(context)
                    .setPositiveButton(R.string.auth_ok, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            connectPBAPClient(context,device);
                            if (dialog != null) {
                                dialog = null;
                            }
                        }

                    })
                    .setNegativeButton(R.string.auth_cancel, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            
                            if (dialog != null) {
                                dialog = null;
                            }
                        }

                    }).create();
        dialog.setTitle(R.string.pbap_connect_alert);
        dialog.setMessage(context.getResources().getString(R.string.pbap_connect_alert_message));
        dialog.show();
    }
    
    public static void setCheckedItems(Context context,boolean[] mCheckedItems) {
    	for(int i=0;i<mCheckedItems.length;i++){
            SharedPreferencesHelper.getInstance(context, CHECK_ITEM).saveBooleanValue(CHECK_ITEM+i, mCheckedItems[i]);
    	}
    }
    
    public static boolean[]  getCheckedItems(Context context) {
    	
    	boolean[] mCheckedItems ={false,false,false,false,false,false};    
    	for(int i=0;i<mCheckedItems.length;i++){
    		mCheckedItems[i]=SharedPreferencesHelper.getInstance(context, CHECK_ITEM).getBooleanValue(CHECK_ITEM+i, mCheckedItems[i]);
    	}
    	return mCheckedItems;
    }

    public static void setInputViewX(Context context, int x) {
        SharedPreferencesHelper.getInstance(context, SP_NAME).saveIntValue(KEY_INPUT_VIEW_X, x);
    }

    public static int getInputViewX(Context context, int defaultX) {
        SharedPreferencesHelper sph = SharedPreferencesHelper.getInstance(context, SP_NAME);
        return sph.getIntValue(KEY_INPUT_VIEW_X, defaultX);
    }

        public static void setInputViewY(Context context, int y) {
        SharedPreferencesHelper.getInstance(context, SP_NAME).saveIntValue(KEY_INPUT_VIEW_Y, y);
    }

    public static int getInputViewY(Context context, int defaultY) {
        SharedPreferencesHelper sph = SharedPreferencesHelper.getInstance(context, SP_NAME);
        return sph.getIntValue(KEY_INPUT_VIEW_Y, defaultY);
    }

}
