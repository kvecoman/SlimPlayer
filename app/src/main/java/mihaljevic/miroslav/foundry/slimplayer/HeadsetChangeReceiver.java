package mihaljevic.miroslav.foundry.slimplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by Miroslav on 17.12.2016..
 */

public class HeadsetChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (BuildConfig.DEBUG)
        {
            Toast.makeText(context,"Headphones state changed",Toast.LENGTH_SHORT).show();
        }

        MediaPlayerService playerService = SlimPlayerApplication.getInstance().getMediaPlayerService();

        if (intent.hasExtra("state"))
        {
            if (playerService != null)
            {
                //If headset is plugged out, pause the playback
                if (intent.getIntExtra("state",0) == 0)
                {
                    playerService.pause();
                }
            }

        }
    }
}
