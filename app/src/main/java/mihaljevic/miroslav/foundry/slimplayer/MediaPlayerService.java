package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

//TODO - add intent filter so we can run songs from anywhere
public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener {

    //Notification ID
    public static final int NOTIFICATION_PLAYER_ID = 1;

    public static final String  NOTIFICATION_ACTION_CLOSE = "mihaljevic.miroslav.foundry.slimplayer.action.close";
    public static final String  NOTIFICATION_ACTION_PREVIOUS = "mihaljevic.miroslav.foundry.slimplayer.action.previous";
    public static final String  NOTIFICATION_ACTION_PLAY_PAUSE = "mihaljevic.miroslav.foundry.slimplayer.action.play_pause";
    public static final String  NOTIFICATION_ACTION_NEXT = "mihaljevic.miroslav.foundry.slimplayer.action.next";



    //Points to location of our custom font file
    public static final String ICON_FONT_PATH = "fonts/icons.ttf";

    public static final int ICON_FONT_SIZE_SP = 20;


    private RemoteViews mNotificationView;

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

        //Get action from intent while checking for null
        String action = intent == null ? null : intent.getAction();

        if (action != null)
        {
            if (action.equals(NOTIFICATION_ACTION_CLOSE))
            {
                endService();
            }
            else if (action.equals(NOTIFICATION_ACTION_PREVIOUS))
            {
                playPrevious();
            }
            else if(action.equals(NOTIFICATION_ACTION_PLAY_PAUSE))
            {
                if (mPlaying)
                    pause();
                else
                    resume();
            }
            else if (action.equals(NOTIFICATION_ACTION_NEXT))
            {
                playNext();
            }
        }


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {


        super.onDestroy();
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

            showNotification(false, true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public void pause()
    {
        if (mPlayer != null)
        {
            mPlaying = false;
            mPlayer.pause();
            showNotification(true,false);
        }
    }

    public void resume()
    {
        if (mPlayer != null)
        {
            mPlaying = true;
            mPlayer.start();
            showNotification(false, false);
        }
    }

    public void playNext()
    {
        //If the player is not even started then just dont do anything
        if (mPosition == -1)
            return;

        if (mPosition == mCount - 1)
        {
            //If we are at the end of playlist
            if (mRepeatPlaylist)
            {
                //If we are repeating playlist then start from the begining
                play(0);
            }
        }
        else
        {
            //Play next song
            play(mPosition + 1);
        }
    }

    public void playPrevious()
    {
        //If the player is not even started then just don't do anything
        if (mPosition == -1 || mPosition == 0)
            return;


        //Play previous song
        play(mPosition - 1);

    }

    public void endService()
    {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        mPlaying = false;
        stopForeground(true);
        stopSelf();
    }

    //This is called when song is finished playing
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("slim","MediaPlayerService - onCompletion()");

        MediaPlayerService.this.playNext();


    }


    //Display notification player for this service
    public void showNotification(boolean playIcon, boolean ticker)
    {
        RemoteViews notificationView = new RemoteViews(getPackageName(),R.layout.notification_player);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        notificationView.setTextViewText(R.id.notification_title,getCurrentSong().getTitle());

        //TODO - cache this
        //Render font icons in scale with current device screen density
        notificationView.setImageViewBitmap(R.id.notification_close,    Utils.renderFont(this, getString(R.string.icon_close),      Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));
        notificationView.setImageViewBitmap(R.id.notification_previous, Utils.renderFont(this, getString(R.string.icon_previous),   Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));
        notificationView.setImageViewBitmap(R.id.notification_play,     Utils.renderFont(this, getString(playIcon ? R.string.icon_play : R.string.icon_pause),       Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));
        notificationView.setImageViewBitmap(R.id.notification_next,     Utils.renderFont(this, getString(R.string.icon_next),       Color.LTGRAY,ICON_FONT_SIZE_SP, ICON_FONT_PATH));

        //Set-up notification
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContent(notificationView)
                .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,NowPlayingActivity.class),0));

        //If needed, set the ticker text with song title
        if (ticker)
            builder.setTicker(getCurrentSong().getTitle());

        //Set-up control actions
        Intent intent;
        PendingIntent pendingIntent;

        //Show play screen/main action
        //We build intent with MainActivity as parent activity in stack
        intent = new Intent(this, NowPlayingActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(NowPlayingActivity.class);
        stackBuilder.addNextIntent(intent);
        pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        //Close action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_CLOSE);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_close, pendingIntent);

        //Previous song action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_PREVIOUS);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_previous, pendingIntent);

        //Pause song action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_PLAY_PAUSE);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_play, pendingIntent);

        //Next song action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_NEXT);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_next, pendingIntent);

        //Save notification view for updating
        mNotificationView = notificationView;

        //Build and show notification
        Notification notification = builder.build();
        //NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        startForeground(NOTIFICATION_PLAYER_ID,notification);

    }


    //TODO - method that starts playing from list, and it chekcs if it is the same list with hash functions


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
