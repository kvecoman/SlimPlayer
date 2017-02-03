package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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
//TODO - restore auto play feature (when list is selected from home screen and uses PLAY_POSITION_KEY)
public class SongRecyclerFragment extends SlimRecyclerFragment{

    //If bundle contains this key
    public static final String PLAY_POSITION_KEY = "play_position";

    //Indicate whether this list/queue is loaded in MediaPlayerService
    protected boolean mQueueLoaded = false;




    protected class ControllerCallbacks extends MediaControllerCompat.Callback
    {
        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );
        }

        @Override
        public void onQueueChanged( List<MediaSessionCompat.QueueItem> queue )
        {
            super.onQueueChanged( queue );

            PlaybackStateCompat playbackState;
            Bundle extras;
            String source;
            String parameter;

            extras = mMediaController.getExtras();

            if (extras == null)
            {
                mQueueLoaded = false;
                return;
            }

            source = extras.getString( Const.SOURCE_KEY, null );
            parameter = extras.getString( Const.PARAMETER_KEY, null );


            //If the source is different than the one this list represents
            if (!Utils.equalsIncludingNull( mCurrentSource,source) || !Utils.equalsIncludingNull( mCurrentParameter,parameter))
            {
                mQueueLoaded = false;
            }
            else
            {
                mQueueLoaded = true;
            }
        }
    }


    public SongRecyclerFragment() {
        // Required empty public constructor
    }

    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mControllerCallbacks = new ControllerCallbacks();

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

        //SlimPlayerApplication.getInstance().registerPlayerServiceListener(this);
    }


    /*@Override
    protected void onDataLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
        super.onDataLoaded(parentId, children, options);

    }*/




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

        //SlimPlayerApplication.getInstance().unregisterPlayerServiceListener(this);
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



    @Override
    public void onClick(View v) {

        int position = mRecyclerView.getChildLayoutPosition(v);
        List<MediaBrowserCompat.MediaItem> mediaItems = mAdapter.getMediaItemsList();


        //If we are not selecting items, then we want to play them
        if (!mSelectMode && !mSelectSongsForResult && mMediaBrowser.isConnected())
        {
            Bundle bundle;
            Intent intent;


            if (mQueueLoaded)
            {
                mMediaController.getTransportControls().skipToQueueItem( position );
            }
            else
            {
                bundle = new Bundle();
                bundle.putString( Const.SOURCE_KEY, mCurrentSource );
                bundle.putString( Const.PARAMETER_KEY, mCurrentParameter );
                bundle.putInt( Const.POSITION_KEY, position );

                mMediaController.getTransportControls().playFromMediaId( mediaItems.get( position ).getMediaId(), bundle );
            }


            //Start NowPlayingActivity
            intent = new Intent(mContext,NowPlayingActivity.class);
            intent.putExtra( Const.SOURCE_KEY, mCurrentSource );
            intent.putExtra( Const.PARAMETER_KEY, mCurrentParameter );
            intent.putExtra( Const.POSITION_KEY, position );

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
