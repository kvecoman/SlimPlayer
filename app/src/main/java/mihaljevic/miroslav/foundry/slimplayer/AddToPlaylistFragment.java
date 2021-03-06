package mihaljevic.miroslav.foundry.slimplayer;


import android.content.ContentResolver;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Fragment with functionality to add selected songs to playlist
 *
 * @author Miroslav Mihaljević
 */
public class AddToPlaylistFragment extends PlaylistsFragment{

    //Fields that need to be accessible to AsyncTask (when adding to playlist)
    private List<String> mIdList;
    private int mSongCount;
    private Cursor mPlaylistCursor;
    private Uri mPlaylistUri;
    private List<ContentValues> mValuesList;


    public AddToPlaylistFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       return super.onCreateView(inflater,container,savedInstanceState);
    }

    //Here we do most of init
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //super.onItemClick(parent, view, position, id);
        if (position < getListAdapter().getCount())
        {
            //Get id of selected playlist
            /*Cursor cursor = mCursorAdapter.getCursor();
            cursor.moveToPosition(position);
            final long playlistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
            mPlaylistUri = MediaStore.Audio.Playlists.Members.getContentUri("external",playlistId);

            //Here we add songs to selected playlist
            mPlaylistCursor = mContext.getContentResolver().query(mPlaylistUri,new String[]{MediaStore.Audio.Playlists.Members._ID, MediaStore.Audio.Playlists.Members.AUDIO_ID},null,null,null);
            mSongCount = mPlaylistCursor.getCount();
            mIdList = getActivity().getIntent().getStringArrayListExtra(AddToPlaylistActivity.ID_LIST_KEY);
            mValuesList = new ArrayList<>();

            //Do rest of this in AsyncTask, heavy work ahead
            (new AsyncTask<Void,Void,Void>(){

                @Override
                protected Void doInBackground(Void... params) {

                    //Create values for every song we are adding
                    ContentValues values;

                    for(int i = 0;i < mIdList.size();i++)
                    {
                        if (!Utils.playlistCheckForDuplicate(mPlaylistCursor,Long.parseLong(mIdList.get(i)))) {

                            values = new ContentValues();
                            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, Long.valueOf(mIdList.get(i)));
                            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, mValuesList.size() + mSongCount);
                            mValuesList.add(values);
                        }
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    //Insert all selected songs values in playlist
                    ContentResolver resolver = mContext.getContentResolver();
                    ContentValues[] valuesArray = new ContentValues[mValuesList.size()];
                    mValuesList.toArray(valuesArray);
                    int result = resolver.bulkInsert(mPlaylistUri, valuesArray);
                    resolver.notifyChange(mPlaylistUri,null);

                    Log.d("slim","Number of items added " + result + "");
                    Toast.makeText(mContext,result + " " + getString(R.string.playlist_add_succes),Toast.LENGTH_SHORT).show();
                }
            }).execute();*/

            Cursor cursor = mCursorAdapter.getCursor();
            List<String> ids  = getActivity().getIntent().getStringArrayListExtra(AddToPlaylistActivity.ID_LIST_KEY);
            final long playlistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));

            //Insert songs in playlist
            new AsyncTask<List<String>,Void,Integer>(){

                @Override
                protected Integer doInBackground(List<String>... params)
                {
                    List<String> ids = params[0];

                    if (ids == null)
                        return null;

                    return Utils.insertIntoPlaylist(getContext(),ids,playlistId);
                }

                @Override
                protected void onPostExecute(Integer result) {
                    Toast.makeText(mContext,result + " " + getString(R.string.playlist_add_succes),Toast.LENGTH_SHORT).show();
                    //loadDataAsync();
                }
            }.execute(ids);

            //Exit after we start adding songs to playlist
            getActivity().finish();

        }

    }




}
