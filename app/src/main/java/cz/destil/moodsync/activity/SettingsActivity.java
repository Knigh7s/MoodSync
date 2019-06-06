package cz.destil.moodsync.activity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.Map;

import cz.destil.moodsync.R;
import cz.destil.moodsync.core.App;
import cz.destil.moodsync.event.PreferenceEvent;

public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainSettingsFragment()).commit();
        App.bus().register(this);
    }

    @Override
    protected void onDestroy() {
        App.bus().unregister(this);
        super.onDestroy();
    }


    public static class MainSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            Map<String,?> keys = PreferenceManager.getDefaultSharedPreferences(getActivity()).getAll();
            for(Map.Entry<String,?> entry : keys.entrySet()){
                Preference preference = findPreference(entry.getKey());
                bindPreferenceChange(preference);
            }
        }
    }

    private static void bindPreferenceChange(Preference preference){
        preference.setOnPreferenceChangeListener(listener);
    }

    private static Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            App.bus().post(new PreferenceEvent());
            return true;
        }
    };
}
