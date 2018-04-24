package edu.whut.liufeilin.miaoyi;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import edu.whut.liufeilin.miaoyi.fragment.settingFragment;

/**
 * Created by xch97 on 2018/4/21.
 */



public class settingActivity extends Activity {
    String Language;
    FloatService floatService=MainActivity.floatService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new settingFragment())
                .commit();
    }

    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if(key.equals("language")){
                Language = prefs.getString("language", "");
                Log.e("settingActivity", " " + Language);
                floatService.initTessBaseData(Language);
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
