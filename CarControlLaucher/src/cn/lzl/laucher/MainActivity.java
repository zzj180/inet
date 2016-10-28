
package cn.lzl.laucher;

import cn.colink.serialport.service.ISerialPortCallback;
import cn.colink.serialport.service.ISerialPortService;
import cn.lzl.laucher.activity.BaseActivity;
import cn.lzl.laucher.model.ApplicationInfo;
import cn.lzl.laucher.model.IconCache;
import cn.lzl.laucher.model.ItemInfo;
import cn.lzl.laucher.model.LauncherModel;
import cn.lzl.laucher.utils.Contacts;
import cn.lzl.laucher.utils.Trace;
import cn.lzl.laucher.view.AppIconView;
import cn.lzl.laucher.view.AppIconView.ImgClickListener;
import cn.lzl.laucher.viewpagerindicator.IconPageIndicator;
import cn.lzl.laucher.viewpagerindicator.IconPagerAdapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends BaseActivity implements ImgClickListener, 
                                OnClickListener, OnLongClickListener,LauncherModel.Callbacks{

    
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Contacts.MSG_UPDATA_UI:
                    byte[] packet = (byte[]) msg.obj;
                    radioDataAndUpdateUI(packet);
                    break;
                case Contacts.MSG_UPDATE_TIME_LABEL:
                    onUpdateTimeLabel();
                    break;
                case Contacts.MSG_GPS_CALL_BACK:
                    break;
                case Contacts.MSG_BOOT_COMPLETED:
                    showDialog();
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
    private ISerialPortService mISpService;
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
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mISpService = null;
        }
    };
    
    private void startSpService() {
        try {
            Intent intent = new Intent();
            intent.setClassName("cn.colink.serialport",
                    "cn.colink.serialport.service.SerialPortService");
            startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private AppIconView mMainBtmHulian;
    private AppIconView mMainBtmFm;
    private AppIconView mMainBtmNavigation;
    private AppIconView mMainBtmCamera;
    private AppIconView mMainBtmBt;
    private AppIconView mMainBtmVoiceCon;
    private ViewPager mMainViewPager;
    private IconPageIndicator mMainPageIndicator;
    private TextView mMainTimeTv;
    private TextView mMainDateTv;
    private SimpleDateFormat mMainTimeSdf;
    private ImageView mMainCloseScreen;
    private PowerManager mPowerManager; 
    @SuppressWarnings("unused")
    private PowerManager.WakeLock mWakeLock;
    private RelativeLayout mMainRootRl; 
    private LocationClient mLocationClient;
    private MyLocationListener myListener;
    private TextView mMainCityTv;
    private TextView mMainWeatherTv;
    private AudioManager mAudioManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.i("onCreate");
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        super.onCreate(savedInstanceState);
    }
    @Override
    protected void onResume() {
        Trace.i("onResume--");
        super.onResume();
        bindSpService();
        onUpdateTimeLabel();
        mLocationClient.registerLocationListener(myListener);
        mLocationClient.start();
        mPaused = false;
        for (int i = 0; i < mOnResumeCallbacks.size(); i++) {
            mOnResumeCallbacks.get(i).run();
        }
        mOnResumeCallbacks.clear();
    }
    @Override
    protected void onPause() {
        Trace.i("onPause--");
        super.onPause();
        unbindSpService();
        releaseWakeLock();
        mLocationClient.unRegisterLocationListener(myListener);
        mLocationClient.stop();
        mPaused = true;
    }
    @Override
    protected void onDestroy() {
        Trace.i("onDestroy");
        super.onDestroy();
        unRegisterBootReceiver();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void initializeView() {
        mMainBtmHulian = (AppIconView)findViewById(R.id.mMainBtmHulian);
        mMainBtmFm = (AppIconView)findViewById(R.id.mMainBtmFm);
        mMainBtmNavigation = (AppIconView)findViewById(R.id.mMainBtmNavigation);
        mMainBtmCamera = (AppIconView)findViewById(R.id.mMainBtmCamera);
        mMainBtmBt = (AppIconView)findViewById(R.id.mMainBtmBt);
        mMainBtmVoiceCon = (AppIconView)findViewById(R.id.mMainBtmVoiceCon);
        
        mMainBtmHulian.setAppIcon(getResources().getDrawable(R.drawable.music_btn_selector));
        mMainBtmFm.setAppIcon(getResources().getDrawable(R.drawable.fm_btn_selector));
        mMainBtmNavigation.setAppIcon(getResources().getDrawable(R.drawable.navigation_btn_selector));
        mMainBtmCamera.setAppIcon(getResources().getDrawable(R.drawable.camera_btn_selector));
        mMainBtmBt.setAppIcon(getResources().getDrawable(R.drawable.bt_btn_selector));
        mMainBtmVoiceCon.setAppIcon(getResources().getDrawable(R.drawable.movies_btn_selector));
        
        mMainBtmHulian.setAppTv(getResources().getString(R.string.music));
        mMainBtmFm.setAppTv(getResources().getString(R.string.fm));
        mMainBtmNavigation.setAppTv(getResources().getString(R.string.navigation));
        mMainBtmCamera.setAppTv(getResources().getString(R.string.camera));
        mMainBtmBt.setAppTv(getResources().getString(R.string.bt));
        mMainBtmVoiceCon.setAppTv(getResources().getString(R.string.movies));
        
        mMainBtmHulian.setId(R.id.mMainBtmHulian);
        mMainBtmFm.setId(R.id.mMainBtmFm);
        mMainBtmNavigation.setId(R.id.mMainBtmNavigation);
        mMainBtmCamera.setId(R.id.mMainBtmCamera);
        mMainBtmBt.setId(R.id.mMainBtmBt);
        mMainBtmVoiceCon.setId(R.id.mMainBtmVoiceCon);
        
        mMainBtmHulian.setImgClickListener(this);
        mMainBtmFm.setImgClickListener(this);
        mMainBtmNavigation.setImgClickListener(this);
        mMainBtmCamera.setImgClickListener(this);
        mMainBtmBt.setImgClickListener(this);
        mMainBtmVoiceCon.setImgClickListener(this);
        
        mMainViewPager = (ViewPager)findViewById(R.id.mMainViewPager);
        mMainPageIndicator = (IconPageIndicator)findViewById(R.id.mMainPageIndicator);
        
        mMainTimeTv = (TextView)findViewById(R.id.mMainTimeTv);
        mMainDateTv = (TextView)findViewById(R.id.mMainDateTv);
        
        mMainCloseScreen = (ImageView)findViewById(R.id.mMainCloseScreen);
        mMainCloseScreen.setOnClickListener(this);
        
        mMainRootRl = (RelativeLayout)findViewById(R.id.mMainRootRl);
        mMainRootRl.setOnLongClickListener(this);
        
        mMainCityTv = (TextView)findViewById(R.id.mMainCityTv);
        mMainWeatherTv = (TextView)findViewById(R.id.mMainWeatherTv);
        mMainCityTv.setOnClickListener(this);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    protected void initializeData() {
        registerBootReceiver();
        startSpService();
        initWakeLock();
        //loadApplications();
        mAdapter = new MainViewPagerAdapter(this);
        mMainViewPager.setAdapter(mAdapter);
        mMainPageIndicator.setViewPager(mMainViewPager);
        mMainTimeSdf = new SimpleDateFormat("HH:mm:ss");
        mLocationClient = new LocationClient(this);
        myListener = new MyLocationListener();
        InitLocation();
        
        IconCache iconCache = new IconCache(this);
        LauncherModel mModel = new LauncherModel(this, iconCache);
        mModel.initialize(this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mModel, filter);
        mModel.startLoader(true, -1);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mPaused = false;
    }

    /**
     * CustomerBroadcastReceiver start
     */
    private CustomerBroadcastReceiver mBootBroadcastReceiver = null;
    public static final String CUSTOMER_BOOT_COMPLETED = "customer_boot_completed";
    private void registerBootReceiver() {
        mBootBroadcastReceiver = new CustomerBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CUSTOMER_BOOT_COMPLETED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        /**
         * 注册当音量发生变化时接收的广播
         */
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(mBootBroadcastReceiver, intentFilter);
    }

    private void unRegisterBootReceiver() {
        if (null != mBootBroadcastReceiver) {
            unregisterReceiver(mBootBroadcastReceiver);
        }
    }

    class CustomerBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Trace.i("BootBroadcastReceiver action : " + action);
            if (TextUtils.equals(action, CUSTOMER_BOOT_COMPLETED)) {
                sendMsg(Contacts.HEX_START);
                mHandler.sendEmptyMessageDelayed(Contacts.MSG_BOOT_COMPLETED, 1000);
            }else if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,50,0);
                mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM,50,0);
            }
        }
    }

    /**
     * viewpager adapter
     */
    private List<ApplicationInfo> appInfos = new ArrayList<ApplicationInfo>();
    private MainViewPagerAdapter mAdapter = null;
    @SuppressLint("InflateParams")
    public class MainViewPagerAdapter extends PagerAdapter implements IconPagerAdapter{
        protected final int[] ICONS = new int[] { R.drawable.perm_group_point,R.drawable.perm_group_point,R.drawable.perm_group_point};
        private Context mContext;
        private List<View> views;

        public MainViewPagerAdapter(Context context) {
            this.mContext = context;
            views = new ArrayList<View>();
        }
        
       private void initAppIconView(int index,int position,ViewGroup parent){
           AppIconView iconView = (AppIconView)parent.getChildAt(position);
           iconView.setAppIcon(appInfos.get(index).iconBitmap);
           iconView.setAppTv(appInfos.get(index).title.toString());
           iconView.setId(index);
           iconView.setImgClickListener(MainActivity.this);
       }

        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View itemView = (View) object;
            ((ViewPager) container).removeView(itemView);
        }

        @Override
        public Object instantiateItem(View container, int position) {
            View itemView = views.get(position);
            ((ViewPager) container).addView(itemView);
            return itemView;
        }

        @Override
        public int getIconResId(int index) {
            return ICONS[index % ICONS.length];
        }
        
        @Override
        public int getItemPosition(Object object)   {          
              if (getCount() > 0) {
                  return POSITION_NONE;
              }
              return super.getItemPosition(object);

        }
        
        public List<View> getViews(){
            return views;
        }
        
        public void setDatas(){
            int temp = appInfos.size() % 6;
            int page = temp == 0 ? appInfos.size() / 6 : appInfos.size() / 6 + 1;
            views.clear();
            for (int i = 0; i < page; i++) {
                View view = LayoutInflater.from(mContext).inflate(R.layout.activity_main_bottom,null);
                LinearLayout layout = (LinearLayout) view.findViewById(R.id.mMainBtmRoot);
                layout.setBackgroundColor(Color.TRANSPARENT);
                //layout.removeAllViews();
                if(i == page - 1){//最后一页
                    if(temp == 0){
                        for (int j = 0; j < 6; j++) {
                            initAppIconView(i * 6 + j,j,layout);
                        }
                    }else{
                        for (int j = 0; j < temp; j++) {
                            initAppIconView(i * 6 + j,j,layout);
                        }
                        for(int j = temp;j < 6;j++ ){
                            View childView = layout.getChildAt(j);
                            childView.setVisibility(View.GONE);
                        }
                    }
                }else{
                    for (int j = 0; j < 6; j++) {
                        initAppIconView(i * 6 + j,j,layout);
                    }
                }
                views.add(view);
            }
            notifyDataSetChanged();
        }
    }
    /**APP PACKETNAME*/
    public final static String FM_PACKET = "cn.colink.fm";
    public final static String PARTYCONTROL_PACKET = "cn.lzl.partycontrol";
    public final static String SOUND_PACKET = "cn.lzl.soundeffect";
    public final static String BT_PACKET = "com.colink.bluetoolthe";
    public final static String CAMERA_PACKET = "com.android.camera2";
    public final static String NAVI_PACKET = "com.autonavi.xmgd.navigator";
    public final static String MUSIC_PACKET = "cn.kuwo.kwmusiccar";
    public final static String SETTING_PACKET = "com.android.settings";
    public final static String BROWSER_PACKET = "com.android.browser";
    public final static String EXPLORE_PACKET = "com.softwinner.explore";
    public final static String MOBOPLAYER_PACKET = "com.clov4r.android.nil";
    public final static String BAIDU_NAVI_PACKET = "com.baidu.navi";
    public final static String CAR_ASSISTANT_PACKET = "com.coogo.inet.vui.assistant.car";
    public final static String MEDIA_PLAYER_PACKET = "com.mxtech.videoplayer.ad";
    public final static String IFLY_PACKET = "com.iflytek.speechcloud";
    /**APP CLASSNAME*/
    public final static String FM_CLASSNAME = "cn.colink.fm.MainActivity";
    public final static String PARTYCONTROL_CLASSNAME = "cn.lzl.partycontrol.MainActivity";
    public final static String SOUND_CLASSNAME = "cn.lzl.soundeffect.MainActivity";
    public final static String BT_CLASSNAME = "com.colink.bluetoothe.MainActivity";
    public final static String CAMERA_CLASSNAME = "com.android.camera.CameraLauncher";
    public final static String NAVI_CLASSNAME = "com.autonavi.xmgd.navigator.Warn";
    public final static String MUSIC_CLASSNAME = "cn.kuwo.kwmusiccar.MainActivity";
    public final static String MOBOPLAYER_CLASSNAME = "com.clov4r.android.nil.entrance.WelcomeActivity";
    public final static String BAIDU_NABI_CLASSNAME = "com.baidu.navi.NaviActivity";
    public final static String CAR_ASSISTANT_CLASSNAME = "cn.yunzhisheng.vui.assistant.MainActivity";
    public final static String MEDIA_PLAYER_CLASSNAME = "com.mxtech.videoplayer.ad.ActivityMediaList";
    public final static String IFLY_CLASSNAME = "com.iflytek.speechcloud.SpeechWelcome";
    private long firstTime = 0;
    private int times = 0;
    
    @Override
    public void click(int id) {
        switch (id) {
            case R.id.mMainBtmHulian:
                gotoApp(MUSIC_PACKET, MUSIC_CLASSNAME);
                break;
            case R.id.mMainBtmFm:
                gotoApp(FM_PACKET, FM_CLASSNAME);
                break;
            case R.id.mMainBtmBt:
                sendMsg(Contacts.HEX_BT_MODEL);
                gotoApp(BT_PACKET, BT_CLASSNAME);
                break;
            case R.id.mMainBtmCamera:
                gotoApp(CAMERA_PACKET, CAMERA_CLASSNAME);
                break;
            case R.id.mMainBtmNavigation:
                gotoApp(BAIDU_NAVI_PACKET, BAIDU_NABI_CLASSNAME);
                break;
            case R.id.mMainBtmVoiceCon:
                gotoApp(MEDIA_PLAYER_PACKET,MEDIA_PLAYER_CLASSNAME);
                break;
            default:
                try {
                    if(id < appInfos.size() && id >= 0){
                        String packetName = (String) appInfos.get(id).componentName.getPackageName();
                        if(!TextUtils.equals(packetName, FM_PACKET) &&
                           !TextUtils.equals(packetName, PARTYCONTROL_PACKET) &&
                           !TextUtils.equals(packetName, SOUND_PACKET))
                            sendMsg(Contacts.HEX_OTHER_MODEL);
                        startActivity(appInfos.get(id).intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
    

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mMainCloseScreen:
                //shutDownDevice();
                //goBrightness();
                sendMsg(Contacts.HEX_CLOSE_SCREEN);
                break;
            case R.id.mMainCityTv:
                Trace.i("mMainWeatherLy");
                times++;
                if (System.currentTimeMillis() - firstTime < 3000) {
                    if(times >= 6){
                        times = 0;
                        goLauncher2();
                    }
                } else {
                    firstTime = System.currentTimeMillis();
                    times = 0;
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.mMainRootRl:
                startWallpaper();
                break;

            default:
                break;
        }
        return false;
    }
    
    private void onUpdateTimeLabel() {
        long time = System.currentTimeMillis();
        String timeLabel = mMainTimeSdf.format(new Date(time));
        String date = DateFormat.getDateInstance(DateFormat.FULL).format(new Date(time));
        mMainTimeTv.setText(timeLabel);
        mMainDateTv.setText(date);
        mHandler.sendEmptyMessageDelayed(Contacts.MSG_UPDATE_TIME_LABEL, 1000);
    }
    
    private void initWakeLock() {
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    }
    
    private void releaseWakeLock() {
        
    }
    
    @SuppressWarnings("unused")
    private void screenOff(){
        if(mPowerManager != null){
            Trace.i("go to sleep");
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
    }
    
    public void shutDownDevice() {
    }
    private static final int REQUEST_PICK_WALLPAPER = 10;
    private void startWallpaper() {
        Trace.i("startWallpaper");
        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper,
                "选择壁纸");
        startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
    }
    
    private void radioDataAndUpdateUI(byte[] packet) {
        if (packet == null || packet.length <= 0) {
            return;
        }
        if (packet[0] == Contacts.MODE_RADIO) {
            //goFM();
        }else if(packet[0] == Contacts.SWITCH_MODE && packet[4] == Contacts.MODE_BLUETOOTH){
            //gotBt();
        }
    }
    
    private void gotoApp(String packetName,String className){
        try {
            sendMsg(Contacts.HEX_OTHER_MODEL);
            Intent intent = new Intent();
            intent.setClassName(packetName, className);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void goLauncher2(){
        Trace.i("goLauncher2");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ComponentName cn = new ComponentName(
                    "com.android.launcher3",
                    "com.android.launcher3.Launcher");
            intent.setComponent(cn);
            startActivity(intent);
            return;
        } catch (ActivityNotFoundException e) {
        }
        try {
            ComponentName cn = new ComponentName(
                    "com.android.launcher",
                    "com.android.launcher2.Launcher");
            intent.setComponent(cn);
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
        }
    }
    
    @SuppressWarnings("unused")
    private void goBrightness(){
        try {
            Intent intent = new Intent();
            ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.DisplaySettings");
            intent.setComponent(comp);
            intent.setAction("android.intent.action.VIEW");
            startActivityForResult( intent , 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void InitLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        int span = 5 * 60 * 1000;
        option.setScanSpan(span);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
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
    
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            onHttpRequest(location);
        }
    }
    
    public void onHttpRequest(final BDLocation location){
        if(location == null || TextUtils.isEmpty(location.getCity())){
            Trace.i("onHttpRequest fail");
            return;
        }
        Trace.i("location city " + location.getCity());
        final String url = "http://scv2.hivoice.cn/service/iss?history=&text="
                + location.getCity()
                + "的天气"
                + "&scenario=" +
                "incar&appkey=" +
                "sknvnxhkddkf4vz2l2nl464imv5ymzjhs5x55oag&method=" +
                "iss.getTalk&udid=357941050558026&ver=" +
                "2.0&appsig=" +
                "CC8FA3BB8C56277DA8D47CC3D5BF250E3589AF60&appver=" +
                "2.1.0.27";
        System.gc();
        new HttpClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,url);
    }
    
    
    class HttpClientTask extends AsyncTask<String, Void,JSONObject>{
        StringBuffer string = new StringBuffer("");
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            string.delete(0,string.length());
        }

        @Override
        protected JSONObject doInBackground(String... url) {
            Trace.i("HttpClientTask doInBackground");
            HttpParams httpParameters;  
            int timeoutConnection = 15 * 1000;  
            int timeoutSocket = 15 * 1000; 
            BufferedReader in = null;  
            @SuppressWarnings("unused")
            HttpClient httpclient;
            JSONObject object = null;
            try {
                httpParameters = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);  
                httpclient = new DefaultHttpClient(httpParameters);  
                HttpClient client = new DefaultHttpClient();  
                HttpGet request = new HttpGet(url[0]);  
                HttpResponse response = client.execute(request);  
                in = new BufferedReader(  
                        new InputStreamReader(  
                                response.getEntity().getContent()));  

                String lineStr = "";  
                while ((lineStr = in.readLine()) != null) {  
                    string.append(lineStr + "\n");  
                } 
                in.close();  
            } catch(Exception e) { 
                
            } finally {  
                if (in != null) {  
                    try {  
                        in.close();  
                    } catch (IOException e) {  
                        e.printStackTrace();  
                    }  
                }  
            }  
            try {
                 object = new JSONObject(string.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return object;
        }
        
        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            if(result != null && !result.toString().trim().equals("")){
                try {
                    String city = result.getString("text");
                    mMainCityTv.setText(city);
                    JSONObject data = result.getJSONObject("data");
                    String header = data.getString("header");
                    String[] weather = header.split("："); 
                    if(weather.length > 1){
                        mMainWeatherTv.setText(weather[1]);
                    }else{
                        mMainWeatherTv.setText(header);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean setLoadOnResume() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getCurrentWorkspaceScreen() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void startBinding() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void finishBindingItems() {
        // TODO Auto-generated method stub
        
    }
    
    private ArrayList<ApplicationInfo> mApps;
    @Override
    public void bindAllApplications(ArrayList<ApplicationInfo> apps) {
        mApps = apps;
        Trace.i("size : " + mApps.size());
//        for (ApplicationInfo applicationInfo : apps) {
//            Trace.i(" packetName : " + applicationInfo.componentName.getPackageName()
//                    + " className : " + applicationInfo.componentName.getClassName()
//                    + "  title : " + applicationInfo.title);
//        }
        onChangeVpData();
    }

    @Override
    public void bindAppsAdded(final ArrayList<ApplicationInfo> apps) {
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindAppsAdded(apps);
            }
        })) {
            return;
        }
        Trace.i("size : " + mApps.size());
        mApps.addAll(apps);
        onChangeVpData();
    }

    @Override
    public void bindAppsUpdated(final ArrayList<ApplicationInfo> apps) {
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindAppsUpdated(apps);
            }
        })) {
            return;
        }
        
        onChangeVpData();
    }

    @Override
    public void bindComponentsRemoved(final ArrayList<String> packageNames,
            final ArrayList<ApplicationInfo> appInfos, final boolean matchPackageNamesOnly) {
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindComponentsRemoved(packageNames, appInfos, matchPackageNamesOnly);
            }
        })) {
            return;
        }
        mApps.removeAll(appInfos);
        Trace.i("size : " + mApps.size());
        onChangeVpData();
    }

    @Override
    public void bindPackagesUpdated(ArrayList<Object> widgetsAndShortcuts) {
    }

    @Override
    public boolean isAllAppsVisible() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAllAppsButtonRank(int rank) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void bindSearchablesChanged() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPageBoundSynchronously(int page) {
        // TODO Auto-generated method stub
    }
    
    private boolean mPaused = true;
    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<Runnable>();
    private boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused) {
            Trace.i("Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mOnResumeCallbacks.remove(run)) {
                }
            }
            mOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    public void dealWithAllApps(ArrayList<ApplicationInfo> apps) {
        if (apps != null) {
            final int count = apps.size();
            appInfos.clear();
            for (int i = 0; i < count; i++) {
                ApplicationInfo info = apps.get(i);
                ComponentName componentName = info.componentName;
                String packageName = componentName.getPackageName();
                Trace.i("packet name : " + packageName + "  class name : " + componentName.getClassName());
                if(TextUtils.equals(packageName, FM_PACKET)                 ||
                        TextUtils.equals(packageName, BT_PACKET)            ||
                        TextUtils.equals(packageName, CAMERA_PACKET)        ||
                        TextUtils.equals(packageName, MUSIC_PACKET)         ||
                        TextUtils.equals(packageName, NAVI_PACKET)          ||
                        TextUtils.equals(packageName, BAIDU_NAVI_PACKET)    ||
                        TextUtils.equals(packageName, IFLY_PACKET)          ||
                        TextUtils.equals(packageName, CAR_ASSISTANT_PACKET) ||
                        TextUtils.equals(packageName, MEDIA_PLAYER_PACKET)) {
                    continue;
                }
                if((info.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) <= 0){
                    if(TextUtils.equals(packageName,EXPLORE_PACKET) || 
                            TextUtils.equals(packageName,BROWSER_PACKET) ||
                            TextUtils.equals(packageName,SETTING_PACKET)){
                        appInfos.add(info);
                     }                
                }else{
                    appInfos.add(info);
                }
            }
        }
    }
    
    public void onChangeVpData(){
        dealWithAllApps(mApps);
        mAdapter.setDatas();
        mMainPageIndicator.notifyDataSetChanged();
    }
    
    public void showDialog(){
        new AlertDialog.Builder(MainActivity.this) 
        .setTitle("行车记录仪")
        .setMessage("是否打开")
        .setPositiveButton("是", new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                gotoApp(CAMERA_PACKET,CAMERA_CLASSNAME);
            }
        })   
        .setNegativeButton("否", null)
        .show();
    }
}
