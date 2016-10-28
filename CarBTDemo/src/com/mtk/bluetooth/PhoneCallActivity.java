package com.mtk.bluetooth;


import com.mtk.bluetooth.R;
import android.app.AlertDialog;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.mediatek.bluetooth.BluetoothProfileManager;
import com.mediatek.bluetooth.BluetoothProfileManager.Profile;
import com.mtk.bluetooth.common.CachedBluetoothDeviceManager;
import com.mtk.bluetooth.common.LocalBluetoothAdapter;
import com.mtk.bluetooth.common.LocalBluetoothManager;
import com.mtk.bluetooth.common.LocalBluetoothProfileManager;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientConstants;
import com.mtk.bluetooth.pbapclient.BluetoothPbapClientManager;
import com.mtk.bluetooth.pbapclient.BluetoothPabapClientCallback;
import android.bluetooth.client.pbap.BluetoothPbapClient;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntry.PhoneData;
import android.bluetooth.client.pbap.BluetoothPbapCard;
import java.util.ArrayList;
import java.util.List;
import com.mtk.bluetooth.util.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.format.Time;
import android.view.MotionEvent;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.app.ActivityManager;


public class PhoneCallActivity extends Activity implements OnClickListener{
	private static final String TAG = "PhoneCallActivity";
	private static final boolean DEBUG = Utils.D;
	String m_strPhoneNum;
	String m_strPhoneName;
	Time m_callStartTime;
	int callfrom;
	boolean bIsMissedCall = false ;
	boolean bIsOnCalling = false;
	boolean bIsVoiceOut = true;
	boolean bIsCurrentAudioTowardsAG = true;
	EditText m_subcallnumber_et;
	Editable m_subcallnumstr_edt;
	//private BluetoothHfAdapter m_hfadpter ;
	private TextView phonenumber_TV;
        private ImageButton voiceSourceCar_Btn;
        private TextView callstatus_TV;

	private BluetoothHeadsetClient mHeadsetClient = null;
	private static BluetoothDevice mConnectedDevice = null;
	private AudioManager mAudiomanager;
    private BluetoothHeadsetClientCall mActiveCall = null;
    private BluetoothHeadsetClientCall mwaitCall = null;
    private LocalBluetoothManager mLocalManager ;
	//add for pbap start
	private BluetoothPbapClientManager mManager;
	private final int PBAP_DISCONNECED = 
		BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED;
	
	private static final byte MODE_INPUT_NUMBER = BluetoothPbapClient.SEARCH_ATTR_NUMBER;
	private static final int MAX_LIST_COUNT = 65535;
	private String PB_PATH = BluetoothPbapClientConstants.PB_PATH;
	private String SIM_PB_PATH = BluetoothPbapClientConstants.SIM_PB_PATH;
	private String mTargetFolder = PB_PATH;
	private final int MESSAGE_RECHECK_PATH = 0;
	private final int MESSAGE_PULL_PB_VCARDLIST = 1;
	private final int MESSAGE_PULL_SIM_PB_VCARDLIST = 2;
    private final int MESSAGE_SET_DIALFRAGMENT_CALLSTATUS = 3;
    private final int MESSAGE_CALLBACK_DIALACTIVITY = 4;

	private final int HANDLER_DELAY = 500;
	//add for pbap end
	
	private static final String UNKOWN_PHONE_NUMBER = "unkown";
	public static final String ACTION_TEL = "com.console.TEL";
	public static final String ACTION_TEL_ANSWER = "com.console.TEL_ANSWER";
	public static final String ACTION_TEL_HANDUP = "com.console.TEL_HANDUP";
	
	private static final String ACTION_HANGUP = "com.colink.service.TelphoneService.TelephoneHandupReceive";
	private static final String ACTION_ANSWER = "com.colink.service.TelphoneService.TelephoneAnswerReceive";
	private  boolean isSoftkeyPadVisible   = false;

    private AlertDialog dialog;

    private FloatView mFloatView;

