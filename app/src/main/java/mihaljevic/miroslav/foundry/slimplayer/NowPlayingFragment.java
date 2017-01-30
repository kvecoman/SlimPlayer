package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

/**
 *
 * Fragment that displays info about current song that is played by media service
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, ViewTreeObserver.OnGlobalLayoutListener{

    private final String TAG = getClass().getSimpleName();


    private Context mContext;

    private int mPosition;

    private View    mContentView;
    private SeekBar mSeekBar;

    private boolean mAlbumArtDisplayed = false;
    private boolean mOnGlobalLayoutCalled = false;
    private Bitmap  mAlbumArt;


    protected MediaBrowserCompat    mMediaBrowser;
    protected MediaControllerCompat mMediaController;

    protected MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback()
    {
        @Override
        public void onConnected()
        {

            try
            {
                mMediaController = new MediaControllerCompat( getContext(), mMediaBrowser.getSessionToken() );
                mMediaController.registerCallback( mControllerCallbacks );
            }
            catch (RemoteException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConnectionSuspended()
        {}

        @Override
        public void onConnectionFailed()
        {}
    };

    private MediaControllerCompat.Callback mControllerCallbacks = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if (state.getState() == PlaybackStateCompat.STATE_PLAYING || state.getState() == PlaybackStateCompat.STATE_PAUSED)
            {
                //Update this seek bar
                if (state.getActiveQueueItemId() == mPosition)
                {
                    mSeekBar.setProgress( (int)state.getPosition() );
                }
                else
                {
                    //If this is not active fragment, then set seek bar to 0
                    mSeekBar.setProgress( 0 );
                }
            }
        }
    };



    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG,"onCreate()");

        //Make sure that we get onCreateOptionsMenu() call
        setHasOptionsMenu(true);

        //Keep alive this fragment after configuration changes (so we can re-use data)
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG,"onCreateView()");

        // Inflate the layout for this fragment
        mContentView = inflater.inflate(R.layout.fragment_now_playing, container, false);
        return mContentView;
    }

    //Here is usually place for most of the init
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.v(TAG,"onActivityCreated()");

        mContext = getContext();

        mSeekBar = (SeekBar) mContentView.findViewById(R.id.seek_bar);
        mSeekBar.setProgress(0);
        mSeekBar.setOnSeekBarChangeListener(this);


        //Handle taps on screen
        if (mContext instanceof View.OnClickListener)
            mContentView.setOnClickListener((View.OnClickListener)mContext);


        //Load album art if it is not loaded already (we do this here after we get MediaPlayerService with registering listener)
        if (mAlbumArt == null)
            loadArtAsync();

        //Little hack so we know that UI is already set up when we need to use it
        mContentView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        mMediaBrowser = new MediaBrowserCompat( getContext(), new ComponentName( getContext(), MediaPlayerService.class ), mConnectionCallbacks, null );

        loadSongInfo();
    }

    @Override
    public void onStart() {
        super.onStart();

        mMediaBrowser.connect();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG,"onResume()");

    }


    @Override
    public void onGlobalLayout()
    {
        //We keep this listener for as long as we need until we get valid width and height values
        if (mContentView == null || mContentView.getWidth() <= 0 || mContentView.getHeight() <= 0)
            return;


        mOnGlobalLayoutCalled = true;

        if (Build.VERSION.SDK_INT >= 16)
            mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        else
            mContentView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

        //This will crop and display album art
        if (!mAlbumArtDisplayed && mAlbumArt != null && mOnGlobalLayoutCalled)
            displayArtAsync();

    }



    @Override
    public void onStop() {
        super.onStop();
        Log.v(TAG,"onStop()");


        if (mMediaController != null)
            mMediaController.unregisterCallback( mControllerCallbacks );

        if (mMediaBrowser != null)
            mMediaBrowser.disconnect();


    }

    //Here we do cleanup of things expecting hosting activity will be recreated, so we don't want to leak anything
    @Override
    public void onDetach() {
        super.onDetach();

        mContext = null;
        mContentView = null;
        mAlbumArtDisplayed = false;
        mOnGlobalLayoutCalled = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"onDestroy()");


    }


    public void loadSongInfo()
    {
        //Bind song info to this fragment
        Log.v(TAG,"loadSongInfo()");

        Bundle args;
        MediaMetadataCompat metadata;

        args = getArguments();

        if (args == null)
            return;

        metadata = args.getParcelable( Const.METADATA_KEY );

        if (metadata == null)
            return;

        mPosition = args.getInt(Const.POSITION_KEY, -1);


        mSeekBar.setMax((int)metadata.getLong( MediaMetadataCompat.METADATA_KEY_DURATION ));

        //Update text views with new info
        ((TextView) mContentView.findViewById(R.id.song_title)).setText(metadata.getString( MediaMetadataCompat.METADATA_KEY_TITLE ));
        ((TextView) mContentView.findViewById(R.id.song_artist)).setText(metadata.getString( MediaMetadataCompat.METADATA_KEY_ARTIST ));

    }

    //Tries to load album art if it exist (with async task), returns true if task is going to be executed
    public void loadArtAsync()
    {
        Log.v(TAG,"loadArtAsync()");


        new AsyncTask<Void,Void,Void>()
        {

            @Override
            protected Void doInBackground(Void... params)
            {

                /*if (mPlayerService == null)
                    return null;*/

                //TODO - fix, uncomment
                //Load art
                //mAlbumArt = mPlayerService.getSongs().getArt(mPosition);

                return null;

            }

            @Override
            protected void onPostExecute(Void aVoid) {

                //This will crop and display album art
                if (!mAlbumArtDisplayed && mAlbumArt != null && mOnGlobalLayoutCalled)
                    displayArtAsync();
            }
        }.execute();
    }

    private void displayArtAsync()
    {
        Log.v(TAG,"displayArtAsync()");

        if (mContentView.getWidth() <= 0 || mContentView.getHeight() <= 0)
            return;

        final float viewRatio = (float)mContentView.getWidth() / (float)mContentView.getHeight();

        new AsyncTask<Void,Void,Bitmap>(){
            @Override
            protected Bitmap doInBackground(Void... params) {
                //We crop album art so it fits screen(s)
                return Utils.cropBitmapToRatio(mAlbumArt,viewRatio);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null)
                    return;

                //Update background with album art if it exist
                if(Build.VERSION.SDK_INT >= 16) {
                    mContentView.setBackground(new BitmapDrawable(getResources(),bitmap));
                }
                else
                {
                    mContentView.setBackgroundDrawable(new BitmapDrawable(getResources(),bitmap));
                }

                mAlbumArtDisplayed = true;
            }

        }.execute();
    }



    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (!mMediaBrowser.isConnected())
            return;

        long actions = mMediaController.getPlaybackState().getActions();

        //Only if touch is coming from user then seek song (and that action is available)
        if (fromUser && (actions & PlaybackStateCompat.ACTION_SEEK_TO) == PlaybackStateCompat.ACTION_SEEK_TO)
        {
            Log.v(TAG,"onProgressChanged() - user changed progress");

            mMediaController.getTransportControls().seekTo( progress );
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
