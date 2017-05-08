package mihaljevic.miroslav.foundry.slimplayer.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import mihaljevic.miroslav.foundry.slimplayer.Const;
import mihaljevic.miroslav.foundry.slimplayer.R;
import mihaljevic.miroslav.foundry.slimplayer.activities.SettingsActivity;
import mihaljevic.miroslav.foundry.slimplayer.activities.TestPlayerActivity;

/**
 *Base activity that shares common options in options menu
 *
 *@author Miroslav Mihaljević
 *
 */

public abstract class SlimActivity extends AppCompatActivity
{
    protected final String TAG = getClass().getSimpleName();

    public static String REQUEST_CODE_KEY = "request_code";


    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        //Log.v(TAG,"onCreateOptionsMenu()");

        //Inflate options menu
        getMenuInflater().inflate( R.menu.options_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        int id;

        id = item.getItemId();

        //Activate corresponding action that was selected in menu
        switch (id)
        {
            case R.id.test_activity:
                startActivity( new Intent( this, TestPlayerActivity.class ) );
            case R.id.toggle_repeat:
                //Toggle repeat action - whether to repeat playlist or not at the end
                SharedPreferences   preferences;
                boolean             repeatState;
                boolean             newRepeatState;
                int                 icon;

                preferences     = PreferenceManager.getDefaultSharedPreferences( this );
                repeatState     = preferences.getBoolean( getString(R.string.pref_key_repeat),true );

                newRepeatState = !repeatState;

                //Determine which icon to use
                icon = newRepeatState ? R.drawable.ic_repeat_white_24dp : R.drawable.ic_repeat_gray_24dp;

                item.setIcon( icon );

                preferences .edit()
                            .putBoolean( Const.REPEAT_PREF_KEY, newRepeatState )
                            .apply();


                break;
            case R.id.settings:
                //Start settings screen
                Intent settingsIntent;

                settingsIntent = new Intent( this, SettingsActivity.class );

                startActivity( settingsIntent );
                break;
            case R.id.exit:
                //Exit the app
                System.exit( 1 );
                break;
            default:
                return super.onOptionsItemSelected( item );
        }

        return true;
    }
}
