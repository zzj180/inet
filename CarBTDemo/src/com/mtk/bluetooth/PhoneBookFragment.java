package com.mtk.bluetooth;

import android.content.res.Resources;
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
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.view.Window;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.mtk.bluetooth.util.ActionItem;
import com.mtk.bluetooth.util.QuickActions;
import com.mtk.bluetooth.R;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import com.mediatek.bluetooth.BluetoothProfileManager;
import com.mediatek.bluetooth.BluetoothProfileManager.Profile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.view.ViewGroup;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.client.pbap.BluetoothPbapClient;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntry.PhoneData;
import android.bluetooth.client.pbap.BluetoothPbapCard;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientManager;
import com.mtk.bluetooth.pbapclient.BluetoothPabapClientCallback;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientConstants;
import android.bluetooth.client.pbap.BluetoothPbapClient.ConnectionState;
import com.mtk.bluetooth.util.Utils;
import android.text.InputType;
import android.content.DialogInterface;
import android.app.AlertDialog;
import com.mtk.bluetooth.common.*;

import android.util.Log;

public class PhoneBookFragment extends Fragment {

    protected static final String TAG = "PhoneBookFragment";

    Context mContext;

    private static final boolean DEBUG=true;
	
	private final static int MSG_HANDLE_ONESTEP_UPDATE = 1;
	private final static int MSG_HANDLE_ALL_UPDATE = 2;
	private final static int MSG_HANDLE_CLEAR = 4;

    private static final int ITEM_COUNT_PER_PAGE = 20;
    private static final int MESSAGE_RECHECK_PATH = 0;
    private static final int MESSAGE_PULL_PHONEBOOK_SIZE = 1;
	private static final int MESSAGE_RECHECK_PB_PATH = 2;
	private static final int MESSAGE_SET_PATH_FAIL = 3;	
	private static final int MAX_FAILED_TIMES = 5;
    private static final int MESSAGE_TOAST_PULL_PHONEBOOK_SIZE_FAIL = 2;
    private static final int MESSAGE_CLEAR_DATA = 6;

    private static final int RECHECK_PATH_DELAY = 200;
    private static final int MAX_SET_PATH_TIMES = 10;
    private static final int MAX_GET_PHONEBOOK_SIZE = 10;

    public static final int ACTIVITY_RESULT_CODE = 100;
	private static final int MAX_LIST_COUNT = 65535;
	private static final int MODE_INPUT_NAME = BluetoothPbapClient.SEARCH_ATTR_NAME;
	private static final int MODE_INPUT_NUMBER = BluetoothPbapClient.SEARCH_ATTR_NUMBER;

    public static final String EXTRA_BT_ADDRESS = "address";
	private byte mCurrentMode = MODE_INPUT_NAME;

    private BluetoothPbapClientManager mManager = BluetoothPbapClientManager.getInstance();

	
    private int mCurrentPage = 0;
    private String mTargetFolder = BluetoothPbapClientConstants.PB_PATH;
    private int mSetPathFailedTimes ;
    private int mGetPhonebookSizeTimes = 0;
    private int fail_times = 0;

    private ArrayList<VCardEntry> mVCardsList = new ArrayList<VCardEntry>();
	public ArrayList<BluetoothPbapCard> mPbapCardsList = new ArrayList<BluetoothPbapCard>();
    private VcardAdapter mAdapter = new VcardAdapter();
	private SearchedVcardAdapter msearchAdapter = new SearchedVcardAdapter();

	
	private static final String PHONE_KEY = "phone";
	private static final String NAME_KEY = "name";

