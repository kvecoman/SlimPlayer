package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;


//TODO - add intent filter so we can run songs from anywhere
public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    protected final String TAG = getClass().getSimpleName();

    //Notification ID
    public static final int NOTIFICATION_PLAYER_ID = 1;

    public static final String  NOTIFICATION_ACTION_CLOSE = "mihaljevic.miroslav.foundry.slimplayer.action.close";
    public static final String  NOTIFICATION_ACTION_PREVIOUS = "mihaljevic.miroslav.foundry.slimplayer.action.previous";
    public static final String  NOTIFICATION_ACTION_PLAY_PAUSE = "mihaljevic.miroslav.foundry.slimplayer.action.play_pause";
    public static final String  NOTIFICATION_ACTION_NEXT = "mihaljevic.miroslav.foundry.slimplayer.action.next";
    public static final String  NOTIFICATION_ACTION_SWIPE = "mihaljevic.miroslav.foundry.slimplayer.action.swipe";


    //Points to location of our custom font file
    //public static final String ICON_FONT_PATH = "fonts/icons.ttf";

    //private Map<String,Bitmap> mBitmapIcons;

    private MediaPlayerBinder mBinder = new MediaPlayerBinder();

    public MediaPlayer mPlayer;

    private boolean mPlaying = false;
    private boolean mStopped = true;

    private AudioManager mAudioManager;

    private AsyncTask<Void, Void, Void> mCurrentPlayTask;

   // private Cursor mCursor;
    //private List<Song> mSongList;
    private CursorSongs mSongs;
    private int mPosition;
    private int mCount;
    private boolean mReadyToPlay = false; //Indicates if we have list loaded

    private boolean mRepeatPlaylist;

    private List<SongPlayListener> mOnPlayListeners;
    private List<SongResumeListener> mOnResumeListeners;

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

        mOnPlayListeners = new ArrayList<>();
        mOnResumeListeners = new ArrayList<>();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

        //Get last known repeat status from preferences
        refreshRepeat();
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
                NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_PLAYER_ID);
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

        mAudioManager.abandonAudioFocus(this);

        super.onDestroy();
    }

    public class MediaPlayerBinder extends Binder {
        MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange <= 0)
        {
            if (mPlaying == true)
            {
                pause();
            }

        }
        else
        {
            resume();
        }
    }

    public void play(int position)
    {
        Log.d(TAG,"play() - position " + position);

        //If something is wrong then do nothing
        if (mPosition == position || position < 0 || position >= mCount || mSongs == null)
            return;

        if (mCurrentPlayTask != null)
            mCurrentPlayTask.cancel(true);

        mPosition = position;
        //Song song = mSongList.get(mPosition);
        Toast.makeText(getApplicationContext(),mSongs.getData(mPosition),Toast.LENGTH_SHORT).show();

        mCurrentPlayTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try
                {
                    //Set up media player and start playing when ready
                    mPlayer.reset();
                    mPlayer.setDataSource(mSongs.getData(mPosition));
                    mPlayer.setOnCompletionListener(MediaPlayerService.this);

                    //If this task is cancelled, no need to do anything
                    if (isCancelled()) {
                        Log.d(TAG,"Current play task is cancelled");
                        return null;
                    }

                    mPlayer.prepare();
                    mPlayer.start();

                    //If this task is cancelled, no need to do anything
                    if (isCancelled()) {
                        Log.d(TAG,"Current play task is cancelled");
                        return null;
                    }


                    mPlaying = true;
                    mStopped = false;


                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                showNotification(false, true);
                //Notify NowPlayingActivity that we changed playing song
                notifyListenersPlay();
            }
        };
        mCurrentPlayTask.execute();
        /*try
        {
            //Set up media player and start playing when ready
            mPlayer.reset();
            mPlayer.setDataSource(mSongs.getData(mPosition));
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
        }*/

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
        if (mPlayer != null && mPosition != -1)
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
            else
            {
                stop();
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
        mSongs = null;
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

    //Function to get latest state of repeat option
    public void refreshRepeat()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRepeatPlaylist = preferences.getBoolean(getString(R.string.pref_key_repeat),true);
    }

    public boolean isCursorUsed(Cursor cursor)
    {
        if (cursor == null)
            return false;

        if (mSongs != null)
        {
            if (cursor == mSongs.getCursor())
                return true;
        }

        return false;
    }


    public void registerPlayListener(SongPlayListener listener)
    {
        Log.d(TAG,"registerListener - " + listener.toString());
        mOnPlayListeners.add(listener);
    }

    public void unregisterPlayListener(SongPlayListener listener)
    {
        Log.d(TAG,"unregisterListener - " + listener.toString());
        mOnPlayListeners.remove(listener);
    }

    public void registerResumeListener(SongResumeListener listener)
    {
        Log.d(TAG,"registerListener - " + listener.toString());
        mOnResumeListeners.add(listener);
    }

    public void unregisterResumeListener(SongResumeListener listener)
    {
        Log.d(TAG,"unregisterListener - " + listener.toString());
        mOnResumeListeners.remove(listener);
    }

    public void notifyListenersPlay()
    {
        try
        {
            for (SongPlayListener listener : mOnPlayListeners)
            {
                    listener.onPlay(mSongs, mPosition);
            }
        }
        catch (ConcurrentModificationException e)
        {
            e.printStackTrace();
        }
    }

    public void notifyListenersResume()
    {
        for (SongResumeListener listener : mOnResumeListeners)
        {
            listener.onSongResume();
        }
    }


    //Display notification player for this service
    public void showNotification(boolean playIcon, boolean ticker)
    {
        RemoteViews notificationView = new RemoteViews(getPackageName(),R.layout.notification_player);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        notificationView.setTextViewText(R.id.notification_title,mSongs.getTitle(mPosition));

        //Check if we have notifications icons, if not generate them right now
        /*if (mBitmapIcons == null)
        {
            generateBitmapIcons();
        }*/

        //Render font icons in scale with current device screen density
        /*notificationView.setImageViewBitmap(R.id.notification_close,        mBitmapIcons.get(getString(R.string.icon_close)));
        notificationView.setImageViewBitmap(R.id.notification_previous,     mBitmapIcons.get(getString(R.string.icon_previous)));
        notificationView.setImageViewBitmap(R.id.notification_play,         mBitmapIcons.get(getString(playIcon ? R.string.icon_play : R.string.icon_pause)));
        notificationView.setImageViewBitmap(R.id.notification_next,         mBitmapIcons.get(getString(R.string.icon_next)));*/

        notificationView.setImageViewResource(R.id.notification_play, playIcon ? R.drawable.ic_play_arrow_ltgray_36dp : R.drawable.ic_pause_ltgray_36dp);

        //Set-up notification
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(false)
                .setAutoCancel(false)
                .setContent(notificationView)
                .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,NowPlayingActivity.class),0));

        //If needed, set the ticker text with song title
        if (ticker)
            builder.setTicker(mSongs.getTitle(mPosition));

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
    /*public void generateBitmapIcons()
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
    }*/


    //Set the list and start playing
    public void playList(CursorSongs songs, int startPosition)
    {
        setCursorSongs(songs);
        play(startPosition);
    }


    //Setter for song list
    public void setCursorSongs(CursorSongs songs)
    {
        //This is starting position before we find out anything
        mPosition = -1;

        if (songs != null)
        {
            //Get song count and song list
            mCount = songs.getCount();
            mSongs = songs;
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
        Log.d(TAG,"setCurrentPlayInfoToListener()");
        //Check if we have something in this service
        if (mCount > 0 && mPosition >= 0 && mSongs != null)
        {
            notifyListenersPlay();
        }

    }

    public CursorSongs getSongs()
    {
        return mSongs;
    }

    public boolean isReadyToPlay() {
        return mReadyToPlay;
    }

    public MediaPlayer getMediaPlayer()
    {
        return mPlayer;
    }

    /*public Song getCurrentSong()
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

    }*/

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

    public interface SongPlayListener
    {
        void onPlay(CursorSongs songs, int position);

    }

    public interface SongResumeListener
    {
        void onSongResume();
    }


}
