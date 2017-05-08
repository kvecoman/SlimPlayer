package mihaljevic.miroslav.foundry.slimplayer.fragments;


import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.view.View;

import java.util.List;

import mihaljevic.miroslav.foundry.slimplayer.R;
import mihaljevic.miroslav.foundry.slimplayer.Utils;
import mihaljevic.miroslav.foundry.slimplayer.activities.AddToPlaylistActivity;

/**
 *
 * Fragment with functionality to add previously selected songs to playlist
 *
 * @author Miroslav Mihaljević
 */
public class AddToPlaylistsRecyclerFragment extends PlaylistsRecyclerFragment
{


    public AddToPlaylistsRecyclerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onClick( View v )
    {

        final int                                   position;
        final List<MediaBrowserCompat.MediaItem>    mediaItems;
        final List<String>                          songIDs;
        final long                                  playlistId;
        String                                      mediaID;

        position    = mRecyclerView.getChildLayoutPosition( v );
        mediaItems  = mAdapter.getMediaItemsList();
        mediaID     = mediaItems.get(position).getMediaId();
        songIDs     = getActivity().getIntent().getStringArrayListExtra( AddToPlaylistActivity.SELECTED_IDS_KEY );
        playlistId  = Long.parseLong( mediaID );

        //Insert songs in playlist
        new AsyncTask<Void,Void,Integer>()
        {
            @Override
            protected Integer doInBackground( Void... params )
            {
                return Utils.insertIntoPlaylist( songIDs, playlistId );
            }

            @Override
            protected void onPostExecute( Integer result )
            {
                Utils.toastShort(result + " " + getString( R.string.playlist_add_succes));
                refreshData();
            }
        }.execute();

        //Exit after we start adding songs to playlist
        getActivity().finish();
    }
}
