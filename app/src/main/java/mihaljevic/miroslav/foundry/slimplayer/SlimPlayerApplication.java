package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

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
    //private List<PlayerServiceBoundListener> mServiceBoundListeners;

    private MediaPlayerService mPlayerService;
    private boolean mServiceBound;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected()");


            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            SlimPlayerApplication.this.mPlayerService = playerBinder.getService();
            SlimPlayerApplication.this.mServiceBound = true;

            //notifyPlayerServiceBound();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected()");

            SlimPlayerApplication.this.mServiceBound = false;

        }
    };

    @Override
    public void onCreate()
    {

        sInstance = this;

        //Start media player service
        startService(new Intent(this,MediaPlayerService.class));

        //Here we init MediaPlayerService
        Intent playerServiceIntent = new Intent(this, MediaPlayerService.class);
        bindService(playerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        //Init list for listeners
        //mServiceBoundListeners = new ArrayList<>();

        super.onCreate();
    }

    @Override
    public void onTerminate() {
        //Unbind service
        if (mServiceBound)
            unbindService(mServiceConnection);

        super.onTerminate();
    }

    public static SlimPlayerApplication getInstance()
    {
        return sInstance;
    }

    public MediaPlayerService getMediaPlayerService() {
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

    /*public void registerPlayerServiceBoundListener(PlayerServiceBoundListener listener)
    {
        mServiceBoundListeners.add(listener);
    }

    public void unregisterPlayerServiceBoundListener(PlayerServiceBoundListener listener)
    {
        mServiceBoundListeners.remove(listener);
    }

    private void notifyPlayerServiceBound()
    {
        for (PlayerServiceBoundListener listener : mServiceBoundListeners)
        {
            if (listener != null)
            {
                listener.onPlayerServiceBound();
            }
        }
    }*/

    /*public interface PlayerServiceBoundListener
    {
        void onPlayerServiceBound();
    }*/


}
