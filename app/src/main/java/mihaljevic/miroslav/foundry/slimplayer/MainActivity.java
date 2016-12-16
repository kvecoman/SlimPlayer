package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;


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

//TODO - separate activities like AddToPlaylistActivity or SongListActivity can be unified and made into HolderActivity (actually they can't, but they can have a superclass)
//TODO - remove empty genres
//TODO - make visual style for project
//TODO - indicate that tap paused or resumed the song
//TODO - earphones in/out resume/stop playback
//TODO - make homescreen
//TODO - make visualization

    //TODO - continue here - continue optimizing
public class MainActivity extends SelectSongsActivity implements TextView.OnClickListener{

    public static final String SCREEN_POSITION_KEY = "screen_position";



    //Pager that hold different screens (All music, Playlists etc)
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        //Init pager and show screens
        initPager();

        //Check if there is savedInstanceState and try to restore last page in pager
        if (savedInstanceState != null)
        {
            if (savedInstanceState.containsKey(SCREEN_POSITION_KEY))
            {
                //Restore last position in pager
                int position = savedInstanceState.getInt(SCREEN_POSITION_KEY);
                if (position < mPagerAdapter.getCount() && position >= 0)
                {
                    mPager.setCurrentItem(position);
                }
            }
        }


    }

    @Override
    protected void onStart() {
        super.onStart();

        //If preferences have changed respond accordingly
        if (((SlimPlayerApplication) getApplicationContext()).isPreferencesChanged()) {
            initPager();
            ((SlimPlayerApplication) getApplicationContext()).consumePreferenceChange();
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SCREEN_POSITION_KEY,mPager.getCurrentItem());
    }

    //Empty page click handler, opens preferences so user can select screens to be shown
    @Override
    public void onClick(View v) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    //Set-up pager related stuff
    public void initPager()
    {
        //Set up pager and adapter to show list screens
        mPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new MainScreenPagerAdapter(getSupportFragmentManager(),this,R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(0);
    }



}
