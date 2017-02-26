package mihaljevic.miroslav.foundry.slimplayer;


import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.view.View;

import java.util.List;

/**
 *
 * Fragment with functionality to add previously selected songs to playlist
 *
 * @author Miroslav Mihaljević
 */
public class AddToPlaylistsRecyclerFragment extends PlaylistsRecyclerFragment {


    public AddToPlaylistsRecyclerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onClick(View v)
    {

        final int position;
        final List<MediaBrowserCompat.MediaItem> mediaItems;
        final List<String> songIDs;
        final long playlistId;

        position = mRecyclerView.getChildLayoutPosition(v);
        mediaItems = mAdapter.getMediaItemsList();
        songIDs = getActivity().getIntent().getStringArrayListExtra(AddToPlaylistActivity.ID_LIST_KEY);
        playlistId = Long.parseLong(mediaItems.get(position).getMediaId());

        //Insert songs in playlist
        new AsyncTask<Void,Void,Integer>()
        {
            @Override
            protected Integer doInBackground(Void... params)
            {
                return Utils.insertIntoPlaylist( songIDs, playlistId );
            }

            @Override
            protected void onPostExecute(Integer result)
            {
                Utils.toastShort(result + " " + getString(R.string.playlist_add_succes));
                refreshData();
            }
        }.execute();

        //Exit after we start adding songs to playlist
        getActivity().finish();
    }
}
