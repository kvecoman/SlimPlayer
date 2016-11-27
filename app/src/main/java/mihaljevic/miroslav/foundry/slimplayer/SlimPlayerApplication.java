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
 */
public class SlimPlayerApplication extends Application {

    //public static Object MEDIA_PLAYER_SERVICE_LOCK = new Object();

    private static SlimPlayerApplication sInstance;

    private MediaPlayerService mPlayerService;
    private boolean mServiceBound;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("slim","SlimPlayerApplication - onServiceConnected()");


            //TODO - continue here - this locks wont work because all of this, even services run on same thread (UI/Main)
            //find some other way for async checking of service binding or dont check at all (idk what is going to happen then)

            //maybe message handlers?

            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            SlimPlayerApplication.this.mPlayerService = playerBinder.getService();
            SlimPlayerApplication.this.mServiceBound = true;
            //MEDIA_PLAYER_SERVICE_LOCK.notifyAll();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("slim","SlimPlayerApplication - onServiceDisconnected()");

            SlimPlayerApplication.this.mServiceBound = false;

        }
    };

    @Override
    public void onCreate()
    {

        sInstance = this;

        //Start media player service
        startService(new Intent(this,MediaPlayerService.class));

        //TODO - maybe try block for this, looks dangerous
        //Here we init MediaPlayerService
        Intent playerServiceIntent = new Intent(this, MediaPlayerService.class);
        bindService(playerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

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
}
