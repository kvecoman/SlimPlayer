package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.View;

import java.util.List;

/**
 * Fragment used for displaying of different categories like all genres, all abums etc.
 *
 * @author Miroslav Mihaljević
 */
public class CategoryRecyclerFragment extends SlimRecyclerFragment {



    public CategoryRecyclerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onClick(View v) {

        int position = mRecyclerView.getChildLayoutPosition(v);

        //Get cursor and move it to current position
        Cursor cursor = mAdapter.getCursor();
        cursor.moveToPosition(position);

        //Check if we are in select for result mode
        if (mSelectSongsForResult)
        {
            Intent intent = new Intent(PlaylistSongsRecyclerFragment.ACTION_SELECT_SONGS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,getContext(),SongListActivity.class);

            //Choose bundle and send it to songList fragment
            intent.putExtra(    SongListActivity.FRAGMENT_BUNDLE_KEY,
                    ScreenBundles.getBundleForSubScreen(mCurrentSource, cursor, mContext ));
            intent.putExtra(SlimActivity.REQUEST_CODE_KEY,PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2);

            startActivityForResult(intent, PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2);
        }
        else
        {
            //If we are in normal mode just start activity
            Intent intent = new Intent(mContext,SongListActivity.class);

            //Choose bundle and send it to songList fragment
            intent.putExtra(    SongListActivity.FRAGMENT_BUNDLE_KEY,
                    ScreenBundles.getBundleForSubScreen(mCurrentSource, cursor, mContext ));

            //Start next screen
            startActivity(intent);
        }
    }

    //We don't use long click here, but must implement it
    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data != null && requestCode == PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2 && data.hasExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY))
        {
            //Add all selected IDs to existing song ID collection
            List<String> IDs = data.getStringArrayListExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY);
            for (String id : IDs)
            {
                ((SelectSongsActivity) mContext).getSelectedSongsList().add(id);
            }
        }
    }

}
