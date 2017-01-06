package mihaljevic.miroslav.foundry.slimplayer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                                    .replace(android.R.id.content, new SettingsFragment())
                                    .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Register this activity as listener for changed preferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Unregister preference change listener when exiting activity
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        //Notify app that we have changed preferences
        ((SlimPlayerApplication) getApplicationContext()).notifyPreferencesChanged();

    }
}
