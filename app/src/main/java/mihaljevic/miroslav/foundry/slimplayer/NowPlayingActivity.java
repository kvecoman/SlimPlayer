package mihaljevic.miroslav.foundry.slimplayer;

import android.content.SharedPreferences;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;


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


    }

    @Override
    protected void onResume() {
        super.onResume();

        //When we are connected request current play info
        mApplication.getMediaPlayerService().registerPlayListener(NowPlayingActivity.this);

        //This might not be necessary as on play already calls all listeners
        //mApplication.getMediaPlayerService().setCurrentPlayInfoToListener();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mApplication.getMediaPlayerService().unregisterPlayListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu,menu);
        getMenuInflater().inflate(R.menu.now_playing_menu,menu);

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Toggle repeat option
        if (item.getItemId() == R.id.toggle_repeat)
        {

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean repeat = preferences.getBoolean(getString(R.string.pref_key_repeat),true);

            repeat = !repeat;

            if (repeat)
                item.setIcon(R.drawable.ic_repeat_white_24dp);
            else
                item.setIcon(R.drawable.ic_repeat_gray_24dp);

            preferences.edit().putBoolean(getString(R.string.pref_key_repeat),repeat).commit();
            mApplication.getMediaPlayerService().refreshRepeat();

        }

        return super.onOptionsItemSelected(item);
    }

    public ViewPager getPager() {
        return mPager;
    }

    @Override
    public void onPlay(List<Song> songList, int position) {
        //TODO - this looks sloppy
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
