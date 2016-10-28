package com.mtk.bluetooth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.Menu;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.mtk.bluetooth.R;
import com.mtk.bluetooth.util.ActionItem;
import com.mtk.bluetooth.util.QuickActions;
import com.mtk.bluetooth.util.Utils;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.mediatek.bluetooth.BluetoothProfileManager;
import android.bluetooth.client.pbap.BluetoothPbapClient.ConnectionState;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntry.PhoneData;
import android.bluetooth.client.pbap.BluetoothPbapCard;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientManager;
import com.mtk.bluetooth.pbapclient.BluetoothPabapClientCallback;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientConstants;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.view.Window;
import android.view.WindowManager;

import android.util.Log;

public class CallHistoryFragment extends Fragment {

	protected static final String TAG = "CallHistoryFragment";
	Context mContext;
    private static final boolean DEBUG = true;

    private final static int MSG_HANDLE_ONESTEP_UPDATE = 1;
    private final static int MSG_HANDLE_ALL_UPDATE = 2;
    private final static int MSG_HANDLER_ADDCALL = 3;
    private final static int MSG_HANDLE_CLEAR = 4;

    private static final int ITEM_COUNT_PER_PAGE = 20;
    private static final int MESSAGE_RECHECK_PATH = 0;
    private static final int MESSAGE_PULL_PHONEBOOK_SIZE = 1;
    private static final int MESSAGE_TOAST_PULL_PHONEBOOK_SIZE_FAIL = 2;
    private static final int MESSAGE_CLEAR_DATA = 3;

    private static final int RECHECK_PATH_DELAY = 200;
    private static final int MAX_SET_PATH_TIMES = 10;
    public static final int ACTIVITY_RESULT_CODE = 100;

    public static final String EXTRA_BT_ADDRESS = "address";

    private final static int MSG_SHOW_RECORDNUM = 10;
    private final static int MSG_NOTIFY_DATACHANGED = 11;

    private ListView mHistoryListView;

   // private SimpleAdapter mAdapter;
    private int mDownloadPath;
    private int mShowedRecordNum;
	private Button received_btn;
	private Button dialed_btn;
	private Button missed_btn;
	private Button history_all_btn;
	private Button callhistory_syn_btn;	
    private Button callClear;

    private long mStartTime;
    private long mEndTime;

    /*
     * for change tab
     */
    private boolean mIsSyncing = false;

    /*
     * Sync State, for recieve call intent
     */
    private boolean mSyncState = false;

    /*
     * Stop Sync Active or Passive
     */
    private boolean mStopSyncActive = false;

    /*
     * last time download bt addr
     */
    private String mLastDwnldAddr = "";

    /*
     * is Sync finish
     */
    private boolean mIsSyncFinish = false;

    /*
     * is loading data
     */
    private boolean mIsLoadingData = false;

    private boolean mIsHfConnected = false;
    public  BluetoothDevice mConnectedDevice ;
	

    private ActionItem action_call = new ActionItem();

