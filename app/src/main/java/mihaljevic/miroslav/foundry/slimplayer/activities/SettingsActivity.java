package mihaljevic.miroslav.foundry.slimplayer.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;

import mihaljevic.miroslav.foundry.slimplayer.fragments.SettingsFragment;
import mihaljevic.miroslav.foundry.slimplayer.SlimPlayerApplication;

public class SettingsActivity extends AppCompatActivity  implements SharedPreferences.OnSharedPreferenceChangeListener
{

    //private PermissionListener mPermissionListener;


    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                                    .replace( android.R.id.content, new SettingsFragment() )
                                    .commit();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //Register this activity as listener for changed preferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        //Unregister preference change listener when exiting activity
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        //Notify app that we have changed preferences
        SlimPlayerApplication.getInstance().notifyPreferencesChanged();

    }

    /*@Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
    {
        super.onRequestPermissionsResult( requestCode, permissions, grantResults );

        if ( mPermissionListener != null )
            mPermissionListener.onRequestPermissionsResult( requestCode, permissions, grantResults );
    }

    public void registerPermissionListener( PermissionListener permissionListener )
    {
        mPermissionListener = permissionListener;
    }



    //Interface used to pass on permission request result
    public interface PermissionListener
    {
        void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults );
    }*/
}
