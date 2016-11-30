package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
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

    //Static variable representing base id for next instance of this adapter
    //private static int sBaseId = 0;

    //private FragmentManager mFragmentManager;

    //Base id for this instance
    //private int mBaseId;

    //How many screens there are, based on preferences
    private int mNumScreens;

    //Whether to show empty screen if none of the screens are selected
    private boolean mShowEmpty = false;

    //Array of screen names/keys that user selected
    private List<String> mScreensList;

    //Collection of fragments for different screens
    private Map<String, ListFragment> mFragmentMap;

    //Context where this adapter is used
    private Context mContext;

    public MainScreenPagerAdapter(FragmentManager fragmentManager, Context context, int pagerID)
    {
        super(fragmentManager);

        mContext = context;
        //mFragmentManager = fragmentManager;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        //Get base ID for this instance
        /*mBaseId = sBaseId;
        sBaseId++;*/

        //If there are any fragments before in fragment manager, clear them
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments != null)
        {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (Fragment fragment : fragments)
            {
                if (fragment != null)
                {
                    //Check if fragment isn't null and that it is coming from view pager
                    //NOTE - it might happen one day that pagerID is some low number like position and
                    // ...it would delete all non null fragments (not really bad, but unnecessary
                    if (fragment.getTag().contains("" + pagerID));
                    {
                        transaction.remove(fragment);
                    }
                }

            }
            transaction.commitAllowingStateLoss();
        }

        //TODO - optimize screen name loading from preferences
        //A small bit of complex code used to load all screen names but leave only the ones that are actually used in order they need to be
        mScreensList = new ArrayList<>(Arrays.asList(mContext.getResources().getStringArray(R.array.pref_screens_set_default)));
        Set<String> defaultSet = new HashSet<>(mScreensList);
        List<String> tempList = new ArrayList<>(mScreensList);
        List<String> tempList2= new ArrayList<>();
        tempList2.addAll(preferences.getStringSet(mContext.getResources().getString(R.string.pref_key_screens_set),defaultSet));
        ((ArrayList)tempList).removeAll(tempList2);
        ((ArrayList)mScreensList).removeAll(tempList);

        mNumScreens = ((ArrayList) mScreensList).size();

        //If none of the screens are selected, then we will show EmptyMessage fragment
        if (mNumScreens == 0)
        {
            mNumScreens = 1;
            mShowEmpty = true;
        }

    }

    //Note - caching of fragments is already done internally, we only need to do first time init here
    @Override
    public Fragment getItem(int position) {

        Fragment fragment;

        //TODO - maybe move fragment loading to somewhere else? (idk)
        //Init fragments
        if (mShowEmpty)
        {
            fragment = new EmptyMessageFragment();
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_all_screen)))
        {
            fragment = new SongListFragment();
            Bundle bundle = ScreenBundles.getAllSongsBundle(mContext);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_playlists_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getPlaylistsBundle(mContext);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_albums_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getAlbumsBundle(mContext);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_artists_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getArtistsBundle(mContext);
            fragment.setArguments(bundle);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_genres_screen)))
        {
            fragment = new CategoryListFragment();
            Bundle bundle = ScreenBundles.getGenresBundle(mContext);
            fragment.setArguments(bundle);
        }
        else
        {
            fragment = new SongListFragment();
            Bundle bundle = ScreenBundles.getAllSongsBundle(mContext);
            fragment.setArguments(bundle);
        }

        return fragment;
    }

    //Clears all fragments that are loaded
    /*public void clearFragments()
    {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        Fragment fragment;
        for (int i = 0;i < mNumScreens)
        {
            fragment = mFragmentManager.findFragmentByTag(getItemId(i));
            transaction.
        }
    }*/

    @Override
    public int getCount() {
        return mNumScreens;
    }

    //This gets called when notifyDataSetHasChanged
   /* @Override
    public int getItemPosition(Object object)
    {
        return POSITION_NONE;
    }*/

    //This is part of solution to clear all fragments when updtating view pager
    /*@Override
    public long getItemId(int position)
    {
        return mBaseId + position;
    }*/

}
