package mihaljevic.miroslav.foundry.slimplayer.fragments;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;

import mihaljevic.miroslav.foundry.slimplayer.Const;
import mihaljevic.miroslav.foundry.slimplayer.MediaPlayerService;
import mihaljevic.miroslav.foundry.slimplayer.MusicProvider;
import mihaljevic.miroslav.foundry.slimplayer.R;
import mihaljevic.miroslav.foundry.slimplayer.Utils;
import mihaljevic.miroslav.foundry.slimplayer.activities.SelectSongsActivity;
import mihaljevic.miroslav.foundry.slimplayer.activities.SlimActivity;
import mihaljevic.miroslav.foundry.slimplayer.adapters.MediaAdapter;
import mihaljevic.miroslav.foundry.slimplayer.fragments.BackHandledRecyclerFragment;

/**
 *
 * This is base fragment to display either a list of
 * categories/playlists or a list of songs
 *
 * @author Miroslav Mihaljević
 *
 *
 *
 */
public abstract class SlimRecyclerFragment extends BackHandledRecyclerFragment implements View.OnClickListener, View.OnLongClickListener {
    protected final String TAG = getClass().getSimpleName();


    protected RecyclerView mRecyclerView;
    protected MediaAdapter mAdapter;

    protected TextView mEmptyView;

    //Current source for this fragment (all songs, songs by genre, songs by artist etc...)
    protected String mSource;
    protected String mParameter;
    protected Bundle mSubscriptionBundle;

    //Are we only selecting songs for playlists
    protected boolean mSelectSongsForResult;

    //Are we selecting something
    protected boolean mSelectMode = false;

    //protected SparseBooleanArray mSelectedItems;
    protected HashSet<Integer> mSelectedItems;

    protected MediaBrowserCompat    mMediaBrowser;
    protected MediaControllerCompat mMediaController;

    protected MediaBrowserCompat.ConnectionCallback     mConnectionCallbacks;
    protected MediaBrowserCompat.SubscriptionCallback   mSubscriptionCallbacks;
    protected MediaControllerCompat.Callback            mControllerCallbacks;

    protected ActionBar mActionBar;


    protected class ConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();

            String parentString;

            parentString = Utils.createParentString( mSource, mParameter );

            mMediaBrowser.subscribe( parentString, mSubscriptionCallbacks );

            try
            {
                mMediaController = new MediaControllerCompat( getContext(), mMediaBrowser.getSessionToken() );
                mMediaController.registerCallback( mControllerCallbacks );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }

        @Override
        public void onConnectionSuspended()
        {
            super.onConnectionSuspended();

            updateContentDisplay( getString( R.string.empty_connection_suspended) );

            Log.i(TAG, "Connection is suspended");
        }

