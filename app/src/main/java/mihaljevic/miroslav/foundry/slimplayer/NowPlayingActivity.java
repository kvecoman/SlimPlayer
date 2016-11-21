package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

//TODO - now playing screen doesnt update when song is changed from MediaPlayerService
public class NowPlayingActivity extends BackHandledFragmentActivity implements ViewPager.OnPageChangeListener {

    public static final String SONG_COUNT_KEY = "song_count";
    public static final String SONG_POSITION_KEY = "song_position";

    private ViewPager mPager;
    private NowPlayingPagerAdapter mPagerAdapter;

    private MediaPlayerService mPlayerService;
    private boolean mServiceBound;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            NowPlayingActivity.this.mPlayerService = playerBinder.getService();
            NowPlayingActivity.this.mServiceBound = true;

            if (mPagerAdapter == null)
            {
                //If pager is not already init, do it here
                mPagerAdapter = new NowPlayingPagerAdapter(getSupportFragmentManager(),NowPlayingActivity.this,mPlayerService.getCount());
                mPager.setAdapter(mPagerAdapter);
                //TODO - this could produce out of bounds exception, insert some checks
                mPager.setCurrentItem(mPlayerService.getPosition());
            }

            Log.d("slim","NowPlayingActivity - onServiceConnected()");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            NowPlayingActivity.this.mServiceBound = false;
            Log.d("slim","NowPlayingActivity - onServiceDisconnected()");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        //Here we init MediaPlayerService
        Intent playerServiceIntent = new Intent(this, MediaPlayerService.class);
       bindService(playerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        mPager = (ViewPager)findViewById(R.id.pager);

        Intent intent = getIntent();
        //If we have some info we will init pager right now, if now then when MediaPlayerService is connected
        if (intent.hasExtra(SONG_COUNT_KEY) && intent.hasExtra(SONG_POSITION_KEY))
        {
            mPagerAdapter = new NowPlayingPagerAdapter(getSupportFragmentManager(),this,intent.getIntExtra(SONG_COUNT_KEY,0));
            mPager.setAdapter(mPagerAdapter);
            mPager.setCurrentItem(intent.getIntExtra(SONG_POSITION_KEY,0));
        }

        mPager.addOnPageChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }

    public MediaPlayerService getPlayerService() {
        return mPlayerService;
    }

    public ViewPager getPager() {
        return mPager;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        mPlayerService.play(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
