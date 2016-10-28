package com.mtk.bluetooth;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.Context; 


public class BTApplication extends Application {

 private static Context mContext;
	@Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
		CrashHandler.getInstance().init(this);
	}
	
 	public static Context getContext() {
		if (mContext == null) {
			throw new RuntimeException("Unknown Error");
		}
		return mContext;
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}
}
