package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.HashSet;
import java.util.Set;


/**
 * As its name says, this is main activity, tasked with showing starting screens like
 * homescreen, all songs, playlist etc.
 *
 * @author Miroslav Mihaljević
 *
 *
 */

//TODO - Implement ViewPager
//TODO - make the app
//TODO - $$_PROFIT_$$

//TODO - remove empty genres
//TODO - limit width of notification text
//TODO - make notification button fatter
//TODO - tap pauses/plays song
//TODO - back button returns to last viewed page
//TODO - make title for "Select directories" preference nicer

public class MainActivity extends BackHandledFragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    public static final String SCREEN_POSITION_KEY = "screen_position";

    //Indicate whether the preferences have changed
    private boolean mPreferencesChanged = false;

    //Pager that hold different screens (All music, Playlists etc)
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        //Init pager and show screens
        initPager();

        //Register this activity as listener for changed preferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);


    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mPreferencesChanged)
            initPager();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Inflate options menu
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //Activate corresponding action that was selected in menu
        switch (id)
        {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.exit:
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //Set-up pager related stuff
    public void initPager()
    {
        //Set up pager and adapter to show list screens
        mPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new MainScreenPagerAdapter(getSupportFragmentManager(),this,R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        mPagerAdapter.notifyDataSetChanged();
        //TODO - make pager update its fragments
        mPager.setCurrentItem(0);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        //Update a collection set of screens that user wants to see
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor pEditor = pref.edit();

        Set<String> screensSet = new HashSet<>();

        if (pref.getBoolean(getString(R.string.pref_key_home_screen),getResources().getBoolean(R.bool.pref_home_screen_default)))
        {
            screensSet.add(getString(R.string.pref_key_home_screen));
        }

        if (pref.getBoolean(getString(R.string.pref_key_all_screen),getResources().getBoolean(R.bool.pref_all_default)))
        {
            screensSet.add(getString(R.string.pref_key_all_screen));
        }

        if (pref.getBoolean(getString(R.string.pref_key_playlists_screen),getResources().getBoolean(R.bool.pref_playlists_default)))
        {
            screensSet.add(getString(R.string.pref_key_playlists_screen));
        }

        if (pref.getBoolean(getString(R.string.pref_key_albums_screen),getResources().getBoolean(R.bool.pref_albums_default)))
        {
            screensSet.add(getString(R.string.pref_key_albums_screen));
        }

        if (pref.getBoolean(getString(R.string.pref_key_artists_screen),getResources().getBoolean(R.bool.pref_artists_default)))
        {
            screensSet.add(getString(R.string.pref_key_artists_screen));
        }

        if (pref.getBoolean(getString(R.string.pref_key_genres_screen),getResources().getBoolean(R.bool.pref_genres_default)))
        {
            screensSet.add(getString(R.string.pref_key_genres_screen));
        }

        pEditor.putStringSet(getString(R.string.pref_key_screens_set),screensSet);
        pEditor.commit();

        mPreferencesChanged = true;

    }



}
