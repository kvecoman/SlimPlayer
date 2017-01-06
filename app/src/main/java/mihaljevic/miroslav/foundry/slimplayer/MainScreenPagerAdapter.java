package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

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
    private void clearCachedFragments(FragmentManager fragmentManager, int pagerID)
    {
        //If there are any fragments before in fragment manager, clear them
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments != null)
        {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            for (Fragment fragment : fragments)
            {
                //Check if fragment isn't null and that it is coming from view pager
                //NOTE - it might happen that one day fragment tag doesn't contain pager ID...
                //...or it might happen that it is not in format "switcher:*pager_id*"
                if (fragment != null && fragment.getTag().contains("switcher:" + pagerID))
                {
                    transaction.remove(fragment);
                }
            }
            transaction.commitNowAllowingStateLoss();
        }
    }

    //Method that loads screen names from preferences
    private void loadScreenNames(SharedPreferences preferences)
    {
        Resources resources = mContext.getResources();
        mScreensList = new ArrayList<>(6);
        String [] allScreenKeys = resources.getStringArray(R.array.pref_screens_set_default);
        for (String key : allScreenKeys)
        {
            if (preferences.getBoolean(key,true))
                mScreensList.add(key);
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
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_home_screen)))
        {
            fragment = new HomeFragment();
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_all_screen)))
        {
            fragment = new SongRecyclerFragment();
            args = ScreenBundles.getAllSongsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_playlists_screen)))
        {
            fragment = new PlaylistsRecyclerFragment();
            args = ScreenBundles.getPlaylistsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_albums_screen)))
        {
            fragment = new CategoryRecyclerFragment();
            args = ScreenBundles.getAlbumsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_artists_screen)))
        {
            fragment = new CategoryRecyclerFragment();
            args = ScreenBundles.getArtistsBundle(mContext);
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(mContext.getString(R.string.pref_key_genres_screen)))
        {
            fragment = new CategoryRecyclerFragment();
            args = ScreenBundles.getGenresBundle(mContext);
            fragment.setArguments(args);
        }
        else
        {
            fragment = new SongRecyclerFragment();
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
