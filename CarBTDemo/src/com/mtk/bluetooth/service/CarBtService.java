package com.mtk.bluetooth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mtk.bluetooth.BtPairConnectActivity;
import com.mtk.bluetooth.R;
import com.mtk.bluetooth.common.BluetoothCallback;
import com.mtk.bluetooth.common.CachedBluetoothDevice;
import com.mtk.bluetooth.common.LocalBluetoothAdapter;
import com.mtk.bluetooth.common.LocalBluetoothManager;
import com.mtk.bluetooth.common.LocalBluetoothProfile;
import com.mtk.bluetooth.common.LocalBluetoothProfileManager;
import com.mtk.bluetooth.util.Utils;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import com.mtk.bluetooth.MainActivity;
import com.mtk.bluetooth.BtPairConnectActivity;

public class CarBtService extends Service implements BluetoothCallback {

	protected static final String TAG = "CarBtService";
	private LocalBluetoothAdapter mLocalAdapter;
	private LocalBluetoothManager mLocalManager;

	private List<CachedBluetoothDevice> mDeviceList;
	private int mProfileState = 0;
	private boolean mOpenBluetooth = false; //  

//	private btThread mBtThread = null;
	private CharSequence[] mConnectItems = {};

	private static final int AUTOCONNECT = 0;
	private static final int MSG_ACCON_MSG = 1;
	private static final int MSG_ACCOFF_MSG = 2;
	public static final String ACC_STATE = "acc_state";
	private static final long CONECT_DELAY = 10000;
	boolean state = true;
	

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case AUTOCONNECT:
				Log.i(TAG, "====CarBtService=====AUTOCONNECT==========="
						+ AUTOCONNECT);
				if(state){
					deviceConnect();
					mHandler.sendEmptyMessageDelayed(AUTOCONNECT,CONECT_DELAY);
				}else{
					if (mLocalAdapter != null)
					mLocalAdapter.setBluetoothEnabled(false);
				}
				break;
			case MSG_ACCON_MSG:
				Log.i(TAG, "====CarBtService=====MSG_ACCON_MSG===========");
				if (mLocalAdapter != null)
					mLocalAdapter.setBluetoothEnabled(true);
				mHandler.sendEmptyMessageDelayed(AUTOCONNECT,400);
				break;
			case MSG_ACCOFF_MSG:
			
				Log.i(TAG, "====CarBtService=====MSG_ACCOFF_MSG===========");
				mHandler.removeMessages(AUTOCONNECT);
			/*	if (mBtThread != null)
					mBtThread.interrupt();*/
				if (mLocalAdapter != null)
					mLocalAdapter.setBluetoothEnabled(false);				
				mHandler.removeMessages(AUTOCONNECT);
				break;
			default:
				break;
			}
		}
	};

