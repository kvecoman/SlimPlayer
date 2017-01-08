package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static mihaljevic.miroslav.foundry.slimplayer.PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY;
import static mihaljevic.miroslav.foundry.slimplayer.PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2;

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

            if (mContext instanceof AppCompatActivity)
                ((AppCompatActivity)mContext).startActivityForResult(intent, PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2);
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
        /*super.onActivityResult(requestCode, resultCode,data);

        if (data != null && requestCode == PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2 && data.hasExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY))
        {
            if (!(mContext instanceof SelectSongsActivity))
                return;

            SelectSongsActivity selectSongActivity = ((SelectSongsActivity) mContext);

            //Add all selected IDs to existing song ID collection
            List<String> IDs = data.getStringArrayListExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY);
            for (String id : IDs)
            {
                selectSongActivity.getSelectedSongsList().add(id);
            }

            //Check if we need to close this activity right now (this is if user confirmed his selection)
            if (data.hasExtra(SelectSongsActivity.SELECTING_FINISHED_KEY))
            {
                Set<String> selectedSongs = selectSongActivity.getSelectedSongsList();
                int request_code = selectSongActivity.getIntent().getIntExtra(SlimActivity.REQUEST_CODE_KEY, SELECT_SONGS_REQUEST_2);

                Intent intent = new Intent();
                intent.putStringArrayListExtra(SELECTED_SONGS_KEY,new ArrayList<>(selectedSongs));
                intent.putExtra(SelectSongsActivity.SELECTING_FINISHED_KEY,true);

                selectSongActivity.setResult(request_code,intent);
                selectSongActivity.finish();
            }
        }*/
    }

}
