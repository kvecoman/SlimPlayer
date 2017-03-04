package mihaljevic.miroslav.foundry.slimplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;

import java.util.List;


public class NowPlayingActivity extends BackHandledFragmentActivity implements  ViewPager.OnPageChangeListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener
{


    private ViewPager               mPager;
    private NowPlayingPagerAdapter  mPagerAdapter;

    private SeekBar     mSeekBar;
    private Handler     mSeekBarHandler;
    private Runnable    mSeekBarRunnable;

    private String mQueueSource;
    private String mQueueParameter;

    protected MediaBrowserCompat    mMediaBrowser;
    protected MediaControllerCompat mMediaController;

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback()
    {
        @Override
        public void onConnected()
        {
            super.onConnected();

            try
            {
                mMediaController = new MediaControllerCompat( NowPlayingActivity.this, mMediaBrowser.getSessionToken() );
                mMediaController.registerCallback( mMediaControllerCallbacks );



                startFilePlayingIfNeeded();

                initQueue();

                mSeekBarHandler.post( mSeekBarRunnable );

            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onConnectionSuspended()
        {
            super.onConnectionSuspended();

            Log.i( TAG, "Connection is suspended" );
        }

        @Override
        public void onConnectionFailed()
        {
            super.onConnectionFailed();

            Log.e( TAG, "Connection has failed" );
        }
    };

    private void initQueue()
    {
        //This is so we prevent calls at some crazy times (like configuration change callbacks when things are not initialized)
        if ( mMediaBrowser == null || !mMediaBrowser.isConnected() )
            return;

        if ( !isThisQueueLoaded() )
            return;

        //If this queue is loaded then
        initPagerAdapter( mMediaController.getQueue() );

    }




    private MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback()
    {
        @Override
        public void onQueueChanged( List<MediaSessionCompat.QueueItem> queue )
        {
            super.onQueueChanged( queue );

            //This will update queue if needed or appropriate
            initQueue();


        }

        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            int activeQueueId;
            int stateInt;

            activeQueueId   = (int)state.getActiveQueueItemId();
            stateInt        = state.getState();

            if ( stateInt == PlaybackStateCompat.STATE_PLAYING )
            {
                //Check if we need to switch to current page
                if ( activeQueueId != mPager.getCurrentItem() )
                    updatePagerWithCurrentSong();
            }
        }

        @Override
        public void onMetadataChanged( MediaMetadataCompat metadata )
        {
            super.onMetadataChanged( metadata );

            if ( isThisQueueLoaded() )
                updatePagerWithCurrentSong();
        }
    };

    private class SeekBarRunnable implements Runnable
    {

        @Override
        public void run()
        {
            PlaybackStateCompat state;

            if ( mMediaBrowser.isConnected() && mMediaController != null )
            {
                state = mMediaController.getPlaybackState();

                if ( state.getState() == PlaybackStateCompat.STATE_PLAYING || state.getState() == PlaybackStateCompat.STATE_PAUSED )
                {
                    if ( state.getActiveQueueItemId() == mPager.getCurrentItem() )
                    {
                        mSeekBar.setProgress( ( int )state.getPosition() );
                    }
                    else
                    {
                        //If something is wrong
                        mSeekBar.setProgress( 0 );
                    }
                }
            }

            mSeekBarHandler.postDelayed( this, 1000 );
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent      intent;
        ImageButton rewindButton;
        ImageButton fastForwardButton;

        intent = getIntent();

        setContentView(R.layout.activity_now_playing);

        if (intent != null && intent.hasExtra( Const.SOURCE_KEY ))
        {
            mQueueSource    = intent.getStringExtra( Const.SOURCE_KEY );
            mQueueParameter = intent.getStringExtra( Const.PARAMETER_KEY );
        }

        mPager = ( ViewPager )findViewById( R.id.pager );
        mPager.addOnPageChangeListener( this );

        mSeekBar = ( SeekBar ) findViewById( R.id.seek_bar );
        mSeekBar.setProgress( 0 );
        mSeekBar.setOnSeekBarChangeListener( this );

        mSeekBarHandler     = new Handler(  );
        mSeekBarRunnable    = new SeekBarRunnable();

        rewindButton        = ( ImageButton ) findViewById( R.id.rewind_button );
        fastForwardButton   = ( ImageButton ) findViewById( R.id.fast_forward_button );

        rewindButton.setOnClickListener     ( new RewindListener() );
        fastForwardButton.setOnClickListener( new FastForwardListener() );

        mMediaBrowser = new MediaBrowserCompat( this, MediaPlayerService.COMPONENT_NAME, mConnectionCallbacks, null );
    }

    /*@Override
    protected void onNewIntent( Intent intent )
    {
        super.onNewIntent( intent );

        //NOTE - intent handling to function?
        if (intent != null && intent.hasExtra( Const.SOURCE_KEY ))
        {
            mQueueSource    = intent.getStringExtra( Const.SOURCE_KEY );
            mQueueParameter = intent.getStringExtra( Const.PARAMETER_KEY );
        }

    }*/

    @Override
    protected void onStart() {
        super.onStart();

        //Ask for permissions if needed
        if ( Build.VERSION.SDK_INT >= 16 )
        {
            Utils.askPermission(    this,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    getString( R.string.permission_storage_explanation ),
                                    Const.STORAGE_PERMISSIONS_REQUEST,
                                    new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick( DialogInterface dialog, int which )
                                        {
                                            //Go back if we don't have permission
                                            onBackPressed();
                                        }
                                    } );
        }


        mMediaBrowser.connect();
    }



    @Override
    protected void onResume() {
        super.onResume();

        //Update pager with current song when we return to this activity (if we are connected to media service)
        updatePagerWithCurrentSong();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        super.onCreateOptionsMenu( menu );
        getMenuInflater().inflate( R.menu.now_playing_menu, menu );

        updateRepeatIcon( menu );

        return true;
    }


    @Override
    public void onBackPressed()
    {
        Intent intent;

        if ( isTaskRoot() )
        {
            //If we came from notification and this activity is task root then we want back button to return (open) Main activity
            intent = new Intent(this,MainActivity.class);
            intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
            startActivity(intent);
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onStop()
    {
        super.onStop();


        if (mMediaController != null)
            mMediaController.unregisterCallback( mMediaControllerCallbacks );

        if (mMediaBrowser.isConnected())
            mMediaBrowser.disconnect();

        if (mSeekBarHandler != null && mSeekBarRunnable != null)
            mSeekBarHandler.removeCallbacks( mSeekBarRunnable );
    }


    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults )
    {
        Intent intent;

        switch (requestCode)
        {
            case Const.STORAGE_PERMISSIONS_REQUEST:

                if (permissions.length != 0 && permissions[0].equals( Manifest.permission.READ_EXTERNAL_STORAGE ))
                {
                    if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    {

                        intent = getIntent();

                        if ( intent != null && intent.getAction() != null && intent.getAction().equals( Intent.ACTION_VIEW ) )
                        {
                            startFilePlayingIfNeeded();
                        }
                        else
                        {
                            //Go back
                            onBackPressed();
                        }
                    }
                    else
                    {
                        //If we don't get permission
                        onBackPressed();
                    }
                }


                break;
        }

        super.onRequestPermissionsResult( requestCode, permissions, grantResults );
    }

    //Check if we need to start playing a file
    private void startFilePlayingIfNeeded()
    {
        Intent  intent;
        Uri     fileUri;
        Bundle  extras;

        intent = getIntent();

        //Here we handle if playback is started from file
        if ( intent != null && intent.getAction() != null && intent.getAction().equals( Intent.ACTION_VIEW ) && mMediaBrowser.isConnected() )
        {
            //This activity is called from outside, when playing audio files
            fileUri = intent.getData();
            extras  = new Bundle(  );


            if ( fileUri.getScheme().contains("file") )
            {
                mQueueSource = Const.FILE_URI_KEY;
                mQueueParameter = fileUri.toString();

                extras.putString( Const.SOURCE_KEY,     Const.FILE_URI_KEY );
                extras.putString( Const.PARAMETER_KEY,  fileUri.toString() );
                extras.putInt   ( Const.POSITION_KEY,   0 );

                //We use position as mediaID
                mMediaController.getTransportControls().playFromMediaId( fileUri.toString(), extras );
            }


        }
    }

    private void initPagerAdapter( List<MediaSessionCompat.QueueItem> queue )
    {

        mPagerAdapter = new NowPlayingPagerAdapter( getSupportFragmentManager(), queue );
        mPager.setAdapter( mPagerAdapter );

        updatePagerWithCurrentSong();
    }

    public void updatePagerWithCurrentSong()
    {
        int                 position;
        PlaybackStateCompat playbackState;

        if ( !mMediaBrowser.isConnected() )
            return;

        playbackState = mMediaController.getPlaybackState();

        if ( playbackState == null )
            return;

        position = ( int )playbackState.getActiveQueueItemId();

        if ( position != PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN && position >= 0 && position < mPagerAdapter.getCount() )
        {
            mPager.setCurrentItem   ( position );
            updateSeekBarMax        ( position );

        }
    }

    public void updateSeekBarMax( int position )
    {
        MediaMetadataCompat             metadata;
        MediaSessionCompat.QueueItem    queueItem;

        queueItem = mPagerAdapter.getData().get( position );

        mSeekBar.setProgress( 0 );

        metadata = MusicProvider.getInstance().getMetadata( queueItem.getDescription().getMediaId() );

        if (metadata != null)
            mSeekBar.setMax(( int )metadata.getLong( MediaMetadataCompat.METADATA_KEY_DURATION ));

    }

    public void updateRepeatIcon( Menu menu )
    {
        //Set correct icon for toggle repeat action
        MenuItem repeatItem;

        repeatItem = menu.findItem( R.id.toggle_repeat );

        if (repeatItem != null)
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( this );

            if (preferences.getBoolean( getString( R.string.pref_key_repeat ), true ) )
                repeatItem.setIcon( R.drawable.ic_repeat_white_24dp );
            else
                repeatItem.setIcon( R.drawable.ic_repeat_gray_24dp );
        }
    }

    private boolean isThisQueueLoaded()
    {
        Bundle sessionExtras;
        String sessionSource;
        String sessionParameter;

        if ( mMediaBrowser == null || !mMediaBrowser.isConnected() || mMediaController == null )
            return false;

        sessionExtras = mMediaController.getExtras();

        if ( sessionExtras == null )
            return false;


        sessionSource       = sessionExtras.getString( Const.SOURCE_KEY );
        sessionParameter    = sessionExtras.getString( Const.PARAMETER_KEY );


        return !Utils.isSourceDifferent( mQueueSource, mQueueParameter, sessionSource, sessionParameter );
    }


    //Handle onscreen taps, change between play/pause
    @Override
    public void onClick( View v )
    {

        PlaybackStateCompat playbackState;
        int                 state;

        playbackState   = mMediaController.getPlaybackState();
        state           = playbackState.getState();


        switch(state)
        {
            case PlaybackStateCompat.STATE_PLAYING:
                //Pause playback
                mMediaController.getTransportControls().pause();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                //Resume playback
                mMediaController.getTransportControls().play();
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                mMediaController.getTransportControls().skipToQueueItem( mPager.getCurrentItem() );
                break;
            case PlaybackStateCompat.STATE_NONE:
                Bundle  bundle;
                int     position;
                String  mediaId;

                position = mPager.getCurrentItem();

                mediaId = mPagerAdapter.getData().get( position ).getDescription().getMediaId();

                bundle = new Bundle();
                bundle.putString( Const.SOURCE_KEY, mQueueSource );
                bundle.putString( Const.PARAMETER_KEY, mQueueParameter );
                bundle.putInt   ( Const.POSITION_KEY,  position);

                mMediaController.getTransportControls().playFromMediaId( mediaId, bundle );
                break;
        }


    }

    @Override
    public void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser )
    {
        if ( mMediaBrowser == null || !mMediaBrowser.isConnected() )
            return;

        long actions;

        actions = mMediaController.getPlaybackState().getActions();

        //Only if touch is coming from user then seek song (and that action is available)
        if ( fromUser && ( actions & PlaybackStateCompat.ACTION_SEEK_TO ) == PlaybackStateCompat.ACTION_SEEK_TO )
            mMediaController.getTransportControls().seekTo( progress );

    }

