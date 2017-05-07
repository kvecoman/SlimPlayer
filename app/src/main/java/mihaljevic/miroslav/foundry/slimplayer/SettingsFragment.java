package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import java.io.File;
import java.util.Set;

/**
 * Fragment that load preferences from xml file and display it to user
 *
 * @author Miroslav Mihaljević
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    private final String TAG = getClass().getSimpleName();

    //private final int DIRECTORY_SELECT_REQUEST_CODE = 28;


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.settings, rootKey);

        //OnClick listener for Refresh preference
        findPreference(Const.RESCAN_PREF_KEY).setOnPreferenceClickListener( new RescanClickListener() );
    }

    @Override
    public void onDisplayPreferenceDialog( Preference preference )
    {
        DialogFragment  dialogFragment;
        Bundle          args;

        //If the selected preference is Directory Select Preference we then send it
        if ( preference instanceof DirectorySelectPreference )
        {
            args = new Bundle( 1 );
            args.putString( "key", preference.getKey() );

            dialogFragment = new DirectorySelectDialogPreferenceFrag();
            dialogFragment.setArguments     ( args );
            dialogFragment.setTargetFragment( this, 0 );
            dialogFragment.show             ( this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG" );
        }
        else
        {
            super.onDisplayPreferenceDialog( preference );
        }
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        super.onActivityResult( requestCode, resultCode, data );
    }

    private void scanMedia()
    {
        //Get selected directories and run rescan on them
        SharedPreferences   preferences;
        Set<String>         directoriesSet;
        int                 deletedGenres;
        Intent              scanIntent;
        Uri                 scanUri;

        preferences     = PreferenceManager.getDefaultSharedPreferences(getContext());
        directoriesSet  = preferences.getStringSet(getString(R.string.pref_key_directories_set),null);


        //Delete empty genres
        deletedGenres = Utils.deleteEmptyGenres();
        Log.i(TAG,  deletedGenres + " genres deleted");



        if ( directoriesSet != null && !directoriesSet.isEmpty() )
        {
            //Send broadcasts to scan selected directories
            for ( String directory : directoriesSet )
            {
                scanUri     = Uri.fromFile( new File( directory ) );
                scanIntent  = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, scanUri );

                getContext().sendBroadcast( scanIntent );
            }
            Utils.toastShort( getString( R.string.toast_rescan_directories ) );
            Log.d(TAG, "Scanning selected directories for new songs");
        }
        else
        {
            //If there are none selected directories, then scan whole system
            scanUri     = Uri.fromFile( Environment.getRootDirectory() );
            scanIntent  = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, scanUri );


            getContext().sendBroadcast( scanIntent );

            Utils.toastShort( getString( R.string.toast_rescan_storage ) );

            Log.d(TAG, "Scanning storage for new songs");
        }

        MusicProvider.getInstance().invalidateAllData();
    }

    private class RescanClickListener implements Preference.OnPreferenceClickListener
    {
        @Override
        public boolean onPreferenceClick( Preference preference )
        {
            scanMedia();

            return true;
        }
    }
}


