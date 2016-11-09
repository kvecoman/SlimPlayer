package com.example.miroslav.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

/**
 * Fragment that displays categories/playlists and opens apropriate song lists
 */

public class CategoryListFragment extends SlimListFragment {


    public CategoryListFragment() {
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



    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Bundle bundle = null;
        String parameter;
        Cursor cursor = mCursorAdapter.getCursor();

        Intent intent = new Intent(mContext,SongListActivity.class);
        cursor.moveToPosition(position);

        //TODO - move this somewhere sane, probably ScreenCursors
        //Choose which screen we are showing next and we send apropriate bundle for it
        if(mCurrentScreen.equals(getString(R.string.pref_key_playlists_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
            bundle = ScreenCursors.getSongsByPlaylistBundle(mContext, parameter);
        }
        else if (mCurrentScreen.equals(getString(R.string.pref_key_albums_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            bundle = ScreenCursors.getSongsByAlbumBundle(mContext,parameter);
        }
        else if (mCurrentScreen.equals(getString(R.string.pref_key_artists_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            bundle = ScreenCursors.getSongsByArtistsBundle(mContext,parameter);
        }
        else if (mCurrentScreen.equals(getString(R.string.pref_key_genres_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres._ID));
            bundle = ScreenCursors.getSongsByGenreBundle(mContext,parameter);
        }

        //Bundle is chosen at this point and we send it to our custom SongListActivity which will display apropriate list fragment
        intent.putExtra(SongListActivity.FRAGMENT_BUNDLE_KEY,bundle);
        startActivity(intent);

    }
}
