package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaylistSongsRecyclerFragment extends SongRecyclerFragment {

    public static final int SELECT_SONGS_REQUEST = 1;

    //We use this request when we are in secondary screen like songs by specific genre
    public static final int SELECT_SONGS_REQUEST_2 = 2;

    public static final String ACTION_SELECT_SONGS = "action_select_songs";

    //Key with which we retrieve result
    public static final String SELECTED_SONGS_KEY = "selected_songs";


    public PlaylistSongsRecyclerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        if (!mSelectSongsForResult)
        {
            MenuItem addToThisPlaylistItem = menu.findItem(R.id.playlist_add_to_this);
            MenuItem deleteItem = menu.findItem(R.id.delete_item);

            if (mSelectMode && mSelectedItems.size() > 0)
            {
                deleteItem.setVisible(true);
                addToThisPlaylistItem.setVisible(false);
            }
            else
            {
                deleteItem.setVisible(false);
                addToThisPlaylistItem.setVisible(true);
            }
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mSelectMode)
        {
            if (item.getItemId() == R.id.playlist_add_to_this)
            {
                //If we have not selected anything, then we run MainActivity for result
                Toast.makeText(getContext(), "Starting MainActivity for result", Toast.LENGTH_SHORT).show();


                Intent intent = new Intent(ACTION_SELECT_SONGS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,getContext(),MainActivity.class);
                intent.putExtra(SlimActivity.REQUEST_CODE_KEY,SELECT_SONGS_REQUEST);

                startActivityForResult(intent,SELECT_SONGS_REQUEST);


            }
        }
        else
        {
            if (item.getItemId() == R.id.delete_item)
            {
                new AsyncTask<SparseBooleanArray,Void,Integer>()
                {
                    @Override
                    protected Integer doInBackground(SparseBooleanArray... params) {

                        return Utils.deleteFromList(getContext(),
                                mAdapter.getCursor(),
                                MediaStore.Audio.Playlists.Members.getContentUri("external",Long.valueOf(getArguments().getString(ScreenBundles.CURSOR_PARAMETER_KEY))),
                                params[0]);
                    }

                    @Override
                    protected void onPostExecute(Integer result) {
                        Toast.makeText(mContext,result + " items deleted",Toast.LENGTH_SHORT).show();
                        deselect();
                        loadDataAsync();
                    }
                }.execute(mSelectedItems);
            }
            else
            {
                //If none of previous conditions was met then allow upper class to handle this
                return super.onOptionsItemSelected(item);
            }


        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data != null && requestCode == SELECT_SONGS_REQUEST && data.hasExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY))
        {
            List<String> ids = data.getStringArrayListExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY);

            //Insert songs in playlist and then refresh data
            new AsyncTask<List<String>,Void,Integer>(){

                @Override
                protected Integer doInBackground(List<String>... params)
                {
                    List<String> ids = params[0];

                    if (ids == null)
                        return null;

                    return Utils.insertIntoPlaylist(getContext(),ids,Long.valueOf(getArguments().getString(ScreenBundles.CURSOR_PARAMETER_KEY)));
                }

                @Override
                protected void onPostExecute(Integer result) {
                    Toast.makeText(mContext,result + " " + getString(R.string.playlist_add_succes),Toast.LENGTH_SHORT).show();
                    loadDataAsync();
                }
            }.execute(ids);
        }
    }


}