        @Override
        public void onConnectionFailed()
        {
            super.onConnectionFailed();

            updateContentDisplay( getString( R.string.empty_connection_failed ) );

            Log.w(TAG, "Connection has failed");
        }
    }

    protected class SubscriptionCallbacks extends MediaBrowserCompat.SubscriptionCallback
    {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children)
        {
            super.onChildrenLoaded( parentId, children );

            onDataLoaded(parentId, children, null);
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options)
        {
            super.onChildrenLoaded( parentId, children, options );

            onDataLoaded(parentId, children, options);
        }

        @Override
        public void onError(@NonNull String parentId)
        {
            Log.d( TAG, "Error happened when receiving subscription callback" );
            super.onError( parentId );

            updateContentDisplay( getString( R.string.empty_connection_failed ) );

        }

        @Override
        public void onError(@NonNull String parentId, @NonNull Bundle options)
        {
            Log.d( TAG, "Error happened when receiving subscription callback" );
            super.onError( parentId, options );

            updateContentDisplay( getString( R.string.empty_connection_failed ) );

        }
    }


    protected class ControllerCallbacks extends MediaControllerCompat.Callback {}


    protected void onDataLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, Bundle options)
    {

        //Check if we need to show empty message
        mAdapter.setMediaItemsList(children);
        mAdapter.notifyDataSetChanged();

        updateContentDisplay( getString( R.string.empty_placeholder ) );

    }




    public SlimRecyclerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate( @Nullable Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        //Set all media callback objects to this fragment's implementation of them
        mConnectionCallbacks    = new ConnectionCallbacks();
        mSubscriptionCallbacks  = new SubscriptionCallbacks();
        mControllerCallbacks    = new ControllerCallbacks();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View contentView;

        contentView = inflater.inflate(R.layout.fragment_slim_recycler, container, false);

        mRecyclerView   = ( RecyclerView ) contentView.findViewById( R.id.recycler );
        mEmptyView      = ( TextView ) contentView.findViewById( R.id.empty );

        return contentView;
    }



    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        LinearLayoutManager     layoutManager;
        DividerItemDecoration   dividerDecoration;

        layoutManager       = new LinearLayoutManager( getContext() );
        dividerDecoration   = new DividerItemDecoration( getContext(), layoutManager.getOrientation() );

        //Set up selection
        mSelectedItems = new HashSet<>(  );

        //Adapter is initiated here, but data will be loaded later
        mAdapter = new MediaAdapter( getContext(), null, R.layout.recycler_item, this, mSelectedItems );

        //Set up recycler view
        mRecyclerView.setHasFixedSize   ( true );
        mRecyclerView.setLayoutManager  ( layoutManager );
        mRecyclerView.setAdapter        ( mAdapter );
        mRecyclerView.addItemDecoration ( dividerDecoration );




        //Get current source and parameter
        mSource     = getArguments().getString( Const.SOURCE_KEY );
        mParameter  = getArguments().getString( Const.PARAMETER_KEY );

        //This is used in connection callbacks
        mSubscriptionBundle = new Bundle();
        mSubscriptionBundle.putString( Const.SOURCE_KEY, mSource );
        mSubscriptionBundle.putString( Const.PARAMETER_KEY, mParameter );


        //Check if we are selecting songs for playlists
        if (getContext() instanceof SelectSongsActivity )
        {
            if (((SelectSongsActivity)getContext()).isSelectSongsForResult())
            {
                mSelectSongsForResult = true;
            }

            mActionBar = ((SelectSongsActivity)getContext()).getSupportActionBar();
        }

        //Init media browser
        mMediaBrowser = new MediaBrowserCompat( getContext(), MediaPlayerService.COMPONENT_NAME, mConnectionCallbacks, null );





    }

    @Override
    public void onStart()
    {
        super.onStart();


        mMediaBrowser.connect();
    }



    @Override
    public void onStop()
    {
        super.onStop();

        if (mMediaController != null)
            mMediaController.unregisterCallback( mControllerCallbacks );

        if (mMediaBrowser.isConnected())
            mMediaBrowser.disconnect();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {


        //In this callback we know that fragment is really visible/selected in pager, so notify hosting activity
        if ( getContext() instanceof BackHandledRecyclerFragment.BackHandlerInterface )
            ( ( BackHandledRecyclerFragment.BackHandlerInterface ) getContext() ).setBackHandledFragment( this );


    }

    @Override
    public boolean onBackPressed()
    {

        //Here we store if the back button event is consumed
        boolean backConsumed;

        backConsumed = false;

        //If we are in normal mode, just deselect everything, if we are in select for result mode, no deselection
        if ( mSelectMode && !mSelectSongsForResult )
        {
            //Deselect everything
            deselect();
            backConsumed = true;
        }

        return backConsumed;
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu )
    {
        MenuItem cancelSelectionItem;

        cancelSelectionItem = menu.findItem( R.id.cancel_selection );

        if ( mSelectMode )
        {
            if ( cancelSelectionItem != null )
                cancelSelectionItem.setVisible( true );
        }
        else
        {
            if ( cancelSelectionItem != null )
                cancelSelectionItem.setVisible( false );
        }

        super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {

        int id;

        id = item.getItemId();

        switch( id )
        {
            case R.id.cancel_selection:
                deselect();
                item.setVisible( false );
                break;
            default:
                return super.onOptionsItemSelected( item );
        }

        return true;
    }

    protected void updateContentDisplay()
   {
       updateContentDisplay( null );
   }

    //It decides whether to show empty message or to display content
    protected void updateContentDisplay( @Nullable String message )
    {
        if ( mEmptyView == null || mRecyclerView == null )
        {
            Log.w(TAG, "Could not find views to update display");
            return;
        }

        List<MediaBrowserCompat.MediaItem> data;

        data = mAdapter.getMediaItemsList();

        if ( message != null )
            mEmptyView.setText( message );

        if (data == null || data.size() == 0)
        {
            mEmptyView.setVisibility    ( View.VISIBLE );
            mRecyclerView.setVisibility ( View.GONE );
        }
        else
        {
            mEmptyView.setVisibility    ( View.GONE );
            mRecyclerView.setVisibility ( View.VISIBLE );
        }
    }

    public void activateSelectMode()
    {
        //If we are not in select mode, activate it
        mSelectMode = true;

        //Make sure menu picks up the changes
        if (getContext() instanceof AppCompatActivity) {
            ((AppCompatActivity) getContext()).invalidateOptionsMenu();
        }


    }

    //Deselects selection in recycler
    public void deselect()
    {
        View listItem;

        mSelectMode = false;

        //Remove everything from selected items
        mSelectedItems.clear();

        //Un-highlight everything
        for ( int i = 0; i < mRecyclerView.getChildCount(); i++ )
        {
            listItem = mRecyclerView.getChildAt( i );
            listItem.setSelected( false );
        }

        if ( getContext() instanceof SlimActivity )
        {
            ( ( SlimActivity ) getContext() ).restoreActionBarTitle();
            ( ( SlimActivity ) getContext() ).invalidateOptionsMenu();
        }





    }

    public void setItemSelected(int pos, boolean selected,View view)
    {
        //Check if positions are in range
        if ( pos < 0 || pos >= mAdapter.getItemCount() )
            return;


        if ( selected )
        {
            //If we are selecting item
            //mSelectedItems.put( pos, true );
            mSelectedItems.add( pos );
        }
        else
        {
            //If we are deselecting just delete that key
            //mSelectedItems.delete( pos );
            mSelectedItems.remove( pos );
        }

        //Higlight or not the view
        view.setSelected( selected );

        //Update number of selected items on title bar
        if ( mActionBar != null )
        {
            mActionBar.setTitle( mSelectedItems.size() + " " + getString( R.string.selected ) );
        }

    }

    public boolean isItemSelected( int pos )
    {
        return mSelectedItems.contains( pos );
    }

    protected void refreshData()
    {
        if ( mMediaBrowser != null && mMediaBrowser.isConnected() )
        {
            MusicProvider.getInstance().invalidateDataAndNotify( mSource, mParameter );
        }
    }

    protected void deleteItemsAsync( final Uri uri, final String idField )
    {
        new AsyncTask<Void,Void,Integer>()
        {
            @Override
            protected Integer doInBackground(Void... params)
            {

                return Utils.deleteFromList(    mAdapter.getMediaItemsList(),
                                                uri,
                                                mSelectedItems,
                                                idField );
            }

            @Override
            protected void onPostExecute( Integer result )
            {

                Utils.toastShort(result + " " + getString( R.string.toast_items_deleted ));
                deselect();
                refreshData();
            }
        }.execute();
    }


}
