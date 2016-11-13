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

//TODO - close cursor when needed
public abstract class SlimListFragment extends BackHandledListFragment implements ListView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    public static final String CURSOR_SCREEN_KEY = "cursor_screen";

    public static final String CURSOR_URI_KEY = "cursor_uri";
    public static final String CURSOR_PROJECTION_KEY = "cursor_projection";
    public static final String CURSOR_SELECTION_KEY = "cursor_selection";
    public static final String CURSOR_SELECTION_ARGS_KEY = "cursor_selection_args";
    public static final String CURSOR_SORT_ORDER_KEY = "cursor_sort_order";

    public static final String DISPLAY_FIELD_KEY = "display_field";


    protected Context mContext;
    protected CursorAdapter mCursorAdapter;

    protected String mCurrentScreen;


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

        Bundle bundle = getArguments();
        //TODO - add try-catch for this code

        mCurrentScreen = bundle.getString(CURSOR_SCREEN_KEY);

        String displayField = bundle.getString(DISPLAY_FIELD_KEY);

        mCursorAdapter = new SongCursorAdapter(getContext(),null,0, displayField);

        setListAdapter(mCursorAdapter);

        getListView().setOnItemClickListener(this);

        getLoaderManager().initLoader(0,bundle,this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = Uri.parse(args.getString(CURSOR_URI_KEY));
        String [] projection = args.getStringArray(CURSOR_PROJECTION_KEY);
        String selection = args.getString(CURSOR_SELECTION_KEY);
        String [] selectionArgs = args.getStringArray(CURSOR_SELECTION_ARGS_KEY);
        String sortOrder = args.getString(CURSOR_SORT_ORDER_KEY);

        return new CursorLoader(getActivity(), uri, projection, selection,selectionArgs,sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }
}
