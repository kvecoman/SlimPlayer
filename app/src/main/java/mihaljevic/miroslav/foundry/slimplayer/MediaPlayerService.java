package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
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
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO - add intent filter so we can run songs from anywhere
public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener {

    //Notification ID
    public static final int NOTIFICATION_PLAYER_ID = 1;

    public static final String  NOTIFICATION_ACTION_CLOSE = "mihaljevic.miroslav.foundry.slimplayer.action.close";
    public static final String  NOTIFICATION_ACTION_PREVIOUS = "mihaljevic.miroslav.foundry.slimplayer.action.previous";
    public static final String  NOTIFICATION_ACTION_PLAY_PAUSE = "mihaljevic.miroslav.foundry.slimplayer.action.play_pause";
    public static final String  NOTIFICATION_ACTION_NEXT = "mihaljevic.miroslav.foundry.slimplayer.action.next";
    public static final String  NOTIFICATION_ACTION_SWIPE = "mihaljevic.miroslav.foundry.slimplayer.action.swipe";



    //TODO - maybe move this also to resource file
    //Points to location of our custom font file
    public static final String ICON_FONT_PATH = "fonts/icons.ttf";

    private Map<String,Bitmap> mBitmapIcons;

    private MediaPlayerBinder mBinder = new MediaPlayerBinder();

    public MediaPlayer mPlayer;

    private boolean mPlaying = false;
    private boolean mStopped = true;

   // private Cursor mCursor;
    private List<Song> mSongList;
    private int mPosition;
    private int mCount;
    private boolean mReadyToPlay = false; //Indicates if we have list loaded

    //TODO - Init this somewhere else
    private boolean mRepeatPlaylist = true;

    private List<MediaPlayerListener> mPlayerListenersList;

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

        mPlayerListenersList = new ArrayList<>();

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
                stop();
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
            else if (action.equals(NOTIFICATION_ACTION_SWIPE))
            {
                stop();
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

        //If something is wrong then do nothing
        if (mPosition == position || position < 0 || position >= mCount || mSongList == null)
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
                    mStopped = false;

                    //Notify NowPlayingActivity that we changed playing song
                    notifyListenersPlay();
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

    //This is called when song is finished playing
    @Override
    public void onCompletion(MediaPlayer mp) {
        //Continue to next song only if we are set to be playing
        if (mPlaying) {
            MediaPlayerService.this.playNext();
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
            notifyListenersResume();
        }
    }

    //Function that tries to resume playback if the song is loaded, or load and play the song if not
    public void resumeOrPlay(int position)
    {
        if (position == mPosition && mStopped == false)
        {
            resume();
        }
        else
        {
            play(position);
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

    public void stop()
    {

        if (mPlayer != null)
        {
            mPlaying = false;
            mStopped = true;
            mPosition = -1;
            mPlayer.stop();
            mPlayer.reset();
        }
    }

    //Stop playing and clear list
    public void stopAndClearList()
    {
        mCount = 0;
        mPosition = -1;
        mSongList = null;
        mReadyToPlay = false;
        stop();

    }

    //Start ending this service
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


    public void registerListener(MediaPlayerListener listener)
    {
        mPlayerListenersList.add(listener);
    }

    public void unregisterListener(MediaPlayerListener listener)
    {
        mPlayerListenersList.remove(listener);
    }

    public void notifyListenersPlay()
    {
        for (MediaPlayerListener listener : mPlayerListenersList)
        {
            try {
                listener.onPlay(mSongList, mPosition);
            }
            catch (ConcurrentModificationException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void notifyListenersResume()
    {
        for (MediaPlayerListener listener : mPlayerListenersList)
        {
            listener.onSongResume();
        }
    }


    //Display notification player for this service
    public void showNotification(boolean playIcon, boolean ticker)
    {
        RemoteViews notificationView = new RemoteViews(getPackageName(),R.layout.notification_player);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        notificationView.setTextViewText(R.id.notification_title,getCurrentSong().getTitle());

        //Check if we have notifications icons, if not generate them right now
        if (mBitmapIcons == null)
        {
            generateBitmapIcons();
        }

        //Render font icons in scale with current device screen density
        notificationView.setImageViewBitmap(R.id.notification_close,        mBitmapIcons.get(getString(R.string.icon_close)));
        notificationView.setImageViewBitmap(R.id.notification_previous,     mBitmapIcons.get(getString(R.string.icon_previous)));
        notificationView.setImageViewBitmap(R.id.notification_play,         mBitmapIcons.get(getString(playIcon ? R.string.icon_play : R.string.icon_pause)));
        notificationView.setImageViewBitmap(R.id.notification_next,         mBitmapIcons.get(getString(R.string.icon_next)));

        //Set-up notification
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(false)
                .setAutoCancel(false)
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

        //Swipe notification intent
        intent = new Intent(this, this.getClass());
        intent.setAction(NOTIFICATION_ACTION_SWIPE);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        builder.setDeleteIntent(pendingIntent);


        //Build and show notification
        Notification notification = builder.build();

        //If we are playing, start notification as foreground otherwise start it so it can be dismissed
        /*if (playIcon)
        {
            startForeground(NOTIFICATION_PLAYER_ID,notification);
        }
        else
        {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_PLAYER_ID, notification);
        }*/

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_PLAYER_ID, notification);



    }

    //Generates bitmap icons used for notification player
    public void generateBitmapIcons()
    {
        String[] iconCodes = getResources().getStringArray(R.array.icon_codes);
        Bitmap bitmap;
        Map<String,Bitmap> bitmaps = new HashMap<>(iconCodes.length);

        for (String iconCode : iconCodes)
        {
            bitmap = Utils.renderFont(this, iconCode, Color.LTGRAY,getResources().getInteger(R.integer.notification_icon_size_sp), ICON_FONT_PATH);
            bitmaps.put(iconCode,bitmap);
        }

        mBitmapIcons = bitmaps;
    }


    //Set the list and start playing
    public void playList(List<Song> songList, int startPosition)
    {
        setSongList(songList);
        play(startPosition);
    }


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
            mReadyToPlay = true;
        }
        else
        {
            //We have nothing, respond appropriately
            stopAndClearList();
        }

    }

    //Used to send info when NowPlayingActivity is starting to get up to date with player service
    public void setCurrentPlayInfoToListener()
    {
        Log.d("slim","MediaPlayerService - setCurrentPlayInfoToListener()");
        //Check if we have something in this service
        if (mCount > 0 && mPosition >= 0 && mSongList != null)
        {
            notifyListenersPlay();
        }

    }

    public boolean isReadyToPlay() {
        return mReadyToPlay;
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

    public interface MediaPlayerListener
    {
        void onPlay(List<Song> songList, int position);

        void onSongResume();
    }


}
