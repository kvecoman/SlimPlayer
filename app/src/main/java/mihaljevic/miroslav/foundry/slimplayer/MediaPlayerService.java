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
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class MediaPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    protected final String TAG = getClass().getSimpleName();

    public static final ComponentName COMPONENT_NAME = new ComponentName( SlimPlayerApplication.getInstance(), MediaPlayerService.class );

    //Preference keys to restore last played list
    public static final String LAST_SOURCE_KEY      = "mihaljevic.miroslav.foundry.slimplayer.last_source";
    public static final String LAST_PARAMETER_KEY   = "mihaljevic.miroslav.foundry.slimplayer.last_parameter";
    public static final String LAST_POSITION_KEY    = "mihaljevic.miroslav.foundry.slimplayer.last_position";
    public static final String LAST_TITLE_KEY       = "mihaljevic.miroslav.foundry.slimplayer.last_title";
    public static final String LAST_STATE_PLAYED    = "mihaljevic.miroslav.foundry.slimplayer.last_state_played";

    //Notification ID
    public static final int NOTIFICATION_PLAYER_ID = 111;

    public static final String  NOTIFICATION_ACTION_CLOSE       = "mihaljevic.miroslav.foundry.slimplayer.action.close";
    public static final String  NOTIFICATION_ACTION_PREVIOUS    = "mihaljevic.miroslav.foundry.slimplayer.action.previous";
    public static final String  NOTIFICATION_ACTION_PLAY_PAUSE  = "mihaljevic.miroslav.foundry.slimplayer.action.play_pause";
    public static final String  NOTIFICATION_ACTION_NEXT        = "mihaljevic.miroslav.foundry.slimplayer.action.next";

    //Constant that allows media subscribers to have access to media content
    private static final String MEDIA_ROOT_ID = "slim_player_root";


    //Machinery that actually plays our music
    private MediaPlayer mPlayer;

    //AudioManager notifies us whenever phone is receiving call or when headset is plugged out
    private AudioManager mAudioManager;

    //Whether we have audio focus, if we don't have it, then we should'n play anything
    private boolean mAudioFocus = false;

    //List of songs as queue items that hold info necessary to start playing them
    private List<MediaSessionCompat.QueueItem> mQueue;

    //Current playing position in queue
    private int mPosition = -1;

    //Number of songs in queue
    private int mCount = 0;

    //Current state of our queue/media session
    private int mState = PlaybackStateCompat.STATE_NONE;

    //Source determines from what kind of list are we loading/playing songs (like from genres list, or albums list etc.)
    private String mQueueSource;

    //Parameter (usually ID of some kind) determines (in conjuction with source) from which EXACTLY list are we loading/playing songs (genre with ID of 5, or album with id of 8)
    private String mQueueParameter;

    //Media session object, used for communication with system and subscribing apps about media
    private MediaSessionCompat mMediaSession;

    //Helper object that performs loading metadata info about our songs/media items and caching them
    private MusicProvider mMusicProvider;

    //Whitelist validator that decides whether we allow app to see and browse media content or not
    private PackageValidator mPackageValidator;

    private ExecutorService mExecutorService;

    private StatsDbHelper mStatsDbHelper;

    //Used to hold type of task of which can be only one in queue
    private Future<?> mCancelableTask;




    //Used to detect if headphones get plugged out
    private BroadcastReceiver mHeadsetChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {

            //If something is wrong just return
            if (!intent.hasExtra("state") )
                return;


            //If headset is plugged out, pause the playback
            if (intent.getIntExtra("state",0) == 0)
            {
                if (mState == PlaybackStateCompat.STATE_PLAYING)
                {
                    pauseRunnable();
                }
            }
        }
    };


    private BroadcastReceiver mNoisyReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            //If something is wrong just return
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals( intent.getAction() ))
            {
                if (mState == PlaybackStateCompat.STATE_PLAYING)
                {
                    pauseRunnable();
                }
            }

        }
    };



    public MediaPlayerService() {
    }



    @Override
    public void onCreate()
    {
        Log.v(TAG,"onCreate()");
        super.onCreate();

        Intent intent;
        PendingIntent pendingIntent;


        mMediaSession = new MediaSessionCompat(this,TAG);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback( new MediaSessionCallback() );
        mMediaSession.setActive( true );

        setSessionToken(mMediaSession.getSessionToken());

        updateState( PlaybackStateCompat.STATE_NONE );



        //If we are on lollipop or latter then we set media button receiver using this method
        if ( Build.VERSION.SDK_INT >= 21 ) //Lollipop 5.0
        {

            intent = new Intent( Intent.ACTION_MEDIA_BUTTON );
            intent.setClass( getApplicationContext(), MediaPlayerService.class );

            pendingIntent = PendingIntent.getService( this, 0, intent, 0 );

            mMediaSession.setMediaButtonReceiver( pendingIntent );
        }


        mMusicProvider = MusicProvider.getInstance();
        mMusicProvider.registerDataListener(this);

        mQueue = new ArrayList<>();

        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mPackageValidator = PackageValidator.getInstance();

        mExecutorService = Executors.newSingleThreadExecutor();

        mStatsDbHelper = StatsDbHelper.getInstance();

        //Register to detect headphones in/out
        registerReceiver( mHeadsetChangeReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG ) );
        registerReceiver( mNoisyReceiver, new IntentFilter( AudioManager.ACTION_AUDIO_BECOMING_NOISY ) );


    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.v(TAG,"onStartCommand()");

        //Handle action from media button
        MediaButtonReceiver.handleIntent( mMediaSession, intent );

        //Get action from intent while checking for null
        String action = intent == null ? null : intent.getAction();

        if (action != null)
        {
            switch (action)
            {
                case NOTIFICATION_ACTION_CLOSE:
                    stopRunnable();
                    break;
                case NOTIFICATION_ACTION_PREVIOUS:
                    playPreviousRunnable();
                    break;
                case NOTIFICATION_ACTION_PLAY_PAUSE:
                    if (mState == PlaybackStateCompat.STATE_PLAYING)
                        pauseRunnable();
                    else
                        resumeRunnable();
                    break;
                case NOTIFICATION_ACTION_NEXT:
                    playNextRunnable();
                    break;
            }
        }

        return START_NOT_STICKY;
    }



    @Override
    public void onDestroy()
    {
        Log.v(TAG,"onDestroy()");

        giveUpAudioFocus();
        unregisterReceiver(mHeadsetChangeReceiver);
        unregisterReceiver( mNoisyReceiver );
        mMusicProvider.unregisterDataListener();
        mMediaSession.release();
        mExecutorService.shutdown();

        if (mPlayer != null)
        {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }


        StatsDbHelper.closeInstance();



        super.onDestroy();
    }



    @Nullable
    @Override
    public BrowserRoot onGetRoot( @NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints )
    {
        //This method doesn't have runnable version because it is fast, and also complicated to make it to runnable, so no need

        if ( mPackageValidator.validate( clientPackageName, clientUid ) )
            return new BrowserRoot( MEDIA_ROOT_ID, rootHints );

        return null;
    }


    @Override
    public void onLoadChildren( @NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result )
    {
        onLoadChildren( parentId, result, null );
    }

    @Override
    public void onLoadChildren( @NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result, @Nullable Bundle options )
    {
        loadChildrenAsync( parentId, result, options );
    }




    private final class MediaSessionCallback extends MediaSessionCompat.Callback
    {
        @Override
        public void onPlay()
        {
            super.onPlay();

            resumeRunnable();
        }

        @Override
        public void onPause()
        {
            super.onPause();

            pauseRunnable();
        }

        @Override
        public void onSkipToNext()
        {
            super.onSkipToNext();

            playNextRunnable();
        }

        @Override
        public void onSkipToPrevious()
        {
            super.onSkipToPrevious();

            playPreviousRunnable();
        }

        @Override
        public void onPlayFromMediaId(final String mediaId,final Bundle extras )
        {
            playFromMediaIDRunnable( mediaId, extras );
        }


        @Override
        public void onSkipToQueueItem( long id )
        {
            super.onSkipToQueueItem( id );

            skipToQueueItemRunnable( id );

        }

        @Override
        public void onSeekTo( long pos )
        {
            super.onSeekTo( pos );

            seekToRunnable( (int) pos );
        }

        @Override
        public boolean onMediaButtonEvent( Intent mediaButtonEvent )
        {
            return super.onMediaButtonEvent( mediaButtonEvent );


        }

        @Override
        public void onFastForward()
        {
            super.onFastForward();


            fastForwardRunnable();

        }

        @Override
        public void onRewind()
        {
            super.onRewind();

            rewindRunnable();
        }
    }






    //Returns whether the source is different than one before
    public synchronized boolean setQueue( @Nullable final String source, @Nullable final String parameter, @Nullable final String queueTitle )
    {
        Log.v(TAG,"setQueue()");

        SharedPreferences                   preferences;
        List<MediaBrowserCompat.MediaItem>  mediaItems;
        MediaSessionCompat.QueueItem        queueItem;
        MediaBrowserCompat.MediaItem        mediaItem;
        boolean                             isSourceChanged;
        String                              oldSource;
        String                              oldParameter;
        Bundle                              sessionExtras;
        long                                id;

        //We use old* variables to determine if change is actually made to queue
        oldSource       = mQueueSource;
        oldParameter    = mQueueParameter;
        sessionExtras   = new Bundle();
        preferences     = PreferenceManager.getDefaultSharedPreferences( MediaPlayerService.this );

        //Since we are changing queue let's set state to none before we know anything
        stopAndClearList();


        if ( source == null )
            return true;


        //If we are loading from file
        if ( source.equals( Const.FILE_URI_KEY ) )
        {

            mediaItems  = new ArrayList<>(1);
            mediaItem   = mMusicProvider.mediaFromFile( parameter );

            if ( mediaItem == null )
                return true;

            mediaItems.add( mediaItem );

        }
        else
        {
            mediaItems = mMusicProvider.loadMedia( source, parameter );
        }


        if ( mediaItems == null || mediaItems.size() <= 0 )
            return true;


        mQueue = new ArrayList<>( mediaItems.size() );

        id = 0;

        //Convert all media items to queue items
        for ( MediaBrowserCompat.MediaItem item : mediaItems)
        {
            //Set queue item id to be same as position index
            queueItem = new MediaSessionCompat.QueueItem( item.getDescription(), id++ );
            mQueue.add( queueItem );
        }


        mCount = mQueue.size();


        sessionExtras.putString( Const.SOURCE_KEY,      source );
        sessionExtras.putString( Const.PARAMETER_KEY,   parameter );

        //State stopped means that nothing is playing but we have media loaded
        updateState( PlaybackStateCompat.STATE_STOPPED );


        mMediaSession.setExtras ( sessionExtras );
        mMediaSession.setQueue  ( mQueue );

        if (queueTitle != null)
            mMediaSession.setQueueTitle( queueTitle );

        isSourceChanged = Utils.isSourceDifferent( source, oldSource, parameter, oldParameter );


        //If the source is different and not file then update stats database
        if ( !source.equals( Const.FILE_URI_KEY ) &&  isSourceChanged )
        {
            mStatsDbHelper.updateStatsAsync( source, parameter, queueTitle );
        }

        mQueueSource    = source;
        mQueueParameter = parameter;


        //Save song list source in preferences so we remember this list for auto-start playback
        preferences .edit()
                    .putString( LAST_SOURCE_KEY, source)
                    .putString( LAST_PARAMETER_KEY, parameter)
                    .putString( LAST_TITLE_KEY, queueTitle )
                    .apply();


        return isSourceChanged;

    }





    //Updates media session PlaybackState
    public synchronized void updateState(@PlaybackStateCompat.State int state)
    {
        PlaybackStateCompat.Builder stateBuilder;

        mState          = state;
        stateBuilder    = new PlaybackStateCompat.Builder(  );


        switch (mState)
        {
            case PlaybackStateCompat.STATE_NONE:
                stateBuilder.setActions         ( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID )
                        .setState               ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId   ( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                stateBuilder.setActions         ( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                                | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState               ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId   ( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                stateBuilder.setActions         ( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                                | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                                | PlaybackStateCompat.ACTION_FAST_FORWARD
                                                | PlaybackStateCompat.ACTION_REWIND
                                                | PlaybackStateCompat.ACTION_PAUSE
                                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                                | PlaybackStateCompat.ACTION_SEEK_TO )
                        .setState               ( mState, mPlayer.getCurrentPosition(), 1.0f )
                        .setActiveQueueItemId   ( mPosition );
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                stateBuilder.setActions         ( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                                | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
                                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                                | PlaybackStateCompat.ACTION_FAST_FORWARD
                                                | PlaybackStateCompat.ACTION_REWIND
                                                | PlaybackStateCompat.ACTION_PLAY
                                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                                | PlaybackStateCompat.ACTION_SEEK_TO )
                        .setState               ( mState, mPlayer.getCurrentPosition(), 1.0f )
                        .setActiveQueueItemId   ( mPosition );
                break;
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                stateBuilder.setActions         ( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                                | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState               ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId   ( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                stateBuilder.setActions         ( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                                | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState               ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId   ( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                stateBuilder.setActions         ( PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                                                | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
                        .setState               ( mState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f )
                        .setActiveQueueItemId   ( MediaSessionCompat.QueueItem.UNKNOWN_ID );
                break;

        }

        mMediaSession.setPlaybackState( stateBuilder.build() );

    }

    private void cancelCurrentTask()
    {
        if ( mCancelableTask == null )
            return;

        mCancelableTask.cancel( false );

        mCancelableTask = null;
    }


    /*private void loadChildrenRunnable( final @NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result, final @Nullable Bundle options )
    {
        result.detach();

        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                loadChildren( parentId, result, options );
            }
        } );
    }*/

    private void loadChildrenAsync ( final @NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result, final @Nullable Bundle options )
    {
        Log.v(TAG, "loadChildrenAsync()");

        new AsyncTask<Void, Void, Void>()
        {

            @Override
            protected void onPreExecute()
            {
                result.detach();
            }

            @Override
            protected Void doInBackground( Void... params )
            {
                loadChildren( parentId, result, options );

                return null;
            }
        }.execute();
    }

    private void loadChildren( @NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result, @Nullable Bundle options )
    {
        final String                        source;
        final String                        parameter;
        String[]                            split;
        List<MediaBrowserCompat.MediaItem>  mediaItems;


        split = Utils.splitParentString( parentId );

        source      = split[ 0 ];
        parameter   = split[ 1 ];

        if (source == null)
            return;

        //Load media from music provider
        mediaItems = mMusicProvider.loadMedia( source, parameter );

        result.sendResult( mediaItems );
    }

    private void seekToRunnable( final int pos )
    {
        cancelCurrentTask();

        mCancelableTask = mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                seekTo( pos );
            }
        } );
    }

   /* private void seekToAsync( final long pos )
    {
        Log.v(TAG, "seekToAsync()");

        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {
                seekTo( pos );

                return null;
            }
        }.execute(  );
    }*/

    private synchronized void seekTo ( int pos )
    {
        //Seek to position in ms if we have song loaded and update state with new position
        if ( !(mState == PlaybackStateCompat.STATE_PLAYING || mState == PlaybackStateCompat.STATE_PAUSED) )
            return;

        if ( pos < 0 || pos >= mPlayer.getDuration() )
            return;

        mPlayer.seekTo( pos );
        updateState( mState );

    }

    private void skipToQueueItemRunnable( final long id )
    {
        cancelCurrentTask();

        mCancelableTask = mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                skipToQueueItem( id );
            }
        } );
    }

    /*private void skipToQueueItemAsync( final long id )
    {
        Log.v(TAG, "skipToQueueItemAsync()");

        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {
                skipToQueueItem( id );

                return null;
            }
        }.execute(  );
    }*/

    private synchronized void skipToQueueItem ( long id )
    {
        int count;
        int position;

        //Abort if something is wrong
        if ( mState == PlaybackStateCompat.STATE_NONE || mQueue == null || mQueue.isEmpty() )
            return;

        //ID is also used as the index(position) in queue
        position    = ( int ) id;
        count       = mQueue.size();

        //Abort if position is wrong
        if ( position < 0 || position >= count )
            return;

        updateState( PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM );

        play( position );
    }

    private void playFromMediaIDRunnable(final String mediaID, final Bundle extras)
    {
        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                playFromMediaID( mediaID, extras );
            }
        } );
    }

    /*private void playFromMediaIDAsync(final String mediaID, final Bundle extras)
    {
        Log.v(TAG, "playFromMediaIDAsync()");

        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {
                playFromMediaID( mediaID, extras );

                return null;
            }

        }.execute();
    }*/

    private synchronized void playFromMediaID(String mediaID, Bundle extras)
    {
        String  source;
        String  parameter;
        String  displayName;
        int     position;

        if ( extras == null )
        {
            //If something is loaded, try to play it using mediaID
            tryPlayMediaID( mediaID );

            return;
        }


        source      = extras.getString  ( Const.SOURCE_KEY, null );
        parameter   = extras.getString  ( Const.PARAMETER_KEY, null );
        position    = extras.getInt     ( Const.POSITION_KEY, -1 );
        displayName = extras.getString  ( Const.DISPLAY_NAME, "" );

        if ( source == null )
            return;

        //If none of the cases above worked then do full list loading
        setQueue( source, parameter, displayName );

        if (mQueue == null)
            return;

        //Check if bundle provided correct play position, if not set it to -1
        if ( !isPositionOK( mQueue, position, mediaID ) )
            position = -1;

        //If position from bundle wasn't correct, try to find it here
        if (position == -1)
            position = findPosition( mQueue, mediaID );


        if ( position >= 0 )
            play( position );
    }

    private synchronized boolean tryPlayMediaID(String mediaID)
    {

        int position;

        if (mState == PlaybackStateCompat.STATE_NONE || mediaID == null)
            return false;

        position = findPosition( mQueue, mediaID );

        if (position >= 0)
        {
            //We found something to play
            play( position );
            return true;
        }

        //We can't play anything
        return false;
    }

    //Checks if position in queue matches with target media ID
    private boolean isPositionOK( List<MediaSessionCompat.QueueItem> queue, int targetPosition, String targetID )
    {
        String mediaID;


        if ( queue == null || targetPosition < 0 || targetPosition > queue.size() )
            return false;

        //If target ID is unknown then we assume position is ok (if within bounds)
        if ( targetID == null || TextUtils.equals( targetID, Const.UNKNOWN ) )
            return true;

        mediaID = queue.get( targetPosition ).getDescription().getMediaId();

        if ( mediaID == null )
            return false;

        //Return true if the ID's match, it means the position is ok
        return mediaID.equals( targetID );
    }

    private int findPosition( List<MediaSessionCompat.QueueItem> queue, String targetID )
    {
        if (queue == null || targetID == null || targetID.equals( Const.UNKNOWN ))
            return -1;

        String mediaID;

        for ( int i = 0; i < queue.size(); i++ )
        {
            mediaID = queue.get( i ).getDescription().getMediaId();

            if ( TextUtils.equals( mediaID, targetID ) )
            {
                //We found position, return it
                return i;
            }
        }

        //We didn't find position
        return -1;

    }

    private void fastForwardRunnable()
    {
        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                fastForward();
            }
        } );
    }

    private synchronized void fastForward()
    {
        int position;

        position = mPlayer.getCurrentPosition();

        position += 4000;

        //If position is out of bounds, seekTo() will do nothing
        seekTo( position );
    }

    private void rewindRunnable()
    {
        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                rewind();
            }
        } );
    }

    private synchronized void rewind()
    {
        int position;

        position = mPlayer.getCurrentPosition();

        position -= 4000;

        //If position is out of bounds, seekTo() will do nothing
        seekTo( position );
    }


    //Returns position of last played song
    private int recreateLastPlaybackState()
    {
        //Recreate last playback state
        SharedPreferences   prefs;
        String              source;
        String              parameter;
        String              queueTitle;
        int                 position;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        source      = prefs.getString ( LAST_SOURCE_KEY, null );
        parameter   = prefs.getString ( LAST_PARAMETER_KEY, null );
        position    = prefs.getInt    ( LAST_POSITION_KEY, -1 );
        queueTitle  = prefs.getString ( LAST_TITLE_KEY, "" );

        if ( source != null )
        {
            //This method is called async so no need to wrap it in async task
            setQueue( source, parameter, queueTitle );
            return position;
        }
        return -1;
    }

    //Try to get last playback state (if there is none, nothing will happen)
    public void playLastStateAsync()
    {
        Log.v(TAG, "playLastStateAsync()");



        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {
                playLastState();

                return null;
            }


        }.execute();
    }

    private void playLastStateRunnable()
    {
        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                playLastState();
            }
        } );
    }

    private void playLastState()
    {

        int lastPosition;

        lastPosition = recreateLastPlaybackState();

        //If we could recreate state then play the position
        if ( lastPosition != -1 )
            play( lastPosition );
    }

    private void setLastStateFailed()
    {
        SharedPreferences preferences;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferences .edit()
                    .putBoolean( LAST_STATE_PLAYED, false )
                    .apply();
    }

    private void setLastStateSuccess()
    {
        SharedPreferences preferences;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferences .edit()
                    .putBoolean( LAST_STATE_PLAYED, true )
                    .apply();
    }

    private boolean isLastStateSuccess()
    {
        SharedPreferences preferences;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        return preferences.getBoolean( LAST_STATE_PLAYED, false );
    }

    private synchronized void tryToGetAudioFocus()
    {
        if (!mAudioFocus)
        {
            if ( mAudioManager.requestAudioFocus( this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                mAudioFocus = true;
            }
        }
    }

    private synchronized void giveUpAudioFocus()
    {
        if (mAudioFocus)
        {
            if ( mAudioManager.abandonAudioFocus( this ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED )
            {
                mAudioFocus = false;
            }
        }
    }

    @Override
    public synchronized void onAudioFocusChange(int focusChange)
    {
        if (focusChange <= 0 && mState == PlaybackStateCompat.STATE_PLAYING)
        {
            pauseRunnable();
        }
    }

    public synchronized void play(int position)
    {
        Log.v( TAG, "play() position: " + position );


        Uri                     mediaFileUri;
        SharedPreferences       preferences;
        Intent                  startedServiceIntent;
        MediaMetadataCompat     metadata;
        MediaDescriptionCompat  mediaDescription;


        //If something is wrong then do nothing
        if ( mState == PlaybackStateCompat.STATE_NONE || position < 0 || position >= mCount || mQueue == null )
            return;

        mediaDescription        = mQueue.get(position).getDescription();
        mediaFileUri            = mediaDescription.getMediaUri();
        startedServiceIntent    = new Intent( getApplicationContext(), MediaPlayerService.class );

        if (mediaFileUri == null)
        {
            Log.e(TAG, "");
            return;
        }

        tryToGetAudioFocus();

        if (!mAudioFocus)
        {
            //If we failed to get audio focus just stop and return
            stop();
            return;
        }

        //NOTE - this is commented out because it messes lock screen controls,implemented in media control callbacks
        //updateState( PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM );


        mPosition = position;

        //Save current position so we can get it later on
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences .edit()
                    .putInt( LAST_POSITION_KEY, mPosition )
                    .apply();


        try
        {
            //Set up media player and start playing when ready
            mPlayer.reset();
            mPlayer.setDataSource           ( MediaPlayerService.this, mediaFileUri );
            mPlayer.setOnCompletionListener ( MediaPlayerService.this );
            mPlayer.prepare();

            mPlayer.start();

            showNotificationAsync( false, true );

            //Set service as started
            startService( startedServiceIntent );

            //Update playback state
            updateState( PlaybackStateCompat.STATE_PLAYING );


            mStatsDbHelper.updateLastPositionAsync( mQueueSource, mQueueParameter, mPosition );

            metadata = mMusicProvider.getMetadata( mediaDescription.getMediaId() );

            mMediaSession.setMetadata( metadata );

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }

    public void playRunnable(final int position)
    {
        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                play( position );
            }
        } );
    }


    /*public void playAsync(final int position)
    {
        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {
                play(position);

                return null;
            }
        }.execute();
    }*/


    //This is called when song is finished playing
    @Override
    public synchronized void onCompletion(MediaPlayer mp) {
        Log.v(TAG,"onCompletion()");

        //Continue to next song only if we are set to be playing
        if ( mState == PlaybackStateCompat.STATE_PLAYING )
        {
            MediaPlayerService.this.playNextRunnable();
        }
    }

    private void pauseRunnable()
    {
        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                pause();
            }
        } );
    }

    public synchronized void pause()
    {
        if ( mPlayer != null && ( mState == PlaybackStateCompat.STATE_PLAYING ) )
        {
            mPlayer.pause();
            giveUpAudioFocus();
            updateState( PlaybackStateCompat.STATE_PAUSED );
            showNotificationAsync( true, false );
            stopSelf();
        }
    }

    private void resumeRunnable()
    {
        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                resume();
            }
        } );
    }

    public synchronized void resume()
    {
        if ( mPlayer != null && ( mPosition != -1 || mState == PlaybackStateCompat.STATE_PAUSED ) )
        {
            tryToGetAudioFocus();

            if (!mAudioFocus)
                return;

            mPlayer.start();
            updateState( PlaybackStateCompat.STATE_PLAYING );
            showNotificationAsync( false, false );
        }
    }


    private void playNextRunnable()
    {
        cancelCurrentTask();

        mCancelableTask = mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                playNext();
            }
        } );
    }

    public synchronized void playNext()
    {
        //If the player is not even started then just dont do anything
        if ( mPosition == -1 ||  mState == PlaybackStateCompat.STATE_NONE || mState == PlaybackStateCompat.STATE_STOPPED )
            return;

        //NOTE - commented out because it makes lock screen controlls disappear
        //updateState( PlaybackStateCompat.STATE_SKIPPING_TO_NEXT );

        if ( mPosition == mCount - 1 )
        {
            //If we are at the end of playlist
            if ( shouldRepeatPlaylist() )
            {
                //If we are repeating playlist then start from the begining
                play( 0 );
            }
            else
            {
                stop();
            }
        }
        else
        {
            //Play next song
            play( mPosition + 1 );
        }
    }

    private void playPreviousRunnable()
    {
        cancelCurrentTask();

        mCancelableTask = mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                playPrevious();
            }
        } );
    }

    public synchronized void playPrevious()
    {
        //If the player is not even started then just don't do anything
        if ( mPosition == -1 || mPosition == 0 || mState == PlaybackStateCompat.STATE_NONE || mState == PlaybackStateCompat.STATE_STOPPED )
            return;

        //NOTE - commented out because it makes lock screen controlls disappear
        //updateState( PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);

        //Play previous song
        play( mPosition - 1 );

    }

    private void stopRunnable()
    {
        cancelCurrentTask();

        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                stop();
            }
        } );
    }

    public synchronized void stop()
    {

        if ( mPlayer != null )
        {
            updateState( PlaybackStateCompat.STATE_STOPPED );

            giveUpAudioFocus();

            //Allow this service to be destroyed (only if it becomes non-bound from all activities)
            stopSelf();
            stopForeground( true );

            mPosition = -1;

            if ( mPlayer.isPlaying() )
                mPlayer.stop();

            mPlayer.reset();
        }
    }

    private void stopAndClearListRunnable()
    {
        cancelCurrentTask();

        mExecutorService.submit( new Runnable()
        {
            @Override
            public void run()
            {
                stopAndClearList();
            }
        } );
    }

    //Stop playing and clear list
    public synchronized void stopAndClearList()
    {
        stop();
        mCount          = 0;
        mQueue          = null;
        mQueueSource    = null;
        mQueueParameter = null;
        updateState( PlaybackStateCompat.STATE_NONE );

    }




    public boolean shouldRepeatPlaylist()
    {
        SharedPreferences preferences;

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        return preferences.getBoolean( getString(R.string.pref_key_repeat), true );
    }

    public void showNotificationAsync ( final boolean playIcon, final boolean ticker )
    {
        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {
                showNotification( playIcon, ticker );

                return null;
            }
        }.execute(  );
    }


    public synchronized void showNotification( final boolean playIcon, final boolean ticker)
    {
        final Notification              notification;
        final NotificationManager       notificationManager;
        MediaSessionCompat.Token        sessionToken;           //For purposes of keeping all media session calls on same thread
        NotificationCompat.MediaStyle   mediaStyle;
        NotificationCompat.Builder      builder;
        MediaSessionCompat.QueueItem    currentQueueItem;
        MediaMetadataCompat             mediaMetadata;
        Intent                          intent;
        PendingIntent                   pendingIntent;
        String                          artist;
        Bitmap                          art;

        notificationManager = ( NotificationManager )getSystemService(NOTIFICATION_SERVICE);
        sessionToken        = mMediaSession.getSessionToken();
        currentQueueItem    = mQueue.get( mPosition );
        mediaMetadata       = mMusicProvider.getMetadata( currentQueueItem.getDescription().getMediaId() );


        artist = mediaMetadata == null ? "" : mediaMetadata.getString( MediaMetadataCompat.METADATA_KEY_ARTIST );


        art  = loadArt( mediaMetadata );


        //If there is no album art just download default art
        if ( art == null )
        {

            try
            {
                art = Glide.with    ( MediaPlayerService.this )
                        .load       ( R.mipmap.ic_launcher )
                        .asBitmap()
                        .into       ( 200, 200 )
                        .get();

            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            catch ( ExecutionException e )
            {
                //This is called if image loading fails for any reason, mostly because there is no image
            }
        }

        //Create close button intent
        intent = new Intent( getApplicationContext(), MediaPlayerService.class );
        intent.setAction( NOTIFICATION_ACTION_CLOSE );
        pendingIntent = PendingIntent.getService( getApplicationContext(), 0, intent, 0 );

        mediaStyle = new NotificationCompat.MediaStyle();
        mediaStyle.setMediaSession              ( sessionToken );
        mediaStyle.setCancelButtonIntent        ( pendingIntent );
        mediaStyle.setShowActionsInCompactView  ( 1, 2 );
        mediaStyle.setShowCancelButton          ( true );


        builder = new NotificationCompat.Builder( MediaPlayerService.this );
        builder.setSmallIcon    ( R.mipmap.ic_launcher )
                .setLargeIcon   ( art )
                .setContentTitle( currentQueueItem.getDescription().getTitle() )
                .setContentText ( artist )
                .setOngoing     ( !playIcon )
                .setAutoCancel  ( false )
                .setShowWhen    ( false )
                .setStyle       ( mediaStyle )
                .setDeleteIntent( pendingIntent );

        //If needed, set the ticker text with song title
        if ( ticker )
            builder.setTicker( currentQueueItem.getDescription().getTitle() );

        //Show play screen/main action
        //We build intent with MainActivity as parent activity in stack
        intent = new Intent( getApplicationContext(), NowPlayingActivity.class );
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );

        pendingIntent = PendingIntent.getActivity( MediaPlayerService.this, 0, intent, 0 );
        builder.setContentIntent( pendingIntent );


        //Add play actions (previous, play/pause, next)
        builder.addAction( generateAction( R.drawable.ic_skip_previous_ltgray_36dp, getString( R.string.title_play_previous ), NOTIFICATION_ACTION_PREVIOUS ) );
        builder.addAction( generateAction( playIcon ? R.drawable.ic_play_arrow_ltgray_36dp : R.drawable.ic_pause_ltgray_36dp, playIcon ? getString( R.string.title_play ) : getString( R.string.title_pause ), NOTIFICATION_ACTION_PLAY_PAUSE ) );
        builder.addAction( generateAction( R.drawable.ic_skip_next_ltgray_36dp, getString( R.string.title_play_next), NOTIFICATION_ACTION_NEXT ) );


        notification = builder.build();


        //Make sure that show notification is run on UI thread
        SlimPlayerApplication.getInstance().getHandler().post( new Runnable()
        {
            @Override
            public void run()
            {


                if (playIcon)
                {
                    //If we are paused
                    notificationManager.notify( NOTIFICATION_PLAYER_ID, notification );
                    stopForeground( false );
                }
                else
                {
                    startForeground( NOTIFICATION_PLAYER_ID, notification );
                }
            }
        } );

    }


    private Bitmap loadArt( MediaMetadataCompat metadata)
    {
        Bitmap art;
        String filePath;

        if ( metadata == null )
            return null;

        filePath    = Uri.parse( metadata.getString( MediaMetadataCompat.METADATA_KEY_MEDIA_URI ) ).toString();
        art         = null;

        //Load album art
        try
        {
            art = Glide.with    ( MediaPlayerService.this )
                    .load       ( new EmbeddedArtGlide( filePath ) )
                    .asBitmap()
                    .override   ( 200, 200 )
                    .centerCrop()
                    .into       ( 200, 200 )
                    .get();

        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        catch ( ExecutionException e )
        {
            //This is called if image loading fails for any reason, mostly because there is no image
        }

        return art;

    }

    private NotificationCompat.Action generateAction(int icon,  String title, String action)
    {
        Intent          intent;
        PendingIntent   pendingIntent;

        intent = new Intent(this,this.getClass());
        intent.setAction(action);

        pendingIntent = PendingIntent.getService(this,0,intent,0);

        return new NotificationCompat.Action.Builder( icon, title, pendingIntent ).build();
    }

}