    @Override
    public void onStartTrackingTouch( SeekBar seekBar ) {}

    @Override
    public void onStopTrackingTouch( SeekBar seekBar ) {}

    @Override
    public void onPageScrolled( int position, float positionOffset, int positionOffsetPixels ) {}

    @Override
    public void onPageSelected( int position )
    {
        if ( !mMediaBrowser.isConnected() || mMediaController == null || mMediaController.getPlaybackState().getActiveQueueItemId() == position )
            return;

        //Play this position when user selects it
        mMediaController.getTransportControls().skipToQueueItem( mPager.getCurrentItem() );

        //If we changed song then we will immediately set seek bar to 0
        mSeekBar.setProgress( 0 );
        updateSeekBarMax( position );
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    private class RewindListener implements Button.OnClickListener
    {
        @Override
        public void onClick( View v )
        {
            if ( mMediaBrowser == null || !mMediaBrowser.isConnected() || mMediaController == null )
                return;

            mMediaController.getTransportControls().rewind();
        }
    }

    private class FastForwardListener implements Button.OnClickListener
    {
        @Override
        public void onClick( View v )
        {
            if ( mMediaBrowser == null || !mMediaBrowser.isConnected() || mMediaController == null )
                return;

            mMediaController.getTransportControls().fastForward();
        }
    }
}
