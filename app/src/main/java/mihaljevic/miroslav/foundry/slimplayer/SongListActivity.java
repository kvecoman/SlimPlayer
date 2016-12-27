package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;

/**
 * Activity that is used to display SongListFragment after one of categories is selected from CategoryListFragment
 *
 * @author Miroslav Mihaljević
 */


public class SongListActivity extends SelectSongsActivity {

    //Key for bundle that is intended to be sent with SlimListFragment
    public static final String FRAGMENT_BUNDLE_KEY = "fragment_bundle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_holder);

        //Retrieve bundle intended to be sent with SlimListFragment
        Bundle fragmentBundle = getIntent().getBundleExtra(FRAGMENT_BUNDLE_KEY);

        //If there is bundle for fragment then create that fragment and add it to container
        if (fragmentBundle != null)
        {
            Fragment fragment;

            //If we are opening playlist then load PlaylistSongsFragment
            if (fragmentBundle.getString(SlimListFragment.CURSOR_SCREEN_KEY).contains(getString(R.string.pref_key_playlists_screen)))
                fragment = new PlaylistSongsFragment();
            else
                fragment = new SongListFragment();


            fragment.setArguments(fragmentBundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,fragment).commit();


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //If we are in normal mode and this is playlist songs screen then inflate playlist songs menu
        if (!mSelectSongsForResult && getIntent().getBundleExtra(FRAGMENT_BUNDLE_KEY).getString(SlimListFragment.CURSOR_SCREEN_KEY).contains(getString(R.string.pref_key_playlists_screen)))
        {
            getMenuInflater().inflate(R.menu.playlist_songs_menu,menu);
        }


        return super.onCreateOptionsMenu(menu);
    }
}
