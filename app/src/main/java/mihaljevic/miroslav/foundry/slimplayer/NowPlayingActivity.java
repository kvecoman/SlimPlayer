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


public class NowPlayingActivity extends BackHandledFragmentActivity implements MediaPlayerService.MediaPlayerListener, ViewPager.OnPageChangeListener {

    public static final String SONG_COUNT_KEY = "song_count";
    public static final String SONG_POSITION_KEY = "song_position";

    private SlimPlayerApplication mApplication;

    private ViewPager mPager;
    private NowPlayingPagerAdapter mPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        mApplication = ((SlimPlayerApplication)getApplicationContext());

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();


        //If pager is not set with intent extras, then set it with MediaPlayerService
        if (mPagerAdapter == null && mApplication.isMediaPlayerServiceBound())
        {
            //Check that media player service has any list loaded and is ready to play
            if (mApplication.getMediaPlayerService().isReadyToPlay())
            {
                mPagerAdapter = new NowPlayingPagerAdapter(getSupportFragmentManager(),NowPlayingActivity.this,mApplication.getMediaPlayerService().getCount());
                mPager.setAdapter(mPagerAdapter);
                mPager.setCurrentItem(mApplication.getMediaPlayerService().getPosition());
            }

        }

        //When we are connected request current play info
        mApplication.getMediaPlayerService().setMediaPlayerListener(NowPlayingActivity.this);
        mApplication.getMediaPlayerService().setCurrentPlayInfoToListener();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public void onSongChanged(List<Song> songList, int position) {
        mPager.setCurrentItem(position,true);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {

        mApplication.getMediaPlayerService().play(position);

    }

    @Override
    public void onPageScrollStateChanged(int state) {}
}
