package mihaljevic.miroslav.foundry.slimplayer;


import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment that displays list of playlists + options to create new one or delete one
 *
 * @author Miroslav Mihaljević
 */
public class PlaylistsFragment extends CategoryListFragment {

    //private TextView mCreatePlaylistView;

    private AlertDialog mCreatePlaylistDialog;
    private EditText mCreatePlaylistEditText;

    public PlaylistsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return super.onCreateView(inflater,container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Set up AlertDialog for creating playlists
        initCreatePlaylistDialog();

        //Handler for long click to enter into select mode
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (!mSelectMode)
                {
                    activateSelectMode();
                    ((ListView)parent).setItemChecked(position, true);
                }
                return true;
            }
        });

        //getLoaderManager().initLoader(0,getArguments(),this);
    }

    public void initCreatePlaylistDialog()
    {
        //Set up edit text that will be used in alert dialog to get playlist name
        mCreatePlaylistEditText = new EditText(mContext);
        mCreatePlaylistEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);


        //Build alert dialog that will take name of new playlist
        mCreatePlaylistDialog = new AlertDialog.Builder(mContext)
                .setTitle("Create new playlist")
                .setMessage("Enter playlist name!")
                .setView(mCreatePlaylistEditText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Here we create new playlist
                        String playlistName;

                        playlistName = mCreatePlaylistEditText.getText().toString();

                        //Check if playlist with that name already exists
                        /*Cursor playlistsCursor = mCursorAdapter.getCursor();
                        for (int i = 0;i < playlistsCursor.getCount();i++)
                        {
                            playlistsCursor.moveToPosition(i);
                            if (playlistName.equals(playlistsCursor.getString(playlistsCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))))
                            {
                                Toast.makeText(mContext,getString(R.string.toast_playlist_exists),Toast.LENGTH_SHORT).show();
                                mCreatePlaylistEditText.selectAll();
                                return;
                            }
                        }*/

                        //Async task in async task, in first one we check if playlist with that name already exist,
                        // and in second one we actually create playlist
                        new AsyncTask<String,Void,Boolean>()
                        {
                            private String mPlaylistName;

                            @Override
                            protected Boolean doInBackground(String... params) {
                                mPlaylistName = params[0];

                                return Utils.checkIfPlaylistExist(mContext, mPlaylistName);
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                //If playlist already exist
                                if (result)
                                {
                                    Toast.makeText(mContext,getString(R.string.toast_playlist_exists),Toast.LENGTH_SHORT).show();
                                    mCreatePlaylistEditText.selectAll();
                                }
                                else
                                {
                                    //If not then we can create new playlist
                                    new AsyncTask<String,Void,Long>()
                                    {
                                        @Override
                                        protected Long doInBackground(String... params) {
                                            return Utils.createPlaylist(getContext(),params[0]);
                                        }

                                        @Override
                                        protected void onPostExecute(Long result) {
                                            //Refresh data
                                            loadDataAsync();
                                        }
                                    }.execute(mPlaylistName);
                                }
                            }
                        }.execute(playlistName);




                        //Create content values with new playlist info
                        /*ContentValues inserts = new ContentValues();
                        inserts.put(MediaStore.Audio.Playlists.NAME, playlistName);
                        inserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
                        inserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

                        //Insert new playlist values
                        Uri uri = mContext.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,inserts);

                        //Get ID of newly created playlist
                        Cursor c = mContext.getContentResolver().query(uri, new String[]{MediaStore.Audio.Playlists._ID},null,null,null);
                        c.moveToFirst();
                        int pId = c.getInt(0);
                        c.close();*/

                        //Toast.makeText(mContext,uri.toString() + " ------ " + pId,Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();

        mCreatePlaylistDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                //Code to automatically show soft keyboard when dialog is shown
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,0);
            }
        });


    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //Show options only if we are in normal mode (and not select for result mode)
        if (!mSelectSongsForResult)
        {
            MenuItem deleteItem = menu.findItem(R.id.delete_item);
            MenuItem createPlaylistItem = menu.findItem(R.id.playlist_create);

            //Decide whether to show option for deleting playlists
            if (mSelectMode && getListView().getCheckedItemCount() > 0)
            {
                //If we are selecting items then we want option to delete them
                deleteItem.setVisible(true);
                createPlaylistItem.setVisible(false);
            } else {
                //Hide option to add to playlist
                deleteItem.setVisible(false);
                createPlaylistItem.setVisible(true);
            }




        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.delete_item)
        {
            //Delete playlists
            //Get all checked positions
            /*SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();
            Cursor cursor = mCursorAdapter.getCursor();
            int position = -1;
            long id;
            int deletedCount = 0;

            for (int i = 0;i < checkedPositions.size();i++)
            {
                position = checkedPositions.keyAt(i);

                if (position >= 0 && position < cursor.getCount() && checkedPositions.get(position))
                {
                    cursor.moveToPosition(position);
                    id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
                    deletedCount += mContext.getContentResolver().delete(   MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                                                            MediaStore.Audio.Playlists._ID + "=?",
                                                                            new String  [] {id + ""});
                }

            }*/


            new AsyncTask<SparseBooleanArray,Void,Integer>()
            {
                @Override
                protected Integer doInBackground(SparseBooleanArray... params) {

                    return Utils.deleteFromList(getContext(), mCursor, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,params[0]);
                }

                @Override
                protected void onPostExecute(Integer result) {
                    Toast.makeText(mContext,result + " items deleted",Toast.LENGTH_SHORT).show();
                    loadDataAsync();
                }
            }.execute(getListView().getCheckedItemPositions());





        }
        else if (item.getItemId() == R.id.playlist_create)
        {
            //Create new playlist

            mCreatePlaylistEditText.setText(R.string.playlist_default_name);
            mCreatePlaylistEditText.selectAll();

            mCreatePlaylistDialog.show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //Open playlist only if we are not in select mode
        if (!mSelectMode)
            super.onItemClick(parent, view, position, id);

    }

}
