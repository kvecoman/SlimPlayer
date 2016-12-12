package mihaljevic.miroslav.foundry.slimplayer;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Fragment that displays songs from selected playlist and adds option to add songs to it.
 *
 * @author Miroslav Mihaljević
 */
public class PlaylistSongsFragment extends SongListFragment {

    public static final int SELECT_SONGS_REQUEST = 1;

    //We use this request when we are in secondary screen like songs by specific genre
    public static final int SELECT_SONGS_REQUEST_2 = 2;

    public static final String ACTION_SELECT_SONGS = "action_select_songs";

    //Key with which we retrieve result
    public static final String SELECTED_SONGS_KEY = "selected_songs";


    public PlaylistSongsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container,savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //We need different ID field when loading playlist songs
        mAudioIdField = MediaStore.Audio.Playlists.Members.AUDIO_ID;
    }

    /*@Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mSelectMode)
        {
            if (item.getItemId() == R.id.playlist_add_to_this) {
                //If we have not selected anything, then we run MainActivity for result
                Toast.makeText(getContext(), "Starting MainActivity for result", Toast.LENGTH_SHORT).show();


                Intent intent = new Intent(ACTION_SELECT_SONGS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,getContext(),MainActivity.class);
                intent.putExtra(SlimActivity.REQUEST_CODE_KEY,SELECT_SONGS_REQUEST);

                startActivityForResult(intent,SELECT_SONGS_REQUEST);


            }
        }
        else
        {
            //Upper class will handle selection mode
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data != null && requestCode == SELECT_SONGS_REQUEST && data.hasExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY))
        {
            //TODO - set this into async task
            List<String> ids = data.getStringArrayListExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY);
            insertIntoPlaylist(ids);

        }
    }

    public void insertIntoPlaylist(List<String> IDs)
    {
        //long [] IDs = data.getLongArrayExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY);
        long playlistId = Long.valueOf(getArguments().getString(SlimListFragment.CURSOR_PARAMETER_KEY));
        Uri playlistUri = MediaStore.Audio.Playlists.Members.getContentUri("external",playlistId);
        List<ContentValues> valuesList = new ArrayList<>();

        Cursor playlistCursor = mContext.getContentResolver().query(playlistUri,new String[]{MediaStore.Audio.Playlists.Members._ID, MediaStore.Audio.Playlists.Members.AUDIO_ID},null,null,null);
        int songCount = playlistCursor.getCount();

        ContentValues values;

        long id;
        for(String audio_id : IDs)
        {
            id = Long.parseLong(audio_id);
            if (!Utils.playlistCheckForDuplicate(playlistCursor,id)) {

                values = new ContentValues();
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id);
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, valuesList.size() + songCount);
                valuesList.add(values);
            }
        }

        ContentResolver resolver = mContext.getContentResolver();
        ContentValues[] valuesArray = new ContentValues[valuesList.size()];
        valuesList.toArray(valuesArray);
        int result = resolver.bulkInsert(playlistUri, valuesArray);
        resolver.notifyChange(playlistUri,null);

        Log.d("slim","Number of items added " + result + "");
        Toast.makeText(mContext,result + " " + getString(R.string.playlist_add_succes),Toast.LENGTH_SHORT).show();
    }

    //A small change in that we use audio_ID and not normal ID when creating song list
    /*@Override
    public List<Song> getSongListFromCursor(Cursor cursor) {
        List<Song> songList = new ArrayList<>();
        Song song;

        //If there are nothing in cursor just remove empty list
        if (cursor.getCount() == 0)
            return songList;

        //Transfer all data from cursor to ArrayList of songs
        cursor.moveToFirst();
        do
        {
            song = new Song(
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)), //This is the only change
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));

            songList.add(song);

        }
        while (cursor.moveToNext());

        return songList;
    }*/

    //Async task to create song list, almost same as in SongListFragment with slight difference
    /*private class AsyncGetSongList extends AsyncTask<Cursor,Void,List<Song>>
    {
        @Override
        protected List<Song> doInBackground(Cursor... params) {
            Cursor cursor = params[0];
            List<Song> songList = new ArrayList<>();
            Song song;

            //If there are nothing in cursor just return empty list
            if (cursor == null || cursor.getCount() == 0)
                return songList;

            //Transfer all data from cursor to ArrayList of songs
            cursor.moveToFirst();
            do
            {
                song = new Song(
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)), //This is the only change
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));

                songList.add(song);

            }
            while (cursor.moveToNext());

            return songList;
        }

        @Override
        protected void onPostExecute(List<Song> songs) {
            mSongList = songs;
        }
    }*/
}
