package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment that displays categories/playlists and opens appropriate song lists
 *
 * @author Miroslav Mihaljević
 */

public class CategoryListFragment extends SlimListFragment {


    public CategoryListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //Get cursor and move it to current position
        Cursor cursor = mCursorAdapter.getCursor();
        cursor.moveToPosition(position);

        //Check if we are in select for result mode
        if (mSelectSongsForResult)
        {
            Intent intent = new Intent(PlaylistSongsFragment.ACTION_SELECT_SONGS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,getContext(),SongListActivity.class);

            //Choose bundle and send it to songList fragment
            intent.putExtra(    SongListActivity.FRAGMENT_BUNDLE_KEY,
                    ScreenBundles.getBundleForSubScreen( mCurrentScreen, cursor, mContext ));
            intent.putExtra(SlimActivity.REQUEST_CODE_KEY,PlaylistSongsFragment.SELECT_SONGS_REQUEST_2);

            startActivityForResult(intent, PlaylistSongsFragment.SELECT_SONGS_REQUEST_2);
        }
        else
        {
            //If we are in normal mode just start activity
            Intent intent = new Intent(mContext,SongListActivity.class);

            //Choose bundle and send it to songList fragment
            intent.putExtra(    SongListActivity.FRAGMENT_BUNDLE_KEY,
                                ScreenBundles.getBundleForSubScreen( mCurrentScreen, cursor, mContext ));

            //Start next screen
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);

        if (data != null && requestCode == PlaylistSongsFragment.SELECT_SONGS_REQUEST_2 && data.hasExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY))
        {
            //Add all selected IDs to existing song ID collection
            List<String> IDs = data.getStringArrayListExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY);
            for (String id : IDs)
            {
                ((SelectSongsActivity) mContext).getSelectedSongsList().add(id);
            }
        }
    }

}
