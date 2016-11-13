package com.example.miroslav.slimplayer;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

//TODO - add intent filter so we can run any song
public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener {

    private MediaPlayerBinder mBinder = new MediaPlayerBinder();

    public MediaPlayer mPlayer;

   // private Cursor mCursor;
    private List<Song> mSongList;
    private int mPosition;
    private int mCount;

    //TODO - Init this somewhere else
    private boolean mRepeatPlaylist = true;

    public MediaPlayerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    //NOTE - THIS IS CALLED WHEN SERVICE IS CALLED WHILE IT IS ALREADY RUNNING
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    public class MediaPlayerBinder extends Binder {
        MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }


    public void play(int position)
    {
        mPosition = position;
        Song song = mSongList.get(mPosition);
        Toast.makeText(getApplicationContext(),song.getData(),Toast.LENGTH_SHORT).show();

        try
        {
            mPlayer.reset();
            mPlayer.setDataSource(song.getData());
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                //This is called when media player is prepared with its data source
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mPlayer.setOnCompletionListener(this);
            mPlayer.prepareAsync();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        //Old code for playing from cursor
       /* try
        {
            if (mCursor == null)
            {
                Log.d("slim","Media Player Service - mCursor is null");
                return;
            }

            //Try to move to position
            if (!mCursor.moveToPosition(position))
            {
                Log.d("slim","Media Player Service - Failed to move to position (out of bounds?)");
                return;
            }

            mPosition = position;
            String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            Toast.makeText(getApplicationContext(),path,Toast.LENGTH_SHORT).show();

            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                //This is called when media player is prepared with its data source
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mPlayer.setOnCompletionListener(this);
            mPlayer.prepareAsync();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }*/
    }

    //This is called when song is finished playing
    @Override
    public void onCompletion(MediaPlayer mp) {

        if (mPosition == mCount - 1)
        {
            //If we are at the end of playlist
            if (mRepeatPlaylist)
            {
                //If we are repeating playlist then start from the begining
                MediaPlayerService.this.play(0);
            }
        }
        else
        {
            //Play next song
            MediaPlayerService.this.play(mPosition + 1);
        }
    }

   /* public void setCursor(Cursor cursor)
    {
        //TODO - maybe work with actual position from cursor
        //Whenever cursor is changed, we set position to -1
        mPosition = -1;

        if (cursor == null)
        {
            mCursor = null;
            mCount = 0;
        }
        else
        {
            mCursor = cursor;
            mCount = mCursor.getCount();
        }


    }*/

    public void setSongList(List<Song> songList)
    {
        mPosition = -1;

        if (songList != null)
        {
            mCount = songList.size();
            mSongList = songList;
        }
        else
        {
            mCount = 0;
            mSongList = null;
        }

    }


}