/*	private void startAutoConnect() {
		// TODO Auto-generated method stub
		if(mBtThread!=null){
			mBtThread.interrupt();
		}
		mBtThread = new btThread();
		mBtThread.start();
	};*/

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(android.content.Context context, Intent intent) {
			String recievedAction = intent.getAction();
			Log.d(TAG, "onRecieve:action->" + recievedAction);
			if (recievedAction.equals(CachedBluetoothDevice.ACTION_PROFILE_STATE_CHANGED)
					|| recievedAction.equals(LocalBluetoothProfileManager.ACTION_PROFILE_UPDATE)) {

				saveCacheAddressFromDevList(mDeviceList);

				

			} else if (recievedAction.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				saveCacheAddress(device.getAddress());
				mHandler.removeMessages(AUTOCONNECT);
				mHandler.sendEmptyMessageDelayed(AUTOCONNECT, 2000);
			}
		}

	};

	@Override
	public void onCreate() {
	
		super.onCreate();
		Log.i(TAG, "=======CarBtService=========");
		mLocalManager = LocalBluetoothManager.getInstance(this);
		mLocalAdapter = mLocalManager.getBluetoothAdapter();
		mLocalManager.getEventManager().registerCallback(this);
		mDeviceList = (ArrayList) mLocalManager.getCachedDeviceManager()
				.getCachedDevicesCopy();
		checkBluetoothStatus();
		refreshDataList();

		mRegisterReceiver();
	/*	Intent t = new Intent(this, MainActivity.class);
            t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            t.putExtra("flag", 2);
            startActivity(t);*/
	}

	private Boolean checkDeviceConnect(List<CachedBluetoothDevice> mDeviceList) {
		
		Boolean deviceIsConnect = false;
		for (CachedBluetoothDevice mDevice : mDeviceList) {
			if ((mDevice.getConnectedState() != R.string.bt_status_paired)
					&& (mDevice.getConnectedState() != R.string.bt_status_unpair)) {
				saveCacheAddress(mDevice.getDevice().getAddress());
				deviceIsConnect = true;
			}
		}
		return deviceIsConnect;

	}

	private void mRegisterReceiver() {
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter
				.addAction(CachedBluetoothDevice.ACTION_PROFILE_STATE_CHANGED);
		intentFilter
				.addAction(LocalBluetoothProfileManager.ACTION_PROFILE_UPDATE);
		intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		registerReceiver(mReceiver, intentFilter);

		getContentResolver().registerContentObserver(
				android.provider.Settings.System.getUriFor(ACC_STATE), true,
				mAccStateObserver);
	}

	private AccStateObserver mAccStateObserver = new AccStateObserver();

	public class AccStateObserver extends ContentObserver {
		public AccStateObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			 state = android.provider.Settings.System.getInt(
					getContentResolver(), ACC_STATE, 0) == 1;
			handleAccState(state);
		}
	}

	private void handleAccState(boolean state) {
		
		mHandler.removeMessages(MSG_ACCON_MSG);
		mHandler.removeMessages(MSG_ACCOFF_MSG);
		if (state) {
			mHandler.sendEmptyMessageDelayed(MSG_ACCON_MSG, 1000);
		} else {
			mHandler.sendEmptyMessageDelayed(MSG_ACCOFF_MSG, 1000);
		}
	}

	private class btThread extends Thread {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Looper.prepare();
			while (!isInterrupted()) {
				try {
					if (mLocalManager == null) {
						mLocalManager = LocalBluetoothManager
								.getInstance(CarBtService.this);
					}

					if (!checkDeviceConnect(mDeviceList)) { // Éè±¸Ã»ÓÐÁ¬½Ó
						String devicename = getCacheAddress();
						if (!devicename.equals("")) {

							CachedBluetoothDevice mDevice = mLocalManager
									.getCachedDeviceManager().findDeviceByaddr(
											devicename);
							if(mDevice==null){
							   break;
							}
							int state = mDevice.getBondState();
							Log.d(TAG, "state ==" + state);
							if (state == BluetoothDevice.BOND_BONDED) {
								List<String> mProfileList = initBtSettings(
										mDevice, mProfileState, false);
								mConnectItems = (CharSequence[]) mProfileList
										.toArray(new CharSequence[mProfileList
												.size()]);
								boolean[] mCheckedItems=Utils.getCheckedItems(CarBtService.this);
								
								 for (int i = 0; i < mConnectItems.length; i++) {
										//save checkitem for autoconnect
									 Log.i("TAG","=======mCheckedItems[i]============"+mCheckedItems[i]);
									 Log.i("TAG","=======mConnectItems[i].toString()============"+mConnectItems[i].toString());
										if(mCheckedItems[i]){
		 									mDevice.connectProfileName(mConnectItems[i].toString());
										}else{
											mDevice.disconnectProfileName(mConnectItems[i].toString());
										}
								 }	

							} else {
								break;
							}
						} else {
							break;
						}
					} else {
						break;
					}
					Log.i(TAG, "=============sleep(10000)===========" + getId());
					sleep(10000);
				} catch (Exception e) {
					// TODO: handle exception
					Log.i(TAG, "=====Exception===========" + e);
					break;
				}
			}
			Looper.loop();
		}
	}

	private List<String> initBtSettings(
			CachedBluetoothDevice mCachedBluetoothDevice, int mProfileState,
			boolean firstTime) {
		int index = 0;
		List<String> items = new ArrayList<String>();
		Log.d(TAG, "mCachedBluetoothDevice =" + mCachedBluetoothDevice
				+ " ,mProfileState=" + mProfileState);
		for (LocalBluetoothProfile profile : mCachedBluetoothDevice
				.getConnectableProfiles()) {
			Log.d(TAG, "initBtSettings getConnectableProfiles =  "
					+ mCachedBluetoothDevice.getConnectableProfiles());
			items.add(getString(profile.getNameResource(mCachedBluetoothDevice
					.getDevice())));
			index++;
		}
		return items;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		refreshDataList();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}

	@Override
	public void onDestroy() {
		
		unregisterReceiver(mReceiver);
		getContentResolver().unregisterContentObserver(mAccStateObserver);
		super.onDestroy();
	}

	private void checkBluetoothStatus() {
		if (mLocalManager.getBluetoothAdapter().isEnabled()) {
			mOpenBluetooth = true;
			if (!checkDeviceConnect(mDeviceList)) { 
				mHandler.removeMessages(AUTOCONNECT);	
				mHandler.sendEmptyMessageDelayed(AUTOCONNECT,5000);
			}
		} else {
			mOpenBluetooth = false;
			mHandler.removeMessages(AUTOCONNECT);			
		}
	}

	@Override
	public void onBluetoothStateChanged(int bluetoothState) {
		
		Log.d(TAG, "onBluetoothStateChanged bluetoothState =" + bluetoothState);
		checkBluetoothStatus();
		
		refreshDataList();

	}

	@Override
	public void onScanningStateChanged(boolean started) {
		

	}

	@Override
	public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
		
		Log.d(TAG, "onDeviceAdded");
		if (!mDeviceList.contains(cachedDevice)) {
			mDeviceList.add(cachedDevice);
		} else {
			Log.d(TAG, "onDeviceAdded contains");

		}
	}

	@Override
	public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
		
		Log.d(TAG, "onDeviceAdded");
		for (int i = mDeviceList.size() - 1; i >= 0; i--) {
			if (cachedDevice.getDevice().getAddress()
					.equals(mDeviceList.get(i).getDevice().getAddress())) {
				mDeviceList.remove(i);
			}
		}
	}

	@Override
	public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice,
			int bondState) {
		

	}

	private void refreshDataList() {
		if (mDeviceList == null) {
			return;
		}
		if (!mOpenBluetooth) {
			mDeviceList.clear();
			return;
		}
		if (mDeviceList.isEmpty() || mDeviceList.size() <= 0) {
			Set<BluetoothDevice> pairedDevices = mLocalManager.getBluetoothAdapter().getBondedDevices();
			Log.d(TAG, "pairedDevices size = " + pairedDevices.size());

			if (!pairedDevices.isEmpty() && pairedDevices.size() > 0) {
				for (BluetoothDevice device : pairedDevices) {
					if (mLocalManager.getCachedDeviceManager().findDevice(device) == null) {
						mLocalManager.getCachedDeviceManager().addDevice(
								mLocalAdapter,mLocalManager.getProfileManager(), device);
					}

				}
			}
			mDeviceList = (ArrayList) mLocalManager.getCachedDeviceManager()
					.getCachedDevicesCopy();
		}
	}

	private void saveCacheAddressFromDevList(
			List<CachedBluetoothDevice> mDeviceList) {
		
		for (CachedBluetoothDevice mDevice : mDeviceList) {
			if ((mDevice.getConnectedState() != R.string.bt_status_paired)
					&& (mDevice.getConnectedState() != R.string.bt_status_unpair)) {
				saveCacheAddress(mDevice.getDevice().getAddress());
				
			}
		}
	}

	private void saveCacheAddress(String address) {
	
		SharedPreferences.Editor sharedata = getSharedPreferences(
				"save_address", Context.MODE_PRIVATE).edit();
		sharedata.putString("DEVICEADDRESS", address);
		sharedata.commit();
	};

	private String getCacheAddress() {
		
		SharedPreferences sharedata = getSharedPreferences("save_address",
				Context.MODE_PRIVATE);
		return sharedata.getString("DEVICEADDRESS", "");
	};

	private void deviceConnect(){
		if (mLocalManager == null) {
				mLocalManager = LocalBluetoothManager.getInstance(CarBtService.this);
		}

		if (!checkDeviceConnect(mDeviceList)) { 
			String devicename = getCacheAddress();
			if (!devicename.equals("")) {

				CachedBluetoothDevice mDevice = mLocalManager.getCachedDeviceManager().findDeviceByaddr(devicename);
				if(mDevice==null){
					return;
				}
				int state = mDevice.getBondState();
				Log.d(TAG, "state ==" + state);
				if (state == BluetoothDevice.BOND_BONDED) {
					List<String> mProfileList = initBtSettings(mDevice, mProfileState, false);
					mConnectItems = (CharSequence[]) mProfileList.toArray(new CharSequence[mProfileList.size()]);
					 for (int i = 0; i < mConnectItems.length; i++) {	
					 Log.d(TAG, mConnectItems[i].toString()+" ==" + BtPairConnectActivity.mCheckedItems[i]);		
						if(BtPairConnectActivity.mCheckedItems[i])
						mDevice.connectProfileName(mConnectItems[i].toString());
					}

				} else {
					return;
				}
			} else {
				return;
			}
		} else {
			return;
		}
	}

}
