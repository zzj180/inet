package cn.colink.fm.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

public class PreferenceUtil {

    public static final String KEY_ST_OR_MD = "st_or_md";//0:ST 1:MD
    /**0:FM1 1:FM2 2:FM3 3:AM1 4:AM2*/
    public static final String KEY_CURRENT_BAND = "current_band";
    public static final String KEY_FAVORITE_FQ = "favorite_fq";
    public static final String KEY_FQ_0 = "fq_0";
    public static final String KEY_FQ_1 = "fq_1";
    public static final String KEY_FQ_2 = "fq_2";
    public static final String KEY_FQ_3 = "fq_3";
    public static final String KEY_FQ_4 = "fq_4";
    public static final String KEY_FQ_5 = "fq_5";
    public static final String[] ALL_FQ_KEY = {
        KEY_FQ_0,KEY_FQ_1,KEY_FQ_2,KEY_FQ_3,KEY_FQ_4,KEY_FQ_5};
    public static final int DEFAULT_FM = 9500;
    public static final int DEFAULT_AM = 522;
    
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
    
    public static int getBand(Context context){
        return getPreferences(context).getInt(KEY_CURRENT_BAND, 0);
    }
    
    public static void setBand(Context context,int band){
        getPreferencesEditor(context).putInt(KEY_CURRENT_BAND, band).apply();
    }
    
    public static int getFavoriteFq(Context context){
        return getPreferences(context).getInt(KEY_FAVORITE_FQ, 0);
    }
    
    public static void setFavoriteFq(Context context,int item){
        getPreferencesEditor(context).putInt(KEY_FAVORITE_FQ, item).apply();
    }
    
    public static void getAllFq(Context context,List<Integer> allFqs,boolean isFM){
        if(allFqs == null){
            return;
        }
        SharedPreferences pref = getPreferences(context);
        int defaultValue = 0;
        if(isFM){
            defaultValue = DEFAULT_FM;
        }else{
            defaultValue = DEFAULT_AM;
        }
        allFqs.clear();
        for (int i = 0; i < ALL_FQ_KEY.length; i++) {
            int value = pref.getInt(ALL_FQ_KEY[i], defaultValue);
            Trace.i(ALL_FQ_KEY[i] + " : " + value);
            allFqs.add(value);
        }
    }
    
    @SuppressWarnings("null")
    public static void setAllFq(Context context,List<Integer> allFqs){
        if(allFqs == null && allFqs.size() < ALL_FQ_KEY.length){
            return;
        }
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        for (int i = 0; i < ALL_FQ_KEY.length; i++) {
            editor.putInt(ALL_FQ_KEY[i],allFqs.get(i)).apply();
        }
    }
 
}
