package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
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
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
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



public class MediaPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {

    protected final String TAG = getClass().getSimpleName();

    //Preference keys to restore last played list
    public static final String LAST_SOURCE_KEY =    "mihaljevic.miroslav.foundry.slimplayer.last_source";
    public static final String LAST_PARAMETER_KEY = "mihaljevic.miroslav.foundry.slimplayer.last_parameter";
    public static final String LAST_POSITION_KEY =  "mihaljevic.miroslav.foundry.slimplayer.last_position";
    public static final String LAST_TITLE_KEY =     "mihaljevic.miroslav.foundry.slimplayer.last_title";

    //Notification ID
    public static final int NOTIFICATION_PLAYER_ID = 111;

    public static final String  NOTIFICATION_ACTION_CLOSE =         "mihaljevic.miroslav.foundry.slimplayer.action.close";
    public static final String  NOTIFICATION_ACTION_PREVIOUS =      "mihaljevic.miroslav.foundry.slimplayer.action.previous";
    public static final String  NOTIFICATION_ACTION_PLAY_PAUSE =    "mihaljevic.miroslav.foundry.slimplayer.action.play_pause";
    public static final String  NOTIFICATION_ACTION_NEXT =          "mihaljevic.miroslav.foundry.slimplayer.action.next";

    /*public static final String  NOTIFICATION_ACTION_SWIPE =         "mihaljevic.miroslav.foundry.slimplayer.action.swipe";*/

    private static final String MEDIA_ROOT_ID = "slim_player_root";



    private MediaPlayer mPlayer;



    private AudioManager mAudioManager;
    private boolean mAudioFocus = false;

    /*private AsyncTask<Void, Void, Void> mCurrentPlayTask;*/


    private List<MediaSessionCompat.QueueItem> mQueue;
    private int mPosition = -1;
    private int mCount = 0;
    private int mState = PlaybackStateCompat.STATE_NONE;

    //Source and parameter of currently used song list
    private String mQueueSource;
    private String mQueueParameter;

    private MediaSessionCompat mMediaSession;

    private PlaybackStateCompat.Builder mStateBuilder;

    private MusicProvider mMusicProvider;

    private PackageValidator mPackageValidator;



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

            //If something is wrong just return
            if (!intent.hasExtra("state") )
                return;


