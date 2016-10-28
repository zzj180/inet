package cn.lzl.laucher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent ;
import android.text.TextUtils;

public class BootBroadcastReceiver extends BroadcastReceiver {

	public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
	public static final String CUSTOMER_BOOT_COMPLETED = "customer_boot_completed";
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction();
		if (TextUtils.equals(action, ACTION_BOOT_COMPLETED)){
		    context.sendBroadcast(new Intent(CUSTOMER_BOOT_COMPLETED));
		}
	}
}
