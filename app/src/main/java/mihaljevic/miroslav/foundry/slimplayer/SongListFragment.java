package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays only songs, and responds appropriately to click
 *
 * @author Miroslav Mihaljević
 */
public class SongListFragment extends SlimListFragment {

    //If bundle contains this key
    public static final String PLAY_POSITION_KEY = "play_position";



    private SlimPlayerApplication mApplication;

    //Provides easy access to cursor and fields within it
    protected CursorSongs mSongs;

    //protected List<Song> mSongList;

    //protected String mAudioIdField = MediaStore.Audio.Media._ID;


    public SongListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return super.onCreateView(inflater, container,savedInstanceState);
    }

    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        mApplication = ((SlimPlayerApplication) getContext().getApplicationContext());


        //Handler for long click
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



    }

    @Override
    public void onStart()
    {
        super.onStart();

        //If we are selecting songs for result then we need to enforce select mode, so we don't play songs here
        if (mSelectSongsForResult)
        {
            activateSelectMode();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //We override this so that when data is refreshed we also load new cursor into mSongs
    @Override
    protected void swapCursor() {
        super.swapCursor();

        onDataLoaded();
    }

    private void onDataLoaded()
    {
        mSongs = new CursorSongs(mCursor);

        //If we are in normal mode
        if (!mSelectSongsForResult)
        {
            //Check if we need to start any song right now
            if (getArguments().containsKey(PLAY_POSITION_KEY))
            {
                int play_position = getArguments().getInt(PLAY_POSITION_KEY);

                mApplication.getMediaPlayerService().playList(mSongs,play_position,mCurrentScreen,getArguments().getString(CURSOR_PARAMETER_KEY));

                //Once we have started the intended song from intent, we delete it so we don't start it again and again
                getArguments().remove(PLAY_POSITION_KEY);
            }
        }


    }

    //Here we load all songs in songList when Cursor loader is finished
    /*@Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);


        //Get song list in mSongList
        //new AsyncGetSongList().execute(data);
        //mSongList = getSongListFromCursor(data);
        //getLoaderManager().initLoader(SONG_LIST_LOADER,null, new GetSongsLoaderCallback()).forceLoad();
        if (mSongs != null)
            mSongs.swapCursor(data);
        else
            mSongs = new CursorSongs(data);

    }*/

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {


        //If we are not selecting items, then we want to play them
        if (!mSelectMode && !mSelectSongsForResult && mSongs != null)
        {
            //Pass list of songs from which we play and play current position
            mApplication.getMediaPlayerService().playList(mSongs,position,mCurrentScreen,getArguments().getString(CURSOR_PARAMETER_KEY));

            //Start NowPlayingActivity
            Intent intent = new Intent(mContext,NowPlayingActivity.class);
            startActivity(intent);
        }

        //If we are in select for result mode
        if (mSelectSongsForResult)
        {
            if (getListView().isItemChecked(position)) {
                ((SelectSongsActivity) mContext).getSelectedSongsList().add(mSongs.getId(position) + "");
            }
            else
            {
                ((SelectSongsActivity) mContext).getSelectedSongsList().remove(mSongs.getId(position) + "");
            }
        }

    }






    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        //Show add to playlist only if we are in normal mode (and not select for result mode)
        if (!mSelectSongsForResult)
        {
            MenuItem playlistAddMenuItem = menu.findItem(R.id.playlist_add);

            if (mSelectMode && getListView().getCheckedItemCount() > 0) {
                //If we are selecting items then we want option to add them to playlist
                playlistAddMenuItem.setVisible(true);
            } else {
                //Hide option to add to playlist
                playlistAddMenuItem.setVisible(false);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //Allow this option only if we are in normal mode
        if (mSelectMode && !mSelectSongsForResult)
        {
            if (item.getItemId() == R.id.playlist_add)
            {
                //Get all checked positions
                SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();


                Cursor cursor = mCursorAdapter.getCursor();
                List<String> idList = new ArrayList<>();

                //Transfer IDs from selected songs to ID list
                for (int i = 0; i < checkedPositions.size();i++)
                {
                    int position = checkedPositions.keyAt(i);

                    if (checkedPositions.get(position))
                    {
                        cursor.moveToPosition(position);
                        idList.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                    }
                }

                //Here we call add to playlists activity and pass ID list
                Intent intent = new Intent(mContext,AddToPlaylistActivity.class);
                intent.putExtra(AddToPlaylistActivity.ID_LIST_KEY,(ArrayList)idList);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }





    //Here we create an ArrayList of all songs from cursor
    /*public List<Song> getSongListFromCursor(Cursor cursor)
    {
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
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
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

    //Async task to create song list
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
                        cursor.getLong(cursor.getColumnIndex(mAudioIdField)),
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


    /*private class GetSongsLoaderCallback implements LoaderManager.LoaderCallbacks<List<Song>>
    {
        protected final String TAG = getClass().getSimpleName();

        @Override
        public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
            Log.d(TAG,"onCreateLoader() for SONG_LIST_LOADER");
            return new SongListAsyncLoader(getContext(),mCursorAdapter.getCursor(),mAudioIdField);
        }

        @Override
        public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
            Log.d(TAG,"onLoadFinished() for SONG_LIST_LOADER");
            mSongList = data;
        }

        @Override
        public void onLoaderReset(Loader<List<Song>> loader) {
            Log.d(TAG,"onLoaderReset() for SONG_LIST_LOADER");
            mSongList = null;
        }
    }*/
}
