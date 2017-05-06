package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Fragment that displays only songs, and responds appropriately to click
 *
 * @author Miroslav Mihaljević
 */

public class SongRecyclerFragment extends SlimRecyclerFragment{


    //Indicate whether this list/queue is loaded in MediaPlayerService
    protected boolean mQueueLoaded = false;

    protected String mDisplayName;


    protected class ConnectionCallbacks extends SlimRecyclerFragment.ConnectionCallbacks
    {
        @Override
        public void onConnected()
        {
            super.onConnected();

            //Run first time check of whether our list is loaded in media service
            determineThisQueueLoaded();
        }
    }


    protected class ControllerCallbacks extends SlimRecyclerFragment.ControllerCallbacks
    {

        @Override
        public void onQueueChanged( List<MediaSessionCompat.QueueItem> queue )
        {
            super.onQueueChanged( queue );

            //We use this callback to check if our queue is loaded in media service
            determineThisQueueLoaded();

        }
    }




    public SongRecyclerFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate( @Nullable Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        //Set all media callback objects to this fragment's implementation of them
        mConnectionCallbacks     = new ConnectionCallbacks();
        mSubscriptionCallbacks   = new SubscriptionCallbacks();
        mControllerCallbacks     = new ControllerCallbacks();
    }

    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mControllerCallbacks = new ControllerCallbacks();

        mDisplayName = getArguments().getString( Const.DISPLAY_NAME, "" );

    }

    @Override
    public void onStart()
    {
        super.onStart();

        //If we are selecting songs for result then we need to enforce select mode, so we don't play songs here
        if (mSelectSongsForResult)
            activateSelectMode();

    }






    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        Log.v(TAG,"onPrepareOptionsMenu()");

        //Show add to playlist only if we are in normal mode (and not select for result mode)
        if ( !mSelectSongsForResult )
        {
            MenuItem playlistAddMenuItem;

            playlistAddMenuItem = menu.findItem( R.id.playlist_add );

            if (mSelectMode && mSelectedItems.size() > 0)
            {
                //If we are selecting items then we want option to add them to playlist
                playlistAddMenuItem.setVisible(true);
            }
            else
            {
                //Hide option to add to playlist
                playlistAddMenuItem.setVisible(false);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id;

        id = item.getItemId();

        //Allow this option only if we are in normal mode
        if (mSelectMode && !mSelectSongsForResult)
        {
            if ( id == R.id.playlist_add )
            {

                //Get all checked positions
                //NOTE - this looks a little bit messed after the migration to recycler view from list view
                HashSet<Integer>                    selectedPositions;
                List<MediaBrowserCompat.MediaItem>  mediaItemsList;
                ArrayList<String>                   selectedIDsList;
                //int                                 position;
                boolean                             isPositionSelected;
                String                              mediaID;
                Intent                              addToPlaylistIntent;

                selectedPositions = mSelectedItems;


                mediaItemsList = mAdapter.getMediaItemsList();
                selectedIDsList = new ArrayList<>();

                //Transfer IDs from selected songs to ID list
                //for ( int i = 0; i < selectedPositions.size(); i++ )
                for ( Integer position : selectedPositions )
                {
                    /*position            = selectedPositions.keyAt    ( i );
                    isPositionSelected  = selectedPositions.valueAt  ( i );*/


                    mediaID = mediaItemsList.get(position).getMediaId();
                    selectedIDsList.add( mediaID );

                }

                //Here we call add to playlists activity and pass ID list
                addToPlaylistIntent = new Intent( getContext(), AddToPlaylistActivity.class );
                addToPlaylistIntent.putExtra( AddToPlaylistActivity.SELECTED_IDS_KEY, selectedIDsList );

                startActivity( addToPlaylistIntent );
                deselect();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //Detect whether is this queue loaded in MediaPlayerService
    private void determineThisQueueLoaded()
    {
        Bundle extras;
        String source;
        String parameter;

        //If something is wrong assume queue is not loaded and return
        if ( mMediaBrowser == null || !mMediaBrowser.isConnected() || mMediaController == null )
        {
            mQueueLoaded = false;
            return;
        }

        extras = mMediaController.getExtras();

        if ( extras == null )
        {
            mQueueLoaded = false;
            return;
        }

        source      = extras.getString( Const.SOURCE_KEY, null );
        parameter   = extras.getString( Const.PARAMETER_KEY, null );


        //Determine if this queue is actually loaded in media service
        mQueueLoaded = !Utils.isSourceDifferent( mSource, mParameter, source, parameter );
    }



    @Override
    public void onClick(View v)
    {

        int                                 position;
        List<MediaBrowserCompat.MediaItem>  mediaItems;
        Context                             context;
        String                              mediaID;

        position    = mRecyclerView.getChildLayoutPosition( v );
        mediaItems  = mAdapter.getMediaItemsList();
        context     = getContext();
        mediaID     = mediaItems.get( position ).getMediaId();


        //If we are not selecting items, then we want to play them
        if ( !mSelectMode && !mSelectSongsForResult && mMediaBrowser.isConnected() )
        {
            Bundle              extras;
            Intent              intent;
            PlaybackStateCompat playbackState;


            //Check if this queue is already loaded, no need to load it again
            if ( mQueueLoaded )
            {
                playbackState = mMediaController.getPlaybackState();

                //Play song only if it is not already playing, or resume it if it is paused
                if ( playbackState.getActiveQueueItemId() != position )
                    mMediaController.getTransportControls().skipToQueueItem( position );
                else if ( playbackState.getState() == PlaybackStateCompat.STATE_PAUSED )
                    mMediaController.getTransportControls().play();
            }
            else
            {
                //We need to load queue

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, mSource );
                extras.putString( Const.PARAMETER_KEY, mParameter );
                extras.putString( Const.DISPLAY_NAME, mDisplayName );
                extras.putInt   ( Const.POSITION_KEY, position );

                mMediaController.getTransportControls().playFromMediaId( mediaID, extras );
            }


            //Start NowPlayingActivity
            intent = new Intent(context,NowPlayingActivity.class);
            intent.putExtra( Const.SOURCE_KEY, mSource );
            intent.putExtra( Const.PARAMETER_KEY, mParameter );
            intent.putExtra( Const.POSITION_KEY, position );

            startActivity( intent );
        }

        //If we are selecting items
        if ( mSelectMode )
        {
            setItemSelected( position, !isItemSelected(position), v );
        }

        //If we are in select for result mode
        if ( mSelectSongsForResult && getContext() instanceof SelectSongsActivity )
        {
            SelectSongsActivity selectSongsActivity;

            selectSongsActivity = ( SelectSongsActivity ) getContext();


            if ( isItemSelected( position ) )
            {
                //Select
                selectSongsActivity.getSelectedSongsList().add( mediaID );
            }
            else
            {
                //Deselect
                selectSongsActivity.getSelectedSongsList().remove( mediaID );
            }
        }
    }

    @Override
    public boolean onLongClick( View v )
    {

        if ( !mSelectMode )
        {
            //Activate select mode and select pressed item

            activateSelectMode();
            setItemSelected( mRecyclerView.getChildLayoutPosition( v ) ,true , v );
        }
        return true;
    }
}
