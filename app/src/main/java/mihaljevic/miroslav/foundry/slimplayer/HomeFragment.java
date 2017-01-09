package mihaljevic.miroslav.foundry.slimplayer;


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
public class HomeFragment extends Fragment {

    private RecyclerView mRecyclerView;

    private RecyclerView.LayoutManager mLayoutManager;

    private HomeAdapter mAdapter;

    //Number of items shownin home screen
    private int mNumberOfItems;

    //Here we store update task so we can check its status
    private AsyncTask<Void,Void,Void> mUpdateDatasetTask;


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
        mAdapter = new HomeAdapter(getContext(),null);
        mRecyclerView.setAdapter(mAdapter);

        //Make sure onCreateOptionsMenu() is called
        setHasOptionsMenu(true);

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


    @Override
    public void onDestroy() {

        mAdapter.closeCursor();

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

                Cursor cursor = (new StatsDbHelper(getContext())).getReadableDatabase().query(StatsContract.SourceStats.TABLE_NAME,
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
}
