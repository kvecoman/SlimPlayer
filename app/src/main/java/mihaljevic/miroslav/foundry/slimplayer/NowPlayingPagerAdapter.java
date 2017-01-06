package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by Miroslav on 15.11.2016..
 *
 * Adapter for pager in NowPlayingActivity, it scrolls songs
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingPagerAdapter extends FragmentStatePagerAdapter {


    private int mCount;


    public NowPlayingPagerAdapter(FragmentManager fragmentManager, Context context, int count)
    {
        super(fragmentManager);

        mCount = count;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new NowPlayingFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(NowPlayingFragment.SONG_POSITION_KEY,position);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getCount() {
        return mCount;
    }


}
