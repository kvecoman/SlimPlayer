package mihaljevic.miroslav.foundry.slimplayer.activities;


import android.os.Bundle;
import android.support.v4.app.Fragment;

import mihaljevic.miroslav.foundry.slimplayer.Utils;
import mihaljevic.miroslav.foundry.slimplayer.fragments.AddToPlaylistsRecyclerFragment;
import mihaljevic.miroslav.foundry.slimplayer.R;
import mihaljevic.miroslav.foundry.slimplayer.ScreenBundles;


/**
 * Activity used to add selected songs to playlist (it is accessed from options menu -> add to playlist)
 *
 * @author Miroslav Mihaljević
 */

public class AddToPlaylistActivity extends SlimActivity
{

    public static final String SELECTED_IDS_KEY = "selected_IDs";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_fragment_holder);

        createAddToPlaylistFragment();


        setupActionBarTitle( getString( R.string.add_to_playlist_title ) );

    }

    //Creates and displays AddToPlaylist fragment
    private void createAddToPlaylistFragment()
    {
        Fragment    fragment;
        Bundle      args;

        //Acquire bundle to query all playlists
        args = ScreenBundles.getPlaylistsBundle();

        if ( args == null )
        {
            fragment = Utils.createEmptyMessageFragment( getString( R.string.empty_list ) );
        }
        else
        {
            fragment = new AddToPlaylistsRecyclerFragment();

            fragment.setArguments( args );
        }



        getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container,fragment)
                    .commit();

    }
}