            //If headset is plugged out, pause the playback
            if (intent.getIntExtra("state",0) == 0)
            {
                if (mState == PlaybackStateCompat.STATE_PLAYING)
                {
                    pause();
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
                    pause();
                }
            }

        }
    };

    /*private BroadcastReceiver mMediaButtonReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            String action;

            if (intent == null)
                return;

            action = intent.getAction();

            if (action.equals( Intent.ACTION_MEDIA_BUTTON ))
            {
                MediaButtonReceiver.handleIntent( mMediaSession, intent );
            }
        }
    };*/






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



        //TODO - this needs to be tested on >=LOLLIPOP (media buttons only, lock screen controls work)
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

        mHandler = new Handler();

        mPackageValidator = PackageValidator.getInstance();

        //Register to detect headphones in/out
        registerReceiver( mHeadsetChangeReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG ) );
        registerReceiver( mNoisyReceiver, new IntentFilter( AudioManager.ACTION_AUDIO_BECOMING_NOISY ) );

        //Recreate last remembered state
        playLastStateAsync();

    }



    @Nullable
    @Override
    public BrowserRoot onGetRoot( @NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints )
    {

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
    public void onLoadChildren( @NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result, @Nullable Bundle options )
    {

        String source;
        String parameter;
        String[] split;
        List<MediaBrowserCompat.MediaItem> mediaItems;


        split = parentId.split( "\\:" );

        if ( split.length < 1 || split.length > 2 )
            return;
        else if ( split.length == 1 )
        {
            source = split[ 0 ];
            parameter = null;
        }
        else
        {
            source = split[ 0 ];
            parameter = split[ 1 ];
        }

        //Load media from music provider
        //TODO - this to another thread, you use detach function of result
        mediaItems = mMusicProvider.loadMedia( source, parameter );


        result.sendResult( mediaItems );
    }


    private final class MediaSessionCallback extends MediaSessionCompat.Callback
    {
        @Override
        public void onPlay()
        {
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
        public void onSkipToNext()
        {
            super.onSkipToNext();

            playNext();
        }

        @Override
        public void onSkipToPrevious()
        {
            super.onSkipToPrevious();

            playPrevious();
        }

        @Override
        public void onPlayFromMediaId( String mediaId, Bundle extras )
        {
            if ( extras == null && mState == PlaybackStateCompat.STATE_NONE )
                return;

            //If we have something loaded try to find it using media id
            if ( extras == null && mQueue != null && mCount > 0 && !mediaId.equals( "-1" ) )
            {
                for ( MediaSessionCompat.QueueItem queueItem : mQueue )
                {
                    if ( TextUtils.equals( queueItem.getDescription().getMediaId(), mediaId ) )
                    {
                        play( mQueue.indexOf( queueItem ) );
                        return;
                    }
                }
            }

            if ( extras == null )
                return;

            String source;
            String parameter;
            String displayName;
            int position;
            int i;

            source = extras.getString( Const.SOURCE_KEY, null );
            parameter = extras.getString( Const.PARAMETER_KEY, null );
            position = extras.getInt( Const.POSITION_KEY, -1 );
            displayName = extras.getString( Const.DISPLAY_NAME, "" );

            if ( source == null )
                return;


            //If none of the cases above worked then do full list loading
            setQueue( source, parameter, displayName );

            Utils.toastShort( "Queue is " + displayName );

            //Check if bundle provided correct play position
            if ( !( position >= 0 && position < mQueue.size() && ( mediaId.equals( Const.UNKNOWN ) || mQueue.get( position ).getDescription().getMediaId().equals( mediaId ) ) ) )
                position = -1;

            //If position from bundle wasn't correct, try to find it here
            if ( position == -1 && !mediaId.equals( Const.UNKNOWN ) )
            {
                for ( i = 0; i < mQueue.size(); i++ )
                {
                    if ( mQueue.get( i ).getDescription().getMediaId().equals( mediaId ) )
                    {
                        position = i;
                        break;
                    }
                }
            }


            if ( position >= 0 )
                play( position );

        }

        @Override
        public void onSkipToQueueItem( long id )
        {
            super.onSkipToQueueItem( id );

            int count;
            int position;

            //Abort if something is wrong
            if ( mState == PlaybackStateCompat.STATE_NONE || mQueue == null || mQueue.isEmpty() )
                return;

            //ID is also used as the index(position) in queue
            position = ( int ) id;
            count = mQueue.size();

            //Abort if position is wrong
            if ( position < 0 || position >= count )
                return;

            updateState( PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM );

            play( position );

        }

        @Override
        public void onSeekTo( long pos )
        {
            super.onSeekTo( pos );

            //Seek to position in ms if we have song loaded and update statewith new position
            if ( mState == PlaybackStateCompat.STATE_PLAYING || mState == PlaybackStateCompat.STATE_PAUSED )
            {
                mPlayer.seekTo( ( int ) pos );
                updateState( mState );
            }
        }
    }



    //Returns whether the source is different than one before
    public boolean setQueue( @Nullable final String source, @Nullable final String parameter, @Nullable final String queueTitle)
    {
        Log.v(TAG,"setQueue()");


        List<MediaBrowserCompat.MediaItem>  mediaItems;
        MediaSessionCompat.QueueItem        queueItem;
        MediaBrowserCompat.MediaItem        mediaItem;
        boolean isSourceChanged;
        int     count;
        String oldSource;
        String oldParameter;
        Bundle sessionExtras;

        //We use old* variables to determine if change is actually made to queue
        oldSource = mQueueSource;
        oldParameter = mQueueParameter;
        sessionExtras = new Bundle();

        //Since we are changing queue let's set state to none before we know anything
        stopAndClearList();


        if (source == null)
            return true;


        //If we are loading from file
        if (source.equals(Const.FILE_URI_KEY))
        {

            mediaItems = new ArrayList<>(1);

            mediaItem = MusicProvider.getInstance().mediaFromFile( parameter );

            if (mediaItem == null)
                return true;

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


        sessionExtras.putString( Const.SOURCE_KEY,      source );
        sessionExtras.putString( Const.PARAMETER_KEY,   parameter );

        //State stopped means that nothing is playing but we have media loaded
        updateState( PlaybackStateCompat.STATE_STOPPED );


        mMediaSession.setExtras( sessionExtras );
        mMediaSession.setQueue( mQueue );

        if (queueTitle != null)
            mMediaSession.setQueueTitle( queueTitle );

        isSourceChanged = Utils.isSourceDifferent( source, oldSource, parameter, oldParameter );


        //If the source is different and not file then update stats database
        if (!source.equals( Const.FILE_URI_KEY ) &&  isSourceChanged)
        {

            new AsyncTask<Void,Void,Void>()
            {
                @Override
                protected Void doInBackground(Void... params)
                {
                    StatsDbHelper statsDbHelper = StatsDbHelper.getInstance();
                    statsDbHelper.updateStats(source,parameter, queueTitle);
                    return null;
                }
            }.execute();
        }

        mQueueSource = source;
        mQueueParameter = parameter;


        //Save song list source in preferences so we remember this list for auto-start playback
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MediaPlayerService.this);
        preferences.edit()
                    .putString( LAST_SOURCE_KEY, source)
                    .putString( LAST_PARAMETER_KEY, parameter)
                    .putString( LAST_TITLE_KEY, queueTitle )
                    .apply();




        return isSourceChanged;

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

        //Handle action from media button
        MediaButtonReceiver.handleIntent( mMediaSession, intent );

        //Get action from intent while checking for null
        String action = intent == null ? null : intent.getAction();

        if (action != null)
        {
            switch (action)
            {
                case NOTIFICATION_ACTION_CLOSE:
                    stop();
                    break;
                case NOTIFICATION_ACTION_PREVIOUS:
                    playPrevious();
                    break;
                case NOTIFICATION_ACTION_PLAY_PAUSE:
                    if (mState == PlaybackStateCompat.STATE_PLAYING)
                        pause();
                    else
                        resume();
                    break;
                case NOTIFICATION_ACTION_NEXT:
                    playNext();
                    break;
                /*case NOTIFICATION_ACTION_SWIPE:
                    stopAndClearList();
                    break;*/
            }
        }

        return START_NOT_STICKY;
    }



    @Override
    public void onDestroy() {
        Log.v(TAG,"onDestroy()");

        giveUpAudioFocus();
        unregisterReceiver(mHeadsetChangeReceiver);
        unregisterReceiver( mNoisyReceiver );
        mMusicProvider.unregisterDataListener();
        mMediaSession.release();

        if (mPlayer != null)
        {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }


        StatsDbHelper.closeInstance();



        super.onDestroy();
    }


    //Returns position of last played song
    private int recreateLastPlaybackState()
    {
        //Recreate last playback state
        SharedPreferences prefs;
        String source;
        String parameter;
        String queueTitle;
        int position;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        source =        prefs.getString ( LAST_SOURCE_KEY, null );
        parameter =     prefs.getString ( LAST_PARAMETER_KEY, null );
        position =      prefs.getInt    ( LAST_POSITION_KEY, 0 );
        queueTitle =    prefs.getString ( LAST_TITLE_KEY, "" );

        if (source != null)
        {
            setQueue( source, parameter, queueTitle );
            return position;
        }
        return -1;
    }

    //Try to get last playback state (if there is none, nothing will happen)
    public void playLastStateAsync()
    {
        new AsyncTask<Void, Void, Integer>()
        {
            @Override
            protected Integer doInBackground( Void... params )
            {
                return recreateLastPlaybackState();
            }

            @Override
            protected void onPostExecute( Integer result )
            {
                //If we could recreate state then play the position
                if ( result != -1 )
                    play( result );
            }
        }.execute();
    }

    private void tryToGetAudioFocus()
    {
        if (!mAudioFocus)
        {
            if ( mAudioManager.requestAudioFocus( this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            {
                mAudioFocus = true;
            }
        }
    }

    private void giveUpAudioFocus()
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
    public void onAudioFocusChange(int focusChange)
    {
        if (focusChange <= 0 && mState == PlaybackStateCompat.STATE_PLAYING)
        {
            pause();
        }
    }

    public void play(int position)
    {
        Log.v( TAG, "play() position: " + position );


        Uri mediaFileUri;


        //If something is wrong then do nothing
        if ( mState == PlaybackStateCompat.STATE_NONE || position < 0 || position >= mCount || mQueue == null )
            return;

        mediaFileUri = mQueue.get(position).getDescription().getMediaUri();

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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putInt( LAST_POSITION_KEY,mPosition).apply();




        Utils.toastShort(mediaFileUri.toString());

        try
        {
            //Set up media player and start playing when ready
            mPlayer.reset();
            mPlayer.setDataSource(MediaPlayerService.this, mediaFileUri );
            mPlayer.setOnCompletionListener(MediaPlayerService.this);
            mPlayer.setOnPreparedListener( this );
            mPlayer.prepareAsync();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


    }

    @Override
    public void onPrepared( MediaPlayer mp )
    {
        Log.v(TAG,"onPrepared()");

        Intent intent;
        MediaMetadataCompat metadata;
        Bundle extras;

        extras = mQueue.get( mPosition ).getDescription().getExtras();

        //Try to acquire media metadata if it is bundled with media description
        metadata = mMusicProvider.getMetadata( mQueue.get( mPosition ).getDescription() );

        intent = new Intent( getApplicationContext(), MediaPlayerService.class );

        mp.start();

        showNotification(false, true);

        //Set service as started
        startService( intent );

        mMediaSession.setMetadata( metadata );

        //Update playback state
        updateState( PlaybackStateCompat.STATE_PLAYING );

        //mHandler.post( mSeekBarRunnable );

        updateQueueLastPositionAsync();
    }

    //This is called when song is finished playing
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.v(TAG,"onCompletion()");

        //Continue to next song only if we are set to be playing
        if (mState == PlaybackStateCompat.STATE_PLAYING)
        {
            MediaPlayerService.this.playNext();
        }
    }

    public void pause()
    {
        if (mPlayer != null && ( mState == PlaybackStateCompat.STATE_PLAYING))
        {
            mPlayer.pause();
            giveUpAudioFocus();
            updateState( PlaybackStateCompat.STATE_PAUSED );
            showNotification(true,false);
        }
    }

    public void resume()
    {
        if (mPlayer != null && (mPosition != -1 || mState == PlaybackStateCompat.STATE_PAUSED))
        {
            /*mPlaying = true;*/
            tryToGetAudioFocus();

            if (!mAudioFocus)
                return;

            mPlayer.start();
            updateState( PlaybackStateCompat.STATE_PLAYING );
            showNotification(false, false);
        }
    }



    public void playNext()
    {
        //If the player is not even started then just dont do anything
        if (mPosition == -1 ||  mState == PlaybackStateCompat.STATE_NONE || mState == PlaybackStateCompat.STATE_STOPPED)
            return;

        //NOTE - commented out because it makes lock screen controlls disappear
        //updateState( PlaybackStateCompat.STATE_SKIPPING_TO_NEXT );

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
        if (mPosition == -1 || mPosition == 0 || mState == PlaybackStateCompat.STATE_NONE || mState == PlaybackStateCompat.STATE_STOPPED)
            return;

        //NOTE - commented out because it makes lock screen controlls disappear
        //updateState( PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS);

        //Play previous song
        play(mPosition - 1);

    }

    public void stop()
    {

        if (mPlayer != null)
        {
            updateState( PlaybackStateCompat.STATE_STOPPED );

            giveUpAudioFocus();

            //Allow this service to be destroyed (only if it becomes non-bound from all activities)
            stopSelf();
            stopForeground( true );

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
        updateState( PlaybackStateCompat.STATE_NONE );

    }

    public void updateQueueLastPositionAsync()
    {
        //Start new task to update last play position for this source
        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                StatsDbHelper statsDbHelper = StatsDbHelper.getInstance();
                statsDbHelper.updateLastPosition( mQueueSource, mQueueParameter,mPosition);
                return null;
            }
        }.execute();
    }

    public boolean shouldRepeatPlaylist()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean(getString(R.string.pref_key_repeat),true);
    }


    public void showNotification( final boolean playIcon, final boolean ticker)
    {
        final NotificationManager       notificationManager;
        final MediaSessionCompat.Token  sessionToken;           //For purposes of keeping all media session calls on same thread

        notificationManager = (NotificationManager )getSystemService(NOTIFICATION_SERVICE);
        sessionToken = mMediaSession.getSessionToken();

        new AsyncTask<Void, Void, Notification>()
        {

            @Override
            protected Notification doInBackground( Void... params )
            {

                NotificationCompat.MediaStyle   mediaStyle;
                NotificationCompat.Builder      builder;
                MediaSessionCompat.QueueItem    currentQueueItem;
                MediaMetadataCompat             mediaMetadata;
                Intent          intent;
                PendingIntent   pendingIntent;
                Notification    notification;
                String          filePath;
                String          artist;
                Bitmap          art;


                currentQueueItem = mQueue.get( mPosition );

                mediaMetadata = mMusicProvider.getMetadata( currentQueueItem.getDescription() );

                filePath = Uri.parse( mediaMetadata.getString( MediaMetadataCompat.METADATA_KEY_MEDIA_URI ) ).toString();
                artist = mediaMetadata.getString( MediaMetadataCompat.METADATA_KEY_ARTIST );
                art = null;

                //Load album art
                try
                {
                    art = Glide.with( MediaPlayerService.this )
                            .load( new EmbeddedArtGlide( filePath ) )
                            .asBitmap()
                            .override( 200, 200 )
                            .centerCrop()
                            .into( 200, 200 )
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

                //If there is no album art just download default art
                if ( art == null )
                {

                    try
                    {
                        art = Glide.with( MediaPlayerService.this )
                                .load( R.mipmap.ic_launcher )
                                .asBitmap()
                                .into( 200, 200 )
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
                mediaStyle.setMediaSession( sessionToken );
                mediaStyle.setCancelButtonIntent( pendingIntent );
                mediaStyle.setShowActionsInCompactView( 1, 2 );
                mediaStyle.setShowCancelButton( true );


                builder = new NotificationCompat.Builder( MediaPlayerService.this );
                builder.setSmallIcon( R.mipmap.ic_launcher )
                        .setLargeIcon( art )
                        .setContentTitle( currentQueueItem.getDescription().getTitle() )
                        .setContentText( artist )
                        .setOngoing( !playIcon )
                        .setAutoCancel( false )
                        .setShowWhen( false )
                        .setStyle( mediaStyle )
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

                return notification;

            }

            @Override
            protected void onPostExecute( Notification notification )
            {
                super.onPostExecute( notification );

                startForeground( NOTIFICATION_PLAYER_ID, notification );

                //notificationManager.notify( NOTIFICATION_PLAYER_ID, notification );
            }
        }.execute();





    }

    private NotificationCompat.Action generateAction(int icon,  String title, String action)
    {
        Intent intent;
        PendingIntent pendingIntent;

        intent = new Intent(this,this.getClass());
        intent.setAction(action);
        pendingIntent = PendingIntent.getService(this,0,intent,0);

        return new NotificationCompat.Action.Builder( icon, title, pendingIntent ).build();
    }








}
