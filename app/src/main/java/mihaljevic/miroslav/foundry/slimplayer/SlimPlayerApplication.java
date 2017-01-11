package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Miroslav on 15.11.2016..
 *
 * Store service connection and reference to it so every component can access it.
 *
 * @author Miroslav Mihaljević
 */
public class SlimPlayerApplication extends Application {

    protected final String TAG = getClass().getSimpleName();

    private static SlimPlayerApplication sInstance;

    //Indicate whether the preferences have changed
    private boolean mPreferencesChanged = false;

    //List of listeners that need to be called when service is connected
    private List<PlayerServiceListener> mServiceBoundListeners;

    private MediaPlayerService mPlayerService;
    private boolean mServiceBound;

    //Used to check whether we have alreadytried to play last state from previous instance run
    private boolean mPlayedLastState = false;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG,"onServiceConnected()");


            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            SlimPlayerApplication.this.mPlayerService = playerBinder.getService();
            SlimPlayerApplication.this.mServiceBound = true;

            notifyListenersPlayerServiceBound();

            //Check if we haven't already done first time playback of last state
            if (!mPlayedLastState)
            {
                mPlayerService.playLastStateAsync();
                mPlayedLastState = true;
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG,"onServiceDisconnected()");

            SlimPlayerApplication.this.mServiceBound = false;
            SlimPlayerApplication.this.mPlayerService = null;
        }
    };

    @Override
    public void onCreate()
    {

        sInstance = this;

        //Start media player service
        /*startService(new Intent(this,MediaPlayerService.class));*/

        //Init list for listeners
        mServiceBoundListeners = new ArrayList<>();

        //Init MediaPlayerService
        //bindService(new Intent(this, MediaPlayerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);


        super.onCreate();
    }

    /*@Override
    public void onTerminate() {

        if (mServiceBound)
            unbindService(mServiceConnection);

        super.onTerminate();
    }*/

    public static SlimPlayerApplication getInstance()
    {
        return sInstance;
    }

    public MediaPlayerService getMediaPlayerServiceIfBound() {
        return mPlayerService;
    }

    public boolean isMediaPlayerServiceBound() {
        return mServiceBound;
    }

    //We indicate to any interested component that preferences have changed
    public void notifyPreferencesChanged() {mPreferencesChanged = true;}

    //Components get status on whether the preferences changed
    public boolean isPreferencesChanged() {return mPreferencesChanged;}

    //Components notify that the have responded to changes
    public void consumePreferenceChange() {mPreferencesChanged = false;}

    public void registerPlayerServiceListener(PlayerServiceListener listener)
    {
        Log.v(TAG,"registerPlayerServiceListener() - " + listener);
        //If the service is already bound then serve listener right now
        if (mServiceBound)
            listener.onPlayerServiceBound(mPlayerService);
        else
        {
            //Init MediaPlayerService
            Intent serviceIntent = new Intent(this, MediaPlayerService.class);
            startService(serviceIntent);
            bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }

        mServiceBoundListeners.add(listener);
    }

    public void unregisterPlayerServiceListener(PlayerServiceListener listener)
    {
        Log.v(TAG,"unregisterPlayerServiceListener() - " + listener);
        mServiceBoundListeners.remove(listener);

        if (mServiceBoundListeners.isEmpty() && mServiceBound) {
            Log.d(TAG,"Unbinding from MediaPlayerService");
            unbindService(mServiceConnection);

            //We set it right now to prevent multiple calls to unbindService which throw exception
            mServiceBound = false;
        }
    }

    //NOTE - this must be called only after the MediaPlayerService is bound
    private void notifyListenersPlayerServiceBound()
    {
        for (PlayerServiceListener listener : mServiceBoundListeners)
        {
            if (listener != null)
            {
                listener.onPlayerServiceBound(mPlayerService);
            }
        }
    }

    public interface PlayerServiceListener
    {
        void onPlayerServiceBound(MediaPlayerService playerService);
    }


}
