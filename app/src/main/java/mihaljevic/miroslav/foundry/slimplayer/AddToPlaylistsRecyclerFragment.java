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
    public void onClick(View v) {


        final int position = mRecyclerView.getChildLayoutPosition(v);

        final List<MediaBrowserCompat.MediaItem> mediaItems = mAdapter.getMediaItemsList();
        final List<String> ids  = getActivity().getIntent().getStringArrayListExtra(AddToPlaylistActivity.ID_LIST_KEY);
        final long playlistId = Long.parseLong(mediaItems.get(position).getMediaId());

        //Insert songs in playlist
        new AsyncTask<Void,Void,Integer>()
        {
            @Override
            protected Integer doInBackground(Void... params)
            {

                if (ids == null)
                    return null;

                return Utils.insertIntoPlaylist(ids,playlistId);
            }

            @Override
            protected void onPostExecute(Integer result)
            {
                Utils.toastShort(result + " " + getString(R.string.playlist_add_succes));
                MusicProvider.getInstance().invalidateDataAndNotify( mSource,mediaItems.get(position).getMediaId());
            }
        }.execute();

        //Exit after we start adding songs to playlist
        getActivity().finish();
    }
}
