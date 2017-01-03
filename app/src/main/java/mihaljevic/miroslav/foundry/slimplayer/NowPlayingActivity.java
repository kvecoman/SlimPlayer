package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;


public class NowPlayingActivity extends BackHandledFragmentActivity implements MediaPlayerService.SongPlayListener,ViewPager.OnPageChangeListener {

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

        Intent intent = getIntent();

        //Here we handle if playback is started from file
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW))
        {
            //This activity is called from outside, when playing audio files
            Uri dataUri = intent.getData();

            if (dataUri.getScheme().contains("file"))
            {
                FileSongs songs = new FileSongs();
                songs.addFile(dataUri.getPath());

                mApplication.getMediaPlayerService().playList(songs,0);
            }

        }


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

        //When we are connected request current play info (NOTE - this has been in onResume before, unknown effects could occur)
        mApplication.getMediaPlayerService().registerPlayListener(NowPlayingActivity.this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        //Update pager with current song when we return to this activity
        updatePagerWithCurrentSong();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.now_playing_menu,menu);


        updateRepeatIcon(menu);

        return true;
    }

    public void updatePagerWithCurrentSong()
    {
        int playPosition = mApplication.getMediaPlayerService().getPosition();

        if (playPosition == mPager.getCurrentItem() || playPosition < 0 || playPosition >= mPagerAdapter.getCount())
            return;

        mPager.setCurrentItem(playPosition);

    }

    public void updateRepeatIcon(Menu menu)
    {
        //Set correct icon for toggle repeat action
        MenuItem repeatItem = menu.findItem(R.id.toggle_repeat);
        if (repeatItem != null)
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            if (preferences.getBoolean(getString(R.string.pref_key_repeat),true))
                repeatItem.setIcon(R.drawable.ic_repeat_white_24dp);
            else
                repeatItem.setIcon(R.drawable.ic_repeat_gray_24dp);
        }
    }



    @Override
    protected void onStop() {
        super.onStop();

        mApplication.getMediaPlayerService().unregisterPlayListener(this);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }


    public ViewPager getPager() {
        return mPager;
    }

    @Override
    public void onPlay(Songs songs, int position) {
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
