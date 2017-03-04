package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.List;

/**
 *
 * Fragment that displays info about current song that is played by media service
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingFragment extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener{

    private final String TAG = getClass().getSimpleName();



    private int mPosition;

    private View mContentView;

    private MediaMetadataCompat mMetadata;

    //We assume this song has album art so we try to load it, and after loading we set this variable either to true or false for future loads
    private boolean mHasArt;


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
                mMediaController.registerCallback           ( mControllerCallbacks );

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

            Log.i(TAG, "Connection is suspended");
        }

        @Override
        public void onConnectionFailed()
        {
            super.onConnectionFailed();

            Log.e(TAG, "Connection has failed");
        }
    };

    private MediaControllerCompat.Callback mControllerCallbacks = new MediaControllerCompat.Callback()
    {};



    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG,"onCreate()");

        //Make sure that we get onCreateOptionsMenu() call
        setHasOptionsMenu( true );

        //Keep alive this fragment after configuration changes (so we can re-use data)
        setRetainInstance( true );

        //Assume this song has art, so we start loading process for first time
        mHasArt = true;

    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        Log.v(TAG,"onCreateView()");

        // Inflate the layout for this fragment
        mContentView = inflater.inflate( R.layout.fragment_now_playing, container, false );


        return mContentView;
    }

    //Here is usually place for most of the init
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG,"onActivityCreated()");

        Context context;


        context = getContext();


        //Handle taps on screen
        if ( context instanceof View.OnClickListener )
            mContentView.setOnClickListener( ( View.OnClickListener ) context );


        //OnGlobalLayout listener is little hack so we know that UI is already set up when we need to use it
        if ( mHasArt )
            mContentView.getViewTreeObserver().addOnGlobalLayoutListener( this );

        if ( mPosition == -1 )
            loadSongInfo();



        mMediaBrowser = new MediaBrowserCompat( getContext(), MediaPlayerService.COMPONENT_NAME, mConnectionCallbacks, null );

    }

    @Override
    public void onStart()
    {
        super.onStart();

        mMediaBrowser.connect();

    }


    @Override
    public void onGlobalLayout()
    {
        //We keep this listener for as long as we need until we get valid width and height values
        if ( mContentView == null || mContentView.getWidth() <= 0 || mContentView.getHeight() <= 0 )
            return;

        //Now that we have valid width and height values, display album art
        displayArtAsync();


        //Once we display art we don't need this callback anymore
        if ( Build.VERSION.SDK_INT >= 16 )
            mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        else
            mContentView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

    }



    @Override
    public void onStop()
    {
        super.onStop();
        Log.v(TAG,"onStop()");


        if ( mMediaController != null )
            mMediaController.unregisterCallback( mControllerCallbacks );

        if ( mMediaBrowser != null )
            mMediaBrowser.disconnect();


    }




    public void loadSongInfo()
    {
        //Bind song info to this fragment
        Log.v(TAG,"loadSongInfo()");

        Bundle args;

        args = getArguments();

        if (args == null)
            return;

        mPosition = args.getInt         ( Const.POSITION_KEY, -1);
        mMetadata = args.getParcelable  ( Const.METADATA_KEY );

        if ( mMetadata == null )
            return;

        //Update text views with new info
        ((TextView) mContentView.findViewById(R.id.song_title)).setText ( mMetadata.getString( MediaMetadataCompat.METADATA_KEY_TITLE ) );
        ((TextView) mContentView.findViewById(R.id.song_artist)).setText( mMetadata.getString( MediaMetadataCompat.METADATA_KEY_ARTIST ) );
    }


    //Get album art and display it (if it exists)
    private void displayArtAsync()
    {
        Log.v(TAG,"displayArtAsync()");

        if ( mContentView.getWidth() <= 0 || mContentView.getHeight() <= 0 || !mHasArt || !isAdded() )
            return;

        if ( mMetadata == null )
        {
            mHasArt = false;
            return;
        }

        String  mediaPath;
        int     width;
        int     height;


        mediaPath   = Uri.parse( mMetadata.getString( MediaMetadataCompat.METADATA_KEY_MEDIA_URI ) ).toString();
        width       = mContentView.getWidth();
        height      = mContentView.getHeight();

        //At this point assume it is false, but if we have it, it will be set to true
        mHasArt = false;

        Glide   .with       ( this)
                .load       ( new EmbeddedArtGlide( mediaPath ) )
                .asBitmap()
                .override   ( width, height )
                .centerCrop()
                .into( new SimpleTarget<Bitmap>()
                {
                    @Override
                    public void onResourceReady( Bitmap bitmap, GlideAnimation<? super Bitmap> glideAnimation )
                    {
                        if ( bitmap == null )
                        {
                            mHasArt = false;
                            return;
                        }

                        mHasArt = true;

                        if ( Build.VERSION.SDK_INT >= 16 )
                        {
                            mContentView.setBackground( new BitmapDrawable( getResources(), bitmap ) );
                        }
                        else
                        {
                            mContentView.setBackgroundDrawable( new BitmapDrawable( getResources(), bitmap ) );
                        }
                    }
                } );

    }

}
