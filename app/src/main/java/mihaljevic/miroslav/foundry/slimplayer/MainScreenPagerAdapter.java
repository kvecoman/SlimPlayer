package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Miroslav on 25.9.2016..
 *
 * Pager adapter for main screens like all songs, homescreen, playlists etc.
 *
 * @author Miroslav Mihaljević
 *
 *
 */


public class MainScreenPagerAdapter extends FragmentPagerAdapter {

    //How many screens there are, based on preferences
    private int mNumScreens;

    //Array of screen names/keys that user selected
    private List<String> mScreensList;

    //Collection of fragments for different screens
    private Map<String, ListFragment> mFragmentMap;

    //Activity where this adapter is used
    private AppCompatActivity mActivity;

    public MainScreenPagerAdapter(FragmentManager fragmentManager, Context context)
    {
        super(fragmentManager);

        //TODO - mActivity variable in constructor is susceptible to bugs
        mActivity = (AppCompatActivity) context;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

        //TODO - optimize screen name loading from preferences
        //A small bit of complex code used to load all screen names but leave only the ones that are actually used in order they need to be
        mScreensList = new ArrayList<String>(Arrays.asList(mActivity.getResources().getStringArray(R.array.pref_screens_set_default)));
        Set<String> defaultSet = new HashSet<>(mScreensList);
        List<String> tempList = new ArrayList<>(mScreensList);
        List<String> tempList2= new ArrayList<>();
        tempList2.addAll(preferences.getStringSet(mActivity.getResources().getString(R.string.pref_key_screens_set),defaultSet));
        ((ArrayList)tempList).removeAll(tempList2);
        ((ArrayList)mScreensList).removeAll(tempList);

        mNumScreens = ((ArrayList) mScreensList).size();

        //TODO - do something when none of the screens are selected
    }

    //Note - caching of fragments is already done internally, we only need to do first time init here
    @Override
    public Fragment getItem(int position) {

        SlimListFragment fragment;

        //TODO - maybe move fragment loading to somewhere else? (idk)
        //Init fragments
        if (mScreensList.get(position).equals(mActivity.getString(R.string.pref_key_all_screen)))
        {
            fragment = new SongListFragment();
            Bundle bundle = ScreenBundles.getAllSongsBundle(mActivity);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mActivity.getString(R.string.pref_key_playlists_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getPlaylistsBundle(mActivity);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mActivity.getString(R.string.pref_key_albums_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getAlbumsBundle(mActivity);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mActivity.getString(R.string.pref_key_artists_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getArtistsBundle(mActivity);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mActivity.getString(R.string.pref_key_genres_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getGenresBundle(mActivity);
            fragment.setArguments(bundle);
        }
        else
        {
            fragment = new SongListFragment();
            Bundle bundle = ScreenBundles.getAllSongsBundle(mActivity);
            fragment.setArguments(bundle);
        }

        return fragment;
    }

    @Override
    public int getCount() {
        return mNumScreens;
    }


}
