package com.example.miroslav.slimplayer;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays only songs, and responds appropriately to click
 *
 * @author Miroslav Mihaljević
 */
public class SongListFragment extends SlimListFragment {

    //TODO - move this somewhere sane
    protected MediaPlayerService mPlayerService;
    protected boolean mServiceBound = false;
    //TODO - continue here- discover why this is false when the back button is pressed
    protected boolean mSelectMode = false;

    protected MenuItem mMenuItemPlaylistAdd = null;

    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            SongListFragment.this.mPlayerService = playerBinder.getService();
            SongListFragment.this.mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            SongListFragment.this.mServiceBound = false;
        }
    };


    public SongListFragment() {
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

        //TODO - maybe try block for this, looks dangerous
        //Here we init MediaPlayerService
        Intent playerServiceIntent = new Intent(mContext, MediaPlayerService.class);
        mContext.bindService(playerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);


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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        if (mSelectMode)
        {
            //We are selecting items
            //TODO - make this look sane
        }
        else
        {
            //Here we try to play song
            mPlayerService.setCursor(mCursorAdapter.getCursor());
            mPlayerService.play(position);
        }

    }

    @Override
    public boolean onBackPressed() {

        //Here we store if the back button event is consumed
        boolean backConsumed = false;

        if (mSelectMode)
        {
            mSelectMode = false;
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().clearChoices();
            getListView().requestLayout();
            backConsumed = true;
        }

        return backConsumed;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        //Here we get menu item for later use
        mMenuItemPlaylistAdd = menu.findItem(R.id.playlist_add);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        if (mSelectMode)
        {
            //If we are selecting items then we want option to add them to playlist
            mMenuItemPlaylistAdd.setVisible(true);
        }
        else
        {
            mMenuItemPlaylistAdd.setVisible(false);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {



        if (mSelectMode)
        {
            if (item.getItemId() == mMenuItemPlaylistAdd.getItemId())
            {
                Bundle bundle = new Bundle();
                SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();

                Log.d("slim", getListView().getCount() + " of list items ----- " + checkedPositions.size() + " of items in boolean array");
                Cursor cursor = mCursorAdapter.getCursor();
                List<String> idList = new ArrayList<>(checkedPositions.size());

                for (int i = 0; i < checkedPositions.size();i++)
                {
                    int position = checkedPositions.keyAt(i);
                    Log.d("slim","Position of checked song: " + position);

                    cursor.moveToPosition(position);
                    idList.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));

                }
                //Here we call add to playlists activity
                Intent intent = new Intent(mContext,AddToPlaylistActivity.class);
                intent.putExtra(AddToPlaylistActivity.ID_LIST_KEY,(ArrayList)idList);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {

        //Unbind service
        if (mServiceBound)
            mContext.unbindService(mServiceConnection);

        super.onDestroy();
    }
}
