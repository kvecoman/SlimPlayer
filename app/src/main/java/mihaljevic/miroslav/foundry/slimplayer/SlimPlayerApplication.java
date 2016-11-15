package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Application;
import android.content.Intent;

/**
 * Created by Miroslav on 15.11.2016..
 */
public class SlimPlayerApplication extends Application {

    @Override
    public void onCreate()
    {
        super.onCreate();

        //Start media player service so it is bound throughout whole app
        startService(new Intent(this,MediaPlayerService.class));
    }
}
