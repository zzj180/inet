
package cn.colink.fm;

import android.R.integer;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import cn.colink.fm.baseadapter.BaseAdapterHelper;
import cn.colink.fm.baseadapter.QuickAdapter;
import cn.colink.fm.model.RadioData;
import cn.colink.fm.otto.BusProvider;
import cn.colink.fm.otto.RadioDataEvent;
import cn.colink.fm.utils.BytesUtil;
import cn.colink.fm.utils.Contacts;
import cn.colink.fm.utils.PreferenceUtil;
import cn.colink.fm.utils.ScreenUtils;
import cn.colink.fm.utils.Trace;
import cn.colink.fm.view.DisplayFqView;
import cn.colink.fm.view.FMTitleView;
import cn.colink.fm.view.FMTitleView.TitleIconClickListener;
import cn.colink.fm.view.FontTextView;
import cn.colink.fm.view.FreqIndicator;
import cn.colink.fm.view.FreqIndicator.OnSeekBarHintProgressChangeListener;
import cn.colink.fm.view.NumberPictureView;
import cn.colink.serialport.service.ISerialPortCallback;
import cn.colink.serialport.service.ISerialPortService;

@SuppressWarnings("unused")
public class MainActivity extends BaseActivity implements View.OnClickListener,
        View.OnLongClickListener, OnSeekBarHintProgressChangeListener,TitleIconClickListener{

    private DisplayFqView mMainMiddleDisplay;
    private ListView mMainMiddleLeftList;
    private ListView mMainMiddleRightList;
    private QuickAdapter<Bean> mLeftAdapter;
    private QuickAdapter<Bean> mRightAdapter;
    private List<Bean> mLeftDatas;
    private List<Bean> mRightDatas;
    private FreqIndicator mMainBottomFreq;
    private final int mStepSize = 10;
    private final int FM_START_FREQ = 8750;
    private final int FM_END_FREQ = 10800;
    private final int AM_START_FREQ = 522;
    private final int AM_END_FREQ = 1620;
    private int mCurrentFreq = 9500;
    private final int[] BAND_DRAWABLE_ID = new int[] {
            R.drawable.fm1, R.drawable.fm2, R.drawable.fm3, R.drawable.am1, R.drawable.am2
    };
    private final String[] HEX_ITEM_STRINGS = new String[] {
            Contacts.HEX_ITEM_FIRST, Contacts.HEX_ITEM_SECOND, Contacts.HEX_ITEM_THIRTH,
            Contacts.HEX_ITEM_FOUR, Contacts.HEX_ITEM_FIVE, Contacts.HEX_ITEM_SIXTH
    };
    private final int AM_UNIT = R.drawable.khz;
    private final int FM_UNIT = R.drawable.mhz;
    public static final String ACTION_APP_CANCEL = "action_fm_app_cancel";

    private DecimalFormat df2 = new DecimalFormat("###.00");
    private ISerialPortService mISpService;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case Contacts.MSG_UPDATA_UI:
                    byte[] packet = (byte[]) msg.obj;
                    radioDataAndUpdateUI(new RadioDataEvent(packet));
                    volumnData(packet);
                    break;
                case Contacts.MSG_UPDATE_DATE_AND_TIME:
                    updateDateAndTime();
                    break;
                case Contacts.MSG_HIDE_POPUP_VIEW:
                    mHandler.removeMessages(Contacts.MSG_HIDE_POPUP_VIEW);
                    hidePopup();
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
            sendMsg(Contacts.HEX_HOME_TO_FM);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mISpService = null;
        }
    };
    private FMTitleView mMainTitleView;
    private View mMainSearch;
    private View mMainSTOrMD;
    private View mMainBand;
    private View mMainPs;
    private View mMainPre;
    private View mMainNext;
    private PopupWindow mPopup;
    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000002;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.i("FM MainActivity onCreate");
        super.onCreate(savedInstanceState);
        getAudioManager();
        requestAudioFocus();
    }

    @Override
    protected void onResume() {
        Trace.i("FM MainActivity onResume");
        super.onResume();
        bindSerialPortService();
    }

    @Override
    protected void onPause() {
        Trace.i("FM MainActivity onPause");
        super.onPause();
        unBindSerialPortService();
        hidePopup();
    }

    @Override
    protected void onDestroy() {
        Trace.i("FM MainActivity onDestroy");
        sendBroadcast(new Intent(ACTION_APP_CANCEL));
        saveFqDataWhenOnDestroy();
        abandonAudioFocus();
        super.onDestroy();
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void initializeView() {
        mMainMiddleDisplay = (DisplayFqView) findViewById(R.id.mMainMiddleDisplay);
        mMainMiddleLeftList = (ListView) findViewById(R.id.mMainMiddleLeftList);
        mMainMiddleRightList = (ListView) findViewById(R.id.mMainMiddleRightList);
        mMainBottomFreq = (FreqIndicator) findViewById(R.id.mMainBottomFreq);
        mMainBottomFreq.setOnProgressChangeListener(this);

        mMainSearch = (View)findViewById(R.id.mMainSearch);
        mMainSearch.setOnClickListener(this);
        mMainSTOrMD = (View)findViewById(R.id.mMainSTOrMD);
        mMainSTOrMD.setOnClickListener(this);
        mMainBand = (View)findViewById(R.id.mMainBand);
        mMainBand.setOnClickListener(this);
        mMainPs = (View)findViewById(R.id.mMainPs);
        mMainPs.setOnClickListener(this);
        mMainPre = (View)findViewById(R.id.mMainPre);
        mMainPre.setOnClickListener(this);
        mMainPre.setOnLongClickListener(this);
        mMainNext = (View)findViewById(R.id.mMainNext);
        mMainNext.setOnClickListener(this);
        mMainNext.setOnLongClickListener(this);
        mMainTitleView = (FMTitleView)findViewById(R.id.mMainTitleView);
    }

    @Override
    protected void initializeData() {
        mLeftDatas = new ArrayList<>();
        mRightDatas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Bean leftbean = new Bean();
            leftbean.drawableId = R.drawable.khz;
            leftbean.channelId = i + 1;
            mLeftDatas.add(leftbean);
            Bean rightbean = new Bean();
            rightbean.drawableId = R.drawable.khz;
            rightbean.channelId = i + 4;
            mRightDatas.add(rightbean);
        }
        mLeftAdapter = new QuickAdapter<MainActivity.Bean>(MainActivity.this,
                R.layout.activity_main_middle_list_item, mLeftDatas) {

            @Override
            protected void convert(BaseAdapterHelper helper, Bean item) {
                helper.setImageResource(R.id.mItemUnitImg, item.drawableId);
                helper.setText(R.id.mItemChannelIdTv, ""+item.channelId);
                helper.setText(R.id.mItemFqTv,item.fq);
            }

        };

        mRightAdapter = new QuickAdapter<MainActivity.Bean>(MainActivity.this,
                R.layout.activity_main_middle_list_item, mRightDatas) {

            @Override
            protected void convert(BaseAdapterHelper helper, Bean item) {
                helper.setImageResource(R.id.mItemUnitImg, item.drawableId);
                helper.setText(R.id.mItemChannelIdTv, ""+item.channelId);
                helper.setText(R.id.mItemFqTv,item.fq);
            }

        };

        mMainMiddleLeftList.setAdapter(mLeftAdapter);
        mMainMiddleRightList.setAdapter(mRightAdapter);
        mMainMiddleLeftList.setOnItemClickListener(new LeftItemClick());
        mMainMiddleRightList.setOnItemClickListener(new RightItemClick());
        mMainBottomFreq.setMax(FM_END_FREQ - FM_START_FREQ);
        mMainBottomFreq.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Trace.i("smCurrentFreq : " + mCurrentFreq);
                String msg = getMsgString(mCurrentFreq, 1);
                sendMsg(msg);
                if (mCurrFlikView != null)
                    mCurrFlikView.clearAnimation();
                Trace.i("seekBar send msg : " + msg);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mCurrentFreq = (mMainBottomFreq.getFrequency()) / 10 * 10;
                if(isFM){
                    float fq = (float) mCurrentFreq / 100;
                    updateDisplayFreq(fq, false);
                }else{
                    updateDisplayFreq(mCurrentFreq);
                }
            }
        });
        
        if(PreferenceUtil.getSTOrMD(this) == 0){
            ((TextView) mMainSTOrMD).setText(R.string.st);
            mMainSTOrMD.setSelected(false);
        }else{
            ((TextView) mMainSTOrMD).setText(R.string.md);
            mMainSTOrMD.setSelected(true);
        }
        
        mMainTitleView.setListener(this);
        updateDateAndTime();
        initHintPopup();
        setFqDataWhenOnCreate();
    }

    private void bindSerialPortService() {
        Intent intent = new Intent();
        intent.setClassName("cn.colink.serialport",
                "cn.colink.serialport.service.SerialPortService");
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }
    
    private void unBindSerialPortService() {
        try {
            if(mISpService != null)
                mISpService.removeCliend(mICallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        unbindService(mServiceConnection);
    }
    
    class LeftItemClick implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            sendMsg(HEX_ITEM_STRINGS[position]);
        }

    }

    class RightItemClick implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            sendMsg(HEX_ITEM_STRINGS[position + 3]);
        }

    }

    private View mCurrFlikView;
    private int mCurrIndex;

    public void updateItemBg(ListView listView, int position) {
        int visiblePosition = listView.getFirstVisiblePosition();
        if (position - visiblePosition >= 0) {
            View view = listView.getChildAt(position - visiblePosition);
            mCurrFlikView = view.findViewById(R.id.mItemBg);
        }
    }

    public void updateFreqView(int progress) {
        mMainBottomFreq.setProgress(progress);
    }

    public void updateDisplayFreq(float fq, boolean isMin) {
        mMainMiddleDisplay.showFq(fq, isMin);
    }

    public void updateDisplayFreq(int fq) {
        mMainMiddleDisplay.showFq(fq);
    }

    public void updataDisplayUnit(int unit) {
        mMainMiddleDisplay.changeUnit(unit);
    }

    public void updataDisplayBand(int i) {
        mMainMiddleDisplay.changeBand(BAND_DRAWABLE_ID[i]);
    }

   /***开始背景变化**/
    private void startFlick(View view) {
        if (null == view) {
            return;
        }
        view.setBackgroundResource(R.drawable.common_loading3);
        AnimationDrawable animation = (AnimationDrawable)view.getBackground();
        animation.start();
    }

    /***停止背景变化**/
    private void stopFlick(View view) {
        if (null == view) {
            return;
        }
        view.clearAnimation();
        view.setBackgroundColor(getResources().getColor(R.color.trans));
    }

    class Bean {
        public int channelId;
        public int drawableId;
        public String fq;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mMainSearch:
                sendMsg(Contacts.HEX_AUTO_SCAN);
                break;
            case R.id.mMainSTOrMD:
                if(mMainSTOrMD.isSelected()){
                    ((TextView) mMainSTOrMD).setText(R.string.st);
                    mMainSTOrMD.setSelected(false);
                    PreferenceUtil.setSTOrMD(this, 0);
                }else{
                   ((TextView) mMainSTOrMD).setText(R.string.md);
                    mMainSTOrMD.setSelected(true);
                    PreferenceUtil.setSTOrMD(this, 1);
                }
                sendMsg(Contacts.HEX_ST);
                break;
            case R.id.mMainBand:
                sendMsg(Contacts.HEX_BAND);
                break;
            case R.id.mMainPs:
                sendMsg(Contacts.HEX_PS);
                break;
            case R.id.mMainPre:
                sendMsg(Contacts.HEX_PRE_STEP_MOVE);
                break;
            case R.id.mMainNext:
                sendMsg(Contacts.HEX_NEXT_STEP_MOVE);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.mMainPre:
                sendMsg(Contacts.HEX_PRE_FAST_MOVE);
                break;
            case R.id.mMainNext:
                sendMsg(Contacts.HEX_NEXT_FAST_MOVE);
                break;
            default:
                break;
        }
        return true;
    }

    public void registerBusEvent() {
        BusProvider.getInstance().register(this);
    }

    public void unRegisterBusEvent() {
        BusProvider.getInstance().unregister(this);
    }

    private RadioData mRadioData = new RadioData();
    private int mPopupWidth;

    public void radioDataAndUpdateUI(RadioDataEvent event) {
        byte[] packet = event.mPacket;
        // Log.i("packet", "packet : " + BytesUtil.bytesToHexString(packet));
        //Trace.d("packet : " + BytesUtil.bytesToHexString(packet));
        if(packet[0] != Contacts.MODE_RADIO)return;
        switch (packet[2])
        {
            case Contacts.FM1_FREQ:
            case Contacts.FM2_FREQ:
            case Contacts.FM3_FREQ:
            case Contacts.AM1_FREQ:
            case Contacts.AM2_FREQ:
                mRadioData.curFreq = ((int) packet[3] & 0xFF) << 8 | ((int) packet[4] & 0xFF);
                break;
            case Contacts.FM1_SELECT:
            case Contacts.FM2_SELECT:
            case Contacts.FM3_SELECT:
            case Contacts.AM1_SELECT:
            case Contacts.AM2_SELECT:
                mRadioData.curBand = ((int) packet[3] & 0xFF);
                mRadioData.curFavDown = ((int) packet[4] & 0xFF);
                break;
            case Contacts.FM1_1:
            case Contacts.FM2_1:
            case Contacts.FM3_1:
            case Contacts.AM1_1:
            case Contacts.AM2_1:
                mRadioData.FF[0] = ((int) packet[3] & 0xFF) << 8 | ((int) packet[4] & 0xFF);
                break;
            case Contacts.FM1_2:
            case Contacts.FM2_2:
            case Contacts.FM3_2:
            case Contacts.AM1_2:
            case Contacts.AM2_2:
                mRadioData.FF[1] = ((int) packet[3] & 0xFF) << 8 | ((int) packet[4] & 0xFF);
                break;
            case Contacts.FM1_3:
            case Contacts.FM2_3:
            case Contacts.FM3_3:
            case Contacts.AM1_3:
            case Contacts.AM2_3:
                mRadioData.FF[2] = ((int) packet[3] & 0xFF) << 8 | ((int) packet[4] & 0xFF);
                break;
            case Contacts.FM1_4:
            case Contacts.FM2_4:
            case Contacts.FM3_4:
            case Contacts.AM1_4:
            case Contacts.AM2_4:
                mRadioData.FF[3] = ((int) packet[3] & 0xFF) << 8 | ((int) packet[4] & 0xFF);
                break;
            case Contacts.FM1_5:
            case Contacts.FM2_5:
            case Contacts.FM3_5:
            case Contacts.AM1_5:
            case Contacts.AM2_5:
                mRadioData.FF[4] = ((int) packet[3] & 0xFF) << 8 | ((int) packet[4] & 0xFF);
                break;
            case Contacts.FM1_6:
            case Contacts.FM2_6:
            case Contacts.FM3_6:
            case Contacts.AM1_6:
            case Contacts.AM2_6:
                mRadioData.FF[5] = ((int) packet[3] & 0xFF) << 8 | ((int) packet[4] & 0xFF);
                break;
        }
        switch (mRadioData.curBand) {
            case 0:
            case 1:
            case 2:
                mMainBottomFreq.setMinFrequency(FM_START_FREQ);
                mMainBottomFreq.setMax(FM_END_FREQ - FM_START_FREQ);
                if(mRadioData.curFreq != 65535)
                    updateFreqView(mRadioData.curFreq - FM_START_FREQ);
                updataDisplayBand(mRadioData.curBand);
                updataDisplayUnit(FM_UNIT);
                for (int i = 0; i < 3; i++) {
                    
                    Bean leftbean = new Bean();
                    leftbean.drawableId = R.drawable.mhz;
                    leftbean.channelId = i + 1;
                    float leftFq = mRadioData.FF[i] / 100.00f;
                    leftbean.fq = String.valueOf(df2.format(leftFq));
                    
                    Bean rightbean = new Bean();
                    rightbean.drawableId = R.drawable.mhz;
                    rightbean.channelId = i + 4;
                    float rightFq = mRadioData.FF[i+3] / 100.00f;
                    rightbean.fq = String.valueOf(df2.format(rightFq));
                    
                    mLeftAdapter.set(i, leftbean);
                    mRightAdapter.set(i, rightbean);
                }
                if(mRadioData.curFreq != 65535)
                    updateDisplayFreq((float) mRadioData.curFreq / 100.00f, false);

                break;
            case 3:
            case 4:
                mMainBottomFreq.setMinFrequency(AM_START_FREQ);
                mMainBottomFreq.setMax(AM_END_FREQ - AM_START_FREQ);
                if(mRadioData.curFreq != 65535)
                    updateFreqView(mRadioData.curFreq - AM_START_FREQ);
                updataDisplayBand(mRadioData.curBand);
                updataDisplayUnit(AM_UNIT);
                for (int i = 0; i < 3; i++) {
                    
                    Bean leftbean = new Bean();
                    leftbean.drawableId = R.drawable.khz;
                    leftbean.channelId = i + 1;
                    int leftFq = mRadioData.FF[i];
                    leftbean.fq = String.valueOf(leftFq);
                    
                    Bean rightbean = new Bean();
                    rightbean.drawableId = R.drawable.khz;
                    rightbean.channelId = i + 4;
                    int rightFq = mRadioData.FF[i+3];
                    rightbean.fq = String.valueOf(rightFq);
                    
                    mLeftAdapter.set(i, leftbean);
                    mRightAdapter.set(i, rightbean);
                }
                if(mRadioData.curFreq != 65535)
                    updateDisplayFreq(mRadioData.curFreq);

                break;
            default:
                break;
        }
        switch (mRadioData.curFavDown) {
            case 1:
                stopFlick(mCurrFlikView);
                updateItemBg(mMainMiddleLeftList, 0);
                startFlick(mCurrFlikView);
                break;
            case 2:
                stopFlick(mCurrFlikView);
                updateItemBg(mMainMiddleLeftList, 1);
                startFlick(mCurrFlikView);
                break;
            case 3:
                stopFlick(mCurrFlikView);
                updateItemBg(mMainMiddleLeftList, 2);
                startFlick(mCurrFlikView);
                break;
            case 4:
                stopFlick(mCurrFlikView);
                updateItemBg(mMainMiddleRightList, 0);
                startFlick(mCurrFlikView);
                break;
            case 5:
                stopFlick(mCurrFlikView);
                updateItemBg(mMainMiddleRightList, 1);
                startFlick(mCurrFlikView);
                break;
            case 6:
                stopFlick(mCurrFlikView);
                updateItemBg(mMainMiddleRightList, 2);
                startFlick(mCurrFlikView);
                break;

            default:
                stopFlick(mCurrFlikView);
                break;
        }
        band = mRadioData.curBand;
        item = mRadioData.curFavDown - 1;
        allFqs.clear();
        for (int i = 0; i < mRadioData.FF.length; i++) {
            allFqs.add(mRadioData.FF[i]);
        }
    }

    public void sendMsg(String msg) {
        try {
            if (mISpService != null) {
                mISpService.sendDataToSp(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getMsgString(final int freq, int type) {
        byte[] packet = new byte[RadioData.PACKET_SIZE];
        packet[0] = Integer.valueOf("F1", 16).byteValue();
        if (1 == type) {
            packet[1] = Integer.valueOf("01", 16).byteValue();
        } else if (2 == type) {
            packet[1] = Integer.valueOf("02", 16).byteValue();
        }
        int data3 = freq / 256;
        int data4 = freq - data3 * 256;
        packet[2] = Integer.valueOf("00", 16).byteValue();
        packet[3] = Integer.valueOf(Integer.toHexString(data3), 16).byteValue();
        packet[4] = Integer.valueOf(Integer.toHexString(data4), 16).byteValue();
        byte sum = 0;
        for (int i = 0; i < packet.length - 1; i++)
        {
            sum += packet[i];
        }
        packet[5] = (byte) ((byte) 0xff - sum);
        return BytesUtil.bytesToHexString(packet);
    }
    
    protected void updateDateAndTime() {
        mMainTitleView.updateDateAndTime();
        mHandler.sendEmptyMessageDelayed(Contacts.MSG_UPDATE_DATE_AND_TIME, 60 * 1000);
    }

    @Override
    public String onHintTextChanged(FreqIndicator freqIndicator, int progress) {
        return null;
    }
    private int mCurrentVolumn = 0;
    private DecimalFormat mDecimalFormat = new DecimalFormat("00");
    private TextView popupVolumnValueTv;
    private SeekBar popupSeekBar;
    private void initHintPopup() {
        mPopupWidth = ScreenUtils.getScreenWidth(this);
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View undoView = inflater.inflate(R.layout.volumn_popup, null);
        mPopup = new PopupWindow(undoView, mPopupWidth,80, false);
        mPopup.setAnimationStyle(R.style.fade_animation);
        final View popupLeftIv = undoView.findViewById(R.id.popupLeftIv);
        final View popupRightIv = undoView.findViewById(R.id.popupRightIv);
        final View popupVolumn = undoView.findViewById(R.id.popupVolumn);
        popupSeekBar = (SeekBar)undoView.findViewById(R.id.popupSeekBar);
        popupVolumnValueTv = (TextView)undoView.findViewById(R.id.popupVolumnValueTv);
        popupLeftIv.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mHandler.removeMessages(Contacts.MSG_HIDE_POPUP_VIEW);
                mHandler.sendEmptyMessageDelayed(Contacts.MSG_HIDE_POPUP_VIEW, 5 * 1000);
                sendMsg(Contacts.HEX_VOLUMN_SUB);
//                if(mCurrentVolumn > 0)
//                    mCurrentVolumn -= 1;
                popupSeekBar.setProgress(mCurrentVolumn);
//                popupVolumnValueTv.setText(""+mCurrentVolumn);
            }
        });
        
        popupRightIv.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mHandler.removeMessages(Contacts.MSG_HIDE_POPUP_VIEW);
                mHandler.sendEmptyMessageDelayed(Contacts.MSG_HIDE_POPUP_VIEW, 5 * 1000);
                sendMsg(Contacts.HEX_VOLUMN_ADD);
//                if(mCurrentVolumn < 48)
//                    mCurrentVolumn += 1;
                popupSeekBar.setProgress(mCurrentVolumn);
//                popupVolumnValueTv.setText(""+mCurrentVolumn);
            }
        });
        popupSeekBar.setMax(48);
        popupSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mHandler.removeMessages(Contacts.MSG_HIDE_POPUP_VIEW);
                mHandler.sendEmptyMessageDelayed(Contacts.MSG_HIDE_POPUP_VIEW, 5 * 1000);
                int progress = seekBar.getProgress();
                String msg = getVolumnMsgString(progress);
                Trace.i("msg : " + msg);
                sendMsg(msg);
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHandler.removeMessages(Contacts.MSG_HIDE_POPUP_VIEW);
                mHandler.sendEmptyMessageDelayed(Contacts.MSG_HIDE_POPUP_VIEW, 5 * 1000);
            }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHandler.removeMessages(Contacts.MSG_HIDE_POPUP_VIEW);
                mHandler.sendEmptyMessageDelayed(Contacts.MSG_HIDE_POPUP_VIEW, 5 * 1000);
