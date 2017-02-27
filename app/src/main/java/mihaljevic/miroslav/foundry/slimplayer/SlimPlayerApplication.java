package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

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


    private Handler mHandler;


    @Override
    public void onCreate()
    {

        sInstance = this;

        mHandler = new Handler( Looper.getMainLooper() );


        super.onCreate();
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
