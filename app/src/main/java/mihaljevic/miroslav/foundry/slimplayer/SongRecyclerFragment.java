package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays only songs, and responds appropriately to click
 *
 * @author Miroslav Mihaljević
 */
public class SongRecyclerFragment extends SlimRecyclerFragment {

    //If bundle contains this key
    public static final String PLAY_POSITION_KEY = "play_position";



    //Provides easy access to cursor and fields within it
    protected CursorSongs mSongs;


    public SongRecyclerFragment() {
        // Required empty public constructor
    }

    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);



    }

    @Override
    public void onStart()
    {
        super.onStart();

        //If we are selecting songs for result then we need to enforce select mode, so we don't play songs here
        if (mSelectSongsForResult)
        {
            activateSelectMode();
        }
    }

    //Media player service can't be assigned as member variable at init time because it is not bound, so we just use this
    //... all the time
    private MediaPlayerService getPlayerService()
    {
        return SlimPlayerApplication.getInstance().getMediaPlayerService();
    }

    @Override
    protected void onDataLoaded(Cursor cursor) {
        super.onDataLoaded(cursor);

        mSongs = new CursorSongs(cursor);

        //If we are in normal mode
        if (!mSelectSongsForResult)
        {
            //Check if we need to start any song right now
            if (getArguments().containsKey(PLAY_POSITION_KEY))
            {
                int play_position = getArguments().getInt(PLAY_POSITION_KEY);

                getPlayerService().playList(mSongs,play_position, mCurrentSource,getArguments().getString(ScreenBundles.CURSOR_PARAMETER_KEY));

                //Once we have started the intended song from intent, we delete it so we don't start it again and again
                getArguments().remove(PLAY_POSITION_KEY);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.v(TAG,"onPrepareOptionsMenu()");

        //Show add to playlist only if we are in normal mode (and not select for result mode)
        if (!mSelectSongsForResult)
        {
            MenuItem playlistAddMenuItem = menu.findItem(R.id.playlist_add);

            if (mSelectMode && mSelectedItems.size() > 0) {
                //If we are selecting items then we want option to add them to playlist
                playlistAddMenuItem.setVisible(true);
            } else {
                //Hide option to add to playlist
                playlistAddMenuItem.setVisible(false);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Allow this option only if we are in normal mode
        if (mSelectMode && !mSelectSongsForResult)
        {
            if (item.getItemId() == R.id.playlist_add)
            {
                //Get all checked positions
                //NOTE - this looks a little bit messed after the migration to recycler view from list view
                SparseBooleanArray checkedPositions = mSelectedItems;


                Cursor cursor = mAdapter.getCursor();
                List<String> idList = new ArrayList<>();

                //Transfer IDs from selected songs to ID list
                for (int i = 0; i < checkedPositions.size();i++)
                {
                    int position = checkedPositions.keyAt(i);

                    if (checkedPositions.get(position))
                    {
                        cursor.moveToPosition(position);
                        idList.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                    }
                }

                //Here we call add to playlists activity and pass ID list
                Intent intent = new Intent(mContext,AddToPlaylistActivity.class);
                intent.putExtra(AddToPlaylistActivity.ID_LIST_KEY,(ArrayList)idList);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        int position = mRecyclerView.getChildLayoutPosition(v);

        //If we are not selecting items, then we want to play them
        if (!mSelectMode && !mSelectSongsForResult && mSongs != null)
        {
            //Pass list of songs from which we play and play current position
            getPlayerService().playList(mSongs,position, mCurrentSource,getArguments().getString(ScreenBundles.CURSOR_PARAMETER_KEY));

            //Start NowPlayingActivity
            Intent intent = new Intent(mContext,NowPlayingActivity.class);
            startActivity(intent);
        }

        //If we are selecting items
        if (mSelectMode)
        {
            setItemSelected(position,!isItemSelected(position),v);
        }

        //If we are in select for result mode
        if (mSelectSongsForResult)
        {
            if (isItemSelected(position)) {
                ((SelectSongsActivity) mContext).getSelectedSongsList().add(mSongs.getId(position) + "");
            }
            else
            {
                ((SelectSongsActivity) mContext).getSelectedSongsList().remove(mSongs.getId(position) + "");
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (!mSelectMode)
        {
            activateSelectMode();
            setItemSelected(mRecyclerView.getChildLayoutPosition(v),true,v);
        }
        return true;
    }
}
