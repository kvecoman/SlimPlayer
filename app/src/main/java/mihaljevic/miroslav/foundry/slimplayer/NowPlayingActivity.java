package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;


public class NowPlayingActivity extends BackHandledFragmentActivity implements  ViewPager.OnPageChangeListener, View.OnClickListener
{

    //TODO - save loaded queue using some kind of fragment and saving instance of it


    private ViewPager mPager;
    private NowPlayingPagerAdapter mPagerAdapter;

    private String mQueueSource;
    private String mQueueParameter;

    protected MediaBrowserCompat mMediaBrowser;
    protected MediaControllerCompat mMediaController;

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback(){
        @Override
        public void onConnected()
        {
            super.onConnected();



            try
            {
                mMediaController = new MediaControllerCompat( NowPlayingActivity.this, mMediaBrowser.getSessionToken() );
                mMediaController.registerCallback( mMediaControllerCallbacks );

                startFilePlayingIfNeeded();

                //First time data init
                initPagerAdapter( mMediaController.getQueue() );


            }
            catch (RemoteException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConnectionSuspended()
        {
            super.onConnectionSuspended();
        }

        @Override
        public void onConnectionFailed()
        {
            super.onConnectionFailed();
        }
    };

    /*private MediaBrowserCompat.SubscriptionCallback mSubscriptionCallbacks = new MediaBrowserCompat.SubscriptionCallback()
    {
        @Override
        public void onChildrenLoaded( @NonNull String parentId, List<MediaBrowserCompat.MediaItem> children )
        {
            super.onChildrenLoaded( parentId, children );
            onChildrenLoaded( parentId, children, null );
        }

        @Override
        public void onChildrenLoaded( @NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options )
        {
            super.onChildrenLoaded( parentId, children, options );

            //We get this callback only if we are playing from file
        }
    };*/

    private MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback()
    {
        @Override
        public void onQueueChanged( List<MediaSessionCompat.QueueItem> queue )
        {
            super.onQueueChanged( queue );

            //Only if pager adapter is empty then we want to load queue
            if (mPagerAdapter.getData() == null)
            {
                initPagerAdapter( queue );
            }

        }

        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            int activeQueueId;
            int stateInt;

            activeQueueId = (int)state.getActiveQueueItemId();
            stateInt = state.getState();

            if (stateInt == PlaybackStateCompat.STATE_PLAYING)
            {
                //Check if we need to switch to current page
                if (activeQueueId != mPager.getCurrentItem())
                    updatePagerWithCurrentSong();
            }
        }

    };


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        Intent intent;

        intent = getIntent(); //TODO - check if null and get source

        if (intent != null && intent.hasExtra( Const.SOURCE_KEY ))
        {
            mQueueSource = intent.getStringExtra( Const.SOURCE_KEY );
            mQueueParameter = intent.getStringExtra( Const.PARAMETER_KEY );
        }

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);

        mMediaBrowser = new MediaBrowserCompat( this, new ComponentName( this, MediaPlayerService.class ), mConnectionCallbacks, null );
    }

    @Override
    protected void onStart() {
        super.onStart();


        mMediaBrowser.connect();
    }



    @Override
    protected void onResume() {
        super.onResume();

        //Update pager with current song when we return to this activity (if we are connected to media service)
        updatePagerWithCurrentSong();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.now_playing_menu,menu);


        updateRepeatIcon(menu);

        return true;
    }

    //TODO - this code needs to go in onSubscribe or onChildrenLoaded (now only filePlaying needs to be done)
    /*@Override
    public void onPlayerServiceBound(MediaPlayerService playerService) {
        mPlayerService = playerService;

        startFilePlayingIfNeeded();

        initPagerAdapter();

        //When we are connected request current play info
        mPlayerService.registerPlayListener(NowPlayingActivity.this);
    }*/

    @Override
    public void onBackPressed() {

        if (isTaskRoot())
        {
            //If we came from notification and this activity is task root then we want back button to return (open) Main activity
            Intent intent = new Intent(this,MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();


        mMediaController.unregisterCallback( mMediaControllerCallbacks );
        mMediaBrowser.disconnect();
    }

    @Override
    protected void onDestroy()
    {
        Log.v(TAG, "onDestroy()");
        super.onDestroy();
    }

    //Check if we need to start playing a file
    private void startFilePlayingIfNeeded()
    {
        Intent intent;
        Uri fileUri;
        Bundle extras;

        intent = getIntent();

        //Here we handle if playback is started from file
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW) && mMediaBrowser.isConnected())
        {
            //This activity is called from outside, when playing audio files
            fileUri = intent.getData();
            extras = new Bundle(  );


            if (fileUri.getScheme().contains("file"))
            {
                extras.putString( Const.SOURCE_KEY,     Const.FILE_URI_KEY );
                extras.putString( Const.PARAMETER_KEY,  fileUri.toString() );
                extras.putInt   ( Const.POSITION_KEY,   0 );

                //We use position as mediaID
                mMediaController.getTransportControls().playFromMediaId( "0", extras );
            }


        }
    }

    private void initPagerAdapter( List<MediaSessionCompat.QueueItem> queue )
    {

        mPagerAdapter = new NowPlayingPagerAdapter(getSupportFragmentManager(), queue);
        mPager.setAdapter(mPagerAdapter);

        updatePagerWithCurrentSong();
    }

    public void updatePagerWithCurrentSong()
    {
        int position;
        PlaybackStateCompat playbackState;

        if (!mMediaBrowser.isConnected())
            return;

        playbackState = mMediaController.getPlaybackState();

        if (playbackState == null)
            return;

        position = (int)playbackState.getActiveQueueItemId();

        if (position != PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN && position >= 0 && position < mPagerAdapter.getCount())
        {
            mPager.setCurrentItem( position );
        }
    }

    public void updateRepeatIcon(Menu menu)
    {
        //Set correct icon for toggle repeat action
        MenuItem repeatItem = menu.findItem(R.id.toggle_repeat);
        if (repeatItem != null)
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            if (preferences.getBoolean(getString(R.string.pref_key_repeat),true))
                repeatItem.setIcon(R.drawable.ic_repeat_white_24dp);
            else
                repeatItem.setIcon(R.drawable.ic_repeat_gray_24dp);
        }
    }


    //Handle onscreen taps, change between play/pause
    @Override
    public void onClick(View v)
    {

        PlaybackStateCompat playbackState;
        int state;

        playbackState = mMediaController.getPlaybackState();
        state = playbackState.getState();


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
                Bundle bundle;
                int position;
                String mediaId;

                position = mPager.getCurrentItem();

                mediaId = mPagerAdapter.getData().get( position ).getDescription().getMediaId();

                bundle = new Bundle();
                bundle.putString( Const.SOURCE_KEY, mQueueSource );
                bundle.putString( Const.PARAMETER_KEY, mQueueParameter );
                bundle.putInt( Const.POSITION_KEY,  position);

                mMediaController.getTransportControls().playFromMediaId(mediaId, bundle );
                break;
        }


    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position)
    {
        if (!mMediaBrowser.isConnected() || mMediaController == null || mMediaController.getPlaybackState().getActiveQueueItemId() == position)
            return;

        //Play this position when user selects it
        mMediaController.getTransportControls().skipToQueueItem( mPager.getCurrentItem() );
    }

    @Override
    public void onPageScrollStateChanged(int state) {}
}
