package com.mtk.bluetooth;

import java.util.List;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAvrcp;
import android.bluetooth.BluetoothAvrcpController;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Button;
import android.view.ViewConfiguration;
import android.widget.TextView;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import com.mtk.bluetooth.common.CachedBluetoothDeviceManager;
import com.mtk.bluetooth.common.LocalBluetoothManager;
import com.mtk.bluetooth.common.LocalBluetoothProfileManager;
import android.bluetooth.BluetoothA2dpSink;
import com.mediatek.bluetooth.BluetoothProfileManager;
import com.mediatek.bluetooth.BluetoothProfileManager.Profile;
import com.mtk.bluetooth.R;
import com.mtk.bluetooth.util.Utils;
import android.media.AudioManager;

import android.util.Log;

public class MusicFragment extends Fragment/* implements View.OnClickListener*/ {

    protected static final String TAG = "MusicFragment";
    Context mContext;

    public MusicFragment() {
    }

    private boolean mIsA2dpAvrcpConnected = true;

    int mA2dpState = BluetoothProfileManager.STATE_DISCONNECTED;
    int mAvrcpState = BluetoothProfileManager.STATE_DISABLED;

    private TextView a2dpsinkStateInfo;
    private TextView avrcpctStateInfo;

    ImageButton mStopButton;
    ImageButton mPlayPauseButton;
    ImageButton mPrevButton;
    ImageButton mNextButton;
    ImageButton mFastbackButton;
    ImageButton mFastspeedButton;

    Object obj = new Object();

    private boolean mIsPlay ;
    private int mTouchSlop ;

    private BluetoothAvrcpController mBtControllerService;
    private BluetoothA2dpSink mService;

    private BluetoothDevice connectedDevice = null;
	private LocalBluetoothProfileManager mProfileManager = null;
    private boolean mIsAVRCPControllerConnected = false;
    private AudioManager mAudio;

