package com.example.miroslav.slimplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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

//TODO - prevent screen rotation - done
//TODO - finish settings screen
//TODO - take care of first time shared preferences init for list of screens - DONE
//TODO - clean errors and create apropriate fragments in MainScreenPagerAdapter

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener{

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up pager and adapter to show list screens
        mPager = (ViewPager)findViewById(R.id.pager);
        mPagerAdapter = new MainScreenPagerAdapter(getSupportFragmentManager(),this);
        mPager.setAdapter(mPagerAdapter);

        //Register this activity as listener for changed preferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //Activate coresponding action that was selected in menu
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


    }


}
