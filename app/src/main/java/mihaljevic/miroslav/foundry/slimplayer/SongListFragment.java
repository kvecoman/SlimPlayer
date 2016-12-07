package mihaljevic.miroslav.foundry.slimplayer;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays only songs, and responds appropriately to click
 *
 * @author Miroslav Mihaljević
 */
public class SongListFragment extends SlimListFragment {


    private SlimPlayerApplication mApplication;

    protected boolean mSelectMode = false;

    protected List<Song> mSongList;


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
                    //If we are not in select mode, activate it
                    ((ListView)parent).setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                    ((ListView)parent).setItemChecked(position, true);
                    mSelectMode = true;
                    getActivity().invalidateOptionsMenu();
                }
                return true;
            }
        });



    }

    //Here we load all songs in songList when Cursor loader is finished
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);

        Log.d("slim",mCurrentScreen + " - onLoadFinished()");

        mSongList = getSongListFromCursor(data);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        //If we are not selecting items, then we want to play them
        if (!mSelectMode)
        {
            //Pass list of songs from which we play and play current position
            mApplication.getMediaPlayerService().playList(mSongList,position);

            //Start NowPlayingActivity
            Intent intent = new Intent(mContext,NowPlayingActivity.class);
            startActivity(intent);
        }

    }



    @Override
    public boolean onBackPressed() {

        //Here we store if the back button event is consumed
        boolean backConsumed = false;

        if (mSelectMode)
        {
            //Deselect everything
            deselect();
            backConsumed = true;
        }

        return backConsumed;
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        MenuItem playlistAddMenuItem = menu.findItem(R.id.playlist_add);

        if (mSelectMode)
        {
            //If we are selecting items then we want option to add them to playlist
            playlistAddMenuItem.setVisible(true);
        }
        else
        {
            //Hide option to add to playlist
            playlistAddMenuItem.setVisible(false);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mSelectMode)
        {
            if (item.getItemId() == R.id.playlist_add)
            {

                //Get all checked positions
                SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();

                Log.d("slim", getListView().getCount() + " of list items ----- " + checkedPositions.size() + " of items in boolean array");

                Cursor cursor = mCursorAdapter.getCursor();
                List<String> idList = new ArrayList<>(checkedPositions.size());

                //Transfer IDs from selected songs to ID list
                for (int i = 0; i < checkedPositions.size();i++)
                {
                    int position = checkedPositions.keyAt(i);
                    Log.d("slim","Position of checked song: " + position);

                    cursor.moveToPosition(position);
                    idList.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));

                }
                //Here we call add to playlists activity and pass ID list
                Intent intent = new Intent(mContext,AddToPlaylistActivity.class);
                intent.putExtra(AddToPlaylistActivity.ID_LIST_KEY,(ArrayList)idList);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    //Deselects selection in listView
    public void deselect()
    {
        mSelectMode = false;
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().clearChoices();
        getListView().requestLayout();
    }

    //Here we create an ArrayList of all songs from cursor
    public List<Song> getSongListFromCursor(Cursor cursor)
    {
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
    }
}
