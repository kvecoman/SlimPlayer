package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

//TODO - add intent filter so we can run any song
public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener {

    public static final int NOTIFICATION_PLAYER_ID = 1;

    //Points to location of our custom font file
    public static final String ICON_FONT_PATH = "fonts/icons.ttf";

    public static final int ICON_FONT_SIZE_SP = 20;

    private MediaPlayerBinder mBinder = new MediaPlayerBinder();

    public MediaPlayer mPlayer;

    private boolean mPlaying = false;

   // private Cursor mCursor;
    private List<Song> mSongList;
    private int mPosition;
    private int mCount;

    //TODO - Init this somewhere else
    private boolean mRepeatPlaylist = true;

    private MediaPlayerListener mPlayerListener;

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
        Log.d("slim","MediaPlayerService - play()");
        //TODO - think of better solution
        //play() method can be called two times in row from NowPlayingActivity->onPageChanged()
        //we fix this here
        if (mPosition == position)
            return;

        mPosition = position;
        Song song = mSongList.get(mPosition);
        Toast.makeText(getApplicationContext(),song.getData(),Toast.LENGTH_SHORT).show();

        try
        {
            //Set up media player and start playing when ready
            mPlayer.reset();
            mPlayer.setDataSource(song.getData());
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                //This is called when media player is prepared with its data source
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mPlaying = true;

                    //Notify NowPlayingActivity that we changed playing song
                    if (mPlayerListener != null)
                        mPlayerListener.onSongChanged(mSongList,mPosition);
                }
            });
            mPlayer.setOnCompletionListener(this);
            mPlayer.prepareAsync();

            showNotification();
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
        Log.d("slim","MediaPlayerService - onCompletion()");

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

    //TODO - when notification is pressed, make sure it doesnt restart the song
    //Display notification player for this service
    public void showNotification()
    {
        RemoteViews notificationView = new RemoteViews(getPackageName(),R.layout.notification_player4);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        notificationView.setTextViewText(R.id.notification_title,mSongList.get(mPosition).getTitle());

        //Render font icons in scale with current device screen density
        notificationView.setImageViewBitmap(R.id.notification_close,    Utils.renderFont(this, getString(R.string.icon_close),      Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));
        notificationView.setImageViewBitmap(R.id.notification_previous, Utils.renderFont(this, getString(R.string.icon_previous),   Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));
        notificationView.setImageViewBitmap(R.id.notification_play,     Utils.renderFont(this, getString(R.string.icon_play),       Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));
        notificationView.setImageViewBitmap(R.id.notification_next,     Utils.renderFont(this, getString(R.string.icon_next),       Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));

        //Set-up notification
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContent(notificationView)
                .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,NowPlayingActivity.class),0));

        //Build and show notification
        Notification notification = builder.build();
        //NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        startForeground(NOTIFICATION_PLAYER_ID,notification);

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

    //Setter for song list
    public void setSongList(List<Song> songList)
    {
        //This is starting position before we find out anything
        mPosition = -1;

        if (songList != null)
        {
            //Get song count and song list
            mCount = songList.size();
            mSongList = songList;
        }
        else
        {
            //We have nothing, respond appropriately
            mCount = 0;
            mSongList = null;
        }

    }

    //Used to send info when NowPlayingActivity is starting to get up to date with player service
    public void setCurrentPlayInfoToListener()
    {
        Log.d("slim","MediaPlayerService - setCurrentPlayInfoToListener()");
        //Check if we have something in this service
        if (mCount > 0 && mPosition >= 0 && mSongList != null && mPlayerListener != null)
        {
            mPlayerListener.onSongChanged(mSongList,mPosition);
        }

    }

    public MediaPlayer getMediaPlayer()
    {
        return mPlayer;
    }

    public Song getCurrentSong()
    {
        if (mPosition < 0 || mPosition >= mCount)
            return null;

        return mSongList.get(mPosition);

    }

    public Song getSong(int position)
    {
        if (position < 0 || position >= mCount)
            return null;

        return mSongList.get(position);

    }

    public int getPosition() {
        return mPosition;
    }

    public int getCount() {
        return mCount;
    }

    public boolean isPlaying()
    {
        return mPlaying;
    }

    public void setMediaPlayerListener(MediaPlayerListener listener)
    {
        mPlayerListener = listener;
    }


    public interface MediaPlayerListener
    {
        void onSongChanged(List<Song> songList, int position);
    }


}