    private final BluetoothProfile.ServiceListener mAvrcpServiceListener = 
					new BluetoothProfile.ServiceListener() {
		
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "Bluetooth service connected");
            mBtControllerService = (BluetoothAvrcpController) proxy;
            Log.d(TAG, "====get BluetoothAvrcpController====");
            mBtControllerService.getConnectedDevices();
            updatePlayState();
            Log.d(TAG,"connected devices size is :"+mBtControllerService.getConnectedDevices().size());
            if (mBtControllerService.getConnectedDevices().size()>0) {
                connectedDevice = mBtControllerService.getConnectedDevices().get(0);
                mIsAVRCPControllerConnected = true;
				Log.d(TAG,"connectedDevice =="+connectedDevice);
                updateState(BluetoothProfileManager.Profile.AVRCP,
    				BluetoothProfileManager.STATE_CONNECTED);
            }
        }

        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "Bluetooth service disconnected");
            mIsAVRCPControllerConnected = false;
            mBtControllerService = null;
            updateState(BluetoothProfileManager.Profile.AVRCP,
				BluetoothProfileManager.STATE_DISCONNECTED);
        }
    };

    private final BluetoothProfile.ServiceListener A2dpServiceListener = 
            new BluetoothProfile.ServiceListener() {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "proxy is : " + proxy.toString());
            mService = (BluetoothA2dpSink) proxy;	
            if(mService.getConnectedDevices().size()>0){
                connectedDevice = mService.getConnectedDevices().get(0);
                updateState(BluetoothProfileManager.Profile.A2DP,
                    BluetoothProfileManager.STATE_CONNECTED);
              }
            
        }

        public void onServiceDisconnected(int profile) {
			Log.d(TAG, "A2dp disconnected profile is : " + profile);
            updateState(BluetoothProfileManager.Profile.A2DP,
                    BluetoothProfileManager.STATE_DISCONNECTED);
            connectedDevice = null;         
           
        }
    };

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {            
            adapter.getProfileProxy(getActivity(), A2dpServiceListener,
                    BluetoothProfile.A2DP_SINK);
            adapter.getProfileProxy(getActivity(), mAvrcpServiceListener,
                    BluetoothProfile.AVRCP_CONTROLLER);
			
        }                
        mTouchSlop = ViewConfiguration.get(this.getActivity()).getScaledTouchSlop();
        Log.d(TAG,"++++onCreate+++");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
								    Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bt_music, container, false);
		Log.d(TAG,"++++onCreateView+++");
        mStopButton = (ImageButton) view.findViewById(R.id.btn_music_stop);
        mPlayPauseButton = (ImageButton) view.findViewById(R.id.btn_music_play_pause);
        mPrevButton = (ImageButton) view.findViewById(R.id.btn_music_prev);
        mNextButton = (ImageButton) view.findViewById(R.id.btn_music_next);
        mFastbackButton = (ImageButton) view.findViewById(R.id.btn_music_fastback);
        mFastspeedButton = (ImageButton) view.findViewById(R.id.btn_music_fastspeed);
        a2dpsinkStateInfo = (TextView) view.findViewById(R.id.tv_A2DP_status);
        avrcpctStateInfo = (TextView) view.findViewById(R.id.tv_AVRCP_status);
        mPlayPauseButton = (ImageButton) view.findViewById(R.id.btn_music_play_pause);
        
        IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED);
        intentFilter.addAction(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED);
		this.getActivity().registerReceiver(mPlaystateReceiver, intentFilter);
        if(mAudio == null){
            mAudio = new AudioManager(getActivity());
          }			
       Log.d(TAG,"isMusicActive =="+mAudio.isMusicActive());
	   if(mAudio.isMusicActive()){
			mIsPlay = true;
		}
       updatePlayState();
        initState();
        return view;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        try{
           if(mPlaystateReceiver != null) {
               this.getActivity().unregisterReceiver(mPlaystateReceiver);
            }
        }catch(IllegalArgumentException e){
           Log.e("TAG","IllegalArgumentException");
        }                
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.closeProfileProxy(BluetoothProfile.AVRCP_CONTROLLER,
                (BluetoothProfile)mBtControllerService);
            adapter.closeProfileProxy(BluetoothProfile.A2DP_SINK,
                (BluetoothProfile)mService); 
        } 
        super.onDestroy();
    }

    private void initState() {

		Log.d(TAG, "initState mStopButton = " + mStopButton);
        mStopButton.setOnTouchListener(mTouchListener);
        mPlayPauseButton.setOnTouchListener(mTouchListener);
        mPrevButton.setOnTouchListener(mTouchListener);
        mNextButton.setOnTouchListener(mTouchListener);
        mFastspeedButton.setOnTouchListener( mTouchListener);
        mFastbackButton.setOnTouchListener( mTouchListener);

        if (mA2dpState == BluetoothProfileManager.STATE_CONNECTED) {
            a2dpsinkStateInfo.setText(R.string.a2dpsink_status_connected_info);			
        } else {
            a2dpsinkStateInfo.setText(R.string.a2dpsink_status_notconnected_info);
        }
        if(mAvrcpState == BluetoothProfileManager.STATE_CONNECTED){
           avrcpctStateInfo.setText(R.string.a2dpsink_status_connected_info);
        }else{
           avrcpctStateInfo.setText(R.string.a2dpsink_status_notconnected_info);
        }
        updatePlayState();
    }

    public void updateState(Profile name, int state) {
		Log.d(TAG,"updateState name= "+name + "  state="+state);
        if (BluetoothProfileManager.Profile.A2DP.equals(name)) {
            mA2dpState = state;
            switch (state) {
            case BluetoothProfileManager.STATE_CONNECTED:
                a2dpsinkStateInfo.setText(R.string.a2dpsink_status_connected_info);
                break;
            case BluetoothProfileManager.STATE_DISCONNECTED:
                a2dpsinkStateInfo.setText(R.string.a2dpsink_status_notconnected_info);
                mPlayPauseButton.setImageResource(R.xml.bt_music_pause_play);
                break;
            default:
                break;
            }
        } else if (BluetoothProfileManager.Profile.AVRCP.equals(name)) {
            mAvrcpState = state;
            switch (state) {
            case BluetoothProfileManager.STATE_CONNECTED:
                avrcpctStateInfo.setText(R.string.a2dpsink_status_connected_info);                
                break;
            case BluetoothProfileManager.STATE_DISCONNECTED:
                 avrcpctStateInfo.setText(R.string.a2dpsink_status_notconnected_info);
                 mPlayPauseButton.setImageResource(R.xml.bt_music_pause_play);
                break;
            default:
                break;
            }
        }
        
    }


    private void sendCommand(int command,int comstate) {
        synchronized (obj) {
            if (mBtControllerService == null) {
                Log.d(TAG, "BluetoothAvrcpController is null, wait...");
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        mBtControllerService.sendPassThroughCmd(connectedDevice, command,
                comstate);
    }

    OnTouchListener mTouchListener = new  OnTouchListener(){
        int mLastX = -1;
        int movie_times = -1;
        public boolean onTouch(View v, MotionEvent event){
            if(connectedDevice == null || !mIsAVRCPControllerConnected){
                Log.d(TAG,"device is null or avrcp diconnected");
                return false;
            }            
            Log.d(TAG,"event.getAction() =="+event.getAction());
            if (v == mFastspeedButton) {
                if( event.getAction() == MotionEvent.ACTION_DOWN){
                    movie_times = 0;
                    mLastX = (int) event.getX();
                    mFastspeedButton.setImageResource(R.drawable.btnicon_fastspeed_2); 
                    Log.d(TAG,"mFastspeedButton ++ACTION_DOWN");
                    sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_FAST_FOR,
                        BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
                 }else if(event.getAction() == MotionEvent.ACTION_UP ||
                     event.getAction() == MotionEvent.ACTION_CANCEL){
                     mFastspeedButton.setImageResource(R.drawable.btnicon_fastspeed);
                     Log.d(TAG,"mFastspeedButton --ACTION_UP");
                     sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_FAST_FOR,
                        BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                 }else if(event.getAction() == MotionEvent.ACTION_MOVE){
                     Log.d(TAG,"mFastspeedButton --ACTION_MOVE");
                     int x1 = (int) event.getX();
                     int delta1 = mLastX - x1;
                     if(Math.abs(delta1)>mTouchSlop){
                        movie_times++;
                        Log.d(TAG,"mFastspeedButton --ACTION_MOVE++release mTouchSlop=="+mTouchSlop);
                        mFastspeedButton.setImageResource(R.drawable.btnicon_fastspeed);
                        if(movie_times < 2){
                            sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_FAST_FOR,
                                BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                           movie_times = 0;
                        }
                     }

                 }
            }else if(v == mFastbackButton){
                if( event.getAction() == MotionEvent.ACTION_DOWN){
                    movie_times = 0;
                    mLastX = (int) event.getX();
                    mFastbackButton.setImageResource(R.drawable.btnicon_fastback_2);
                    Log.d(TAG,"mFastbackButton ++ACTION_DOWN");
                    sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_REWIND,
                        BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
                 }else if(event.getAction() == MotionEvent.ACTION_UP ||
                     event.getAction() == MotionEvent.ACTION_CANCEL){
                      mFastbackButton.setImageResource(R.drawable.btnicon_fastback);
                     Log.d(TAG,"mFastbackButton --ACTION_UP");
                     sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_REWIND,
                        BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                 }else if(event.getAction() == MotionEvent.ACTION_MOVE){
                     int x2 = (int) event.getX();
                     int delta2 = mLastX - x2;
                     if(Math.abs(delta2)>mTouchSlop){
                        movie_times ++;
                        mFastbackButton.setImageResource(R.drawable.btnicon_fastback);
                        if(movie_times < 2){
                            sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_REWIND,
                                BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                            movie_times = 0;
                        }                        
                     }

                 }
            }else if(v == mPrevButton){
                if( event.getAction() == MotionEvent.ACTION_DOWN){
                    mPrevButton.setImageResource(R.drawable.btnicon_prev_2);
                    sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_BACKWARD,
                        BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
                 }else if(event.getAction() == MotionEvent.ACTION_UP ||
                     event.getAction() == MotionEvent.ACTION_CANCEL){
                     mPrevButton.setImageResource(R.drawable.btnicon_prev);
                    sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_BACKWARD,
                        BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                 }

            }else if(v == mNextButton){
                if( event.getAction() == MotionEvent.ACTION_DOWN){
                    mNextButton.setImageResource(R.drawable.btnicon_next_2);
                    sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_FORWARD,
                        BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
                 }else if(event.getAction() == MotionEvent.ACTION_UP ||
                     event.getAction() == MotionEvent.ACTION_CANCEL){
                     mNextButton.setImageResource(R.drawable.btnicon_next);
                     sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_FORWARD,
                        BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);                    
                 }

            }else if(v == mPlayPauseButton){
                if( event.getAction() == MotionEvent.ACTION_DOWN){
                    if(mIsPlay) {
                        Log.d(TAG,"pause && ACTION_DOWN=="+ BluetoothAvrcp.PASSTHROUGH_ID_PAUSE);                        
                        sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_PAUSE,
                            BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
                        sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_PAUSE,
                            BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                        mIsPlay = false;
                    } else {                        
                        Log.d(TAG,"play && ACTION_DOWN=="+ BluetoothAvrcp.PASSTHROUGH_ID_PLAY);
                        sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_PLAY,
                            BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
                        sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_PLAY,
                            BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                        mIsPlay = true;
                    }
                    updatePlayState();
                 }
            }else if(v == mStopButton){
                if( event.getAction() == MotionEvent.ACTION_DOWN){
                    Log.d(TAG,"stop && ACTION_DOWN");
                    mStopButton.setImageResource(R.drawable.btnicon_stop_2);
                    sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_STOP,
                        BluetoothAvrcp.PASSTHROUGH_STATE_PRESS);
                    mIsPlay = false;
                 }else if(event.getAction() == MotionEvent.ACTION_UP ||
                     event.getAction() == MotionEvent.ACTION_CANCEL){
                     Log.d(TAG,"stop && ACTION_UP");
                     mStopButton.setImageResource(R.drawable.btnicon_stop);
                     sendCommand(BluetoothAvrcp.PASSTHROUGH_ID_STOP,
                        BluetoothAvrcp.PASSTHROUGH_STATE_RELEASE);
                     updatePlayState();
                 }

            }
            return true;
        }

    };
	
	//update the play/pause button ui
	private BroadcastReceiver mPlaystateReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
            Log.d(TAG,"action-->"+action);
			if(action.equals(BluetoothA2dpSink.ACTION_PLAYING_STATE_CHANGED)){
	            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
					BluetoothA2dpSink.STATE_NOT_PLAYING);
				if(state == BluetoothA2dpSink.STATE_PLAYING){
					mIsPlay = true;
				}else{
					mIsPlay = false;
				}
                updatePlayState();
                
			}else if(action.equals(BluetoothA2dpSink.ACTION_CONNECTION_STATE_CHANGED)){
    			int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                        BluetoothA2dpSink.STATE_DISCONNECTED);
                BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(
					BluetoothDevice.EXTRA_DEVICE);
				if (state == BluetoothA2dpSink.STATE_CONNECTED){
                    connectedDevice = device;
                    updateState(
						BluetoothProfileManager.Profile.A2DP,
						BluetoothProfileManager.STATE_CONNECTED);
                }
                if (state == BluetoothA2dpSink.STATE_DISCONNECTED){
                    updateState(
						BluetoothProfileManager.Profile.A2DP,
						BluetoothProfileManager.STATE_DISCONNECTED);
                }

            }else if (action.equals(
				BluetoothAvrcpController.ACTION_CONNECTION_STATE_CHANGED)){
                BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(
					BluetoothDevice.EXTRA_DEVICE);
                int status = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_CONNECTING);
                Log.d(TAG, "Avrcp action device connect status is " + 
					status+"device ="+device);
                if (BluetoothProfile.STATE_CONNECTED == status) {
                    mIsAVRCPControllerConnected = true;
                    connectedDevice = device;
                    updateState(
						BluetoothProfileManager.Profile.AVRCP,
						BluetoothProfileManager.STATE_CONNECTED);
                }else{
                    mIsAVRCPControllerConnected = false;
                    updateState(
						BluetoothProfileManager.Profile.AVRCP,
						BluetoothProfileManager.STATE_DISCONNECTED);
				}
            }

		}
	};

    private void updatePlayState(){
        Log.d(TAG,"mIsPlay =="+mIsPlay);
        if (mIsPlay) {
            mPlayPauseButton.setImageResource(R.xml.bt_music_play_pause);
        } else {
            mPlayPauseButton.setImageResource(R.xml.bt_music_pause_play);
        }

    }
}