    private BluetoothPbapClientManager mManager = BluetoothPbapClientManager.getInstance();
    private ArrayList<VCardEntry> mCallHistoryVCardsList = new ArrayList<VCardEntry>();
    private VcardAdapter mAdapter = new VcardAdapter();
    private String mTargetFolder = BluetoothPbapClientConstants.CCH_PATH;
    private int mSetPathFailedTimes = 0;
    private int mGetPhonebookSizeTimes = 0;

	
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
        Log.d(TAG, "handleMessage msg.what->" + msg.what);
            switch (msg.what) {
                case MESSAGE_RECHECK_PATH:
                    checkFolderPath();
                    break;
                case MESSAGE_PULL_PHONEBOOK_SIZE:					
                    if(!pullPhoneBookSize()){
                        Utils.dismissPopupWindow();
                        Utils.showShortToast(getActivity(),"pullphonebooksize failed");
                    }
                    break;
               case MESSAGE_CLEAR_DATA:
                    Log.d(TAG,"start clear");
                    mCallHistoryVCardsList.clear();
                    mAdapter.notifyDataSetChanged();
                    break;
            }

        };
    };
	

    public CallHistoryFragment() {}
	
    public CallHistoryFragment(Context context) {
        mContext = context;
    }
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bt_callhistory, container, false);
        Log.d(TAG, "onCreateView start" );
    	mHistoryListView = (ListView) view.findViewById(R.id.history_listview);	
		received_btn = (Button)view.findViewById(R.id.btn_received);
		dialed_btn = (Button)view.findViewById(R.id.btn_dialed);
		missed_btn = (Button)view.findViewById(R.id.btn_missed);
		history_all_btn = (Button)view.findViewById(R.id.btn_history_all);
		callhistory_syn_btn = (Button)view.findViewById(R.id.bt_callhistory_syn);
        callClear = (Button)view.findViewById(R.id.btn_history_clean);

		received_btn.setOnClickListener(CallHistoryButtonOnclick);
		dialed_btn.setOnClickListener(CallHistoryButtonOnclick);
		missed_btn.setOnClickListener(CallHistoryButtonOnclick);
		history_all_btn.setOnClickListener(CallHistoryButtonOnclick);
		callhistory_syn_btn.setOnClickListener(CallHistoryButtonOnclick);
        callClear.setOnClickListener(CallHistoryButtonOnclick);

        mHistoryListView.setAdapter(mAdapter);
        mHistoryListView.setOnItemClickListener(mHistoryListClickListener);
        Log.d(TAG,"onCreateView end");        
        return view;
	}



	@Override
	public void onResume() {
    	Log.d(TAG,"onResume");
		if(this.getUserVisibleHint()){
			registerSyncCallBack();
		}
        if(mManager != null){
            int state = mManager.getConnectState();
            if(state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED) {
                Log.d(TAG,"BluetoothPbapClient not connected");
                cleanData();
            }

        }
        Utils.dismissPopupWindow();
        registerReceiver();
		super.onResume();
	}
	
	
	@Override
	public void onStop() {
		Log.d(TAG, "onStop unRegisterSyncCallBack");
        Utils.dismissPopupWindow();
		super.onStop();
	}

	@Override
	public void onDestroy() {
    	Log.d(TAG,"onDestroy");
		unRegisterSyncCallBack();
        Utils.dismissPopupWindow();
        try{
           if(mReceiver != null){
             this.getActivity().unregisterReceiver(mReceiver);
           }
        }catch(IllegalArgumentException e){
           Log.e("TAG","IllegalArgumentException");
        }             
		super.onDestroy();
	}
	public void registerSyncCallBack(){
		Log.v(TAG, "registerSyncCallBack ");
		mManager.registerCallback(mCallback);
	}

	public void unRegisterSyncCallBack(){
		Log.v(TAG, "unRegisterSyncCallBack ");
		mManager.unregisterCallback(mCallback);
	}


	OnClickListener CallHistoryButtonOnclick = new OnClickListener(){

        @Override
        public void onClick(View v) {                
            int state = mManager.getConnectState();          
			if(state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED ||
                state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTING){ 
                if(MainActivity.mConnectedDevice != null){
                   Utils.showPbapConnectDialog(getActivity(),MainActivity.mConnectedDevice);
                }else{
                   Log.d(TAG,"CallHistoryButtonOnclick device is null");
                   Utils.showShortToast(getActivity(),R.string.pbap_no_device_to_connect);
                }
                cleanData();
                return;
			}else if(state == BluetoothPbapClientConstants.CONNECTION_STATE_CONNECTING){
    			Utils.showShortToast(getActivity(),R.string.pbap_is_connecting);
                return;
            }
				
				switch (v.getId()) {
						
				case R.id.btn_received:
					mCallHistoryVCardsList.clear();
					Log.d(TAG, "Received");
					mAdapter.notifyDataSetChanged();
					Utils.showPopupWindow(getActivity(),CallHistoryFragment.this.getView(),
                    MainActivity.screenW,MainActivity.screenH);	
					mTargetFolder = BluetoothPbapClientConstants.ICH_PATH;
					received_btn.setBackgroundResource(R.drawable.bt_180_50_btn_disable_focused);
					dialed_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					missed_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					history_all_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
			
					Message msg_received = mHandler.obtainMessage(MESSAGE_RECHECK_PATH,
							RECHECK_PATH_DELAY);
					mHandler.sendMessage(msg_received);
                    
					break;
				case R.id.btn_dialed:						
					mCallHistoryVCardsList.clear();
					Log.d(TAG, "Dial ");
					mAdapter.notifyDataSetChanged();
                    Utils.showPopupWindow(getActivity(),CallHistoryFragment.this.getView(),
                    MainActivity.screenW,MainActivity.screenH);	
					mTargetFolder = BluetoothPbapClientConstants.OCH_PATH;
					dialed_btn.setBackgroundResource(R.drawable.bt_180_50_btn_disable_focused);
					received_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					missed_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					history_all_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					Message msg_dialed = mHandler.obtainMessage(MESSAGE_RECHECK_PATH,
							RECHECK_PATH_DELAY);
					mHandler.sendMessage(msg_dialed);
                    
					break;
				case R.id.btn_missed:						
						
					mCallHistoryVCardsList.clear();
					Log.d(TAG, "Missed");
					mAdapter.notifyDataSetChanged();
                    Utils.showPopupWindow(getActivity(),CallHistoryFragment.this.getView(),
                    MainActivity.screenW,MainActivity.screenH);	
			
					mTargetFolder = BluetoothPbapClientConstants.MCH_PATH;
					missed_btn.setBackgroundResource(R.drawable.bt_180_50_btn_disable_focused);
					received_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					dialed_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					history_all_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					Message msg_missed = mHandler.obtainMessage(MESSAGE_RECHECK_PATH,
							RECHECK_PATH_DELAY);                       
					mHandler.sendMessage(msg_missed);
                    
					break;
				case R.id.btn_history_all:
					mCallHistoryVCardsList.clear();
					Log.d(TAG, "All history");
					mAdapter.notifyDataSetChanged();
                    Utils.showPopupWindow(getActivity(),CallHistoryFragment.this.getView(),
                    MainActivity.screenW,MainActivity.screenH);				
					mTargetFolder = BluetoothPbapClientConstants.CCH_PATH;
						
					history_all_btn.setBackgroundResource(R.drawable.bt_180_50_btn_disable_focused);
					received_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					dialed_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
					missed_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
						
					Message msg_all = mHandler.obtainMessage(MESSAGE_RECHECK_PATH,
							RECHECK_PATH_DELAY);
					mHandler.sendMessage(msg_all);
					
					break;
				case R.id.bt_callhistory_syn:
                    Utils.showPopupWindow(getActivity(),CallHistoryFragment.this.getView(),
                    MainActivity.screenW,MainActivity.screenH);	
					Message msg_syn = mHandler.obtainMessage(MESSAGE_RECHECK_PATH,
							RECHECK_PATH_DELAY);
					mHandler.sendMessage(msg_syn);
						
					break;
                case R.id.btn_history_clean:
                    mCallHistoryVCardsList.clear();
                    mAdapter.notifyDataSetChanged();
                    Utils.dismissPopupWindow();
                    break;
				default:
					break;
				}
			
			}


    };
	
   
	private class VcardAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mCallHistoryVCardsList != null) {
                return mCallHistoryVCardsList.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            if (mCallHistoryVCardsList != null) {
                return mCallHistoryVCardsList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            if (mCallHistoryVCardsList != null) {
                return position ;
            } else {
                return 0;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            synchronized (mCallHistoryVCardsList) {
                if (mCallHistoryVCardsList != null && position < mCallHistoryVCardsList.size()) {
                    ItemView view = null;
                    if (convertView != null && convertView instanceof ItemView) {
                        view = (ItemView) convertView;
                    } else {
                        view = new ItemView(getActivity());
                    }
                    String displayName = mCallHistoryVCardsList.get(position).getDisplayName();
                    List<PhoneData> numberList = mCallHistoryVCardsList.get(position).getPhoneList();
                    view.getNameView().setText(displayName);
                    if (numberList != null && numberList.size() > 0) {
                        view.getNumberView().setText(numberList.get(0).getNumber());
                    }
                    return view;
                } else {
                    return null;
                }
            }
        }

    }


	   private BluetoothPabapClientCallback mCallback = new BluetoothPabapClientCallback() {

        @Override
        public void onSetPathDone(boolean success) {
            Log.d(TAG, "onSetPathDone = " + success);
            if (success) {
                mSetPathFailedTimes = 0;
                checkFolderPath();
            } else {
                mSetPathFailedTimes++;
                if (mSetPathFailedTimes < MAX_SET_PATH_TIMES) {
                    checkFolderPath();
                } else {
                    Utils.dismissPopupWindow();
                  /*  Toast.makeText(getActivity(),
                            getActivity().getResources().getString(R.string.not_support_set_path_device),
                            Toast.LENGTH_SHORT).show();*/
                }
            }
        }

        @Override
        public void onPullBookDone(boolean success, int newMissedCalls, ArrayList<VCardEntry> list) {
            Log.d(TAG, "onPullBookDone success = " + success + ", listsize = "
                    + ((list == null) ? 0 : list.size()));
            Utils.dismissPopupWindow();
            if (success) {
                if(newMissedCalls != -1) {
                    String text = getActivity().getResources().
                        getString(R.string.new_missed_call_toast,newMissedCalls);
                    Utils.showShortToast(getActivity(),text);
                }                
                mCallHistoryVCardsList = list;
                
                ((Activity)mContext).runOnUiThread(new Runnable(){  
                	  
                    @Override  
                    public void run() {  
                     
                    	mAdapter.notifyDataSetChanged();  
                    }  
                      
                });
                
            } else {
            //    Toast.makeText(getActivity(), R.string.pull_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPullVcardListingDone(boolean success, int newMissedCalls,
                ArrayList<BluetoothPbapCard> list) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPullVcardEntryDone(boolean success, VCardEntry entry) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPullPhonebookSizeDone(boolean success, int size) {
            Log.d(TAG, "onPullPhonebookSizeDone, success = " + success + "size = " + size);
            if (success) {
                if(getActivity()!= null){
                    String text = getString(R.string.total_pb_size,
                    size);
                    Utils.showShortToast(getActivity(),text);
                }else{
                    Log.d(TAG,"getActivity() == null");

                }
                
				if(size >0){
					pullPhoneBook(null,size);
				}else{
    				Utils.dismissPopupWindow();
                }
				
            }else{
                Utils.dismissPopupWindow();
				Utils.showShortToast(getActivity(),"Get PhoneBookSize Failed");
			}
        }

        @Override
        public void onPullVcardListingSizeDone(boolean success, int size) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onAuthenticationRequest() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onAuthenticationTimeout() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onConnectStatusChange(boolean connected) {
            Log.d(TAG, "onConnectStatusChange");
            int state = BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED;
            if (mManager != null) {
                state = mManager.getConnectState();
            }
            if (state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED) {
                cleanData();
                Utils.dismissPopupWindow();
            }
        }

    };

	 private synchronized void checkFolderPath() {
        String currentFolder = mManager.getCurrentPath();
        Log.d(TAG, "checkFolderPath() started, mTargetFolder = " + mTargetFolder
                + ", mCurrentFolder = " + currentFolder);
        if (mTargetFolder == null) {
            Log.e(TAG, "[Error] mTargetFolder == null");
        } else if (mTargetFolder.startsWith(currentFolder) || currentFolder.isEmpty()) {
            Log.d(TAG, "[OK] mTargetFolder matched");
            if (!pullPhoneBookSize()) {
				Message message1 = mHandler.obtainMessage(MESSAGE_PULL_PHONEBOOK_SIZE,
							RECHECK_PATH_DELAY);
				mHandler.sendMessage(message1);
			}
        } else {
            Log.d(TAG, "mTargetFolder = " + mTargetFolder + ", mCurrentFolder = " + currentFolder);
            if (!mManager.setPhoneBookFolderRoot()) {
				if(this.getUserVisibleHint()){
					Log.d(TAG, "setPhoneBookFolderRoot fail, reset it");
	                Message message = mHandler.obtainMessage(MESSAGE_RECHECK_PATH);
	                mHandler.sendMessageDelayed(message, RECHECK_PATH_DELAY);
				}
                

            }
        }
    }

	
    private void pullPhoneBook(String pbName,int size) {
        pbName = mTargetFolder + ".vcf";
		Log.d(TAG, "pullPhoneBook pbName : " + pbName);
        if (mManager.pullPhoneBook(pbName, size, 0)) {
            Log.d(TAG, "pullPhoneBook start");
        } else {
            Utils.dismissPopupWindow();
            Utils.showShortToast(this.getActivity(),"pull phonebook error");
            Log.d(TAG, "pullPhoneBook error");            
        }
    }

    private boolean pullPhoneBookSize() {
        String pbName = null;
        pbName = mTargetFolder + ".vcf";
        return mManager.pullPhoneBookSize(pbName);
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mHistoryListClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            final String pbname;
            final String pbnumber;
			
            //HashMap<String, String> map = new HashMap<String, String>();
            pbname = mCallHistoryVCardsList.get(arg2).getDisplayName();
            List<PhoneData> numberList = mCallHistoryVCardsList.get(arg2).getPhoneList();
            if(numberList ==null||numberList.isEmpty())
               return ;
			
            pbnumber = numberList.get(0).getNumber();
            if(pbnumber.isEmpty())
                return ;

            Intent intent = new Intent();
			intent.setClass(getActivity(), VcardEntryActivity.class);
		    intent.putExtra(VcardEntryActivity.DISPLAY_NAME, pbname);
			intent.putExtra(VcardEntryActivity.PHONE_NUMBER, pbnumber);
			intent.putExtra(VcardEntryActivity.EXTRA_TARGET_FOLDER, mTargetFolder);
			Log.d(TAG,"currentfolderPath="+mManager.getCurrentPath());
			mManager.unregisterCallback(mCallback);
			startActivity(intent);

            return;

        }
    };

  
    private class ItemView extends LinearLayout {

        private TextView mNameView = null;
        private TextView mNumberView = null;
        public ItemView(Context context) {
            this(context, null);
        }

        public ItemView(Context context, AttributeSet attrs) {
            super(context, attrs);
            LayoutInflater inflator = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflator.inflate(R.layout.phonebook_listitem, null);
            addView(view);
            initView(view);
        }

        private void initView(View view) {
            mNameView = (TextView) findViewById(R.id.item_phonebook_name);
            mNumberView = (TextView) findViewById(R.id.item_phonebook_number);
        }

        public TextView getNameView() {
            return mNameView;
        }

        public TextView getNumberView() {
            return mNumberView;
        }
    }


    private void registerReceiver(){
		IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		getActivity().registerReceiver(mReceiver, filter);
	}

    // refresh the vcardlist after incoming call end
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  Log.v(TAG, "Received " + intent.getAction());
	
			  String action = intent.getAction();
			  if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
    			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if(bondState == BluetoothDevice.BOND_NONE){
                    if(mManager == null || mManager.getDevice()== null)return;
                    if(device.getAddress().equals(mManager.getDevice().getAddress())){
                        cleanData();
                    }

                }

            }else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.STATE_OFF);
                if(state == BluetoothAdapter.STATE_OFF){
                    if(mManager == null)return;                    
                    cleanData();
                }

            }
			  
		  }
	  };

    public void cleanData(){
        Message clear = mHandler.obtainMessage(MESSAGE_CLEAR_DATA,
                    				RECHECK_PATH_DELAY);
    			mHandler.sendMessage(clear);
    }

}

