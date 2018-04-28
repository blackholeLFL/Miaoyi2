package edu.whut.liufeilin.miaoyi.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import edu.whut.liufeilin.miaoyi.fragment.settingFragment;

/**
 * Created by xch97 on 2018/4/21.
 */



public class settingActivity extends Activity {
    private String Language;
    private int size;
    private String color;
    private final FloatService floatService=MainActivity.floatService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new settingFragment())
                .commit();
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if(key.equals("language")){
                Language = prefs.getString("language", "");
                //Log.e("settingActivity", " " + Language);
                floatService.initTessBaseData(Language);
            }
            else if(key.equals("touch_size")){
                size = Integer.parseInt(prefs.getString("touch_size", ""));
                //Log.e("settingActivity size" , " " + size);
                if(floatService.getTouchStatus()==0){
                    floatService.setToucher_size(size);
                    //Log.e("setting.windowManager" , "null");
                }
                else {
                    floatService.setToucher_size(size);
                    floatService.hidePopupWindow();
                    floatService.createToucher();
                }
            }
            else if(key.equals("touch_txt_color")){
                color= prefs.getString("touch_txt_color", "");
                if(floatService.getTouchStatus()==0){
                    floatService.setTextColor(color);
                    //Log.e("setting.windowManager" , "null");
                }
                else {
                    floatService.setTextColor(color);
                    floatService.hidePopupWindow();
                    floatService.createToucher();
                }
            }
        }
    };

    @Override
    protected void onResume() {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        super.onResume();
    }


    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        super.onPause();
    }

}
