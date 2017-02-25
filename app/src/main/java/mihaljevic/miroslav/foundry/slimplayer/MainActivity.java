package mihaljevic.miroslav.foundry.slimplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import junit.framework.Test;


/**
 * As its name says, this is main activity, tasked with showing starting screens like
 * home screen, all songs, playlist etc.
 *
 * @author Miroslav Mihaljević
 *
 *
 */

//TODO - Implement ViewPager
//TODO - make the app
//TODO - $$_PROFIT_$$

//TODO - make trial version of this app
//TODO - indicate that tap paused or resumed the song
//TODO - first time opening the app and home screen is empty, at least add indicators that there are more tabs
//TODO - make visual style for project
//TODO - make visualization
//TODO - ability for some code parts/methods to fail without throwing exception (or with catching exception) (add checks for things you connect to in onStart())
//TODO - add optimizations for screen rotations (already done for NowPlayingFragment)
//TODO - music playback might need its own thread not just AsyncTask
//TODO - sometimes it can happen that Stats.db database is not open at startup, definitively need to check that, it happens at getItemCount()
//TODO - load all songs from folder in queue when playing from file???
//TODO - if playing last song fails, don't do it next time (some sort of pair in preferences that must be completed)

//TODO - continue - MediaPlayerService is synchronized and using AsyncTask but see if it can do more of parallel tasks and whether the syncs are killing performance, also Async other parts of app
public class MainActivity extends SelectSongsActivity implements TextView.OnClickListener
{

    public static final String SCREEN_POSITION_KEY = "screen_position";

    //Pager that hold different screens (All music, Playlists etc)
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);




        //Init pager and show screens
        initPager();

        //Check if there is savedInstanceState and try to restore last page position in pager
        if (savedInstanceState != null && savedInstanceState.containsKey(SCREEN_POSITION_KEY))
        {

            //Restore last position in pager
            int position = savedInstanceState.getInt(SCREEN_POSITION_KEY);
            if ( position >= 0 && position < mPagerAdapter.getCount() )
            {
                mPager.setCurrentItem(position);
            }

        }


    }

    @Override
    protected void onStart()
    {
        super.onStart();

        //Ask for permissions if needed
        if ( Build.VERSION.SDK_INT >= 16 )
        {
            Utils.askPermission( this,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    getString(R.string.permission_storage_explanation),
                    Const.STORAGE_PERMISSIONS_REQUEST );
        }

        updateAfterPreferenceChange();
    }




    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putInt(SCREEN_POSITION_KEY, mPager.getCurrentItem());
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
    {
        switch (requestCode)
        {
            case Const.STORAGE_PERMISSIONS_REQUEST:
                if (permissions.length != 0 && permissions[0].equals( Manifest.permission.READ_EXTERNAL_STORAGE ))
                {
                    if ( grantResults.length != 0 && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED )
                    {
                        //Update screens when we have permission
                        initPager();
                    }

                }
                break;
        }

        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
    }

    //Set-up pager related stuff
    private void initPager()
    {
        //Set up pager and adapter to show list screens
        mPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new MainScreenPagerAdapter(this,getSupportFragmentManager(),R.id.pager,!mSelectSongsForResult);
        mPager.setAdapter(mPagerAdapter);

        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(0);
    }


    private void updateAfterPreferenceChange()
    {
        //If preferences have changed respond accordingly
        if (((SlimPlayerApplication) getApplicationContext()).isPreferencesChanged())
        {
            initPager();
            ((SlimPlayerApplication) getApplicationContext()).consumePreferenceChange();
        }
    }

    //Empty page click handler, opens preferences so user can select screens to be shown
    @Override
    public void onClick(View v) {
        startActivity(new Intent(this, SettingsActivity.class));
    }




}
