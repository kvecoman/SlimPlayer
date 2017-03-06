package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.preference.PreferenceManager;

import static mihaljevic.miroslav.foundry.slimplayer.MediaPlayerService.LAST_PARAMETER_KEY;
import static mihaljevic.miroslav.foundry.slimplayer.MediaPlayerService.LAST_POSITION_KEY;
import static mihaljevic.miroslav.foundry.slimplayer.MediaPlayerService.LAST_SOURCE_KEY;
import static mihaljevic.miroslav.foundry.slimplayer.MediaPlayerService.LAST_STATE_PLAYED;
import static mihaljevic.miroslav.foundry.slimplayer.MediaPlayerService.LAST_TITLE_KEY;

/**
 * Created by Miroslav on 15.11.2016..
 *
 * Store service connection and reference to it so every component can access it.
 *
 * @author Miroslav Mihaljević
 */
public class SlimPlayerApplication extends Application
{

    protected final String TAG = getClass().getSimpleName();

    private static SlimPlayerApplication sInstance;

    //Indicate whether the preferences have changed
    private boolean mPreferencesChanged = false;


    private Handler mHandler;

    private MediaBrowserCompat mMediaBrowser;

    protected class ConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();

            MediaControllerCompat mediaController;


            try
            {
                mediaController = new MediaControllerCompat( SlimPlayerApplication.this, mMediaBrowser.getSessionToken() );

                if ( isLastPlaySuccess() )
                {
                    setLastPlayFailed();
                    playLastState( mediaController );
                }

                setLastPlaySuccess();

            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }


            mMediaBrowser.disconnect();
        }
    }


    @Override
    public void onCreate()
    {

        sInstance = this;

        mHandler = new Handler( Looper.getMainLooper() );


        super.onCreate();

        mMediaBrowser = new MediaBrowserCompat( this, MediaPlayerService.COMPONENT_NAME, new ConnectionCallbacks(), null );

        mMediaBrowser.connect();
    }

    private void playLastState( MediaControllerCompat mediaController)
    {
        //Recreate last playback state
        SharedPreferences   prefs;
        String              source;
        String              parameter;
        String              queueTitle;
        int                 position;
        Bundle              extras;

        if ( mMediaBrowser == null || !mMediaBrowser.isConnected() || mediaController == null )
            return;


        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        source      = prefs.getString ( LAST_SOURCE_KEY, null );
        parameter   = prefs.getString ( LAST_PARAMETER_KEY, null );
        position    = prefs.getInt    ( LAST_POSITION_KEY, -1 );
        queueTitle  = prefs.getString ( LAST_TITLE_KEY, "" );

        if ( source == null || position == -1 )
            return;



        extras = new Bundle();
        extras.putString( Const.SOURCE_KEY, source );
        extras.putString( Const.PARAMETER_KEY, parameter );
        extras.putString( Const.DISPLAY_NAME, queueTitle );
        extras.putInt   ( Const.POSITION_KEY, position );

        mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );


    }

    public void setLastPlayFailed()
    {
        SharedPreferences preferences;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferences .edit()
                .putBoolean( LAST_STATE_PLAYED, false )
                .apply();
    }

    public void setLastPlaySuccess()
    {
        SharedPreferences preferences;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferences .edit()
                .putBoolean( LAST_STATE_PLAYED, true )
                .apply();
    }

    public boolean isLastPlaySuccess()
    {
        SharedPreferences preferences;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        return preferences.getBoolean( LAST_STATE_PLAYED, false );
    }



    public Handler getHandler()
    {
        return mHandler;
    }

    public static SlimPlayerApplication getInstance()
    {
        return sInstance;
    }



    //We indicate to any interested component that preferences have changed
    public void notifyPreferencesChanged() { mPreferencesChanged = true; }

    //Components get status on whether the preferences changed
    public boolean isPreferencesChanged() { return mPreferencesChanged; }

    //Components notify that the have responded to changes
    public void consumePreferenceChange() { mPreferencesChanged = false; }


}
