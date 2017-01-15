package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import static mihaljevic.miroslav.foundry.slimplayer.ScreenBundles.DISPLAY_FIELD_KEY;

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

    protected Context mContext;

    protected RecyclerView mRecyclerView;
    protected CursorRecyclerAdapter mAdapter;

    //Current source for this fragment (all songs, songs by genre, songs by artist etc...)
    protected String mCurrentSource;

    //Are we only selecting songs for playlists
    protected boolean mSelectSongsForResult;

    //Are we selecting something
    protected boolean mSelectMode = false;

    protected SparseBooleanArray mSelectedItems;


    public SlimRecyclerFragment() {
        // Required empty public constructor
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

        mContext = getContext();
        mRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setHasFixedSize(true);

        //Add dividers
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(), ((LinearLayoutManager) mRecyclerView.getLayoutManager()).getOrientation()));

        //Set selection
        mSelectedItems = new SparseBooleanArray();

        Bundle bundle = getArguments();
        if (bundle != null)
        {
            //If we have bundle, then load data accordingly
            mCurrentSource = bundle.getString(ScreenBundles.CURSOR_SOURCE_KEY);
            //Here is cursor null,but it will be set-up properly after loadDataAsync() is called
            mAdapter = new CursorRecyclerAdapter(mContext,null, R.layout.recycler_item,new String[] {bundle.getString(DISPLAY_FIELD_KEY)},this,mSelectedItems);
            mRecyclerView.setAdapter(mAdapter);
        }

        //Check if we are selecting songs for playlists
        if (mContext instanceof SelectSongsActivity)
        {
            if (((SelectSongsActivity)mContext).isSelectSongsForResult())
            {
                mSelectSongsForResult = true;
            }
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        //With every showing of this fragment, load data
        loadDataAsync();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Adapter will check if cursor is used by media player service and close it appropriately
        //Also we need to check if adapter exists because of screen rotation calls
        if (mAdapter != null)
            mAdapter.closeCursor();
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

    protected void loadDataAsync()
    {
        Log.v(TAG,"loadDataAsync()");
        //Load cursor and connect it to cursor adapter
        new AsyncTask<Void,Void,Cursor>()
        {
            @Override
            protected Cursor doInBackground(Void... params) {
                return Utils.querySongListCursor(mContext,getArguments());
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                onDataLoaded(cursor);
            }
        }.execute();


    }


    protected void onDataLoaded(Cursor cursor)
    {
        mAdapter.swapCursor(cursor);
    }

}
