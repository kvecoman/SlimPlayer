package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;

/**
 * Activity that is used to display SongListFragment after one of categories is selected from CategoryListFragment
 *
 * @author Miroslav Mihaljević
 */


public class SongListActivity extends SelectSongsActivity {
    protected final String TAG = getClass().getSimpleName();

    //Key for bundle that is intended to be sent with SlimRecyclerFragment
    //public static final String FRAGMENT_BUNDLE_KEY = "fragment_bundle";

    private String mSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_holder);


        //Fragment that we will display here
        Fragment fragment = null;

        //Retrieve bundle intended to be sent with SlimListFragment
        Bundle fragmentBundle = getIntent().getExtras();


        //Check if everything is okay with bundle
        if (fragmentBundle == null || !getIntent().hasExtra( Const.SOURCE_KEY))
        {
            Log.i(TAG, "onCreate() - Could not load data list fragment, loading empty one instead");
            //If something is wrong with bundle just show empty fragment
            fragment = new EmptyMessageFragment();
            fragmentBundle = new Bundle();
            fragmentBundle.putString(EmptyMessageFragment.MESSAGE_KEY,getString(R.string.empty_songlist));


        }

        //If there is bundle for fragment then create that fragment and add it to container
        if (fragmentBundle != null)
        {
            mSource = fragmentBundle.getString( Const.SOURCE_KEY);

            //If we are opening playlist then load PlaylistSongsFragment
            if (Utils.equalsIncludingNull(mSource,getString(R.string.pref_key_playlists_screen)))
                fragment = new PlaylistSongsRecyclerFragment();
            else
                fragment = new SongRecyclerFragment();

            fragment.setArguments(fragmentBundle);
        }


        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,fragment).commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //If we are in normal mode and this is playlist songs screen then inflate playlist songs menu
        if (!mSelectSongsForResult && Utils.equalsIncludingNull(mSource,getString(R.string.pref_key_playlists_screen)))
            getMenuInflater().inflate(R.menu.playlist_songs_menu,menu);



        return super.onCreateOptionsMenu(menu);
    }
}
