package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

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


        //Clear cached fragments (otherwise we see old fragments instead of new ones)
        clearCachedFragments(fragmentManager,pagerID);

        //Load screen names (only screens that user selected)
        loadScreenNames(PreferenceManager.getDefaultSharedPreferences(mContext));

        //If none of the screens are selected, then we will show EmptyMessage fragment
        if (mNumScreens == 0)
        {
            mNumScreens = 1;
            mShowEmpty = true;
        }
    }

    //Method that clears all cached fragments from pager with pagerID
    public void clearCachedFragments(FragmentManager fragmentManager, int pagerID)
    {
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
    }

    //Method that loads screen names from preferences
    public void loadScreenNames(SharedPreferences preferences)
    {
        Resources resources = mContext.getResources();
        mScreensList = new ArrayList<>(6);
        String [] allScreenKeys = resources.getStringArray(R.array.pref_screens_set_default);
        for (String key : allScreenKeys)
        {
            if (preferences.getBoolean(key,true))
                ((ArrayList) mScreensList).add(key);
        }
        ((ArrayList) mScreensList).trimToSize();

        mNumScreens = ((ArrayList) mScreensList).size();
    }

    //Note - caching of fragments is already done internally, we only need to do first time init here
    @Override
    public Fragment getItem(int position) {

        //If nothing gets select we make sure to return empty message fragment
        Fragment fragment = new EmptyMessageFragment();
        Bundle args = new Bundle();



        //Init appropriate fragment
        if (mShowEmpty)
        {
            //If we need to show empty fragment, set all params and return it right now
            args.putString(EmptyMessageFragment.MESSAGE_KEY,mContext.getString(R.string.empty_main_screen));
            fragment.setArguments(args);
            return fragment;
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_all_screen)))
        {
            fragment = new SongListFragment();
            args = ScreenBundles.getAllSongsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_playlists_screen)))
        {
            fragment = new PlaylistsFragment();
            args = ScreenBundles.getPlaylistsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_albums_screen)))
        {
            fragment = new CategoryListFragment();
            args = ScreenBundles.getAlbumsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_artists_screen)))
        {
            fragment = new CategoryListFragment();
            args = ScreenBundles.getArtistsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_genres_screen)))
        {
            fragment = new CategoryListFragment();
            args = ScreenBundles.getGenresBundle(mContext);
            fragment.setArguments(args);
        }
        else
        {
            fragment = new SongListFragment();
            args = ScreenBundles.getAllSongsBundle(mContext);
            fragment.setArguments(args);
        }

        return fragment;
    }



    @Override
    public int getCount() {
        return mNumScreens;
    }



}
