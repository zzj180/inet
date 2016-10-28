package cn.lzl.soundeffect.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtil {

    public static final String KEY_ST_OR_MD = "st_or_md";//0:ST 1:MD
    
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName(), 0);
    }

    private static SharedPreferences.Editor getPreferencesEditor(Context context) {
        return context.getSharedPreferences(context.getPackageName(), 0).edit();
    }

    public static int getSTOrMD(Context context) {
        SharedPreferences pref = getPreferences(context);
        return pref.getInt(KEY_ST_OR_MD, 0);
    }
    
    public static void setSTOrMD(Context context,int value){
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putInt(KEY_ST_OR_MD, value).apply();
    }
 
}
