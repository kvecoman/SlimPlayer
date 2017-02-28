package mihaljevic.miroslav.foundry.slimplayer;


import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * Starting screen for user, shows most played and recently played lists for quick access
 *
 * @author Miroslav Mihaljević
 */
public class HomeFragment extends Fragment implements View.OnClickListener /*, SlimPlayerApplication.PlayerServiceListener */
{
    protected final String TAG = getClass().getSimpleName();

    private RecyclerView mRecyclerView;

    private HomeAdapter mAdapter;

    //Number of items shown in home screen
    private int mNumberOfItems;

    //Here we store update task so we can check its status
    private AsyncTask<Void, Void, Void> mUpdateDatasetTask;

    private MediaBrowserCompat      mMediaBrowser;
    private MediaControllerCompat   mMediaController;

    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver()
    {
        @Override
        public void onChanged()
        {
            TextView emptyText;

            emptyText = ( TextView ) getView().findViewById( R.id.empty_text );

            if ( emptyText == null )
                return;

            if ( mAdapter == null || mAdapter.getItemCount() == 0 )
            {
                emptyText.setVisibility( View.VISIBLE );
                return;
            }
            emptyText.setVisibility( View.GONE );

        }
    };

    protected class ConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();

            try
            {
                mMediaController = new MediaControllerCompat( getContext(), mMediaBrowser.getSessionToken() );
            } catch ( RemoteException e )
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onConnectionSuspended()
        {
            super.onConnectionSuspended();

            Log.i( TAG, "Connection is suspended" );
        }

