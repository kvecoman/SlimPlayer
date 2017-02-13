package mihaljevic.miroslav.foundry.slimplayer;


import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

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

    //Current source for this fragment (all songs, songs by genre, songs by artist etc...)
    protected String mSource;
    protected String mParameter;
    protected Bundle mSubscriptionBundle;

    //Are we only selecting songs for playlists
    protected boolean mSelectSongsForResult;

    //Are we selecting something
    protected boolean mSelectMode = false;

    protected SparseBooleanArray mSelectedItems;

    protected MediaBrowserCompat mMediaBrowser;
    protected MediaControllerCompat mMediaController;

    protected MediaBrowserCompat.ConnectionCallback     mConnectionCallbacks;
    protected MediaBrowserCompat.SubscriptionCallback   mSubscriptionCallbacks;
    protected MediaControllerCompat.Callback            mControllerCallbacks;


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
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
        }
    }

    protected class SubscriptionCallbacks extends MediaBrowserCompat.SubscriptionCallback
    {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children) {
            super.onChildrenLoaded( parentId, children );

            onDataLoaded(parentId, children, null);
        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
            super.onChildrenLoaded( parentId, children, options );

            onDataLoaded(parentId, children, options);
        }

        @Override
        public void onError(@NonNull String parentId) {
            super.onError( parentId );

        }

        @Override
        public void onError(@NonNull String parentId, @NonNull Bundle options) {
            super.onError( parentId, options );

        }
    }


    protected class ControllerCallbacks extends MediaControllerCompat.Callback {}


    protected void onDataLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options)
    {
        View emptyTextView;
        View recyclerView;

        emptyTextView = getView().findViewById( R.id.empty );
        recyclerView = getView().findViewById( R.id.recycler );

        //Check if we need toshow empty message
        if (children == null || children.size() == 0)
        {
            emptyTextView.  setVisibility( View.VISIBLE );
            recyclerView.   setVisibility( View.GONE );
        }
        else
        {
            emptyTextView.  setVisibility( View.GONE );
            recyclerView.   setVisibility( View.VISIBLE );
        }

        mAdapter.setMediaItemsList(children);
        mAdapter.notifyDataSetChanged();
    }


    public SlimRecyclerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate( @Nullable Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        //Set all media callback objects to this fragment's implementation of them
        mConnectionCallbacks =      new ConnectionCallbacks();
        mSubscriptionCallbacks =    new SubscriptionCallbacks();
        mControllerCallbacks =      new ControllerCallbacks();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_slim_recycler, container, false);
    }



    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        //mContext = getContext();

        //Set up selection
        mSelectedItems = new SparseBooleanArray();

        //Adapter is inited here, but data will be loaded later
        mAdapter = new MediaAdapter(getContext(), null, R.layout.recycler_item, this, mSelectedItems);

        //Set up recycler view
        mRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(), ((LinearLayoutManager) mRecyclerView.getLayoutManager()).getOrientation()));




        //Get current source and parameter
        mSource = getArguments().getString( Const.SOURCE_KEY);
        mParameter = getArguments().getString( Const.PARAMETER_KEY);

        mSubscriptionBundle = new Bundle();
        mSubscriptionBundle.putString( Const.SOURCE_KEY, mSource );
        mSubscriptionBundle.putString( Const.PARAMETER_KEY, mParameter );


        //Check if we are selecting songs for playlists
        if (getContext() instanceof SelectSongsActivity)
        {
            if (((SelectSongsActivity)getContext()).isSelectSongsForResult())
            {
                mSelectSongsForResult = true;
            }
        }

        //Init media browser
        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(),MediaPlayerService.class), mConnectionCallbacks, null);

    }

    @Override
    public void onStart() {
        super.onStart();

        //With every showing of this fragment, load data
        //loadDataAsync();

        mMediaBrowser.connect();
    }



    @Override
    public void onStop() {
        super.onStop();

        if (mMediaController != null)
            mMediaController.unregisterCallback( mControllerCallbacks );

        if (mMediaBrowser.isConnected())
            mMediaBrowser.disconnect();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        //In this callback we know that fragment is really visible/selected in pager, so notify hosting activity
        if (getContext() instanceof BackHandledRecyclerFragment.BackHandlerInterface)
            ((BackHandledRecyclerFragment.BackHandlerInterface)getContext()).setBackHandledFragment(this);
    }

    @Override
    public boolean onBackPressed() {

        //Here we store if the back button event is consumed
        boolean backConsumed = false;

        //If we are in normal mode, just deselect everything, if we are in select for result mode, no deselection
        if (mSelectMode && !mSelectSongsForResult)
        {
            //Deselect everything
            deselect();
            backConsumed = true;
        }

        return backConsumed;
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
        mSelectMode = false;

        //Remove everything from selected items
        mSelectedItems.clear();

        //Un-highlight everything
        for (int i = 0;i < mRecyclerView.getChildCount();i++)
            mRecyclerView.getChildAt(i).setSelected(false);

    }

    public void setItemSelected(int pos, boolean selected,View view)
    {
        //Check if positions are in range
        if (pos < 0 || pos >= mAdapter.getItemCount())
            return;


        if (selected)
        {
            //If we are selecting item
            mSelectedItems.put(pos,true);
        }
        else
        {
            //If we are deselecting just delete that key
            mSelectedItems.delete(pos);
        }

        //Higlight or not the view
        view.setSelected(selected);



    }

    public boolean isItemSelected(int pos)
    {
        return mSelectedItems.get(pos);
    }

    protected void refreshData()
    {
        if (mMediaBrowser != null && mMediaBrowser.isConnected())
        {
            MusicProvider.getInstance().invalidateDataAndNotify( mSource, mParameter );
        }
    }

    protected void deleteItemsAsync(final Uri uri,final String idField)
    {
        new AsyncTask<SparseBooleanArray,Void,Integer>()
        {
            @Override
            protected Integer doInBackground(SparseBooleanArray... params) {

                SparseBooleanArray selectedPositions = params[0];

                return Utils.deleteFromList(
                                        mAdapter.getMediaItemsList(),
                                        uri,
                                        selectedPositions,
                                        idField);
            }

            @Override
            protected void onPostExecute(Integer result) {

                Utils.toastShort(result + " " + getString( R.string.toast_items_deleted ));
                deselect();
                refreshData();
            }
        }.execute(mSelectedItems);
    }

    /*protected void loadDataAsync()
    {
        Log.v(TAG,"loadDataAsync()");
        //Load cursor and connect it to cursor adapter
        new AsyncTask<Void,Void,Cursor>()
        {
            @Override
            protected Cursor doInBackground(Void... params) {
                return Utils.queryMedia(getArguments());
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                onDataLoaded(cursor);
            }
        }.execute();


    }*/


}
