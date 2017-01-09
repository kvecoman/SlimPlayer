package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class NowPlayingActivity extends BackHandledFragmentActivity implements MediaPlayerService.SongPlayListener,ViewPager.OnPageChangeListener, View.OnClickListener {

    //TODO - check if this class can be optimized


    private ViewPager mPager;
    private NowPlayingPagerAdapter mPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        //mApplication = ((SlimPlayerApplication)getApplicationContext());

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

                getPlayerService().playList(songs,0);
            }

        }


        //If pager is not set with intent extras, then set it with MediaPlayerService
        if (mPagerAdapter == null && SlimPlayerApplication.getInstance().isMediaPlayerServiceBound())
        {
            //Check that media player service has any list loaded and is ready to play
            if (getPlayerService().isReadyToPlay())
            {
                mPagerAdapter = new NowPlayingPagerAdapter(getSupportFragmentManager(),NowPlayingActivity.this,getPlayerService().getCount());
                mPager.setAdapter(mPagerAdapter);
                mPager.setCurrentItem(getPlayerService().getPosition());
            }

        }

        //When we are connected request current play info (NOTE - this has been in onResume before, unknown effects could occur)
        getPlayerService().registerPlayListener(NowPlayingActivity.this);

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

    @Override
    public void onBackPressed() {

        if (isTaskRoot())
        {
            //If we came from notification and this activity is task root then we want back button to return (open) Main activity
            Intent intent = new Intent(this,MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        super.onBackPressed();
    }

    public void updatePagerWithCurrentSong()
    {
        int playPosition = getPlayerService().getPosition();

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

    public MediaPlayerService getPlayerService()
    {
        return SlimPlayerApplication.getInstance().getMediaPlayerService();
    }

    //Handle onscreen taps, change between play/pause
    @Override
    public void onClick(View v)
    {
        if (getPlayerService().isReadyToPlay())
        {
            if (getPlayerService().isPlaying())
            {
                getPlayerService().pause();
            }
            else
            {
                getPlayerService().resumeOrPlay(getPager().getCurrentItem());
            }
        }
    }



    @Override
    protected void onStop() {
        super.onStop();

        getPlayerService().unregisterPlayListener(this);
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
    public void onPageSelected(int position)
    {
        //Play this position only if it is not currently being played
        if (getPlayerService().getPosition() != position)
            getPlayerService().play(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {}
}
