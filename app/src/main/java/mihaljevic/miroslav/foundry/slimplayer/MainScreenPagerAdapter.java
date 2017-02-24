package mihaljevic.miroslav.foundry.slimplayer;

import android.Manifest;
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

    //In some cases we want to avoid showing home screen,so we check it with this
    private boolean mShowHomeScreen;

    //Context where this adapter is used
    private Context mContext;

    /*public MainScreenPagerAdapter(FragmentManager fragmentManager, Context context, int pagerID)
    {
        this(fragmentManager,context,pagerID,true);
    }*/

    public MainScreenPagerAdapter( Context context, FragmentManager fragmentManager, int pagerID, boolean showHomeScreen)
    {
        super(fragmentManager);

        mContext = context;

        mShowHomeScreen = showHomeScreen;

        //Clear cached fragments (otherwise we see old fragments instead of new ones)
        clearCachedFragments(fragmentManager,pagerID);

        //Load screen names (only screens that user selected)
        loadScreenNames(PreferenceManager.getDefaultSharedPreferences(mContext));

        //Remove home screen if we don't want to show it
        if (!mShowHomeScreen)
        {
            mScreensList.remove(Const.HOME_SCREEN);
        }

        mNumScreens = ((ArrayList) mScreensList).size();

        //If none of the screens are selected, then we will show EmptyMessage fragment
        if (mNumScreens == 0)
        {
            mNumScreens = 1;
            mShowEmpty = true;
        }

        //If we don't have permissions, show just one screen with message
        if (!Utils.checkPermission( mContext, Manifest.permission.READ_EXTERNAL_STORAGE ))
        {
            mNumScreens = 1;
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

    }

    //Note - caching of fragments is already done internally, we only need to do first time init here
    @Override
    public Fragment getItem(int position) {

        //If nothing gets select we make sure to return empty message fragment
        Fragment fragment = new EmptyMessageFragment();
        Bundle args = new Bundle();

        //If we don't have permissions, show appropriate message
        if (!Utils.checkPermission( mContext, Manifest.permission.READ_EXTERNAL_STORAGE ))
        {
            args.putString(EmptyMessageFragment.MESSAGE_KEY,mContext.getString(R.string.permission_storage_none));
            fragment.setArguments(args);
            return fragment;
        }


        //Init appropriate fragment
        if (mShowEmpty)
        {
            //If we need to show empty fragment, set all params and return it right now
            args.putString(EmptyMessageFragment.MESSAGE_KEY,mContext.getString(R.string.empty_main_screen));
            fragment.setArguments(args);
            return fragment;
        }
        else if (mScreensList.get(position).equals(Const.HOME_SCREEN))
        {
            fragment = new HomeFragment();
        }
        else if (mScreensList.get(position).equals(Const.ALL_SCREEN))
        {
            fragment = new SongRecyclerFragment();
            args = ScreenBundles.getAllSongsBundle();
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(Const.PLAYLISTS_SCREEN))
        {
            fragment = new PlaylistsRecyclerFragment();
            args = ScreenBundles.getPlaylistsBundle();
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(Const.ALBUMS_SCREEN))
        {
            fragment = new CategoryRecyclerFragment();
            args = ScreenBundles.getAlbumsBundle();
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(Const.ARTISTS_SCREEN))
        {
            fragment = new CategoryRecyclerFragment();
            args = ScreenBundles.getArtistsBundle();
            fragment.setArguments(args);
        }
        else if (mScreensList.get(position).equals(Const.GENRES_SCREEN))
        {
            fragment = new CategoryRecyclerFragment();
            args = ScreenBundles.getGenresBundle();
            fragment.setArguments(args);
        }
        else
        {
            fragment = new SongRecyclerFragment();
            args = ScreenBundles.getAllSongsBundle();
            fragment.setArguments(args);
        }

        return fragment;
    }



    @Override
    public int getCount() {
        return mNumScreens;
    }



}
