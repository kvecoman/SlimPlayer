package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Miroslav on 12.12.2016..
 *
 * Loader that loads all songs from cursor into List of Song objects
 *
 * @author Miroslav Mihaljević
 */
@Deprecated
public class SongListAsyncLoader extends AsyncTaskLoader<List<Song>>
{
    protected final String TAG = getClass().getSimpleName();

    private Cursor mCursor;
    private String mAudioIdField;

    public SongListAsyncLoader(Context context, Cursor cursor,String audioIdField) {
        super(context);
        Log.d(TAG,"Constructor");
        mCursor = cursor;
        mAudioIdField = audioIdField;
    }

    @Override
    public List<Song> loadInBackground() {
        Log.d(TAG, "loadInBackground()");
        Cursor cursor = mCursor;
        List<Song> songList = new ArrayList<>();
        Song song;

        //If there are nothing in cursor just return empty list
        if (cursor == null || cursor.isClosed() || cursor.getCount() == 0)
            return songList;

        //Make sure cursor is not used at same time in other threads
        synchronized (mCursor) {
            Log.d(TAG, "acquired cursor lock");

            //Transfer all data from cursor to ArrayList of songs
            cursor.moveToFirst();
            do {
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

            Log.d(TAG, "released cursor lock");
        }

        return songList;
    }
}
