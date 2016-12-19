package mihaljevic.miroslav.foundry.slimplayer;

import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Helper class that allows easy access to fields of different songs
 */

public class CursorSongs implements Songs {
    private final String TAG = getClass().getSimpleName();

    private Cursor mCursor;

    private int mIndexId;
    private int mIndexTitle;
    private int mIndexArtist;
    private int mIndexAlbum;
    private int mIndexDuration;
    private int mIndexData;

    CursorSongs(Cursor cursor)
    {
        Log.d(TAG, "Constructor");
        init(cursor);
    }

    private void init(Cursor cursor)
    {
        Log.d(TAG, "init()");
        if (cursor == null || cursor.isClosed() || cursor.getCount() == 0)
        {
            Log.d(TAG, "Failed to init CursorSongs");
            return;
        }

        mCursor = cursor;

        acquireIndexes();
    }

    public void swapCursor(Cursor cursor)
    {
        init(cursor);
    }

    //Check if we can still work with current cursor
    public boolean checkCursor()
    {
        if (mCursor == null || mCursor.isClosed() || mCursor.getCount() == 0)
            return false;

        return true;
    }

    //Method that acquires all indexes of fields in cursor
    public void acquireIndexes()
    {
        //Try to use AUDIO_ID (for playlist), if it doesn't exist, use normal ID
        mIndexId = mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID) != -1 ? mCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID) : mCursor.getColumnIndex(MediaStore.Audio.Media._ID);

        mIndexTitle = mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        mIndexArtist = mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        mIndexAlbum = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        mIndexDuration = mCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        mIndexData = mCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
    }

    public Cursor getCursor()
    {
        return mCursor;
    }

    @Override
    public int getCount()
    {
        if (!checkCursor())
            return 0;

        return mCursor.getCount();
    }

    @Override
    public long getId(int position) {
        if (!checkCursor() || position < 0 || position >= mCursor.getCount())
            return -1;

        mCursor.moveToPosition(position);

        return mCursor.getLong(mIndexId);
    }

    @Override
    public String getTitle(int position) {
        if (!checkCursor() || position < 0 || position >= mCursor.getCount())
            return null;

        mCursor.moveToPosition(position);

        return mCursor.getString(mIndexTitle);
    }

    @Override
    public String getArtist(int position) {
        if (!checkCursor() || position < 0 || position >= mCursor.getCount())
            return null;

        mCursor.moveToPosition(position);

        return mCursor.getString(mIndexArtist);
    }

    @Override
    public String getAlbum(int position) {
        if (!checkCursor() || position < 0 || position >= mCursor.getCount())
            return null;

        mCursor.moveToPosition(position);

        return mCursor.getString(mIndexAlbum);
    }

    @Override
    public long getDuration(int position) {
        if (!checkCursor() || position < 0 || position >= mCursor.getCount())
            return -1;

        mCursor.moveToPosition(position);

        return mCursor.getLong(mIndexDuration);
    }

    @Override
    public String getData(int position) {
        if (!checkCursor() || position < 0 || position >= mCursor.getCount())
            return null;

        mCursor.moveToPosition(position);

        return mCursor.getString(mIndexData);
    }
}
