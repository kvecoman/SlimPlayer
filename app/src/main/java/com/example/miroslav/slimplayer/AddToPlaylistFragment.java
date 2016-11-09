package com.example.miroslav.slimplayer;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddToPlaylistFragment extends CategoryListFragment{


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

        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        TextView view = (TextView)inflater.inflate(android.R.layout.simple_list_item_activated_1,null);
        view.setText(R.string.playlist_create);
        getListView().addFooterView(view);

        if (BuildConfig.DEBUG == true)
        {
            TextView view2 = (TextView)inflater.inflate(android.R.layout.simple_list_item_activated_1,null);
            view2.setText("Delete playlists");
            getListView().addFooterView(view2);
        }

        getLoaderManager().initLoader(0,getArguments(),this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //super.onItemClick(parent, view, position, id);
        if (position < getListAdapter().getCount())
        {
            //Get id of selected playlist
            Cursor cursor = mCursorAdapter.getCursor();
            cursor.moveToPosition(position);
            long playlistId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",playlistId);

            //Here we add songs to selected playlist
            Cursor playlistCursor = mContext.getContentResolver().query(uri,new String[]{MediaStore.Audio.Playlists.Members._ID},null,null,null);
            int count = playlistCursor.getCount();
            List<String> idList = getActivity().getIntent().getStringArrayListExtra(AddToPlaylistActivity.ID_LIST_KEY);
            ContentValues[] values = new ContentValues[idList.size()];

            //Create values for everysong we are adding
            for(int i = 0;i < idList.size();i++)
            {
                values[i] =  new ContentValues();
                values[i].put(MediaStore.Audio.Playlists.Members.AUDIO_ID,Long.valueOf(idList.get(i)));
                values[i].put(MediaStore.Audio.Playlists.Members.PLAY_ORDER,i + count);
            }

            ContentResolver resolver = mContext.getContentResolver();
            int result = resolver.bulkInsert(uri, values);
            resolver.notifyChange(uri,null);
            Log.d("slim","Number of items added " + result + "");
            Toast.makeText(mContext,result + " " + getString(R.string.playlist_add_succes),Toast.LENGTH_SHORT).show();
        }
        else if (position == getListAdapter().getCount())
        {

            //Create new playlist
            final EditText editText = new EditText(mContext);

            //TODO - set hint text for playlist title
            new AlertDialog.Builder(mContext)
                    .setTitle("Create new playlist")
                    .setMessage("Enter playlist name!")
                    .setView(editText)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            //Here we create new playlist
                            String playlistName;

                            playlistName = editText.getText().toString();

                            ContentValues inserts = new ContentValues();
                            inserts.put(MediaStore.Audio.Playlists.NAME, playlistName);
                            inserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
                            inserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());
                            Uri uri = mContext.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,inserts);

                            Cursor c = mContext.getContentResolver().query(uri, new String[]{MediaStore.Audio.Playlists._ID},null,null,null);
                            c.moveToFirst();
                            int pId = c.getInt(0);
                            c.close();

                            //Toast.makeText(mContext,uri.toString() + " ------ " + pId,Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();



        }
        else if (position == getListAdapter().getCount() + 1)
        {
            //Delete all playlists
            mContext.getContentResolver().delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,null,null);
        }
    }


}
