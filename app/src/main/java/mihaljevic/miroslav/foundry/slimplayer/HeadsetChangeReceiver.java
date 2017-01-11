package mihaljevic.miroslav.foundry.slimplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver when headset plugged state is changed,
 * used to pause playback when headset is plugged out
 *
 * @author Miroslav Mihaljević
 */

public class HeadsetChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        MediaPlayerService playerService = SlimPlayerApplication.getInstance().getMediaPlayerServiceIfBound();

        //If something is wrong just return
        if (!intent.hasExtra("state") || playerService == null)
            return;


        //If headset is plugged out, pause the playback
        if (intent.getIntExtra("state",0) == 0)
        {
            playerService.pause();
        }
    }
}
