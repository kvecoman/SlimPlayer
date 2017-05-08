package mihaljevic.miroslav.foundry.slimplayer.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import mihaljevic.miroslav.foundry.slimplayer.Const;
import mihaljevic.miroslav.foundry.slimplayer.R;
import mihaljevic.miroslav.foundry.slimplayer.Utils;
import mihaljevic.miroslav.foundry.slimplayer.fragments.CategoryRecyclerFragment;

/**
 * Fragment that displays list of playlists + options to create new one or delete one
 *
 * @author Miroslav Mihaljević
 */
public class PlaylistsRecyclerFragment extends CategoryRecyclerFragment
{

    private AlertDialog mCreatePlaylistDialog;
    private EditText    mCreatePlaylistEditText;

    //We use this to check if default name with auto number has been used or not
    private String  mPlaylistAutoName;
    private int     mPlaylistAutoNumber;

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
        final Context context;

        context = getContext();

        //Set up edit text that will be used in alert dialog to get playlist name
        mCreatePlaylistEditText = new EditText(context);
        mCreatePlaylistEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);


        //Build alert dialog that will take name of new playlist
        mCreatePlaylistDialog = new AlertDialog.Builder(context)
                                .setTitle           (getString( R.string.add_playlist_title ))
                                .setMessage         (getString( R.string.add_playlist_message ))
                                .setView            (mCreatePlaylistEditText)
                                .setPositiveButton  (getString( R.string.OK ),        new PositiveButtonListener())
                                .setNegativeButton  (getString( R.string.Cancel ),    new NegativeButtonListener())
                                .create();

        mCreatePlaylistDialog.setOnShowListener(new ShowDialogListener());


    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        //Show options only if we are in normal mode (and not select for result mode)
        if (!mSelectSongsForResult)
        {
            MenuItem deleteItem;
            MenuItem createPlaylistItem;

            deleteItem          = menu.findItem(R.id.delete_item);
            createPlaylistItem  = menu.findItem(R.id.playlist_create);

            //Decide whether to show option for deleting playlists
            if ( mSelectMode && mSelectedItems.size() > 0 )
            {
                //If we are selecting items then we want option to delete them
                deleteItem.setVisible           (true);
                createPlaylistItem.setVisible   (false);
            }
            else
            {
                //Hide option to add to playlist
                deleteItem.setVisible           (false);
                createPlaylistItem.setVisible   (true);
            }




        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {

        int itemID;

        itemID = item.getItemId();

        switch (itemID)
        {
            case R.id.delete_item:
                //Delete selected playlists
                deleteItemsAsync(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, MediaStore.Audio.Playlists._ID);
                break;
            case R.id.playlist_create:
                //Show dialog to create playlist
                mCreatePlaylistDialog.show();
                break;
        }


        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onClick(View v)
    {

        int position;

        position = mRecyclerView.getChildLayoutPosition( v );

        //Open playlist only if we are not in select mode
        if ( !mSelectMode )
            super.onClick(v);
        else
        {
            //If we are in select mode, then select playlist
            setItemSelected( position, !isItemSelected(position), v );
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if ( !mSelectMode )
        {
            activateSelectMode();
            setItemSelected( mRecyclerView.getChildLayoutPosition(v), true, v);
        }
        return true;
    }

    private class ShowDialogListener implements DialogInterface.OnShowListener
    {
        @Override
        public void onShow( DialogInterface dialog )
        {
            SharedPreferences prefs;
            InputMethodManager inputMethodManager;

            prefs = PreferenceManager.getDefaultSharedPreferences( getContext() );

            //Construct default playlist name
            mPlaylistAutoNumber = prefs.getInt( Const.PLAYLIST_NUMBER_PREF_KEY, 1 );
            mPlaylistAutoName = getString( R.string.playlist_default_name ) + " " + mPlaylistAutoNumber;

            //Set-up playlist name edit text
            mCreatePlaylistEditText.setText( mPlaylistAutoName );
            mCreatePlaylistEditText.selectAll();

            //Code to automatically show soft keyboard when dialog is shown
            inputMethodManager = ( InputMethodManager ) getContext().getSystemService( Context.INPUT_METHOD_SERVICE );
            inputMethodManager.toggleSoftInput( InputMethodManager.SHOW_IMPLICIT, 0 );
        }
    }


    private class PositiveButtonListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick( DialogInterface dialog, int which )
        {
            //Here we create new playlist
            final String playlistName;

            playlistName = mCreatePlaylistEditText.getText().toString();

            //Async task to create playlist
            new AsyncTask<Void,Void,Boolean>()
            {

                @Override
                protected Boolean doInBackground(Void... params)
                {
                    long playlistID;


                    playlistID = -1;

                    if ( !Utils.checkIfPlaylistExist(playlistName) )
                    {
                        playlistID = Utils.createPlaylist(playlistName);


                        if (playlistID > 0 && TextUtils.equals( mPlaylistAutoName,playlistName))
                        {
                            //Increase the auto number for default playlist name
                            mPlaylistAutoNumber++;
                            PreferenceManager
                                    .getDefaultSharedPreferences(getContext())
                                    .edit()
                                    .putInt(Const.PLAYLIST_NUMBER_PREF_KEY, mPlaylistAutoNumber )
                                    .apply();

                        }
                    }

                    //Returns whether the playlist have been created
                    return playlistID > 0;
                }

                @Override
                protected void onPostExecute(Boolean playlistCreated)
                {
                    if (playlistCreated)
                        refreshData();
                    else
                    {
                        Utils.toastShort(getString(R.string.toast_playlist_create_failed));
                        mCreatePlaylistEditText.selectAll();
                    }
                }
            }.execute();
        }
    }


    private class NegativeButtonListener implements DialogInterface.OnClickListener
    {
        @Override
        public void onClick( DialogInterface dialog, int which )
        {
            //Nothing to do...
        }
    }
}
