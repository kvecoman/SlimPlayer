package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Fragment that displays list of playlists + options to create new one or delete one
 *
 * @author Miroslav Mihaljević
 */
public class PlaylistsRecyclerFragment extends CategoryRecyclerFragment {

    private AlertDialog mCreatePlaylistDialog;
    private EditText mCreatePlaylistEditText;

    //We use this to check if default name with auto number has been used or not
    private String mDefaultPlaylistName;
    private int mPlaylistNumber;

    public PlaylistsRecyclerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Set up AlertDialog for creating playlists
        initCreatePlaylistDialog();
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

                        //Async task in async task, in first one we check if playlist with that name already exist,
                        // and in second one we actually create playlist
                        new AsyncTask<String,Void,Boolean>()
                        {
                            private String mPlaylistName;

                            @Override
                            protected Boolean doInBackground(String... params) {
                                mPlaylistName = params[0];

                                return Utils.checkIfPlaylistExist(mPlaylistName);
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                //If playlist already exist
                                if (result)
                                {
                                    Utils.toastShort(getString(R.string.toast_playlist_exists));
                                    mCreatePlaylistEditText.selectAll();
                                }
                                else
                                {
                                    //If not then we can create new playlist
                                    new AsyncTask<String,Void,Long>()
                                    {
                                        @Override
                                        protected Long doInBackground(String... params) {
                                            String playlistName = params[0];

                                            //Create playlist
                                            long playlistId = Utils.createPlaylist(playlistName);

                                            if (playlistId > 0 && Utils.equalsIncludingNull(mDefaultPlaylistName,playlistName))
                                            {
                                                //Increase the auto number for default playlist name
                                                mPlaylistNumber++;
                                                PreferenceManager
                                                        .getDefaultSharedPreferences(getContext())
                                                        .edit()
                                                        .putInt(getString(R.string.pref_key_playlist_number),mPlaylistNumber)
                                                        .apply();
                                            }

                                            return playlistId;
                                        }

                                        @Override
                                        protected void onPostExecute(Long result) {
                                            //Refresh data
                                            refreshData();

                                        }
                                    }.execute(mPlaylistName);
                                }
                            }
                        }.execute(playlistName);

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
                //Construct default playlist name
                mPlaylistNumber = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(getString(R.string.pref_key_playlist_number),1);
                mDefaultPlaylistName = getString(R.string.playlist_default_name) + " " + mPlaylistNumber;

                //Set-up playlist name edit text
                mCreatePlaylistEditText.setText(mDefaultPlaylistName);
                mCreatePlaylistEditText.selectAll();

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
            if (mSelectMode && mSelectedItems.size() > 0)
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
            deleteItemsAsync(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MediaStore.Audio.Playlists._ID);
        }
        else if (item.getItemId() == R.id.playlist_create)
        {

            //Show dialog to create playlist
            mCreatePlaylistDialog.show();
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onClick(View v) {

        int position = mRecyclerView.getChildLayoutPosition(v);

        //Open playlist only if we are not in select mode
        if (!mSelectMode)
            super.onClick(v);
        else
        {
            //If we are in select mode, then select playlist
            setItemSelected(position,!isItemSelected(position),v);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (!mSelectMode)
        {
            activateSelectMode();
            setItemSelected(mRecyclerView.getChildLayoutPosition(v),true,v);
        }
        return true;
    }
}
