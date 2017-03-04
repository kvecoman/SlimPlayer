package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.List;

/**
 * Created by Miroslav on 15.11.2016..
 *
 * Adapter for pager in NowPlayingActivity, it scrolls songs
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingPagerAdapter extends FragmentStatePagerAdapter {

    private int mCount = 0;

    private List<MediaSessionCompat.QueueItem> mQueue;


    public NowPlayingPagerAdapter( FragmentManager fragmentManager, List<MediaSessionCompat.QueueItem> queue )
    {
        super( fragmentManager );

        mQueue = queue;

        if ( mQueue != null )
            mCount = mQueue.size();

    }

    @Override
    public Fragment getItem(int position)
    {
        Fragment            fragment;
        Bundle              args;
        MediaMetadataCompat metadata;


        fragment = new NowPlayingFragment();
        args    = new Bundle(  );

        metadata = MusicProvider.getInstance().getMetadata( mQueue.get( position ).getDescription().getMediaId() );

        args.putParcelable( Const.METADATA_KEY, metadata );
        args.putInt( Const.POSITION_KEY, position );

        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public int getCount() {
        return mCount;
    }

    public List<MediaSessionCompat.QueueItem> getData()
    {
        return mQueue;
    }


}
