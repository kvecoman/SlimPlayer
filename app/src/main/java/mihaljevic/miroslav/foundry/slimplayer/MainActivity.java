package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;


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

//TODO - indicate that tap paused or resumed the song
//TODO - loadDataAsync() bug, sometimes during normal work, CursorClosedException is thrown, locate when it is called
//TODO - make visual style for project
//TODO - make visualization
//TODO - ability for some code parts/methods to fail without throwing exception (or with catching exception)
//TODO - 2 step process of adding to playlist?????? - songs from step 1 are added to step 2 and when user confirms everything is added at same moment

//TODO - continue here - keep rolling
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

        updateAfterPreferenceChange();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
    private void initPager()
    {
        //Set up pager and adapter to show list screens
        mPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new MainScreenPagerAdapter(getSupportFragmentManager(),this,R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(0);
    }


    private void updateAfterPreferenceChange()
    {
        //If preferences have changed respond accordingly
        if (((SlimPlayerApplication) getApplicationContext()).isPreferencesChanged()) {
            initPager();
            ((SlimPlayerApplication) getApplicationContext()).consumePreferenceChange();
        }
    }


}
