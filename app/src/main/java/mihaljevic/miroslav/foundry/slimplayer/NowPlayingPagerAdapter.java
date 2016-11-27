package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

/**
 * Created by Miroslav on 15.11.2016..
 */
public class NowPlayingPagerAdapter extends FragmentStatePagerAdapter {

    Context mContext;

    int mCount;


    public NowPlayingPagerAdapter(FragmentManager fragmentManager, Context context, int count)
    {
        super(fragmentManager);

        mContext = context;
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
