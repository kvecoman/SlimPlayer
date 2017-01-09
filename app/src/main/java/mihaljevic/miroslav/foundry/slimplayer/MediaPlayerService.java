package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
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


public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    protected final String TAG = getClass().getSimpleName();

    //Preference keys to restore last played list
    public static final String SONGLIST_SOURCE_KEY = "SONGLIST_SOURCE";
    public static final String SONGLIST_PARAMETER_KEY = "SONGLIST_PARAMETER";
    public static final String SONGLIST_POSITION_KEY = "SONGLIST_POSITION";

    //Notification ID
    public static final int NOTIFICATION_PLAYER_ID = 1;

    public static final String  NOTIFICATION_ACTION_CLOSE = "mihaljevic.miroslav.foundry.slimplayer.action.close";
    public static final String  NOTIFICATION_ACTION_PREVIOUS = "mihaljevic.miroslav.foundry.slimplayer.action.previous";
    public static final String  NOTIFICATION_ACTION_PLAY_PAUSE = "mihaljevic.miroslav.foundry.slimplayer.action.play_pause";
    public static final String  NOTIFICATION_ACTION_NEXT = "mihaljevic.miroslav.foundry.slimplayer.action.next";
    public static final String  NOTIFICATION_ACTION_SWIPE = "mihaljevic.miroslav.foundry.slimplayer.action.swipe";


    private MediaPlayerBinder mBinder = new MediaPlayerBinder();

    public MediaPlayer mPlayer;

    private boolean mPlaying = false;
    private boolean mStopped = true;

    private AudioManager mAudioManager;

    private AsyncTask<Void, Void, Void> mCurrentPlayTask;

    private Songs mSongs;
    private int mPosition;
    private int mCount;
    private boolean mReadyToPlay = false; //Indicates if we have list loaded

    //Source and parameter of currently used song list
    private String mSongsSource;
    private String mSongsParameter;

    private boolean mRepeatPlaylist;

    private List<SongPlayListener> mOnPlayListeners;
    private List<SongResumeListener> mOnResumeListeners;



    //Used to detect if headphones are plugged in
    private BroadcastReceiver mHeadsetChangeReceiver = new HeadsetChangeReceiver();

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

        //Register to detect headphones in/out
        registerReceiver(mHeadsetChangeReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        //Try to get last playback state (if there is none, nothing will happen)
        new AsyncTask<Void,Void,Integer>(){
            @Override
            protected Integer doInBackground(Void... params)
            {
                return recreateLastPlaybackState();
            }

            @Override
            protected void onPostExecute(Integer result) {
                //If we could recreate state then play the position
                if (result != -1)
                    play(result);
            }
        }.execute();

    }



    //NOTE - THIS IS CALLED WHEN SERVICE IS CALLED WHILE IT IS ALREADY RUNNING
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Get action from intent while checking for null
        String action = intent == null ? null : intent.getAction();

        if (action != null)
        {
            switch (action)
            {
                case NOTIFICATION_ACTION_CLOSE:
                    stopAndClearList();
                    NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_PLAYER_ID);
                    break;
                case NOTIFICATION_ACTION_PREVIOUS:
                    playPrevious();
                    break;
                case NOTIFICATION_ACTION_PLAY_PAUSE:
                    if (mPlaying)
                        pause();
                    else
                        resume();
                    break;
                case NOTIFICATION_ACTION_NEXT:
                    playNext();
                    break;
                case NOTIFICATION_ACTION_SWIPE:
                    stopAndClearList();
                    break;
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
        unregisterReceiver(mHeadsetChangeReceiver);
        super.onDestroy();
    }

    public class MediaPlayerBinder extends Binder {
        MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }

    //Returns position of last played song
    private int recreateLastPlaybackState()
    {
        //Recreate last playback state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String source = prefs.getString(SONGLIST_SOURCE_KEY,null);
        String parameter = prefs.getString(SONGLIST_PARAMETER_KEY,null);
        int position = prefs.getInt(SONGLIST_POSITION_KEY,0);

        if (source != null)
        {
            Bundle bundle = ScreenBundles.getBundleForSubScreen(this,source,parameter);
            CursorSongs songs = new CursorSongs(Utils.querySongListCursor(this,bundle));
            setSongs(songs,source,parameter);
            return position;
        }
        return -1;
    }

    @Override
    public void onAudioFocusChange(int focusChange)
    {
        if (focusChange <= 0 && mPlaying)
        {
            pause();
        }
        else
        {
            resume();
        }
    }

    public void play(int position)
    {
        Log.v(TAG,"play() position: " + position);

        //If something is wrong then do nothing
        if (position < 0 || position >= mCount || mSongs == null)
            return;

        if (mCurrentPlayTask != null)
            mCurrentPlayTask.cancel(true);

        mPosition = position;

        //Save current position so we can get it later on
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putInt(SONGLIST_POSITION_KEY,mPosition).apply();

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

                    mPlayer.prepare();
                    mPlayer.start();

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

                //Start new task to update last play position for this source
                if (mSongsSource != null)
                {
                    new AsyncTask<Void,Void,Void>(){
                        @Override
                        protected Void doInBackground(Void... params) {
                            StatsDbHelper statsDbHelper = new StatsDbHelper(MediaPlayerService.this);
                            statsDbHelper.updateLastPosition(mSongsSource,mSongsParameter,mPosition);
                            return null;
                        }
                    }.execute();
                }
            }
        };
        mCurrentPlayTask.execute();


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
        if (mPlayer != null && mPlaying)
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
        if (position == mPosition && !mStopped)
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
        mSongsSource = null;
        mSongsParameter = null;
        mReadyToPlay = false;
        stop();

    }

    //Start ending this service
    /*public void endService()
    {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        mPlaying = false;
        stopForeground(true);
        stopSelf();
    }*/

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

        if (mSongs != null && mSongs instanceof CursorSongs)
        {
            if (cursor == ((CursorSongs)mSongs).getCursor())
                return true;
        }

        return false;
    }


    public void registerPlayListener(SongPlayListener listener)
    {
        Log.v(TAG,"registerPlayListener - " + listener.toString());
        mOnPlayListeners.add(listener);
    }

    public void unregisterPlayListener(SongPlayListener listener)
    {
        Log.v(TAG,"unregisterPlayListener - " + listener.toString());
        mOnPlayListeners.remove(listener);
    }

    public void registerResumeListener(SongResumeListener listener)
    {
        Log.v(TAG,"registerResumeListener - " + listener.toString());
        mOnResumeListeners.add(listener);
    }

    public void unregisterResumeListener(SongResumeListener listener)
    {
        Log.v(TAG,"unregisterResumeListener - " + listener.toString());
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
        RemoteViews bigNotificationView = new RemoteViews(getPackageName(), R.layout.notification_player_big);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        notificationView.setTextViewText(R.id.notification_title,mSongs.getTitle(mPosition));
        bigNotificationView.setTextViewText(R.id.notification_title,mSongs.getTitle(mPosition));

        notificationView.setImageViewResource(R.id.notification_play, playIcon ? R.drawable.ic_play_arrow_ltgray_36dp : R.drawable.ic_pause_ltgray_36dp);
        bigNotificationView.setImageViewResource(R.id.notification_play, playIcon ? R.drawable.ic_play_arrow_ltgray_54dp : R.drawable.ic_pause_ltgray_54dp);

        //Set-up notification
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(false)
                .setAutoCancel(false)
                .setContent(notificationView)
                .setCustomBigContentView(bigNotificationView)
                .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,NowPlayingActivity.class),0));

        //Check if we have album art in mp3 and try to add it to player
        Bitmap artBitmap = mSongs.getArt(mPosition);
        if (artBitmap != null)
        {
            notificationView.setImageViewBitmap(R.id.notification_icon,artBitmap);
            bigNotificationView.setImageViewBitmap(R.id.notification_icon,artBitmap);
        }
        else
        {
            //Load default image
            notificationView.setImageViewResource(R.id.notification_icon,R.mipmap.ic_launcher);
            bigNotificationView.setImageViewResource(R.id.notification_icon,R.mipmap.ic_launcher);
        }


        //If needed, set the ticker text with song title
        if (ticker)
            builder.setTicker(mSongs.getTitle(mPosition));

        //Set-up control actions
        Intent intent;
        PendingIntent pendingIntent;

        //Show play screen/main action
        //We build intent with MainActivity as parent activity in stack
        intent = new Intent(this, NowPlayingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(this,0,intent,0);
        builder.setContentIntent(pendingIntent);

        //Close action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_CLOSE);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_close, pendingIntent);
        bigNotificationView.setOnClickPendingIntent(R.id.notification_close, pendingIntent);

        //Previous song action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_PREVIOUS);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_previous, pendingIntent);
        bigNotificationView.setOnClickPendingIntent(R.id.notification_previous, pendingIntent);

        //Pause song action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_PLAY_PAUSE);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_play, pendingIntent);
        bigNotificationView.setOnClickPendingIntent(R.id.notification_play, pendingIntent);

        //Next song action
        intent = new Intent(this,this.getClass());
        intent.setAction(NOTIFICATION_ACTION_NEXT);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        notificationView.setOnClickPendingIntent(R.id.notification_next, pendingIntent);
        bigNotificationView.setOnClickPendingIntent(R.id.notification_next, pendingIntent);

        //Swipe notification intent
        intent = new Intent(this, this.getClass());
        intent.setAction(NOTIFICATION_ACTION_SWIPE);
        pendingIntent = PendingIntent.getService(this,0,intent,0);
        builder.setDeleteIntent(pendingIntent);


        //Build and show notification
        Notification notification = builder.build();


        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_PLAYER_ID, notification);
    }




    //Sets the list and starts playing, source string is one of screen keys or something else (if list is from files or something)
    public void playList(Songs songs, int startPosition,final String source,final String parameter)
    {

        //Set songs source and note if it is different than before
        boolean isSourceChanged = setSongs(songs,source,parameter);

        //We call play only if the source have changed
        if (isSourceChanged)
            play(startPosition);

    }



    //Set the list and start playing
    public void playList(Songs songs, int startPosition)
    {
        setSongs(songs,null,null);
        play(startPosition);
    }

    //Returns whether the source is different than one before
    public boolean setSongs(Songs songs, @Nullable final String source, @Nullable final String parameter)
    {
        Log.v(TAG,"setSongs()");

        boolean isSourceChanged = true;

        if (songs == null) {
            //We have nothing, respond appropriately
            stopAndClearList();
            return isSourceChanged;
        }

        if (source == null)
        {
            mSongsSource = null;
            mSongsParameter = null;
        }
        else
        {
            //If we have source

            //If the source is different then update stats database
            if (!Utils.equalsIncludingNull(mSongsSource,source) || !Utils.equalsIncludingNull(mSongsParameter,parameter))
            {


                new AsyncTask<Void,Void,Void>(){
                    @Override
                    protected Void doInBackground(Void... params) {
                        StatsDbHelper statsDbHelper = new StatsDbHelper(MediaPlayerService.this);
                        statsDbHelper.updateStats(source,parameter);
                        return null;
                    }
                }.execute();

                //This is starting position before we find out anything (this is only if source or parameter have changed)
                mPosition = -1;
            }
            else
            {
                //Note that the source is same as before
                isSourceChanged = false;
            }

            mSongsSource = source;
            mSongsParameter = parameter;

            //Save song list source in preferences so we remember this list for auto-start playback
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MediaPlayerService.this);
            preferences.edit().putString(SONGLIST_SOURCE_KEY, source)
                    .putString(SONGLIST_PARAMETER_KEY, parameter).apply();
        }


        //Get song count and song list
        mCount = songs.getCount();
        mSongs = songs;
        mReadyToPlay = true;

        return isSourceChanged;

    }





    //Used to send info when NowPlayingActivity is starting to get up to date with player service
    /*public void setCurrentPlayInfoToListener()
    {
        Log.d(TAG,"setCurrentPlayInfoToListener()");
        //Check if we have something in this service
        if (mCount > 0 && mPosition >= 0 && mSongs != null)
        {
            notifyListenersPlay();
        }

    }*/

    public Songs getSongs()
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

    public String getSongsSource() {
        return mSongsSource;
    }

    public String getSongsParameter() {
        return mSongsParameter;
    }

    public interface SongPlayListener
    {
        void onPlay(Songs songs, int position);
    }

    public interface SongResumeListener
    {
        void onSongResume();
    }


}
