package cn.lzl.soundeffect;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import cn.colink.serialport.service.ISerialPortCallback;
import cn.colink.serialport.service.ISerialPortService;
import cn.lzl.soundeffect.utils.BytesUtil;
import cn.lzl.soundeffect.utils.Contacts;
import cn.lzl.soundeffect.utils.Trace;
import cn.lzl.soundeffect.view.CarSelectedView;
import cn.lzl.soundeffect.view.CustomImgView;
import cn.lzl.soundeffect.view.MainTitleView;
import cn.lzl.soundeffect.view.VolumnSelectedView;

public class MainActivity extends BaseActivity implements OnClickListener {
    
    private ISerialPortService mISpService;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case Contacts.MSG_UPDATA_UI:
                    byte[] packet = (byte[]) msg.obj;
                    receiveMsg(packet);
                    break;
                case Contacts.MSG_UPDATE_DATE_AND_TIME:
                    updateDateAndTime();
                    break;
                default:
                    break;
            }
        }
    };

    private ISerialPortCallback mICallback = new ISerialPortCallback.Stub() {

        @Override
        public void readDataFromServer(byte[] bytes) throws RemoteException {
            Message msg = new Message();
            msg.what = Contacts.MSG_UPDATA_UI;
            msg.obj = bytes;
            mHandler.sendMessage(msg);
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Trace.i("onServiceConnected");
            mISpService = ISerialPortService.Stub.asInterface(service);
            try {
                mISpService.addClient(mICallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            sendMsg(Contacts.HEX_RESET_BACK_TIME);
            sendMsg(Contacts.HEX_HOME_TO_SOUND);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mISpService = null;
        }
    };
    
    private void bindSpService() {
        try {
            Intent intent = new Intent();
            intent.setClassName("cn.colink.serialport",
                    "cn.colink.serialport.service.SerialPortService");
            bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unbindSpService() {
        try {
            if(mISpService != null)
                mISpService.removeCliend(mICallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MainTitleView mMainTitleView;

    private Button mMainTreVolumnAddBtn;

    private Button mMainTreVolumnSubBtn;

    private VolumnSelectedView mMainTreVolumnSelected;

    private CustomImgView mMainTreValue;

    private Button mMainBasVolumnAddBtn;

    private Button mMainBasVolumnSubBtn;

    private VolumnSelectedView mMainBasVolumnSelected;

    private CustomImgView mMainBasValue;

    private Button mMainCarUp;

    private Button mMainCarDown;

    private Button mMainCarLeft;

    private Button mMainCarRight;

    private CarSelectedView mMainCarImg;

    private Button mMainBtnReset;

    private Button mMainBtnYaoGun;

    private Button mMainBtnLiuXin;

    private Button mMainBtnJieShi;

    private Button mMainBtnJinDian;
    
    private boolean isHavedReponse = false;
    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.i("onCreate");
        this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED, FLAG_HOMEKEY_DISPATCHED);
        super.onCreate(savedInstanceState);
        bindSpService();
    }
    
    @Override
    protected void onResume() {
        Trace.i("onResume");
        super.onResume();
    }
    
    @Override
    protected void onPause() {
        Trace.i("onPause");
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        Trace.i("onDestroy");
        unbindSpService();
        super.onDestroy();
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void initializeView() {
        mMainTitleView = (MainTitleView)findViewById(R.id.mMainTitleView);
        mMainTreVolumnAddBtn = (Button)findViewById(R.id.mMainTreVolumnAddBtn);
        mMainTreVolumnSubBtn = (Button)findViewById(R.id.mMainTreVolumnSubBtn);
        mMainTreVolumnSelected = (VolumnSelectedView)findViewById(R.id.mMainTreVolumnSelected);
        mMainTreValue = (CustomImgView)findViewById(R.id.mMainTreValue);
        
        mMainTreVolumnAddBtn.setOnClickListener(this);
        mMainTreVolumnSubBtn.setOnClickListener(this);
        
        mMainBasVolumnAddBtn = (Button)findViewById(R.id.mMainBasVolumnAddBtn);
        mMainBasVolumnSubBtn = (Button)findViewById(R.id.mMainBasVolumnSubBtn);
        mMainBasVolumnSelected = (VolumnSelectedView)findViewById(R.id.mMainBasVolumnSelected);
        mMainBasValue = (CustomImgView)findViewById(R.id.mMainBasValue);
        
        mMainBasVolumnAddBtn.setOnClickListener(this);
        mMainBasVolumnSubBtn.setOnClickListener(this);
        
        mMainCarUp = (Button)findViewById(R.id.mMainCarUp);
        mMainCarDown = (Button)findViewById(R.id.mMainCarDown);
        mMainCarLeft = (Button)findViewById(R.id.mMainCarLeft);
        mMainCarRight = (Button)findViewById(R.id.mMainCarRight);
        mMainBtnReset = (Button)findViewById(R.id.mMainBtnReset);
        
        mMainBtnYaoGun = (Button)findViewById(R.id.mMainBtnYaoGun);
        mMainBtnLiuXin = (Button)findViewById(R.id.mMainBtnLiuXin);
        mMainBtnJieShi = (Button)findViewById(R.id.mMainBtnJieShi);
        mMainBtnJinDian = (Button)findViewById(R.id.mMainBtnJinDian);
        
        mMainCarUp.setOnClickListener(this);
        mMainCarDown.setOnClickListener(this);
        mMainCarLeft.setOnClickListener(this);
        mMainCarRight.setOnClickListener(this);
        mMainBtnReset.setOnClickListener(this);
        
        mMainBtnYaoGun.setOnClickListener(this);
        mMainBtnLiuXin.setOnClickListener(this);
        mMainBtnJieShi.setOnClickListener(this);
        mMainBtnJinDian.setOnClickListener(this);
        
        mMainCarImg = (CarSelectedView)findViewById(R.id.mMainCarImg);
        
    }

    @Override
    protected void initializeData() {
        updateDateAndTime();
    }
    
    protected void receiveMsg(byte[] packet) {
        if(!isHavedReponse && packet != null){
            if(packet[0] == Contacts.SWITCH_MODE)
               isHavedReponse = true;
        }
        if(!isHavedReponse)return;
        if(packet == null)return;
        if(packet[0] != Contacts.SYSTEM_INFO)return;
        switch (packet[1]) {
            case 0x01:
                Trace.i("bas value : " + ((int) packet[4] & 0xFF));
                setBasVolumnValue(((int) packet[4] & 0xFF));
                break;
            case 0x02:
                Trace.i("tre value : " + ((int) packet[4] & 0xFF));
                setTreVolumnValue(((int) packet[4] & 0xFF));
                break;
            case 0x03:
                Trace.i("column value : " + ((int) packet[4] & 0xFF));
                setColumnIndex(((int) packet[4] & 0xFF));
                break;
            case 0x04:
                Trace.i("row value : " + ((int) packet[4] & 0xFF));
                setRowIndex(((int) packet[4] & 0xFF));
                break;
            default:
                break;
        }
    }
    
    public void sendMsg(String msg) {
        try {
            if (mISpService != null) {
                Trace.i("Sound MainActivity sendMsg");
                mISpService.sendDataToSp(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    protected void updateDateAndTime() {
        mMainTitleView.updateDateAndTime();
        mHandler.sendEmptyMessageDelayed(Contacts.MSG_UPDATE_DATE_AND_TIME, 60 * 1000);
    }

    @Override
    public void onClick(View v) {
        sendMsg(Contacts.HEX_RESET_BACK_TIME);
        switch (v.getId()) {
            case R.id.mMainTreVolumnAddBtn:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_TRE_ADD);
                break;
            case R.id.mMainTreVolumnSubBtn:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_TRE_SUB);
                break;
            case R.id.mMainBasVolumnAddBtn:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_BAS_ADD);
                break;
            case R.id.mMainBasVolumnSubBtn:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_BAS_SUB);
                break;
            case R.id.mMainCarUp:
                sendMsg(Contacts.HEX_SOUND_CAR_UP);
                break;
            case R.id.mMainCarDown:
                sendMsg(Contacts.HEX_SOUND_CAR_DOWN);
                break;
            case R.id.mMainCarLeft:
                sendMsg(Contacts.HEX_SOUND_CAR_LEFT);
                break;
            case R.id.mMainCarRight:
                sendMsg(Contacts.HEX_SOUND_CAR_RIGHT);
                break;
            case R.id.mMainBtnReset:
                sendMsg(Contacts.HEX_SOUND_CAR_RESET);
                break;
            case R.id.mMainBtnYaoGun:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_YAOGUN);
                mMainBtnYaoGun.setSelected(true);
                break;
            case R.id.mMainBtnLiuXin:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_LIUXING);
                mMainBtnLiuXin.setSelected(true);
                break;
            case R.id.mMainBtnJieShi:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_JIESHI);
                mMainBtnJieShi.setSelected(true);
                break;
            case R.id.mMainBtnJinDian:
                clearBtnSelected();
                sendMsg(Contacts.HEX_SOUND_JINDIAN);
                mMainBtnJinDian.setSelected(true);
                break;
            default:
                break;
        }
    }
    
    private void clearBtnSelected(){
        mMainBtnYaoGun.setSelected(false);
        mMainBtnLiuXin.setSelected(false);
        mMainBtnJinDian.setSelected(false);
        mMainBtnJieShi.setSelected(false);
    }
    
    private void setTreVolumnValue(int treValue){
        mMainTreVolumnSelected.setIndex(treValue);
        mMainTreValue.setIndex(treValue);
    }
    
    private void setBasVolumnValue(int basValue){
        mMainBasVolumnSelected.setIndex(basValue);
        mMainBasValue.setIndex(basValue);
    }
    
    private void setRowIndex(int index){
        mMainCarImg.setRowIndex(index);
    }
    
    private void setColumnIndex(int index){
        mMainCarImg.setColumnIndex(index);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Trace.i("keyCode :" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                Trace.i("home press");
                sendMsg(Contacts.HEX_SOUND_TO_HOME);
                finish();
                break;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        sendMsg(Contacts.HEX_SOUND_TO_HOME);
        finish();
    }
}