//                String msg = getVolumnMsgString(progress);
//                Trace.i("msg : " + msg);
//                sendMsg(msg);
                mCurrentVolumn = progress;
                popupVolumnValueTv.setText(mDecimalFormat.format(mCurrentVolumn));
            }
        });
        
        popupVolumn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                sendMsg(Contacts.HEX_VOLUMN_SILENT);
            }
        });
    }
    
    private void showPopup(){
        if(mPopup != null)
            mPopup.showAtLocation(getWindow().getDecorView(),Gravity.BOTTOM, 0,0);
    }
    
    private void hidePopup(){
        if(mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }
    }

    @Override
    public void onBackClick() {
        finish();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Trace.i("keyCode :" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                Trace.i("home press");
                finish();
                break;

            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onVolumnClick() {
        if(mPopup == null)return;
        showPopup();
        mHandler.sendEmptyMessageDelayed(Contacts.MSG_HIDE_POPUP_VIEW, 5 * 1000);
    }
    
    public String getVolumnMsgString(final int value) {
        byte[] packet = new byte[RadioData.PACKET_SIZE];
        packet[0] = Integer.valueOf("FD", 16).byteValue();
        packet[1] = Integer.valueOf("02", 16).byteValue();
        packet[2] = Integer.valueOf("00", 16).byteValue();
        packet[3] = Integer.valueOf("00", 16).byteValue();
        packet[4] = Integer.valueOf(Integer.toHexString(value), 16).byteValue();
        byte sum = 0;
        for (int i = 0; i < packet.length - 1; i++)
        {
            sum += packet[i];
        }
        packet[5] = (byte) ((byte) 0xff - sum);
        return BytesUtil.bytesToHexString(packet);
    }
    
    private boolean isMuted;
    private void volumnData(byte[] packet) {
        if(packet == null)return;
        if(packet[0] != Contacts.SYSTEM_INFO)return;

        mCurrentVolumn = ((int) packet[4] & 0xff);
        Trace.i("mCurrentVolumn : " + mCurrentVolumn);
        popupVolumnValueTv.setText(mDecimalFormat.format(mCurrentVolumn));
        popupSeekBar.setProgress(mCurrentVolumn);
        
        
    }
    /**MyBroadcastReceiver FM*/
//    private static final String ACTION_CLOSE_FMAUDIO="android.intent.action.CLOSE_FMAUDIO";
//    private static final String ACTION_OPEN_FMAUDIO="android.intent.action.OPEN_FMAUDIO";
//    public void initReceiver(){
//        mBroadcastReceiver = new MyBroadcastReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_CLOSE_FMAUDIO);
//        filter.addAction(ACTION_OPEN_FMAUDIO);
//        registerReceiver(mBroadcastReceiver, filter);
//    }
//    public void removeReceiver(){
//        unregisterReceiver(mBroadcastReceiver);
//    }
//    private MyBroadcastReceiver mBroadcastReceiver;
//    public class MyBroadcastReceiver extends BroadcastReceiver{
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            Trace.i("FM action : " + action);
//            if(TextUtils.equals(action, ACTION_CLOSE_FMAUDIO)){
//                sendMsg(Contacts.HEX_OTHER_MODEL);
//            }else if(TextUtils.equals(action, ACTION_OPEN_FMAUDIO)){
//                sendMsg(Contacts.HEX_HOME_TO_FM);
//            }
//        }
//    }
    
    private boolean isFM = true;
    private int band = 0;
    private int item = 0;
    private List<Integer> allFqs = new ArrayList<Integer>();
    public void setFqDataWhenOnCreate(){
        band = PreferenceUtil.getBand(this);
        if(band < 3){
            isFM = true;
        }else{
            isFM = false;
        }
        item = PreferenceUtil.getFavoriteFq(this);
        PreferenceUtil.getAllFq(this, allFqs, isFM);
        int unitId = (band >= 0 && band < BAND_DRAWABLE_ID.length) ? band : 0;
        updataDisplayBand(unitId);
        if(isFM){
            mMainBottomFreq.setMinFrequency(FM_START_FREQ);
            mMainBottomFreq.setMax(FM_END_FREQ - FM_START_FREQ);
            int valueFM = (item >= 0 && item < allFqs.size()) ? allFqs.get(item) : PreferenceUtil.DEFAULT_FM;
            updateDisplayFreq((float)valueFM / 100.0f,false);
            updateFreqView(valueFM - FM_START_FREQ);
            for (int i = 0; i < 3; i++) {
                
                Bean leftbean = new Bean();
                leftbean.drawableId = R.drawable.mhz;
                leftbean.channelId = i + 1;
                float leftFq = allFqs.get(i) / 100.00f;
                leftbean.fq = String.valueOf(df2.format(leftFq));
                
                Bean rightbean = new Bean();
                rightbean.drawableId = R.drawable.mhz;
                rightbean.channelId = i + 4;
                float rightFq = allFqs.get(i + 3) / 100.00f;
                rightbean.fq = String.valueOf(df2.format(rightFq));
                
                mLeftAdapter.set(i, leftbean);
                mRightAdapter.set(i, rightbean);
            }
        }else{
            mMainBottomFreq.setMinFrequency(AM_START_FREQ);
            mMainBottomFreq.setMax(AM_END_FREQ - AM_START_FREQ);
            int valueAM = (item >= 0 && item < allFqs.size()) ? allFqs.get(item) : PreferenceUtil.DEFAULT_AM;
            updateDisplayFreq(valueAM);
            updateFreqView(valueAM - AM_START_FREQ);
            for (int i = 0; i < 3; i++) {
                
                Bean leftbean = new Bean();
                leftbean.drawableId = R.drawable.khz;
                leftbean.channelId = i + 1;
                int leftFq = allFqs.get(i);
                leftbean.fq = String.valueOf(leftFq);
                
                Bean rightbean = new Bean();
                rightbean.drawableId = R.drawable.khz;
                rightbean.channelId = i + 4;
                int rightFq = allFqs.get(i + 3);
                rightbean.fq = String.valueOf(rightFq);
                
                mLeftAdapter.set(i, leftbean);
                mRightAdapter.set(i, rightbean);
            }
        }
    }
    
    public void saveFqDataWhenOnDestroy(){
        PreferenceUtil.setBand(this, band);
        PreferenceUtil.setFavoriteFq(this, item);
        PreferenceUtil.setAllFq(this, allFqs);
    }
    
    private AudioManager mAudioManager;
    
    private int requestAudioFocus() {
        return getAudioManager().requestAudioFocus(new OnAudioFocusChangeListener() {
            
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        sendMsg(Contacts.HEX_OTHER_MODEL);
                        finish();
                        break;
                    default:
                        break;
                }
            }
        },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

    }

    private void abandonAudioFocus() {
        getAudioManager().abandonAudioFocus(null);
    }

    private AudioManager getAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);
        }
        return mAudioManager;
    }
}