        @Override
        public void onConnectionFailed()
        {
            super.onConnectionFailed();

            Log.e( TAG, "Connection has failed" );
        }
    }


    public HomeFragment()
    {
        // Required empty public constructor
    }


    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState )
    {
        //Inflate the layout for this fragment
        return inflater.inflate( R.layout.fragment_home, container, false );
    }

    @Override
    public void onActivityCreated( @Nullable Bundle savedInstanceState )
    {
        super.onActivityCreated( savedInstanceState );

        RecyclerView.LayoutManager layoutManager;

        layoutManager = new GridLayoutManager( getContext(), 2 );

        //For now we just init adapter and set it to recycler view, data loading starts later
        mAdapter = new HomeAdapter( getContext(), null, this );

        //Find recycler view
        mRecyclerView = ( RecyclerView ) getView().findViewById( R.id.home_recycler_view );
        mRecyclerView.setHasFixedSize( true );
        mRecyclerView.setLayoutManager( layoutManager );
        mRecyclerView.setAdapter( mAdapter );

        mNumberOfItems = getContext().getResources().getInteger( R.integer.num_homescreen_items );


        //Set up observer so we know when to show empty message
        mAdapter.registerAdapterDataObserver( mDataObserver );

        //Make sure onCreateOptionsMenu() is called
        setHasOptionsMenu( true );

        mMediaBrowser = new MediaBrowserCompat( getContext(), MediaPlayerService.COMPONENT_NAME, new ConnectionCallbacks(), null );

    }

    @Override
    public void onStart()
    {
        super.onStart();
        mMediaBrowser.connect();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //We call this here so we make sure we always have latest last play positions
        updateDatasetAsync();
    }

    @Override
    public void onStop()
    {
        super.onStop();

        mMediaBrowser.disconnect();
    }

    //HACK TO KNOW IF THIS FRAGMENT IS VISIBLE ONE IN PAGER
    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        super.onCreateOptionsMenu( menu, inflater );

        //We call this here so we make sure we always have latest last play positions
        updateDatasetAsync();

    }


    @Override
    public void onDestroy()
    {

        //We need this check because onDestroy() is called at awkward times
        if ( mAdapter != null )
        {
            mAdapter.unregisterAdapterDataObserver( mDataObserver );
        }


        super.onDestroy();
    }

    public void updateDatasetAsync()
    {
        if ( mUpdateDatasetTask != null && mUpdateDatasetTask.getStatus() == AsyncTask.Status.RUNNING )
            return;

        mUpdateDatasetTask = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {
                Cursor                          cursor;
                SQLiteDatabase                  database;
                String[]                        projection;
                String                          sortOrder;
                String                          limit;
                ArrayList<HomeAdapter.StatRow>  adapterData;
                HomeAdapter.StatRow             row;


                database = ( StatsDbHelper.getInstance() ).getReadableDatabase();

                projection = new String[] { StatsContract.SourceStats.COLUMN_NAME_SOURCE,
                                            StatsContract.SourceStats.COLUMN_NAME_PARAMETER,
                                            StatsContract.SourceStats.COLUMN_NAME_DISPLAY_NAME,
                                            StatsContract.SourceStats.COLUMN_NAME_LAST_POSITION };

                sortOrder = StatsContract.SourceStats.COLUMN_NAME_RECENT_FREQUENCY + " DESC";

                limit = String.valueOf( mNumberOfItems );

                cursor = database.query(    StatsContract.SourceStats.TABLE_NAME,
                                            projection,
                                            null,
                                            null,
                                            null,
                                            null,
                                            sortOrder,
                                            limit );

                if ( cursor == null )
                {
                    adapterData = new ArrayList<>( 0 );
                }
                else
                {
                    adapterData = new ArrayList<>( cursor.getCount() );

                    while ( cursor.moveToNext() )
                    {
                        row                 = new HomeAdapter.StatRow();
                        row.source          = cursor.getString( cursor.getColumnIndex( projection[0] ) );
                        row.parameter       = cursor.getString( cursor.getColumnIndex( projection[1] ) );
                        row.displayName     = cursor.getString( cursor.getColumnIndex( projection[2] ) );
                        row.lastPosition    = cursor.getInt( cursor.getColumnIndex( projection[3] ) );

                        adapterData.add( row );
                    }
                }

                mAdapter.setData( adapterData );

                cursor.close();

                return null;
            }

            @Override
            protected void onPostExecute( Void aVoid )
            {
                mAdapter.notifyDataSetChanged();
            }
        };

        mUpdateDatasetTask.execute();
    }

    @Override
    public void onClick( View v )
    {
        int     itemPosition;
        int     lastPlayedPosition;
        String  source;
        String  parameter;
        String  sessionSource;
        String  sessionParameter;
        String  displayName;

        Intent  intent;
        Bundle  extras;

        List<HomeAdapter.StatRow>   adapterData;
        HomeAdapter.StatRow         selectedRow;

        itemPosition    = mRecyclerView.getChildLayoutPosition( v );
        adapterData     = mAdapter.getData();

        if ( adapterData == null || adapterData.size() == 0 )
            return;


        selectedRow = adapterData.get( itemPosition );

        source                  = selectedRow.source;
        parameter               = selectedRow.parameter;
        displayName             = selectedRow.displayName;
        lastPlayedPosition      = selectedRow.lastPosition;


        intent = new Intent( getContext(), SongListActivity.class );

        intent.putExtra( Const.SOURCE_KEY, source );
        intent.putExtra( Const.PARAMETER_KEY, parameter );
        intent.putExtra( Const.DISPLAY_NAME, displayName );
        intent.putExtra( Const.POSITION_KEY, lastPlayedPosition );


        //Check if we can start playing
        if ( mMediaBrowser != null && mMediaBrowser.isConnected() && mMediaController != null )
        {
            extras = mMediaController.getExtras();

            sessionSource       = extras == null ? null : extras.getString( Const.SOURCE_KEY );
            sessionParameter    = extras == null ? null : extras.getString( Const.PARAMETER_KEY );

            //Check if we need to start playing
            if ( Utils.isSourceDifferent( source, parameter, sessionSource, sessionParameter ) )
            {
                mMediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, intent.getExtras() );
            }
        }

        //Start activity
        getContext().startActivity( intent );
    }
}
