package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Starting screen for user, shows most played and recently played lists for quick access
 *
 * @author Miroslav Mihaljević
 */
public class HomeFragment extends Fragment implements View.OnClickListener/*, SlimPlayerApplication.PlayerServiceListener */{

    private RecyclerView mRecyclerView;

    private RecyclerView.LayoutManager mLayoutManager;

    private HomeAdapter mAdapter;

    //Number of items shownin home screen
    private int mNumberOfItems;

    //Here we store update task so we can check its status
    private AsyncTask<Void,Void,Void> mUpdateDatasetTask;

    //private MediaPlayerService mPlayerService;


    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Find recycler view
        mRecyclerView = (RecyclerView) getView().findViewById(R.id.home_recycler_view);

        //Set that it has fixed site
        mRecyclerView.setHasFixedSize(true);

        //Set layout manager for recycler view
        mLayoutManager = new GridLayoutManager(getContext(),2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mNumberOfItems = getContext().getResources().getInteger(R.integer.num_homescreen_items);

        //For now we just init adapter and set it to recycler view, data loading starts later
        mAdapter = new HomeAdapter(getContext(),null,this);
        mRecyclerView.setAdapter(mAdapter);

        //Make sure onCreateOptionsMenu() is called
        setHasOptionsMenu(true);

    }

    @Override
    public void onStop() {
        super.onStop();

        //SlimPlayerApplication.getInstance().registerPlayerServiceListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        //We call this here so we make sure we always have latest last play positions
        updateDatasetAsync();
    }

    //HACK TO KNOW IF THIS FRAGMENT IS VISIBLE ONE IN PAGER
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        //We call this here so we make sure we always have latest last play positions
        updateDatasetAsync();

    }

    /*@Override
    public void onPlayerServiceBound(MediaPlayerService playerService) {
        //Player service has been bound, get its object
        mPlayerService = playerService;
    }*/

    @Override
    public void onDestroy() {

        //We need this check because onDestroy() is called at awkward times
        if (mAdapter != null)
            mAdapter.closeCursor();

        StatsDbHelper.closeInstance();

        super.onDestroy();
    }

    public void updateDatasetAsync()
    {
        if (mUpdateDatasetTask != null && mUpdateDatasetTask.getStatus() == AsyncTask.Status.RUNNING)
            return;

        mUpdateDatasetTask = new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground(Void... params) {

                Cursor cursor = ( StatsDbHelper.getInstance(getContext())).getReadableDatabase().query(StatsContract.SourceStats.TABLE_NAME,
                        new String[] {StatsContract.SourceStats.COLUMN_NAME_SOURCE,StatsContract.SourceStats.COLUMN_NAME_PARAMETER,StatsContract.SourceStats.COLUMN_NAME_DISPLAY_NAME, StatsContract.SourceStats.COLUMN_NAME_LAST_POSITION},
                        null,null,null,null, StatsContract.SourceStats.COLUMN_NAME_RECENT_FREQUENCY + " DESC",String.valueOf(mNumberOfItems));
                mAdapter.setCursor(cursor);


                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mAdapter.notifyDataSetChanged();
            }
        };

        mUpdateDatasetTask.execute();
    }

    @Override
    public void onClick(View v) {
        int position = mRecyclerView.getChildLayoutPosition(v);
        Cursor cursor = mAdapter.getCursor();

        if (cursor == null || cursor.isClosed())
            return;

        cursor.moveToPosition(position);

        String source = cursor.getString(cursor.getColumnIndex(StatsContract.SourceStats.COLUMN_NAME_SOURCE));
        String parameter = cursor.getString(cursor.getColumnIndex(StatsContract.SourceStats.COLUMN_NAME_PARAMETER));
        int playPosition = cursor.getInt(cursor.getColumnIndex(StatsContract.SourceStats.COLUMN_NAME_LAST_POSITION));


        //MediaPlayerService playerService = SlimPlayerApplication.getInstance().getMediaPlayerService();

        //Get bundle
        Bundle bundle = ScreenBundles.getBundleForSubScreen(v.getContext(),source,parameter);

        //Insert last remembered position
        bundle.putInt(SongRecyclerFragment.PLAY_POSITION_KEY,playPosition);

        //Check if the same list is already playing
        /*if (playerService != null && !(Utils.equalsIncludingNull(playerService.getSongsSource(),source) && Utils.equalsIncludingNull(playerService.getSongsParameter(),parameter)))
        {

        }*/


        Intent intent = new Intent(v.getContext(),SongListActivity.class);
        intent.putExtra(SongListActivity.FRAGMENT_BUNDLE_KEY, bundle);

        //Start activity
        v.getContext().startActivity(intent);
    }
}
