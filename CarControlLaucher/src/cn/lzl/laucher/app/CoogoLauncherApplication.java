package cn.lzl.laucher.app;

import android.app.Application;
import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;


public class CoogoLauncherApplication extends Application {

//    public LocationClient mLocationClient;
//    public GeofenceClient mGeofenceClient;
//    public MyLocationListener mMyLocationListener;
    
	@Override
	public void onCreate() {
		super.onCreate();
//	     mLocationClient = new LocationClient(this.getApplicationContext());
//	     mMyLocationListener = new MyLocationListener();
//	     mLocationClient.registerLocationListener(mMyLocationListener);
//	     mGeofenceClient = new GeofenceClient(getApplicationContext());
	}
	
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            //Receive Location 
            StringBuffer sb = new StringBuffer(256);
            location.getCity();
            sb.append("time : ");
            sb.append(location.getTime());
            sb.append("\nerror code : ");
            sb.append(location.getLocType());
            sb.append("\ncity : ");
            sb.append(location.getCity());
            sb.append("\nlatitude : ");
            sb.append(location.getLatitude());
            sb.append("\nlontitude : ");
            sb.append(location.getLongitude());
            sb.append("\nradius : ");
            sb.append(location.getRadius());
            if (location.getLocType() == BDLocation.TypeGpsLocation){
                sb.append("\nspeed : ");
                sb.append(location.getSpeed());
                sb.append("\nsatellite : ");
                sb.append(location.getSatelliteNumber());
                sb.append("\ndirection : ");
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append(location.getDirection());
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation){
                sb.append("\naddr : ");
                sb.append(location.getAddrStr());
                sb.append("\noperationers : ");
                sb.append(location.getOperators());
            }
            Log.i("BaiduLocationApiDem", sb.toString());
        }
    }
}
