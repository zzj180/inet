package cn.colink.fm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.colink.fm.utils.Trace;

public class FmBroadcastReceiver extends BroadcastReceiver{
    
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED" ;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Trace.d("action : " + intent.getAction());
        switch (intent.getAction()) {
            case ACTION_BOOT_COMPLETED:
                
                break;

            default:
                break;
        }
    }

}