	private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
        Log.d(TAG, "handleMessage msg.what->" + msg.what);
            switch (msg.what) {
                case MESSAGE_PULL_PB_VCARDLIST:					
					pullVcardList(PB_PATH,m_strPhoneNum);
                    break;
				case MESSAGE_PULL_SIM_PB_VCARDLIST:					
					pullVcardList(SIM_PB_PATH,m_strPhoneNum);
					break;
                case MESSAGE_SET_DIALFRAGMENT_CALLSTATUS:
                    DialFragment.mLastCallState = -1;
                    break;   
                case MESSAGE_CALLBACK_DIALACTIVITY:
                    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
					boolean success = manager.moveTaskToFrontWithResult(getTaskId(), 0,null);
                    break;                
            }

        };
    };	

    private OnClickListener mOnClick = new OnClickListener() {
		@Override
		public void onClick(View v) {	
				Log.d(TAG, "!--->onClick FloatView");
				
				ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
				boolean success = manager.moveTaskToFrontWithResult(getTaskId(), 0,null);
				if(!success){
					startActivity(new Intent(PhoneCallActivity.this,PhoneCallActivity.class));
				}


			//	Intent intent = new Intent(PhoneCallActivity.this,PhoneCallActivity.class);
			//	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			//	getApplicationContext().startActivity(intent);
			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//  android:background="@drawable/bt_call_bg_small"
		Drawable drawable;
		Resources res = getResources();
		drawable = res.getDrawable(R.drawable.bt_call_bg_small);
		this.getWindow().setBackgroundDrawable(drawable);
		
		setContentView(R.layout.bt_calling_status);
		m_callStartTime = new Time(); 
		m_callStartTime.setToNow();
		
		Intent intent= getIntent();
		m_strPhoneNum= intent.getStringExtra("PhoneNumber");
		m_strPhoneName = intent.getStringExtra("PhoneName");
		mConnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		callfrom = intent.getIntExtra("callfrom",0);
        
		initViews();
        if(callfrom == 2) {
            voiceSourceCar_Btn.setVisibility(View.VISIBLE);
        }else {
            voiceSourceCar_Btn.setVisibility(View.GONE);
        }

		//add filter
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE);
		filter.addAction(BluetoothHeadsetClient.ACTION_CALL_CHANGED);
		filter.addAction(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED);
		filter.addAction(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(BluetoothHeadsetClient.ACTION_RESULT);
        filter.addAction(LocalBluetoothProfileManager.ACTION_PROFILE_UPDATE);
        filter.addAction(ACTION_TEL);
        filter.addAction(ACTION_TEL_ANSWER);
        filter.addAction(ACTION_TEL_HANDUP);
		filter.addAction(ACTION_HANGUP);
        filter.addAction(ACTION_ANSWER);
		this.registerReceiver(mBroadcastReceiver, filter);
		
		if(!m_strPhoneNum.isEmpty()){
			if(DEBUG) Log.i(TAG, "phone number is " + m_strPhoneNum);
			if(m_strPhoneNum.equals(UNKOWN_PHONE_NUMBER)!=true){
				updatePhoneNumberDisplay(true,m_strPhoneNum);
			}
		}
		
        if(mHeadsetClient != null){
            int state = mHeadsetClient.getAudioState(mConnectedDevice);
            if(state == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED){
               phoneCallShowVoiceSource(true);
            }else{
               phoneCallShowVoiceSource(false);
            }

        }
		mAudiomanager = new AudioManager(this);
		// don't fisish when touch outside
		this.setFinishOnTouchOutside(false);
		Utils.wakeUpAndUnlock(this);

		mFloatView = new FloatView(this);
		mFloatView.setImgResource(R.drawable.answer);
		mFloatView.setOnClickListener(mOnClick);
		
		mLocalManager = LocalBluetoothManager.getInstance(this);
 		try{
           mHeadsetClient = 
           mLocalManager.getProfileManager().getHeadsetClientProfile().getHeadsetClientServer();
        }catch(NullPointerException e){
           Log.e(TAG,"NullPointerExcetion occured");
        }	    
            	
        if(mHeadsetClient != null && mConnectedDevice == null){
            mConnectedDevice = mHeadsetClient.getConnectedDevices().get(0);
        }
        ImageButton anser_btn = (ImageButton)findViewById(R.id.btn_answer);
		anser_btn.setOnClickListener(this);       
		initPBAP();
	}

    @Override
	protected void onResume() {
	    Log.d(TAG,"===== onResume =====");
      
       
        Log.d(TAG,"callfrom =="+callfrom);
		if((callfrom == 1) && !bIsOnCalling){
			callstatus_TV.setText(R.string.str_income_call_status);
			ImageButton anser_btn = (ImageButton)findViewById(R.id.btn_answer);
			anser_btn.setVisibility(View.VISIBLE);           
		}else if((callfrom == 0 /*&& m_strPhoneName == null*/)){
			callstatus_TV.setText(R.string.str_out_call_status);
		}else if(callfrom == 2) {
		    callstatus_TV.setText(R.string.str_bt_call_active);
        }
        
	   super.onResume();
	   if (mFloatView.isShown()) {
			mFloatView.hide();
		}	
		final IntentFilter homeFilter  = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
	//	registerReceiver(mHomeKeyReceiver, homeFilter);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG)Log.d(TAG, "===== onStart =====");
	}

	@Override
	protected void onPause() {
	//	unregisterReceiver(mHomeKeyReceiver);
		super.onPause();
		
	}

	@Override
	protected void onStop() {
		
		super.onStop();
		 if (!mFloatView.isShown()) {
			mFloatView.show();
		}	
	}
	
	@Override
	protected void onDestroy() {
		Intent phone_idle = new Intent("android.intent.action.BLUETOOTH_PHONE_STATE");
		phone_idle.putExtra("state", TelephonyManager.CALL_STATE_IDLE);
		sendBroadcast(phone_idle);
        Message msg_set_call = mHandler.obtainMessage(MESSAGE_SET_DIALFRAGMENT_CALLSTATUS,
                                800);
                    mHandler.sendMessage(msg_set_call);
         mFloatView.setOnClickListener(null);
         if (mFloatView.isShown()) {
			mFloatView.hide();
		}	
		if(mLocalManager!=null){
			LocalBluetoothAdapter localAdapter = mLocalManager.getBluetoothAdapter();
			localAdapter.startScanning(true);
		}
        super.onDestroy();
		
	}
	
        /*
	 * Function name : updatePhoneNumberDisplay
	 * Parameters:  
	 * 				String PhoneNumber 
	 * 				------	if it is incoming call set it as true ,or else if it is outgoing call set it as false
	 * 				
	 * Description : 
	 */	
	
	private void updatePhoneNumberDisplay(boolean fgForeUpdate,String PhoneNumber){
		if((m_strPhoneNum.equals(PhoneNumber)== false)||fgForeUpdate){
			
			if(DEBUG) Log.i(TAG, "phonenumber_TV setText " + PhoneNumber);
			m_strPhoneNum = PhoneNumber ;
			
			if((m_strPhoneName != null) && (!m_strPhoneName.isEmpty())){
				phonenumber_TV.setText(m_strPhoneName);
			}else{
				phonenumber_TV.setText(PhoneNumber);
			}
		}
	}

        /*
	 * Function name : BroadcastReceiver
	 * Parameters:  
	 * 				
	 * 				
	 * Description : 
	 * 				handle received messages
	 */	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		private int mCallState = 0;
		private int mBtCallsetupState = 0;
		@Override
		public void onReceive(Context context, Intent intent) {
			if(DEBUG) Log.v(TAG, "onReceive:action->" + intent.getAction());

			String action = intent.getAction();
			
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				BluetoothDevice device = intent
				.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.ERROR);
				//mManager.setBluetoothStateInt(state);
				if(state == BluetoothAdapter.STATE_TURNING_OFF || 
                    state == BluetoothAdapter.STATE_OFF){
                    DestroyActivity();
                 }
			}else if(action.equals(BluetoothProfileManager.ACTION_PROFILE_STATE_UPDATE)){
				Profile   profile =(Profile)intent.getSerializableExtra(
					BluetoothProfileManager.EXTRA_PROFILE);
				
					if(profile== null){
						if(DEBUG) Log.w(TAG, "ACTION_PROFILE_STATE_UPDATE,profilename is null");
						return ;
					}
				    if(profile.equals(BluetoothProfileManager.Profile.HEADSET)){
				    	int profilestate = intent.getIntExtra(
							BluetoothProfileManager.EXTRA_NEW_STATE,0);
						
				    	if(profilestate == BluetoothProfileManager.STATE_DISCONNECTED ){
				    		
				    		if(DEBUG) Log.w(TAG, "hf disconnected,exit phonecall activity ");
							Utils.showShortToast(getApplicationContext(),
									"handfree profile disconnected");
				    		DestroyActivity();
				    	}
				    }
				
			}else if(action.equals(BluetoothHeadsetClient.ACTION_CALL_CHANGED)){
				BluetoothHeadsetClientCall changedCall = 
					(BluetoothHeadsetClientCall)intent.getParcelableExtra(
						BluetoothHeadsetClient.EXTRA_CALL);
                Log.d(TAG,"changedCall.getNumber()="+changedCall.getNumber());
    			int state = changedCall.getState();
    			if(DEBUG) Log.d(TAG,"ACTION_CALL_CHANGED state="+state);
    			if(state == BluetoothHeadsetClientCall.CALL_STATE_TERMINATED ){
                    if(mActiveCall == null || mActiveCall.getId() == changedCall.getId()){
                        Log.d(TAG,"TERMINATED it's time to finish");
                        mActiveCall = null;
                        DestroyActivity();
                    }else if(mwaitCall != null && changedCall != null 
                        && (mwaitCall.getId() == changedCall.getId())){
                        dissmissDialog();
                    }                   
    				
    			}else if(state == BluetoothHeadsetClientCall.CALL_STATE_ACTIVE){
        			mActiveCall = changedCall;
        			bIsOnCalling = true;
        			ImageButton anser_btn = (ImageButton)findViewById(R.id.btn_answer);
                    anser_btn.setVisibility(View.GONE);
                    voiceSourceCar_Btn.setVisibility(View.VISIBLE);
                    callstatus_TV.setText(R.string.str_bt_call_active);
                    dissmissDialog();
                    updateCallInfo(mActiveCall);
                }else if(state == BluetoothHeadsetClientCall.CALL_STATE_WAITING){
                    mwaitCall = changedCall;
                    showNewComingCallDialog(changedCall);
                    Utils.showShortToast(PhoneCallActivity.this,"A new Call come in");
                }
                
			}else if(action.equals(BluetoothHeadsetClient.ACTION_AUDIO_STATE_CHANGED)){
    			int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                    BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED);
    			Log.d(TAG,"audio state = "+ state);
    			if(state == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED){
                     boolean wbsSupported = 
                        intent.getBooleanExtra(BluetoothHeadsetClient.EXTRA_AUDIO_WBS, false);
                    if(DEBUG) Log.d(TAG,"wbsSupported ="+wbsSupported);
        			phoneCallShowVoiceSource(true);
    			}else if(state == BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED){
        			phoneCallShowVoiceSource(false);
    			}

			}else if(action.equals(BluetoothHeadsetClient.ACTION_CONNECTION_STATE_CHANGED)){
				int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 
    				BluetoothProfile.STATE_DISCONNECTED);
                if(DEBUG) Log.d(TAG,"state = "+ state);
                if(state != BluetoothProfile.STATE_CONNECTED){
					DestroyActivity();
				}

			}else if(action.equals(BluetoothHeadsetClient.ACTION_RESULT)){
				int result = intent.getIntExtra(BluetoothHeadsetClient.EXTRA_RESULT_CODE,
					BluetoothHeadsetClient.ACTION_RESULT_ERROR);
				if(DEBUG)Log.d(TAG,"result ="+result);
				if(result != BluetoothHeadsetClient.ACTION_RESULT_OK){
					DestroyActivity();
				}
				
			}else if(action.equals(LocalBluetoothProfileManager.ACTION_PROFILE_UPDATE)){
                mLocalManager = LocalBluetoothManager.getInstance(PhoneCallActivity.this);
                mHeadsetClient = 
                mLocalManager.getProfileManager().getHeadsetClientProfile().getHeadsetClientServer();
                if(mHeadsetClient != null && mConnectedDevice == null){
                    mConnectedDevice = mHeadsetClient.getConnectedDevices().get(0);
                    initPBAP();
                }

			}else if(action.equals(ACTION_TEL)){
				handleTEL();
			}else if(action.equals(ACTION_TEL_ANSWER)){
				handleTELANSWER();
			}else if(action.equals(ACTION_TEL_HANDUP)){
				handleTELHANDUP();
			}else if(action.equals(ACTION_ANSWER)){
			     handleTELANSWER();
			}else if(action.equals(ACTION_HANGUP)){
			     handleTELHANDUP();
			}

		}
	};
	//add by cxs
	private void handleTEL(){
		if(mHeadsetClient == null){
            Log.e(TAG,"mHeadsetClient is null");
            return;
        }
		if(bIsOnCalling){
			handleTELHANDUP();
		}else{
			handleTELANSWER();
		}
	}
	
	private void handleTELANSWER(){
		 if(mHeadsetClient == null){
             Log.e(TAG,"mHeadsetClient is null");
             return;
         }
		 
		if(mHeadsetClient.acceptCall(mConnectedDevice,0)){
			bIsOnCalling = true;
			bIsVoiceOut = true;
                            voiceSourceCar_Btn.setVisibility(View.VISIBLE);
		}
        if(bIsOnCalling){
            findViewById(R.id.btn_answer).setVisibility(View.GONE);
        }
	}
	
     private void handleTELHANDUP(){
    	 if(mHeadsetClient == null){
             Log.e(TAG,"mHeadsetClient is null");
             return;
         }
    	 if((callfrom == 1)&& !bIsOnCalling){// reject call
 			Log.d(TAG,"rejectCall mConnectedDevice = "+mConnectedDevice);
				if(mHeadsetClient.rejectCall(mConnectedDevice)){
					bIsOnCalling = false;
                 Log.d(TAG,"rejectCall incoming OK");
                 DestroyActivity();
				}else{
 				Log.e(TAG,"rejectCall failed");
             }
			}else{
				if(mHeadsetClient.terminateCall(mConnectedDevice,0)){
					bIsOnCalling = false;
                 Log.d(TAG,"terminateCall callout OK");
                 DestroyActivity();
				}else{
 				Log.e(TAG,"terminateCall failed");
             }
			}
	}

	
	public boolean addSubPhoneCallInputString(CharSequence str) {
		
		int index = m_subcallnumber_et.getSelectionStart();
		m_subcallnumstr_edt = m_subcallnumber_et.getEditableText();
		if(index < 0 || index >m_subcallnumstr_edt.length()){
			m_subcallnumstr_edt.append(str);
		}else {
			m_subcallnumstr_edt.insert(index ,str);
		}
		m_subcallnumber_et.setText(m_subcallnumstr_edt);
		m_subcallnumber_et.setSelection(index+1);

		return true;
	}
	
	
	private void DestroyActivity(){
		if(DEBUG) Log.i(TAG, "Destroy Activity ");
        finish();
		unregisterCallback();
        try{
           if(mBroadcastReceiver != null) {
              this.unregisterReceiver(mBroadcastReceiver);
            }
        }catch(IllegalArgumentException e){
           Log.e("TAG","IllegalArgumentException");
        }
	 }
	
	private void phoneCallShowSoftkeyPad(boolean fgShow){
		Drawable drawable;
		Resources res = getResources();
		final LinearLayout softkeyPadLayout = (LinearLayout)findViewById(R.id.calling_softkeypad);
		if(fgShow){
			if(DEBUG) Log.e(TAG, "Show SoftkeyPad ");
			//drawable = res.getDrawable(R.drawable.bt_call_bg_big);
			softkeyPadLayout.setVisibility(View.VISIBLE);
			isSoftkeyPadVisible = true;
		
		}else{
			if(DEBUG) Log.e(TAG, "hide SoftkeyPad");
			//drawable = res.getDrawable(R.drawable.bt_call_bg_small);
			softkeyPadLayout.setVisibility(View.GONE);
			isSoftkeyPadVisible = false ;	 
		}
		//this.getWindow().setBackgroundDrawable(drawable);
	}
	
	
	private void phoneCallShowVoiceSource(boolean connected){
		
        if(DEBUG) Log.d(TAG, "connected == "+connected);
		if(!connected){			
			voiceSourceCar_Btn.setImageResource(R.drawable.ic_speaker_phone);
		
		}else{	
			voiceSourceCar_Btn.setImageResource(R.drawable.ic_speaker_mirror);
		}
        voiceSourceCar_Btn.setEnabled(true);
	}	
	
	public void onClick(View v) {
		String dtmf_code = null;
		Byte dtmf = null;
		boolean bsenddtmf = false;
		if(mHeadsetClient == null){
            Log.e(TAG,"mHeadsetClient is null");
            return;
        }
		switch (v.getId()) {
		case R.id.btn_hangup:
			Log.d(TAG,"+++onClick+++hangup");
			if((callfrom == 1)&& !bIsOnCalling){// reject call
    			Log.d(TAG,"rejectCall mConnectedDevice = "+mConnectedDevice);
				if(mHeadsetClient.rejectCall(mConnectedDevice)){
					bIsOnCalling = false;
                    Log.d(TAG,"rejectCall incoming OK");
                    DestroyActivity();
				}else{
    				Log.e(TAG,"rejectCall failed");
                }
			}else{
				if(mHeadsetClient.terminateCall(mConnectedDevice,0)){
					bIsOnCalling = false;
                    Log.d(TAG,"terminateCall callout OK");
                    DestroyActivity();
				}else{
    				Log.e(TAG,"terminateCall failed");
                }
			}
			break;
			
		case R.id.btn_answer:
            Log.d(TAG,"+++onClick+++answer");
			if(mHeadsetClient.acceptCall(mConnectedDevice,0)){
				bIsOnCalling = true;
				bIsVoiceOut = true;
                                voiceSourceCar_Btn.setVisibility(View.VISIBLE);
			}
            if(bIsOnCalling){
                v.setVisibility(View.GONE);
            }
			break;
			
		case R.id.btn_softkeypad:
            Log.d(TAG,"+++onClick+++softkeypad");
			phoneCallShowSoftkeyPad(!isSoftkeyPadVisible);
			break;	
		
		case R.id.btn_voiceswitch_car:
            Log.d(TAG,"+++onClick+++voiceswitch");
            v.setEnabled(false);	       
			int audioState = mHeadsetClient.getAudioState(mConnectedDevice);
            if (audioState == BluetoothHeadsetClient.STATE_AUDIO_DISCONNECTED) {
				mHeadsetClient.connectAudio();
		    } else if (audioState == BluetoothHeadsetClient.STATE_AUDIO_CONNECTED) {
     		    mHeadsetClient.disconnectAudio();
		    }
			break;				
		case R.id.btn_calling_zero:
			addSubPhoneCallInputString("0");
			dtmf_code = "0" ;
			bsenddtmf = true ;
			break;
			
		case R.id.btn_calling_one:
			addSubPhoneCallInputString("1");
			dtmf_code = "1" ;
			bsenddtmf = true ;
			break;
			
		case R.id.btn_calling_two:
			addSubPhoneCallInputString("2");
			dtmf_code = "2" ;
			bsenddtmf = true ;
			break;
		
		case R.id.btn_calling_three:
			addSubPhoneCallInputString("3");
			dtmf_code = "3" ;
			bsenddtmf = true ;
			break;	
			
		case R.id.btn_calling_four:
			addSubPhoneCallInputString("4");
			dtmf_code = "4" ;
			bsenddtmf = true ;
			break;
		
		case R.id.btn_calling_five:
			addSubPhoneCallInputString("5");
			dtmf_code = "5" ;
			bsenddtmf = true ;
			break;	
			
		case R.id.btn_calling_six:	
			addSubPhoneCallInputString("6");
			dtmf_code = "6" ;
			bsenddtmf = true ;
			break;
			
		case R.id.btn_calling_seven:
			addSubPhoneCallInputString("7");
			dtmf_code = "7" ;
			bsenddtmf = true ;
			break;
		
		case R.id.btn_calling_eight:
			addSubPhoneCallInputString("8");
			dtmf_code = "8" ;
			bsenddtmf = true ;
			break;	
			
		case R.id.btn_calling_nine:	
			addSubPhoneCallInputString("9");
			dtmf_code = "9" ;
			bsenddtmf = true ;
			break;	
					
		case R.id.btn_calling_asterisk:
			addSubPhoneCallInputString("*");
			dtmf_code = "*" ;
			bsenddtmf = true ;
			break;	
			
		case R.id.btn_calling_pound:	
			addSubPhoneCallInputString("#");
			dtmf_code = "#" ;
			bsenddtmf = true ;
			break;		
			
		default:
			break;
		}
		
		if(bsenddtmf  && (dtmf_code != null) && (mHeadsetClient != null)){
			byte[] inputBytes = dtmf_code.getBytes();
			dtmf = new Byte(inputBytes[0]);
			mHeadsetClient.sendDTMF(mConnectedDevice,dtmf);
		}
		
	}

	private void initViews(){

		/* three way call  */
		((ImageButton) findViewById(R.id.btn_hangup)).setOnClickListener(this);
		((ImageButton) findViewById(R.id.btn_softkeypad)).setOnClickListener(this);
		
		((Button) findViewById(R.id.btn_calling_zero)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_one)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_two)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_three)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_four)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_five)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_six)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_seven)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_eight)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_nine)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_asterisk)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_calling_pound)).setOnClickListener(this);
                voiceSourceCar_Btn = (ImageButton)findViewById(R.id.btn_voiceswitch_car);
                voiceSourceCar_Btn.setOnClickListener(this);        
        callstatus_TV = (TextView)findViewById(R.id.bt_calling_status_tv);
		phonenumber_TV = (TextView)findViewById(R.id.bt_calling_phone_number);
		m_subcallnumber_et = (EditText)findViewById(R.id.calling_input_et);
		/* hide system input keyboard */
		m_subcallnumber_et.setInputType(InputType.TYPE_NULL);
		m_subcallnumber_et.setText("");
	}

	@Override
	public boolean onKeyDown(int keyCode,KeyEvent event){
		//int i = getCurrentRingValue();
		switch(keyCode){

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			mAudiomanager.adjustStreamVolume(
				AudioManager.STREAM_BLUETOOTH_SCO,
				AudioManager.ADJUST_LOWER,
				AudioManager.FLAG_SHOW_UI);
			break;
		case KeyEvent.KEYCODE_VOLUME_UP:
			mAudiomanager.adjustStreamVolume(
				AudioManager.STREAM_BLUETOOTH_SCO,
				AudioManager.ADJUST_RAISE,
				AudioManager.FLAG_SHOW_UI);
			break;
		case KeyEvent.KEYCODE_BACK:
			 moveTaskToBack(true);
			 break;
		}
		return true;
	}


	private BluetoothPabapClientCallback mCallback = new BluetoothPabapClientCallback() {

        @Override
        public void onSetPathDone(boolean success) {
            if(DEBUG)Log.d(TAG, "onSetPathDone = " + success);
			if(success){
				checkFolder(mTargetFolder);
			}else{
    			Log.d(TAG,"onSetPathDone  result=="+success);
				Utils.showShortToast(PhoneCallActivity.this,"check folder failed");
			}
            
        }

        @Override
        public void onPullBookDone(boolean success, int newMissedCalls,
	        ArrayList<VCardEntry> list) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onPullVcardListingDone(boolean success, int newMissedCalls,
                ArrayList<BluetoothPbapCard> list) {
			if(success){
				if(list !=null && list.size()>0){
					m_strPhoneName = list.get(0).N;
					if(DEBUG)Log.d(TAG,"list.size = "+list.size());
					
					((Activity)PhoneCallActivity.this).runOnUiThread(new Runnable(){  
	                	  
	                    @Override  
	                    public void run() {  
	                     
	                    	phonenumber_TV.setText(m_strPhoneName);
	                    }  
	                      
	                });
					
				}else{
					if(mTargetFolder.equals(SIM_PB_PATH)){
						Utils.showShortToast(PhoneCallActivity.this,
							"There is no matched contacts");
					}else{
						Utils.showShortToast(PhoneCallActivity.this,"search SIM");
						if(DEBUG)Log.d(TAG,"pb is null,start sim");						
						if(m_strPhoneNum != null){
							if(DEBUG)Log.d(TAG,"start search from sim");
                            mTargetFolder = SIM_PB_PATH;
    						checkFolder(mTargetFolder);							
						}
					}
					
				}
				
			}else{
    			Log.d(TAG,"onPullVcardListingDone  result = "+success);
				Utils.showShortToast(PhoneCallActivity.this,"search failed");
			}
            
        }

        @Override
        public void onPullVcardEntryDone(boolean success, VCardEntry entry) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onPullPhonebookSizeDone(boolean success, int size) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onPullVcardListingSizeDone(boolean success, int size) {
           
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
           if(DEBUG) Log.d(TAG, "pbapConnectionChanged "+ connected);
            int state = BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED;
            if (mManager != null) {
                state = mManager.getConnectState();
            }
            if (state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED) {
                
            }
        }

    };

	private boolean pullVcardList(String pbName,String searchVal){

		if (searchVal.isEmpty()) {
            searchVal = null;
        }
        if (mManager.getCurrentPath().length() > 3) {
            pbName = null;
        } else {
            pbName = mTargetFolder+".vcf" ;
        }
        byte order = BluetoothPbapClient.ORDER_BY_ALPHABETICAL;
		if(DEBUG) Log.d(TAG,"pullVcardList pbName ="+pbName+" ,searchVal="+searchVal);
		return mManager.pullVcardList(pbName, order, MODE_INPUT_NUMBER, searchVal, 
				MAX_LIST_COUNT, 0);
		

	}

	private synchronized void checkFolder(String targetFolder){
		 String currentFolder = mManager.getCurrentPath();
		if(DEBUG) Log.d(TAG,"targetFolder + currentFolder"+currentFolder+","+targetFolder);
			if (targetFolder == null) {
                return;				
			} else if (targetFolder.equals(currentFolder)) {
				if(DEBUG)Log.d(TAG, "[OK] mTargetFolder matched");
                if(targetFolder == PB_PATH){
                    Message msg_pb = mHandler.obtainMessage(MESSAGE_PULL_PB_VCARDLIST,
        				HANDLER_DELAY);
        			mHandler.sendMessage(msg_pb);

                }else{
                    Message msg_sim = mHandler.obtainMessage(MESSAGE_PULL_PB_VCARDLIST,
								HANDLER_DELAY);
							mHandler.sendMessage(msg_sim);
                }         
                
			} else {
				if (targetFolder.startsWith(currentFolder) || currentFolder.isEmpty()) {
                    Log.d(TAG,"checkFolder  setPath");
					String nextFolder = targetFolder.substring(currentFolder.length());
					if (nextFolder.startsWith("/")) {
						nextFolder = nextFolder.substring(1);
					}
					String[] folders = nextFolder.split("/");
					nextFolder = folders[0];
					if (!mManager.setPhoneBookFolderDown(nextFolder)) {
						if(DEBUG)Log.d(TAG, "setPhoneBookFolderRoot fail");
					}
				} else {
					if (!mManager.setPhoneBookFolderRoot()) {
						if(DEBUG)Log.d(TAG, "setPhoneBookFolderRoot fail, reset it");		
					}
				}
			}
	}

	private void initPBAP(){
        Log.d(TAG,"initPBAP()");
        mManager = BluetoothPbapClientManager.getInstance();
        Log.d(TAG,"mConnectedDevice = "+mConnectedDevice);
        if(mManager == null || (mConnectedDevice == null)){
            Log.d(TAG,"BluetoothPbapClientManager is null ");
            return;
        }
        int state = mManager.getConnectState();
        Log.d(TAG,"state == "+state);
		if(state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTED ||
             state == BluetoothPbapClientConstants.CONNECTION_STATE_DISCONNECTING ||
             !( mManager.getDevice().getAddress().equals(mConnectedDevice.getAddress()))){
			 mManager.initConnect(mConnectedDevice);
			 mManager.connectDevice();
			   
		}
        mManager.registerCallback(mCallback);
        if(m_strPhoneNum != null){
            mTargetFolder = PB_PATH;
            checkFolder(mTargetFolder);			
    	}	
		
	}

	private void unregisterCallback(){
		if(mManager != null){
			mManager.unregisterCallback(mCallback);
		}
	}

    private void showNewComingCallDialog(final BluetoothHeadsetClientCall newCall){
        if(dialog != null){
            dialog.dismiss();
        }
	Utils.wakeUpAndUnlock(this);//wake up screen when new call coming in
        dialog = new AlertDialog.Builder(this)
                    /*.setPositiveButton(R.string.dialog_accept, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            if(newCall.isMultiParty()){
                                mHeadsetClient.acceptCall(mConnectedDevice,0);
                            }else{
                                mHeadsetClient.rejectCall(mConnectedDevice);
                                Utils.showShortToast(PhoneCallActivity.this,"Don't support multiParty");

                            }
                            if (dialog != null) {
                                dialog = null;
                            }
                        }

                    })*/
                    .setNegativeButton(R.string.dialog_reject, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            mHeadsetClient.rejectCall(mConnectedDevice);
                            if (dialog != null) {
                                dialog = null;
                            }
                        }

                    }).create();
        dialog.setTitle(R.string.call_wait);
        dialog.setMessage(newCall.getNumber());
        dialog.show();
        Utils.showShortToast(PhoneCallActivity.this,"This demo not support multparty call,so reject it please");

    }

    private void dissmissDialog(){
        if(dialog != null){
            dialog.dismiss();
            dialog = null;
            mwaitCall = null;
        }
    }

    private void updateCallInfo(BluetoothHeadsetClientCall comingCall){
        m_strPhoneNum= comingCall.getNumber();
        if(!m_strPhoneNum.isEmpty()){
			if(DEBUG) Log.i(TAG, "phone number is " + m_strPhoneNum);
			if(m_strPhoneNum.equals(UNKOWN_PHONE_NUMBER)!=true){
				updatePhoneNumberDisplay(true,m_strPhoneNum);
			}
		}
        if(m_strPhoneNum != null){
            mTargetFolder = PB_PATH;
            checkFolder(mTargetFolder);			
    	}

    }
    
	
}
