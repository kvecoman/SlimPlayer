package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Bundle;




/**
 * Activity used to add selected songs to playlist (it is accessed from options menu -> add to playlist)
 *
 * @author Miroslav Mihaljević
 */

public class AddToPlaylistActivity extends SlimActivity {

    public static final String ID_LIST_KEY = "id_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_holder);

        //Acquire bundle to query all playlists
        Bundle fragmentBundle = ScreenBundles.getPlaylistsBundle(this);

        if (fragmentBundle != null)
        {
            //Create add to playlist fragment
            AddToPlaylistsRecyclerFragment fragment = new AddToPlaylistsRecyclerFragment();
            fragment.setArguments(fragmentBundle);

            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,fragment).commit();
        }
    }
}
