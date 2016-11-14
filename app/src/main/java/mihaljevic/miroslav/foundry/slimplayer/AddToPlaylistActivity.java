package mihaljevic.miroslav.foundry.slimplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;




/**
 * Activity used to add selected songs to playlist (it is accesed from options menu -> add to playlist)
 *
 * @author Miroslav Mihaljević
 */

public class AddToPlaylistActivity extends AppCompatActivity {

    public static final String ID_LIST_KEY = "id_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_to_playlist);

        //Acquire bundle to query all playlists
        Bundle fragmentBundle = ScreenCursors.getPlaylistsBundle(this);

        if (fragmentBundle != null)
        {
            //Create add to playlist fragment
            AddToPlaylistFragment fragment = new AddToPlaylistFragment();
            fragment.setArguments(fragmentBundle);

            getSupportFragmentManager().beginTransaction().replace(R.id.playlist_fragment_container,fragment).commit();
        }
    }
}
