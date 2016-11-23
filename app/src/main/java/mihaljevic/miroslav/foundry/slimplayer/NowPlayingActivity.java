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

import java.util.List;

//TODO - now playing screen doesnt update when song is changed from MediaPlayerService
public class NowPlayingActivity extends BackHandledFragmentActivity implements MediaPlayerService.MediaPlayerListener, ViewPager.OnPageChangeListener {

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
            Log.d("slim","NowPlayingActivity - onServiceConnected()");

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

            //When we are connected request current play info
            mPlayerService.setMediaPlayerListener(NowPlayingActivity.this);
            mPlayerService.setCurrentPlayInfoToListener();



        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("slim","NowPlayingActivity - onServiceDisconnected()");

            NowPlayingActivity.this.mServiceBound = false;

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

    @Override
    public void onSongChanged(List<Song> songList, int position) {
        Log.d("slim","NowPlayingActivity - onSongChanged()");

        mPager.setCurrentItem(position,true);


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
        Log.d("slim","NowPlayingActivity - onPageSelected()");

        //If this song is already playing no need to start it again
        //(case when you change song outside of this activity)
        mPlayerService.play(position);



    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
