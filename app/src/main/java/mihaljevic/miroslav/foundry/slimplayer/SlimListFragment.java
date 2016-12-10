package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;


/**
 * A simple {@link Fragment} subclass.
 *
 * This is base fragment to display either a list of
 * categories/playlists or a list of songs
 *
 * @author Miroslav Mihaljević
 *
 *
 *
 */


public abstract class SlimListFragment extends BackHandledListFragment implements ListView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    //Keys that are used when transferring data about different screens from ScreenBundles
    public static final String CURSOR_SCREEN_KEY = "cursor_screen";

    public static final String CURSOR_PARAMETER_KEY = "cursor_parameter"; //ID of playlist, or artist, something like that
    public static final String CURSOR_URI_KEY = "cursor_uri";
    public static final String CURSOR_PROJECTION_KEY = "cursor_projection";
    public static final String CURSOR_SELECTION_KEY = "cursor_selection";
    public static final String CURSOR_SELECTION_ARGS_KEY = "cursor_selection_args";
    public static final String CURSOR_SORT_ORDER_KEY = "cursor_sort_order";

    public static final String DISPLAY_FIELD_KEY = "display_field";


    protected Context mContext;
    protected CursorAdapter mCursorAdapter;

    protected String mCurrentScreen;

    //Are we only selecting songs for playlists
    protected boolean mSelectSongsForResult;

    //Are we selecting something
    protected boolean mSelectMode = false;


    public SlimListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_slim_list, container, false);
    }

    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mContext = getContext();


        getListView().setOnItemClickListener(this);


        Bundle bundle = getArguments();
        if (bundle != null)
        {
            //If we have bundle, then load data accordingly
            mCurrentScreen = bundle.getString(CURSOR_SCREEN_KEY);
            mCursorAdapter = new SongCursorAdapter(getContext(),null,0, bundle.getString(DISPLAY_FIELD_KEY));
            setListAdapter(mCursorAdapter);
            getLoaderManager().initLoader(0,bundle,this);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);

        //inflater.inflate(R.menu.options_menu,menu);

        //In this callback we know that fragment is really visible/selected in pager, so notify hosting activity
        if (getContext() instanceof BackHandlerInterface)
            ((BackHandlerInterface)getContext()).setBackHandledFragment(this);
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

    //Turns on selection in listView
    public void activateSelectMode()
    {
        //If we are not in select mode, activate it
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mSelectMode = true;
        getActivity().invalidateOptionsMenu();

    }

    //Deselects selection in listView
    public void deselect()
    {
        mSelectMode = false;
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().clearChoices();
        getListView().requestLayout();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Create query that will fetch songs that we need for this screen
        Uri uri = Uri.parse(args.getString(CURSOR_URI_KEY));
        String [] projection = args.getStringArray(CURSOR_PROJECTION_KEY);
        String selection = args.getString(CURSOR_SELECTION_KEY);
        String [] selectionArgs = args.getStringArray(CURSOR_SELECTION_ARGS_KEY);
        String sortOrder = args.getString(CURSOR_SORT_ORDER_KEY);

        return new CursorLoader(getActivity(), uri, projection, selection,selectionArgs,sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //Update cursor when loading is finished
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}
