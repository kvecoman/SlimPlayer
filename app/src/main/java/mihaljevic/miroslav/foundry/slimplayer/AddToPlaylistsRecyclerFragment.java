package mihaljevic.miroslav.foundry.slimplayer;


import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 *
 * Fragment with functionality to add selected songs to playlist
 *
 * @author Miroslav Mihaljević
 */
public class AddToPlaylistsRecyclerFragment extends PlaylistsRecyclerFragment {


    public AddToPlaylistsRecyclerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onClick(View v) {
        int position = mRecyclerView.getChildLayoutPosition(v);

        Cursor cursor = mAdapter.getCursor();
        List<String> ids  = getActivity().getIntent().getStringArrayListExtra(AddToPlaylistActivity.ID_LIST_KEY);
        final long playlistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));

        //Insert songs in playlist
        new AsyncTask<List<String>,Void,Integer>(){

            @Override
            protected Integer doInBackground(List<String>... params)
            {
                List<String> ids = params[0];

                if (ids == null)
                    return null;

                return Utils.insertIntoPlaylist(getContext(),ids,playlistId);
            }

            @Override
            protected void onPostExecute(Integer result) {
                Toast.makeText(mContext,result + " " + getString(R.string.playlist_add_succes),Toast.LENGTH_SHORT).show();
            }
        }.execute(ids);

        //Exit after we start adding songs to playlist
        getActivity().finish();
    }
}
