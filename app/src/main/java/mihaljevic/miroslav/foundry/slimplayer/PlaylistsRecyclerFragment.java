package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
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
public class PlaylistsRecyclerFragment extends CategoryRecyclerFragment {

    private AlertDialog mCreatePlaylistDialog;
    private EditText mCreatePlaylistEditText;

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
            //Delete playlists
            new AsyncTask<SparseBooleanArray,Void,Integer>()
            {
                @Override
                protected Integer doInBackground(SparseBooleanArray... params) {

                    return Utils.deleteFromList(getContext(), mAdapter.getCursor(), MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,params[0]);
                }

                @Override
                protected void onPostExecute(Integer result) {
                    Toast.makeText(mContext,result + " items deleted",Toast.LENGTH_SHORT).show();
                    loadDataAsync();
                }
            }.execute(mSelectedItems);

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
