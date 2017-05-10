package mihaljevic.miroslav.foundry.slimplayer.fragments;


import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Set;

import mihaljevic.miroslav.foundry.slimplayer.Const;
import mihaljevic.miroslav.foundry.slimplayer.DirectorySelectPreference;
import mihaljevic.miroslav.foundry.slimplayer.MusicProvider;
import mihaljevic.miroslav.foundry.slimplayer.Player;
import mihaljevic.miroslav.foundry.slimplayer.R;
import mihaljevic.miroslav.foundry.slimplayer.Utils;
import mihaljevic.miroslav.foundry.slimplayer.activities.SettingsActivity;

/**
 * Fragment that load preferences from xml file and display it to user
 *
 * @author Miroslav Mihaljević
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    private final String TAG = getClass().getSimpleName();

    //private final int DIRECTORY_SELECT_REQUEST_CODE = 28;

    public static final int RECORD_AUDIO_PERMISSION_CODE = 96;


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource( R.xml.settings, rootKey);

        //OnClick listener for Refresh preference
        findPreference( Const.RESCAN_PREF_KEY).setOnPreferenceClickListener( new RescanClickListener() );

        findPreference( getString( R.string.pref_key_player_engine ) ).setOnPreferenceChangeListener( new InternalPlayerSelectedListener() );

        findPreference( getString( R.string.pref_key_visualization ) ).setOnPreferenceChangeListener( new VisualizationTurnedOnListener() );
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

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
    {

        switch ( requestCode )
        {
            case RECORD_AUDIO_PERMISSION_CODE:
                if ( permissions.length != 0 && permissions[ 0 ].equals( Manifest.permission.RECORD_AUDIO ) )
                {
                    if ( grantResults.length != 0 && grantResults[ 0 ] == PackageManager.PERMISSION_DENIED )
                    {
                        recordAudioPermissionDenied();
                    }
                }
                break;
        }


        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
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


    private void recordAudioPermissionDenied()
    {
        turnOffVisualizerPreference();
    }


    private void turnOffVisualizerPreference()
    {
        SharedPreferences prefs;

        prefs = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences( getContext() );

        prefs.edit().putBoolean( getString( R.string.pref_key_visualization ), false ).apply();

        ( ( CheckBoxPreference )findPreference( getString( R.string.pref_key_visualization ) ) ).setChecked( false );
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

    //In this callback we make sure that when android media player is selected we also get microphone permission
    private class InternalPlayerSelectedListener implements ListPreference.OnPreferenceChangeListener
    {
        @Override
        public boolean onPreferenceChange( Preference preference, Object newValue )
        {
            String      playerCode;
            Activity    settingsActivity;

            if ( !(newValue instanceof String) )
                return false;



            playerCode          = (String)newValue;
            settingsActivity    = getActivity();

            if ( playerCode.isEmpty() )
                return false;

            /*if ( settingsActivity == null )
                return true;*/

            if ( TextUtils.equals( playerCode, getString( R.string.code_media_player ) ) )
            {


                //Ask for microphone permission
                Utils.askPermission(
                        getContext(),
                        SettingsFragment.this,
                        android.Manifest.permission.RECORD_AUDIO,
                        getString( R.string.record_audio_explanation ),
                        RECORD_AUDIO_PERMISSION_CODE,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick( DialogInterface dialog, int which )
                            {
                                //If we didn't get microphone permission then turn off visualizer
                                recordAudioPermissionDenied();
                            }
                        }
                );
            }

            return true;
        }
    }



    private class VisualizationTurnedOnListener implements CheckBoxPreference.OnPreferenceChangeListener
    {
        @Override
        public boolean onPreferenceChange( Preference preference, Object newValue )
        {
            Boolean     turnedOn;
            //Activity    settingsActivity;

            turnedOn            = ( Boolean ) newValue;
            //settingsActivity    = getActivity();

            if ( turnedOn /*&& settingsActivity != null*/ && Utils.getSelectedPlayerEngine() == Player.PLAYER_MEDIA_PLAYER )
            {
                Utils.askPermission(
                        getContext(),
                        SettingsFragment.this,
                        android.Manifest.permission.RECORD_AUDIO,
                        getString( R.string.record_audio_explanation ),
                        RECORD_AUDIO_PERMISSION_CODE,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick( DialogInterface dialog, int which )
                            {
                                //If we don't get permission, then turn off visualizer
                                turnOffVisualizerPreference();
                            }
                        } );
            }

            return true;
        }
    }





}


