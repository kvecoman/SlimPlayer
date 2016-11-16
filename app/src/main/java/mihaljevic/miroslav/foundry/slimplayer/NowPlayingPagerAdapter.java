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

    int mPosition;
    int mCount;

   /* private MediaPlayerService mPlayerService;
    private boolean mServiceBound;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            NowPlayingPagerAdapter.this.mPlayerService = playerBinder.getService();
            NowPlayingPagerAdapter.this.mServiceBound = true;

            //When we are connected request current play info
            mPosition = mPlayerService.getPosition();
            mCount = mPlayerService.getCount();

            Log.d("slim","NowPlayingPagerAdapter - onServiceConnected()");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            NowPlayingPagerAdapter.this.mServiceBound = false;
            Log.d("slim","NowPlayingPagerAdapter - onServiceDisconnected()");
        }
    };

    */
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
