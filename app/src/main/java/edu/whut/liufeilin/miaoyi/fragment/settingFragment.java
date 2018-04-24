package edu.whut.liufeilin.miaoyi.fragment;


import android.os.Bundle;
import android.preference.PreferenceFragment;
import edu.whut.liufeilin.miaoyi.R;

/**
 * Created by xch97 on 2018/4/21.
 */

public class settingFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //引入Preference XML文件
        addPreferencesFromResource(R.xml.preference);
    }



}
