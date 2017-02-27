package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Bundle;




/**
 * Activity used to add selected songs to playlist (it is accessed from options menu -> add to playlist)
 *
 * @author Miroslav Mihaljević
 */

public class AddToPlaylistActivity extends SlimActivity {

    public static final String SELECTED_IDS_KEY = "selected_IDs";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_holder);

        createAddToPlaylistFragment();

    }

    //Creates and displays AddToPlaylist fragment
    private void createAddToPlaylistFragment()
    {
        AddToPlaylistsRecyclerFragment  fragment;
        Bundle                          fragmentBundle;

        //Acquire bundle to query all playlists
        fragment        = new AddToPlaylistsRecyclerFragment();
        fragmentBundle  = ScreenBundles.getPlaylistsBundle();

        if ( fragmentBundle == null )
            return;

        fragment.setArguments( fragmentBundle );

        getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container,fragment)
                    .commit();

    }
}
