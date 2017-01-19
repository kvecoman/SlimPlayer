package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays only songs, and responds appropriately to click
 *
 * @author Miroslav Mihaljević
 */
public class SongRecyclerFragment extends SlimRecyclerFragment implements SlimPlayerApplication.PlayerServiceListener {

    //If bundle contains this key
    public static final String PLAY_POSITION_KEY = "play_position";



    //Provides easy access to cursor and fields within it
    //protected CursorSongs mSongs;

    //private MediaPlayerService mPlayerService;


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

        SlimPlayerApplication.getInstance().registerPlayerServiceListener(this);
    }

    //Media player service can't be assigned as member variable at init time because it is not bound, so we just use this
    //... all the time
   /* private MediaPlayerService getPlayerService()
    {
        return SlimPlayerApplication.getInstance().getMediaPlayerService();
    }*/

    @Override
    protected void onDataLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
        super.onDataLoaded(parentId, children, options);


        autoPlayFromBundle();
    }

    @Override
    public void onPlayerServiceBound(MediaPlayerService playerService) {
        //mPlayerService = playerService;

        autoPlayFromBundle();
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
    public void onStop() {
        super.onStop();

        SlimPlayerApplication.getInstance().unregisterPlayerServiceListener(this);
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


                List<MediaBrowserCompat.MediaItem> mediaItemsList = mAdapter.getMediaItemsList();
                List<String> idList = new ArrayList<>();

                //Transfer IDs from selected songs to ID list
                for (int i = 0; i < checkedPositions.size();i++)
                {
                    int position = checkedPositions.keyAt(i);

                    if (checkedPositions.get(position))
                    {
                        idList.add(mediaItemsList.get(position).getMediaId());
                    }
                }

                //Here we call add to playlists activity and pass ID list
                Intent intent = new Intent(mContext,AddToPlaylistActivity.class);
                intent.putExtra(AddToPlaylistActivity.ID_LIST_KEY,(ArrayList)idList);
                startActivity(intent);
                deselect();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //Automatically start playing if it is said so from the bundle (home-screen case)
    private void autoPlayFromBundle()
    {
        //TODO - fix this and make it play position
        //If we are in normal mode
        /*if (!mSelectSongsForResult && getArguments().containsKey(PLAY_POSITION_KEY) && mPlayerService != null && mSongs != null)
        {
            int play_position = getArguments().getInt(PLAY_POSITION_KEY);

            mPlayerService.playListIfChanged(mSongs,play_position, mCurrentSource,getArguments().getString(ScreenBundles.PARAMETER_KEY));

            //Once we have started the intended song from intent, we delete it so we don't start it again and again
            getArguments().remove(PLAY_POSITION_KEY);
        }*/
    }

    @Override
    public void onClick(View v) {

        int position = mRecyclerView.getChildLayoutPosition(v);
        List<MediaBrowserCompat.MediaItem> mediaItems = mAdapter.getMediaItemsList();

        //TODO - fix this, make this play the item
        //If we are not selecting items, then we want to play them
        /*if (!mSelectMode && !mSelectSongsForResult && mSongs != null && mPlayerService != null)
        {
            //Pass list of songs from which we play and play current position
            mPlayerService.playList(mSongs,position, mCurrentSource,getArguments().getString(ScreenBundles.PARAMETER_KEY));

            //Start NowPlayingActivity
            Intent intent = new Intent(mContext,NowPlayingActivity.class);
            startActivity(intent);
        }*/

        //If we are selecting items
        if (mSelectMode)
        {
            setItemSelected(position,!isItemSelected(position),v);
        }

        //If we are in select for result mode
        if (mSelectSongsForResult)
        {
            if (isItemSelected(position)) {
                ((SelectSongsActivity) mContext).getSelectedSongsList().add(mediaItems.get(position).getMediaId());
            }
            else
            {
                ((SelectSongsActivity) mContext).getSelectedSongsList().remove(mediaItems.get(position).getMediaId());
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
