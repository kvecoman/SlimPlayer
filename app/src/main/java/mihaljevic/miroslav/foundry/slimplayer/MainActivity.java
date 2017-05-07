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
import android.support.v7.app.ActionBar;
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

//TODO - Blank - Disfigure for trailer/promo song

//TODO - make trial version of this app
//TODO - indicate that tap paused or resumed the song
//TODO - first time opening the app and home screen is empty, at least add indicators that there are more tabs ( or add all songs option )
//TODO - make visual style for project
//TODO - ability for some code parts/methods to fail without throwing exception (or with catching exception) ( add checks for things you connect to in onStart() )
//TODO - add optimizations for screen rotations ( already done for NowPlayingFragment )
//TODO - test again receiving a call while playing
//TODO - see licences for glide and maybe some other stuff to put it somewhere
//TODO - bug when returning from song in all screen, either the cause is leaked service connection or illegal state that fragment is not attached to activity
//TODO - handle not having GL ES 2.0
//TODO - text for rescan option ( toast )
//TODO - visual indicator song is paused in NowPlayingScreen
//TODO - on newer android versions, selected directory should be automatically songs directory


public class MainActivity extends SelectSongsActivity implements TextView.OnClickListener
{

    public static final String SCREEN_POSITION_KEY = "screen_position";

    //Pager that hold different screens (All music, Playlists etc)
    private ViewPager       mPager;
    private PagerAdapter    mPagerAdapter;


    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_pager );


        //Init pager and show screens
        initPager();

        //Check if there is savedInstanceState and try to restore last page position in pager
        if ( savedInstanceState != null && savedInstanceState.containsKey( SCREEN_POSITION_KEY ) )
        {

            //Restore last position in pager
            int lastPagerScreen;

            lastPagerScreen = savedInstanceState.getInt( SCREEN_POSITION_KEY );

            if ( lastPagerScreen >= 0 && lastPagerScreen < mPagerAdapter.getCount() )
            {
                mPager.setCurrentItem( lastPagerScreen );
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
            Utils.askPermission(    this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    getString( R.string.permission_storage_explanation ),
                                    Const.STORAGE_PERMISSIONS_REQUEST );
        }

        updateAfterPreferenceChange();
    }


    @Override
    protected void onSaveInstanceState( Bundle outState )
    {
        super.onSaveInstanceState( outState );

        outState.putInt( SCREEN_POSITION_KEY, mPager.getCurrentItem() );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
    {
        switch ( requestCode )
        {
            case Const.STORAGE_PERMISSIONS_REQUEST:
                if ( permissions.length != 0 && permissions[ 0 ].equals( Manifest.permission.READ_EXTERNAL_STORAGE ) )
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
        mPager          = ( ViewPager ) findViewById( R.id.pager );
        mPagerAdapter   = new MainScreenPagerAdapter( this, getSupportFragmentManager(), R.id.pager, !mSelectSongsForResult );
        mPager.setAdapter( mPagerAdapter );

        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem( 0 );
    }


    private void updateAfterPreferenceChange()
    {
        //If preferences have changed respond accordingly
        if ( ( ( SlimPlayerApplication ) getApplicationContext() ).isPreferencesChanged() )
        {
            initPager();
            ( ( SlimPlayerApplication ) getApplicationContext() ).consumePreferenceChange();
        }
    }


    private void setActionBarTitle()
    {
        ActionBar actionBar;

        actionBar = getSupportActionBar();

        actionBar.setTitle( getString( R.string.app_name ) );
    }

    //Empty page click handler, opens preferences so user can select screens to be shown
    @Override
    public void onClick( View v )
    {
        startActivity( new Intent( this, SettingsActivity.class ) );
    }




}
