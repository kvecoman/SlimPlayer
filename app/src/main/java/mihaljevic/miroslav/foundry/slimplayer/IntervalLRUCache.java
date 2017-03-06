package mihaljevic.miroslav.foundry.slimplayer;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by miroslav on 21.02.17..
 * <p>
 * Same as LRUCache but it will delete last entry after passing of every interval until cache is empty
 */

public class IntervalLRUCache<K, V> extends LRUCache<K, V>
{
    private Timer mTimer;

    private TimerTask mTimerTask = new TimerTask()
    {
        @Override
        public void run()
        {
            Log.v( TAG, "remove last node task");
            removeLastNode();

        }
    };

    public IntervalLRUCache( int capacity, int interval )
    {
        super( capacity );

        long intervalMs;

        intervalMs = interval * 60 * 1000;

        mTimer = new Timer( true );
        mTimer.schedule( mTimerTask, 0, intervalMs );
    }

}