	BluetoothDevice mDevice;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
        Log.d(TAG, "handleMessage msg.what->" + msg.what);
            switch (msg.what) {
                case MESSAGE_RECHECK_PATH:
					mSetPathFailedTimes ++;
                    checkFolderPath_forsearch();
                    break;
                case MESSAGE_PULL_PHONEBOOK_SIZE:
					mGetPhonebookSizeTimes++;
                    if (mGetPhonebookSizeTimes > MAX_GET_PHONEBOOK_SIZE) {
						Utils.dismissPopupWindow();
                        String text = BTApplication.getContext().getResources().getString(R.string.pull_phonebook_size_fail,
                                mTargetFolder);
             //           Toast.makeText(BTApplication.getContext(), text, Toast.LENGTH_LONG)
             //                   .show();
                    } else {
                        if(!pullPhoneBookSize()){
                            Utils.dismissPopupWindow();
                        }
                    }                    
                    break;
				case MESSAGE_RECHECK_PB_PATH:
					mSetPathFailedTimes ++;
                    if(mSetPathFailedTimes<MAX_FAILED_TIMES){
                        checkFolderPath();
                    }else{
                        Utils.dismissPopupWindow();
						Utils.showShortToast(BTApplication.getContext(),"set path fail");
                    }
					
					break;
				case MESSAGE_SET_PATH_FAIL:
					fail_times++;
					if(fail_times<MAX_FAILED_TIMES){
						checkFolderPath_forsearch();
					}else{
    					Utils.dismissPopupWindow();
						Utils.showShortToast(BTApplication.getContext(),"set path fail");
					}
                    break;
                case MESSAGE_CLEAR_DATA:
                    Log.d(TAG,"start clear");
                    mVCardsList.clear();
                    mPbapCardsList.clear();
                    mAdapter.notifyDataSetChanged();
                    msearchAdapter.notifyDataSetChanged();
                    break;
                    
            }

        };
    };
	private Button search_btn;
	private Button mSwitchMode;
	private Button pb_btn;
	private Button pause_btn;
	private EditText mSearchValue;
	private Button sim_btn;
	private PhonebookButtonOnclick mOnClickLietener = new PhonebookButtonOnclick();
	
	private ListView mPhonebookListView;
	private ListView mSearchResultView;
	
	//private SimpleAdapter mAdapter;  //Replace ListAdapter
	private int mDownloadPath;
	private int mShowedRecordNum;
	
	private long mStartTime;
	private long mEndTime;

	private String mPbname;
	private String mPbnumber;

	private boolean searchBtnClick = false;
	private boolean sync_btnClick = true;
	
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
	private ActionItem action_call = new ActionItem();
	
    
	public PhoneBookFragment() {}

	
    public PhoneBookFragment(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
	
	@Override
	public void onResume() {
    	Log.d(TAG,"onResume");
        mSearchValue.setFocusable(true);
		if(this.getUserVisibleHint()){
			registerSyncCallBack();
			Log.d(TAG,"onResume registercallback");
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
    	Utils.dismissPopupWindow();
		super.onStop();
	}

    @Override
	public void onPause() {    	
		super.onPause();
	}

    
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy unRegisterSyncCallBack");
		unRegisterSyncCallBack();
        try{
           if(mReceiver != null) {
               this.getActivity().unregisterReceiver(mReceiver);
            }
        }catch(IllegalArgumentException e){
           Log.e("TAG","IllegalArgumentException");
        }        		
        Utils.dismissPopupWindow();
		super.onDestroy();
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {        
		View view = inflater.inflate(R.layout.bt_phonebook, container, false);
		
		pb_btn = (Button)view.findViewById(R.id.bt_phonebook_pb);
		sim_btn = (Button)view.findViewById(R.id.bt_phonebook_sim);
		pause_btn = (Button)view.findViewById(R.id.bt_phonebook_pause);
		search_btn = (Button)view.findViewById(R.id.bt_phonebook_search);
		mSearchValue = (EditText)view.findViewById(R.id.bt_phonebook_edit);
		mSwitchMode = (Button)view.findViewById(R.id.bt_phonebook_search_mode);
		
		pause_btn.setOnClickListener(mOnClickLietener);
		pb_btn.setOnClickListener(mOnClickLietener);
		search_btn.setOnClickListener(mOnClickLietener);
		mSwitchMode.setOnClickListener(mOnClickLietener);
		sim_btn.setOnClickListener(mOnClickLietener);
        mSearchValue.setOnClickListener(mOnClickLietener);

		mPhonebookListView = (ListView)view.findViewById(R.id.phonebook_listview);
		mPhonebookListView.setAdapter(mAdapter);
        mPhonebookListView.setOnItemClickListener(mPhoneBookListClickListener);
        mAdapter.notifyDataSetChanged();

		mSearchResultView = (ListView)view.findViewById(R.id.phonebook_result_listview);
		mSearchResultView.setAdapter(msearchAdapter);
		mSearchResultView.setOnItemClickListener(msearchResultListClickListener);
		msearchAdapter.notifyDataSetChanged();
		
		updateSwitchModeText();	
        return view;
    }
	
    public void registerSyncCallBack(){
        mManager.registerCallback(mCallback);
		Log.d(TAG,"registerSyncCallBack");
    }
	
    public void unRegisterSyncCallBack(){
		try{
			mManager.unregisterCallback(mCallback);			
		}catch(NullPointerException e){
			e.printStackTrace();
		}  
		
    }

    private class PhonebookButtonOnclick implements OnClickListener{

        //private int position;

        public PhonebookButtonOnclick() {
            //this.position = position;
        }

        @Override
        public void onClick(View v) {
            int state = mManager.getConnectState();          
			if(state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED ||
                state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTING){ 
                if(MainActivity.mConnectedDevice != null){
                   Utils.showPbapConnectDialog(getActivity(),MainActivity.mConnectedDevice);
                }else{
                   Log.d(TAG,"PhonebookButtonOnclick device is null");
                   Utils.showShortToast(BTApplication.getContext(),R.string.pbap_no_device_to_connect);
                }              
                cleanData();
                return;
			}else if(state == BluetoothPbapClientConstants.CONNECTION_STATE_CONNECTING){
    			Utils.showShortToast(getActivity(),R.string.pbap_is_connecting);
                return;
            }
			
		// Button Click Listener
		switch (v.getId()) {
		case R.id.bt_phonebook_pb:
             cleanData();
             mPhonebookListView.setVisibility(View.VISIBLE);
             mSearchResultView.setVisibility(View.GONE);                                   		
             Utils.showPopupWindow(getActivity(),PhoneBookFragment.this.getView(),
                     MainActivity.screenW,MainActivity.screenH);
	         mTargetFolder = BluetoothPbapClientConstants.PB_PATH;
		     sync_btnClick = true;
		     searchBtnClick = false;
		     pb_btn.setBackgroundResource(R.drawable.bt_180_50_btn_disable_focused);
		     sim_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
             mSetPathFailedTimes = 0;
		     Message msg_pb = mHandler.obtainMessage(MESSAGE_RECHECK_PB_PATH,
				     RECHECK_PATH_DELAY);
		     mHandler.sendMessage(msg_pb);
			 break;
		case R.id.bt_phonebook_pause:
             Log.d(TAG,"clear");
	         mVCardsList.clear();
		     mPbapCardsList.clear();
		     mAdapter.notifyDataSetChanged();
		     msearchAdapter.notifyDataSetChanged();
	         Utils.dismissPopupWindow();
		     break;
		case R.id.bt_phonebook_search:
     //        mVCardsList.clear();
             msearchAdapter.notifyDataSetChanged();
             Utils.showPopupWindow(getActivity(),PhoneBookFragment.this.getView(),
                  MainActivity.screenW,MainActivity.screenH);            
		     refreshVcardList();
		     break;
		case R.id.bt_phonebook_search_mode:			
             if (mCurrentMode == MODE_INPUT_NAME) {
                  Utils.showShortToast(BTApplication.getContext(),"Switch query mode to NUNBER");
                  mCurrentMode = MODE_INPUT_NUMBER;
              } else {
                  Utils.showShortToast(BTApplication.getContext(),"Switch query mode to NAME");
                  mCurrentMode = MODE_INPUT_NAME;
              }
              updateSwitchModeText();
		      break;
		case R.id.bt_phonebook_sim:
             cleanData();
             mPhonebookListView.setVisibility(View.VISIBLE);
             mSearchResultView.setVisibility(View.GONE);
             Utils.showPopupWindow(getActivity(),PhoneBookFragment.this.getView(),   
                  MainActivity.screenW,MainActivity.screenH);
		     mTargetFolder = BluetoothPbapClientConstants.SIM_PB_PATH;
		     sync_btnClick = true;
		     searchBtnClick = false;            
		     sim_btn.setBackgroundResource(R.drawable.bt_180_50_btn_disable_focused);
		     pb_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
             mSetPathFailedTimes = 0;
		     Message msg_sim = mHandler.obtainMessage(MESSAGE_RECHECK_PB_PATH,
			         RECHECK_PATH_DELAY);
		     mHandler.sendMessage(msg_sim);			
		     break;
               case R.id.bt_phonebook_edit:
                    mSearchValue.setFocusable(true);
                    break;
	       default:
	            break;
		}
	    }
	}

	/**
	 * default phonebook
	 *
	 */
    private class VcardAdapter extends BaseAdapter {
		

        @Override
        public int getCount() {
            if (mVCardsList != null) {
                return mVCardsList.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            if (mVCardsList != null) {
                return mVCardsList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            if (mVCardsList != null) {
                return position ;
            } else {
                return 0;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            synchronized (mVCardsList) {
                if (mVCardsList != null && position < mVCardsList.size()) {
                    ItemView view = null;
                    if (convertView != null && convertView instanceof ItemView) {
                        view = (ItemView) convertView;
                    } else {
                        view = new ItemView(mContext);
                    }
                    String displayName = mVCardsList.get(position).getDisplayName();
                    if(DEBUG) Log.d(TAG,"name ="+displayName);
                    List<PhoneData> numberList = mVCardsList.get(position).getPhoneList();
                    if(DEBUG) Log.d(TAG,"numberList ="+numberList);
                    view.getNameView().setText(displayName);
                    if (numberList != null && numberList.size() > 0) {
                        view.getNumberView().setText(numberList.get(0).getNumber());
                        for(int i=0;i<numberList.size();i++){
                              if(DEBUG) Log.d(TAG,"number ="+numberList.get(i).getNumber());
                        }                        
                    }else{
                        view.getNumberView().setText("");
                    }
                    return view;
                } else {
                    return null;
                }
            }
        }

    }


	/*
	* for search result
	*/
	private class SearchedVcardAdapter extends BaseAdapter {
	
		   @Override
		   public int getCount() {
			   // TODO Auto-generated method stub
			   return mPbapCardsList.size();
		   }
	
		   @Override
		   public Object getItem(int position) {
			   // TODO Auto-generated method stub
			   return mPbapCardsList.get(position);
		   }
	
		   @Override
		   public long getItemId(int position) {
			   return position;
		   }
	
		   @Override
		   public View getView(int position, View convertView, ViewGroup parent) {
			   synchronized (mPbapCardsList) {
				   if (position < mPbapCardsList.size()) {
					   ItemView view = null;
					   if (convertView != null && convertView instanceof ItemView) {
						   view = (ItemView) convertView;
					   } else {
						   view = new ItemView(mContext);
					   }
					   String displayName = mPbapCardsList.get(position).N;
					   String handle = mPbapCardsList.get(position).handle;
					   view.getNameView().setText(displayName);
	                   view.getNumberView().setText(handle);
					   
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
				if(sync_btnClick){
					checkFolderPath();
				}else{
					checkFolderPath_forsearch();
				}
            }else{
            	  if(mSetPathFailedTimes < MAX_FAILED_TIMES){
                	if(mDevice != null){
                		downloadPhoneBook();
                	}
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
                    String text = BTApplication.getContext().getResources().
                        getString(R.string.new_missed_call_toast,newMissedCalls);
				    Utils.showShortToast(BTApplication.getContext(),text);
                }                
				if(list !=null && list.size()>0){
					mVCardsList = list;
					ArrayList<HashMap<String, String>> lt =new ArrayList<HashMap<String,String>>();		
					for(int i=0;i<mVCardsList.size();i++){
		 				String displayName = mVCardsList.get(i).getDisplayName();
		                List<PhoneData> numberList = mVCardsList.get(i).getPhoneList();
                    	if (numberList != null && numberList.size() > 0) {                     
                        	for(int j=0;j<numberList.size();j++){
                        		HashMap<String, String> map =new HashMap<String, String>();
                        		map.put(NAME_KEY, displayName);
                        		map.put(PHONE_KEY, numberList.get(j).getNumber());
								lt.add(map);
                        	}                        
                    	}
                    }
                    Intent it= new Intent("com.zzj.phonebook.send");
                    
                    it.putExtra(PHONE_KEY, lt);
                    BTApplication.getContext().sendBroadcast(it);
				}else{
		//			Toast.makeText(BTApplication.getContext(), "result is null", Toast.LENGTH_SHORT).show();
				}                
					try {
						 mAdapter.notifyDataSetChanged();
					} catch (Exception e) {
						// TODO: handle exception
					}
               



				
            } else {
	            Utils.showShortToast(BTApplication.getContext(),R.string.pull_failed);                
            }
			
        }

        @Override
        public void onPullVcardListingDone(boolean success, int newMissedCalls,
                ArrayList<BluetoothPbapCard> list) {            
			Log.d(TAG, "onPullVcardListingDone, success = " + success + ", list = "
								+ (list == null ? null : list.size()));
			if (success) {
				if (list != null) {
						mPbapCardsList = list;
				} else {
			//			Toast.makeText(BTApplication.getContext(), "search_result is null", Toast.LENGTH_SHORT)
			//				.show();
						mPbapCardsList = new ArrayList<BluetoothPbapCard>();
				}
						msearchAdapter.notifyDataSetChanged();
			} else{
	//			Toast.makeText(BTApplication.getContext(),R.string.pull_failed,Toast.LENGTH_SHORT).show();
			}
				Utils.dismissPopupWindow();
		}	

        

        @Override
        public void onPullVcardEntryDone(boolean success, VCardEntry entry) {
            // TODO Auto-generated method stub      
            if(success){
				String pbnumber = null;
				List<PhoneData> numberList = entry.getPhoneList();
				if(numberList ==null||numberList.isEmpty())
					
					return ;
			
				pbnumber = numberList.get(0).getNumber();
				if(pbnumber.isEmpty())
					return ;
				
				Intent intent = new Intent();
				intent.setClass(getActivity(), VcardEntryActivity.class);
				intent.putExtra(VcardEntryActivity.DISPLAY_NAME, entry.getDisplayName());
				
				intent.putExtra(VcardEntryActivity.PHONE_NUMBER, pbnumber);
				intent.putExtra(VcardEntryActivity.EXTRA_TARGET_FOLDER, mTargetFolder);
				Log.d(TAG,"currentfolderPath="+mManager.getCurrentPath());
				mManager.unregisterCallback(mCallback);
				startActivity(intent);
			}else{
	//			Toast.makeText(BTApplication.getContext(),"PullVcardEntry failed",Toast.LENGTH_SHORT).show();
			}
        }

        @Override
        public void onPullPhonebookSizeDone(boolean success, int size) {
            Log.d(TAG, "onPullPhonebookSizeDone, success = " + success + "size = " + size);
			
            if (!success) {
                Message message = mHandler.obtainMessage(MESSAGE_PULL_PHONEBOOK_SIZE,
                        RECHECK_PATH_DELAY);
                mHandler.sendMessage(message);
            } else {
            	
            	 
	                 String text = BTApplication.getContext().getResources().getString(R.string.total_pb_size, 
	    			   size);
	      //             Toast.makeText(BTApplication.getContext(), text, Toast.LENGTH_LONG).show();
               
               if(size >0){
                   mGetPhonebookSizeTimes = 0;          
                  
                   pullPhoneBook(null,size);
               }else{
                   Utils.dismissPopupWindow();
               }
               
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
                Log.d(TAG,"clear list");
                cleanData();
                Utils.dismissPopupWindow();
                Utils.showShortToast(BTApplication.getContext(),"BluetoothPbapClient Disconnected");
                
                if(mSetPathFailedTimes < MAX_FAILED_TIMES){
                	if(mDevice != null){
                		downloadPhoneBook();
                	}
            	}
            }
			Log.d(TAG, "state == "+state);
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
	             Message mes_sim_size = mHandler.obtainMessage(MESSAGE_PULL_PHONEBOOK_SIZE,
	                         RECHECK_PATH_DELAY);
	             mHandler.sendMessage(mes_sim_size);
	    	}
        } else {
            
            Log.d(TAG, "mTargetFolder = " + mTargetFolder + ", mCurrentFolder = " + currentFolder);
            if (!mManager.setPhoneBookFolderRoot()) {
				if(this.getUserVisibleHint()){
					Log.e(TAG, "setPhoneBookFolderRoot fail, reset it");
	                Message message = mHandler.obtainMessage(MESSAGE_RECHECK_PB_PATH);
	                mHandler.sendMessageDelayed(message, RECHECK_PATH_DELAY);
				}

            }
        }
    }

	private synchronized void checkFolderPath_forsearch() {
			String currentFolder = mManager.getCurrentPath();
			Log.d(TAG, "checkFolderPath_forsearch() mCurrentFolder="+ currentFolder);
			if (mTargetFolder == null) {			
			} else if (mTargetFolder.equals(currentFolder)) {
				fail_times = 0;
				Log.d(TAG, "[OK] mTargetFolder matched");
                pullVcardList(null, 0);
			} else { 				
				if (mTargetFolder.startsWith(currentFolder) || currentFolder.isEmpty()) {
					String nextFolder = mTargetFolder.substring(currentFolder.length());
					if (nextFolder.startsWith("/")) {
						nextFolder = nextFolder.substring(1);
					}
					String[] folders = nextFolder.split("/");
					nextFolder = folders[0];
					
					if (!mManager.setPhoneBookFolderDown(nextFolder)) {
						if(this.getUserVisibleHint()){
                            int state = mManager.getConnectState();
                            if(state == BluetoothPbapClientConstants.CONNECTION_STATE_CONNECTED){
                                Log.e(TAG, "setPhoneBookFolderDown fail, reset it");
    							Message message = mHandler.obtainMessage(MESSAGE_SET_PATH_FAIL);
    							mHandler.sendMessageDelayed(message, RECHECK_PATH_DELAY);
                            }
							
						}
						
					}
				} else {					
					if (!mManager.setPhoneBookFolderRoot()) {
						if(this.getUserVisibleHint()){
							Log.e(TAG, "setPhoneBookFolderRoot fail, reset it");
							Message message = mHandler.obtainMessage(MESSAGE_SET_PATH_FAIL);
							mHandler.sendMessageDelayed(message, RECHECK_PATH_DELAY);
						}
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
            Utils.showShortToast(BTApplication.getContext(),"pull phonebook error");
            Log.d(TAG, "pullPhoneBook error");            
        }
    }

    private boolean pullPhoneBookSize() {
        String pbName = mTargetFolder + ".vcf";
		Log.d(TAG,"pullPhoneBookSize pbName ="+pbName);		
        return mManager.pullPhoneBookSize(pbName);
    }

	 private boolean pullVcardListingSize() {
        
        return mManager.pullVcardListingSize(mTargetFolder);
    }

    private void pullVcardList(String pbName, int currentPage) {
	 Log.d(TAG,"pullVcardList start");
         if (mManager.getCurrentPath().length() > 3) {
            pbName = null;
        } else {
            pbName = mTargetFolder+".vcf" ;
        }
		Log.d(TAG,"pbName = "+ pbName+" CurrentPath="+mManager.getCurrentPath());
		
        String searchVal = mSearchValue.getText().toString();
        if (searchVal.isEmpty()) {
            searchVal = null;
        }
        byte order = BluetoothPbapClient.ORDER_BY_ALPHABETICAL;
		Log.d(TAG,"order = "+order+" mCurrentMode="+mCurrentMode+" searchVal="+searchVal);
        if (mManager.pullVcardList(pbName, order, mCurrentMode, searchVal, MAX_LIST_COUNT, 0)) {
            Log.d(TAG, "pullVcardList start");
            
        } else {
            Log.d(TAG, "pullVcardList error");
           /* Toast.makeText(getApplicationContext(), R.string.pull_failed,
                    Toast.LENGTH_SHORT).show();*/
        }
		
    }

	 private void updateSwitchModeText() {
        if (mCurrentMode == MODE_INPUT_NAME) {
            mSwitchMode.setText(R.string.bt_phonebook_search_mode_name);
            mSearchValue.setHint(getResources().getString(
				R.string.bt_phonebook_search_hint_name));
            mSearchValue.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            mSwitchMode.setText(R.string.bt_phonebook_search_mode_number);
            mSearchValue.setHint(getResources().getString(
				R.string.bt_phonebook_search_hint_number));
            mSearchValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        mSearchValue.setText("");
    }
	
	// The on-click listener for all devices in the ListViews
	private OnItemClickListener mPhoneBookListClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			
			final String pbname;
			final String pbnumber;
			
			pbname = mVCardsList.get(arg2).getDisplayName();
			List<PhoneData> numberList = mVCardsList.get(arg2).getPhoneList();
			if(numberList ==null||numberList.isEmpty())
				return ;
			
			pbnumber = numberList.get(0).getNumber();
			if(pbnumber.isEmpty())
				return ;
			
			if(mManager.getConnectState()==
				BluetoothPbapClientConstants.CONNECTION_STATE_CONNECTED){
				Intent intent = new Intent();
				intent.setClass(getActivity(), VcardEntryActivity.class);
			    intent.putExtra(VcardEntryActivity.DISPLAY_NAME, pbname);
				intent.putExtra(VcardEntryActivity.PHONE_NUMBER, pbnumber);
				intent.putExtra(VcardEntryActivity.EXTRA_TARGET_FOLDER, mTargetFolder);
				Log.d(TAG,"currentfolderPath="+mManager.getCurrentPath());
				mManager.unregisterCallback(mCallback);
				startActivity(intent);
			}else{
				Utils.showShortToast(BTApplication.getContext(),"Pbap is disconnected");
			}
			
			
	
		}
	};


	// The on-click listener for all devices in the ListViews
		private OnItemClickListener msearchResultListClickListener = 
		new OnItemClickListener() {
		
			public void onItemClick(AdapterView<?> av, View v, int position, long arg3) {	
				
				String mHanlde;
				mHanlde = mPbapCardsList.get(position).handle;
				if(mManager.getConnectState()==
					BluetoothPbapClientConstants.CONNECTION_STATE_CONNECTED){
					mManager.pullVcardEntry(mHanlde, 0,BluetoothPbapClient.VCARD_TYPE_21);
				}else{
					Utils.showShortToast(BTApplication.getContext(),"Pbap is disconnected");
				}
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

	private void refreshVcardList(){	
			searchBtnClick = true;
			sync_btnClick = false;
			Log.d(TAG,"search start + currentpath="+mManager.getCurrentPath());
			mSearchResultView.setVisibility(View.VISIBLE);
			mPhonebookListView.setVisibility(View.GONE);
			
			Message message0 = mHandler.obtainMessage(MESSAGE_RECHECK_PATH,
				RECHECK_PATH_DELAY);
			mHandler.sendMessage(message0);

	}
	
	// refresh the vcardlist after incoming call end
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  Log.v(TAG, "Received " + intent.getAction());
	
			  String action = intent.getAction();
			  if(action.equals(BluetoothHeadsetClient.ACTION_CALL_CHANGED)){
				if(mSearchResultView.getVisibility() == View.VISIBLE){
					BluetoothHeadsetClientCall changedCall = 
						(BluetoothHeadsetClientCall)intent.getParcelableExtra(
						BluetoothHeadsetClient.EXTRA_CALL);
					int state = changedCall.getState();
					Log.d(TAG,"ACTION_CALL_CHANGED state="+state);
					if(state == BluetoothHeadsetClientCall.CALL_STATE_TERMINATED){
						refreshVcardList();
					}	
				}
			}else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
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

	private void registerReceiver(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothHeadsetClient.ACTION_CALL_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		getActivity().registerReceiver(mReceiver, filter);
	}
   
    public void cleanData(){
        
       Message clear = mHandler.obtainMessage(MESSAGE_CLEAR_DATA,
                    		RECHECK_PATH_DELAY);
       mHandler.sendMessage(clear);

    }

    public void syncPhoneBook(BluetoothDevice device){
    		  mDevice = device;		
    		  mSetPathFailedTimes = 0;      
			   downloadPhoneBook(); 
		 	
    }

    private void downloadPhoneBook(){
			if(BtPairConnectActivity.mCheckedItems[2]){
			         
		    	     cleanData();
		    	     registerSyncCallBack();
		    	     mManager.initConnect(mDevice);
		      //       mPhonebookListView.setVisibility(View.VISIBLE);
		      //       mSearchResultView.setVisibility(View.GONE);                                   		
		      //       Utils.showPopupWindow(BTApplication.getContext().getApplicationContext(),PhoneBookFragment.this.getView(),
		      //               MainActivity.screenW,MainActivity.screenH);
			         mTargetFolder = BluetoothPbapClientConstants.PB_PATH;
				     sync_btnClick = true;
				     searchBtnClick = false;
			//	     pb_btn.setBackgroundResource(R.drawable.bt_180_50_btn_disable_focused);
			//	     sim_btn.setBackgroundResource(R.drawable.btn_180_50_bg);
		            
				     Message msg_pb = mHandler.obtainMessage(MESSAGE_RECHECK_PB_PATH,
						     1500);
				     mHandler.sendMessage(msg_pb);
		 		}
    }

}
