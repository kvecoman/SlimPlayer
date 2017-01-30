package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


//TODO - setQueueTitle in media session to name of list (Trening, Pitbull, Rock etc...)
//TODO - what the fuck are you supposed to do with action play (and its onPlay() callback)
//TODO - use noisyAudioReciever and AudioManager like in example in Playback class play function
public class MediaPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

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

    //Indicated whether the service has foreground status or not (it might not be accuarate, we don't know if startForeground will actually set it)
    private boolean mForeground = false;

    private MediaPlayer mPlayer;

    private boolean mPlaying = false;
    private boolean mStopped = true;

    private AudioManager mAudioManager;

    private AsyncTask<Void, Void, Void> mCurrentPlayTask;


    private List<MediaSessionCompat.QueueItem> mQueue;
    private int mPosition = -1;
    private int mCount = 0;
    private int mState = PlaybackStateCompat.STATE_NONE;
    private boolean mReadyToPlay = false; //Indicates if we have list loaded

    //Source and parameter of currently used song list
    private String mQueueSource;
    private String mQueueParameter;

    //Whether we repeat playlist at end
    //private boolean mRepeatPlaylist;

    //List of all play and resume listeners
    /*private Set<SongPlayListener> mOnPlayListeners;
    private Set<SongResumeListener> mOnResumeListeners;*/

    //We use this to check whether the service will stop when all components are unbound
    private boolean mPendingStop = false;


    private Handler mHandler;
    private Runnable mSeekBarRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mState == PlaybackStateCompat.STATE_PLAYING || mState == PlaybackStateCompat.STATE_PAUSED)
            {
                //Here we just update the playback position of the existing state
                mStateBuilder = new PlaybackStateCompat.Builder( mMediaSession.getController().getPlaybackState() );
                mStateBuilder.setState( mState, mPlayer.getCurrentPosition(), 1.0f );
                mMediaSession.setPlaybackState( mStateBuilder.build() );

                mHandler.postDelayed( mSeekBarRunnable, 1000 );
            }
        }
    };


    //Used to detect if headphones get plugged out
    private BroadcastReceiver mHeadsetChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            MediaPlayerService playerService = MediaPlayerService.this;

            //If something is wrong just return
            if (!intent.hasExtra("state") || playerService == null)
                return;


            //If headset is plugged out, pause the playback
            if (intent.getIntExtra("state",0) == 0)
            {
                playerService.pause();
            }
        }
    };



    private static final String MEDIA_ROOT_ID = "root_id";

    private MediaSessionCompat mMediaSession;

    private PlaybackStateCompat.Builder mStateBuilder;

    private MusicProvider mMusicProvider;


    public MediaPlayerService() {
    }



    @Override
    public void onCreate()
    {
        Log.v(TAG,"onCreate()");
        super.onCreate();

        Intent intent = new Intent(getApplicationContext(), NowPlayingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        mMediaSession = new MediaSessionCompat(this,TAG,new ComponentName(this,HeadsetChangeReceiver.class),pendingIntent);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new MediaSessionCallback());

        mStateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);
        mMediaSession.setPlaybackState(mStateBuilder.build());


        setSessionToken(mMediaSession.getSessionToken());

        if (Build.VERSION.SDK_INT >= 21) //Lollipop 5.0
        {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setClass(getApplicationContext(),MediaPlayerService.class);
            PendingIntent mediaPendingIntent = PendingIntent.getService(this, 0, mediaButtonIntent, 0);
            mMediaSession.setMediaButtonReceiver(mediaPendingIntent);
        }


        mMusicProvider = MusicProvider.getInstance();
        mMusicProvider.registerDataListener(this);

        mQueue = new ArrayList<>();


        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        /*mOnPlayListeners = new HashSet<>();
        mOnResumeListeners = new HashSet<>();*/

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);

        //Get last known repeat status from preferences
        //refreshRepeat();

        //Register to detect headphones in/out
        registerReceiver(mHeadsetChangeReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        mHandler = new Handler();

    }



    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {

        //TODO - validation of client

        return new BrowserRoot(MEDIA_ROOT_ID,rootHints);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        onLoadChildren(parentId, result, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result,@Nullable Bundle options)
    {

        String parameter = null;
        List<MediaBrowserCompat.MediaItem> mediaItems;

        //Obtain parameter from bundle if it exist
        if (options != null && options.containsKey( Const.PARAMETER_KEY))
            parameter = options.getString( Const.PARAMETER_KEY);


        //Load media from music provider
        //TODO - this to another thread, you use detach function of result
        mediaItems = mMusicProvider.loadMedia(parentId,parameter);


        result.sendResult(mediaItems);
    }



    private final class MediaSessionCallback extends MediaSessionCompat.Callback
    {
        @Override
        public void onPlay() {
            super.onPlay();

            resume();
        }

        @Override
        public void onPause()
        {
            super.onPause();

            pause();
        }

        @Override
        public void onPlayFromMediaId( String mediaId, Bundle extras )
        {
            if (extras == null && mState == PlaybackStateCompat.STATE_NONE)
                return;

            //If we have something loaded try to find it using media id
            if (extras == null && mQueue != null && mCount > 0)
            {
                for ( MediaSessionCompat.QueueItem queueItem : mQueue)
                {
                    if (Utils.equalsIncludingNull(queueItem.getDescription().getMediaId(), mediaId ))
                    {
                        play(mQueue.indexOf( queueItem ));
                        return;
                    }
                }
            }

            String source = null;
            String parameter = null;
            int position;
            int i;

            source    = extras.getString( Const.SOURCE_KEY, null );
            parameter = extras.getString( Const.PARAMETER_KEY, null );
            position  = extras.getInt( Const.POSITION_KEY, -1 );


            //NOTE - we use skipToQueueItem() for this
            //If we have something loaded and position of it, try to play that
            /*if (source == null && position >= 0 && position <= mCount)
            {
                if (mQueue.get( position ).getDescription().getMediaId().equals( mediaId ))
                {
                    play(position);
                    return;
                }
            }*/

            //If none of the cases above worked then do full list loading
            setQueue( source, parameter );

            //Check if bundle provided correct play position
            if (!(position >= 0 && position < mQueue.size() && mQueue.get( position ).getDescription().getMediaId().equals( mediaId )))
                position = -1;

            //If position from bundle wasn't correct, try to find it here
            if (position == -1)
            {
                for (i = 0;i < mQueue.size(); i++)
                {
                    if (mQueue.get( i ).getDescription().getMediaId().equals( mediaId ))
                    {
                        position = i;
                        break;
                    }
                }
            }


            mMediaSession.setQueue( mQueue );


            if (position >= 0)
                play(position);

        }

        @Override
        public void onSkipToQueueItem( long id )
        {
            super.onSkipToQueueItem( id );

            int count;
            int position;

            if (mState == PlaybackStateCompat.STATE_NONE || mQueue == null || mQueue.isEmpty())
                return;

            //ID is also the index in queue
            position = (int)id;
            count = mQueue.size();

            if (position < 0 || position >= count)
                return;

            play( position );

        }

        @Override
        public void onSeekTo( long pos )
        {
            super.onSeekTo( pos );

            //Seek to position in ms if we have song loaded
            if (mState == PlaybackStateCompat.STATE_PLAYING || mState == PlaybackStateCompat.STATE_PAUSED)
            {
                mPlayer.seekTo( (int)pos );
            }
        }
    }



    //Returns whether the source is different than one before
    public boolean setQueue( @Nullable final String source, @Nullable final String parameter)
    {
        Log.v(TAG,"setQueue()");


        List<MediaBrowserCompat.MediaItem>  mediaItems;
        MediaSessionCompat.QueueItem        queueItem;
        boolean isSourceChanged;
        int     count;
        String oldSource;
        String oldParameter;


        isSourceChanged = true;
        oldSource = mQueueSource;
        oldParameter = mQueueParameter;

        //Since we are changing queue let's set state to none before we know anything
        stopAndClearList();


        if (source == null)
            return true;


        //If we are loading from file
        if (source == Const.FILE_URI_KEY)
        {
            MediaMetadataCompat metadata;
            MediaMetadataCompat.Builder metadataBuilder;
            MediaMetadataRetriever retriever;
            Uri fileUri;
            String fileUriString;
            MediaBrowserCompat.MediaItem mediaItem;

            metadataBuilder = new MediaMetadataCompat.Builder(  );
            retriever = new MediaMetadataRetriever();
            fileUriString = parameter;
            fileUri = Uri.parse( fileUriString );

            mediaItems = new ArrayList<>(1);

            if (fileUri == null)
                return true;

            retriever.setDataSource( fileUri.getPath() );

            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "0")
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_TITLE ))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ALBUM ))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ARTIST ))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION )))
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, fileUriString);


            metadata = metadataBuilder.build();

            //Get media item with metadata bundled in its media description object
            mediaItem = mMusicProvider.bundleMetadata( metadata );

            mediaItems.add( mediaItem );


        }
        else
        {
            mediaItems = mMusicProvider.loadMedia( source, parameter );
        }

        count = mediaItems.size();

        if (count <= 0)
            return true;

        mQueue = new ArrayList<>( count );


        //Convert all media items to queue items
        for ( MediaBrowserCompat.MediaItem item : mediaItems)
        {
            //Set queue item id to be same as position index
            queueItem = new MediaSessionCompat.QueueItem( item.getDescription(), count++ );
            mQueue.add( queueItem );
        }


        mCount = mQueue.size();
        mReadyToPlay = true;



        //State stopped means that nothing is playing but we have media loaded
        updateState( PlaybackStateCompat.STATE_STOPPED );
        mMediaSession.setQueue( mQueue );


        //If the source is different then update stats database
        if (!Utils.equalsIncludingNull( oldSource,source) || !Utils.equalsIncludingNull( oldParameter,parameter))
        {


            new AsyncTask<Void,Void,Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    StatsDbHelper statsDbHelper = StatsDbHelper.getInstance(MediaPlayerService.this);
                    statsDbHelper.updateStats(source,parameter);
                    return null;
                }
            }.execute();
        }
        else
        {
            //Note that the source is same as before
            isSourceChanged = false;
        }

        mQueueSource = source;
        mQueueParameter = parameter;



        //Save song list source in preferences so we remember this list for auto-start playback
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MediaPlayerService.this);
        preferences.edit().putString(SONGLIST_SOURCE_KEY, source)
                .putString(SONGLIST_PARAMETER_KEY, parameter).apply();


        return isSourceChanged;

    }


    //Sets the list and starts playing, source string is one of screen keys or something else (if list is from files or something)
    public void playListIfChanged(final String source, final String parameter, int startPosition)
    {

        //Set songs source and note if it is different than before
        boolean isSourceChanged = setQueue(source,parameter);

        //We call play only if the source have changed
        if (isSourceChanged)
            play(startPosition);

    }

    public void playList(final String source, final String parameter, final int startPosition)
    {
        //Set songs source and note if it is different than before
        boolean isSourceChanged = setQueue(source,parameter);

        //We always call play except when we are already playing same song
        if (!(!isSourceChanged && mPosition == startPosition))
            play(startPosition);
    }


    //Updates media session PlaybackState
    public void updateState(@PlaybackStateCompat.State int state)
    {
        mState = state;
        mStateBuilder = new PlaybackStateCompat.Builder(  );

        switch (mState)
        {
            case PlaybackStateCompat.STATE_NONE:
                mStateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID )
                        .setState       ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                mStateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                        | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState       ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                mStateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                        | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                        | PlaybackStateCompat.ACTION_PAUSE
                                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                        | PlaybackStateCompat.ACTION_SEEK_TO )
                        .setState       ( mState, mPlayer.getCurrentPosition(), 1.0f )
                        .setActiveQueueItemId( mPosition );
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mStateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                        | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                                        | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                        | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                        | PlaybackStateCompat.ACTION_PLAY
                                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                        | PlaybackStateCompat.ACTION_SEEK_TO )
                        .setState       ( mState, mPlayer.getCurrentPosition(), 1.0f )
                        .setActiveQueueItemId( mPosition );
                break;
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                mStateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                        | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState       ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                mStateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                        | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState       ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                mStateBuilder.setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                        | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState       ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;

        }

        mMediaSession.setPlaybackState( mStateBuilder.build() );

    }
















    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG,"onStartCommand()");

        //Get action from intent while checking for null
        String action = intent == null ? null : intent.getAction();

        if (action != null)
        {
            switch (action)
            {
                case NOTIFICATION_ACTION_CLOSE:
                    stop();
                    /*NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.cancel(NOTIFICATION_PLAYER_ID);*/
                    stopForeground(true);
                    mForeground = false;
                    stopSelf();
                    mPendingStop = true;
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
                /*case NOTIFICATION_ACTION_SWIPE:
                    stopAndClearList();
                    stopForeground(true);
                    mForeground = false;
                    break;*/
            }
        }

        return START_NOT_STICKY;
    }

    /*@Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }*/



    /*@Override
    public boolean onUnbind(Intent intent)
    {
        return super.onUnbind(intent);
    }*/

    @Override
    public void onDestroy() {
        Log.v(TAG,"onDestroy()");

        mAudioManager.abandonAudioFocus(this);
        unregisterReceiver(mHeadsetChangeReceiver);
        mMusicProvider.unregisterDataListener();
        mMediaSession.release();

        if (mPlayer != null)
        {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        //TODO - all close instance calls except this to application onDestroy
        StatsDbHelper.closeInstance();



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
        String source = prefs.getString( SONGLIST_SOURCE_KEY, null );
        String parameter = prefs.getString( SONGLIST_PARAMETER_KEY, null );
        int position = prefs.getInt( SONGLIST_POSITION_KEY, 0 );

        if (source != null)
        {
            List<MediaBrowserCompat.MediaItem> mediaItems = MusicProvider.getInstance().loadMedia(source, parameter);
            setQueue( source, parameter );
            return position;
        }
        return -1;
    }

    //Try to get last playback state (if there is none, nothing will happen)
    public void playLastStateAsync()
    {
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
        if (mState == PlaybackStateCompat.STATE_NONE || position < 0 || position >= mCount || mQueue == null)
            return;

        updateState( PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM );

        //If we have active task for playing, cancel it
        if (mCurrentPlayTask != null)
            mCurrentPlayTask.cancel(true);

        mPosition = position;

        //If we are about to stop, prevent that
        if (mPendingStop)
        {
            startService(new Intent(this,MediaPlayerService.class));
            mPendingStop = false;
        }

        //Save current position so we can get it later on
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putInt(SONGLIST_POSITION_KEY,mPosition).apply();

        final Uri mediaFileUri = mQueue.get(mPosition).getDescription().getMediaUri();

        Utils.toastShort(mediaFileUri.toString());

        mCurrentPlayTask = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                try
                {
                    //Set up media player and start playing when ready
                    mPlayer.reset();
                    mPlayer.setDataSource(MediaPlayerService.this, mediaFileUri );
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
            protected void onPostExecute(Void aVoid)
            {


                showNotification(false, true);

                //Update playback state
                mState = PlaybackStateCompat.STATE_PLAYING;
                mStateBuilder = new PlaybackStateCompat.Builder()
                        .setActions( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                        .setState( mState, mPlayer.getCurrentPosition(), 1.0f )
                        .setActiveQueueItemId( mPosition );
                mMediaSession.setPlaybackState( mStateBuilder.build() );

                mHandler.post( mSeekBarRunnable );


                //Notify NowPlayingActivity that we changed playing song
                //notifyListenersPlay();

                //Start new task to update last play position for this source
                new AsyncTask<Void,Void,Void>()
                {
                    @Override
                    protected Void doInBackground(Void... params)
                    {
                        StatsDbHelper statsDbHelper = StatsDbHelper.getInstance(MediaPlayerService.this);
                        statsDbHelper.updateLastPosition( mQueueSource, mQueueParameter,mPosition);
                        return null;
                    }
                }.execute();

            }
        };

        mCurrentPlayTask.execute();


    }

    //This is called when song is finished playing
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(TAG,"onCompletion()");

        //Continue to next song only if we are set to be playing
        if (mPlaying || mState == PlaybackStateCompat.STATE_PLAYING)
        {
            MediaPlayerService.this.playNext();
        }
    }

    public void pause()
    {
        if (mPlayer != null && (mPlaying || mState == PlaybackStateCompat.STATE_PLAYING))
        {
            mPlaying = false;
            mPlayer.pause();
            updateState( PlaybackStateCompat.STATE_PAUSED );
            showNotification(true,false);
        }
    }

    public void resume()
    {
        if (mPlayer != null && (mPosition != -1 || mState == PlaybackStateCompat.STATE_PAUSED))
        {
            mPlaying = true;
            mPlayer.start();
            updateState( PlaybackStateCompat.STATE_PLAYING );
            showNotification(false, false);
        }
    }

    //Function that tries to resume playback if the song is loaded, or load and play the song if not
    public void resumeOrPlay(int position)
    {
        if (position == mPosition && (!mStopped || mState == PlaybackStateCompat.STATE_PAUSED))
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
        if (mPosition == -1 || mStopped || !mReadyToPlay || mState == PlaybackStateCompat.STATE_NONE || mState == PlaybackStateCompat.STATE_STOPPED)
            return;

        if (mPosition == mCount - 1)
        {
            //If we are at the end of playlist
            if (shouldRepeatPlaylist())
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
        if (mPosition == -1 || mPosition == 0 || mStopped || !mReadyToPlay || mState == PlaybackStateCompat.STATE_NONE || mState == PlaybackStateCompat.STATE_STOPPED)
            return;


        //Play previous song
        play(mPosition - 1);

    }

    public void stop()
    {

        if (mPlayer != null)
        {
            mState = PlaybackStateCompat.STATE_STOPPED;
            updateState( mState );
            mPlaying = false;
            mStopped = true;
            mPosition = -1;

            if (mPlayer.isPlaying())
                mPlayer.stop();

            mPlayer.reset();
        }
    }

    //Stop playing and clear list
    public void stopAndClearList()
    {
        stop();
        mCount = 0;
        mQueue = null;
        mQueueSource = null;
        mQueueParameter = null;
        mReadyToPlay = false;
        mState = PlaybackStateCompat.STATE_NONE;
        updateState( mState );

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
    /*public void refreshRepeat()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRepeatPlaylist = preferences.getBoolean(getString(R.string.pref_key_repeat),true);
    }*/

    public boolean shouldRepeatPlaylist()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(getString(R.string.pref_key_repeat),true);
    }







    //Display notification player for this service
    public void showNotification(boolean playIcon, boolean ticker)
    {
        //TODO - fix, uncomment
        /*RemoteViews notificationView = new RemoteViews(getPackageName(),R.layout.notification_player);
        RemoteViews bigNotificationView = new RemoteViews(getPackageName(), R.layout.notification_player_big);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        MediaSessionCompat.QueueItem currentQueueItem = mQueue.get(mPosition);

        notificationView.setTextViewText(R.id.notification_title,currentQueueItem.getDescription().getTitle());
        bigNotificationView.setTextViewText(R.id.notification_title,currentQueueItem.getDescription().getTitle());

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
            builder.setTicker(currentQueueItem.getDescription().getTitle());

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

        //If the service is already foreground the just update the notification
        if (mForeground)
        {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_PLAYER_ID, notification);
        }
        else
        {
            startForeground(NOTIFICATION_PLAYER_ID, notification);
            mForeground = true;
        }*/


    }




    /*@Deprecated
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
    }*/


    /*public void registerPlayListener(SongPlayListener listener)
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
                listener.onPlay(mMediaQueue, mPosition);
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
    }*/





    //Set the list and start playing
    /*public void playList(List<MediaBrowserCompat.MediaItem> mediaQueue, int startPosition)
    {
        setQueue(mediaQueue,null,null);
        play(startPosition);
    }*/







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

    /*public List<MediaBrowserCompat.MediaItem> getSongs()
    {
        return mMediaQueue;
    }*/

    //Indicates if list is loaded that can be played
    /*public boolean isReadyToPlay() {
        return mReadyToPlay;
    }

    //Indicates only if media player is stopped
    public boolean isStopped()
    {
        return mStopped;
    }

    public MediaPlayer getMediaPlayer()
    {
        return mPlayer;
    }*/

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

    /*public int getPosition() {
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
        return mQueueSource;
    }

    public String getSongsParameter() {
        return mQueueParameter;
    }

    public interface SongPlayListener
    {
        void onPlay(List<MediaBrowserCompat.MediaItem> mediaQueue, int position);
    }

    public interface SongResumeListener
    {
        void onSongResume();
    }*/


}
