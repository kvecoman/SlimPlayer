package mihaljevic.miroslav.foundry.slimplayer;

import android.net.Uri;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import java.io.File;
import java.io.IOException;

import io.vov.vitamio.MediaPlayer;

/**
 * Created by miroslav on 28.04.17..
 *
 * Front end for different players that might be used internally ( like android MediaPlayer, ExoPlayer or Vitamio framework )
 *
 * @author Miroslav Mihaljević
 */



public class Player
{
    protected final String TAG = getClass().getSimpleName();

    public static final int PLAYER_MEDIA_PLAYER = 1;
    public static final int PLAYER_VITAMIO_PLAYER = 2;
    public static final int PLAYER_EXO_PLAYER = 3;

    public static final int STATE_NONE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_STOPPED = 3;


    private int mState = STATE_NONE;

    private PlayerCallbacks mListener;

    private android.media.MediaPlayer   mMediaPlayer;
    private MediaPlayer                 mVitamioPlayer;

    private ExoPlayer                       mExoPlayer;
    private FileDataSourceFactory           mDataSourceFactory;
    private ExtractorsFactory               mExtractorsFactory;
    private CustomMediaCodecAudioRenderer   mCustomAudioRenderer;



    @IntDef ({PLAYER_MEDIA_PLAYER, PLAYER_VITAMIO_PLAYER, PLAYER_EXO_PLAYER})
    public @interface InternalPlayer{}

    public Player(){}


    public synchronized void initPlayer( @InternalPlayer int selectedPlayer )
    {

        release();

        switch ( selectedPlayer )
        {

            case PLAYER_MEDIA_PLAYER:
                mMediaPlayer = new android.media.MediaPlayer();
                mMediaPlayer.setOnCompletionListener( new MediaPlayerCallbacks() );
                break;
            case PLAYER_VITAMIO_PLAYER:
                mVitamioPlayer = new MediaPlayer( SlimPlayerApplication.getInstance(), true );
                mVitamioPlayer.setOnCompletionListener( new VitamioPlayerCallbacks() );
                break;
            case PLAYER_EXO_PLAYER:
                initExoPlayer();
                break;
        }
    }

    private synchronized void initExoPlayer()
    {
        TrackSelector                   trackSelector;
        LoadControl                     loadControl;
        Renderer[]                      renderers;

        mDataSourceFactory = new FileDataSourceFactory();
        mExtractorsFactory = new DefaultExtractorsFactory();

        mCustomAudioRenderer = new CustomMediaCodecAudioRenderer( MediaCodecSelector.DEFAULT );

        renderers       = new Renderer[] { mCustomAudioRenderer };
        trackSelector   = new DefaultTrackSelector();
        loadControl     = new DefaultLoadControl( new DefaultAllocator( true, 128 * 1000 ), 30000, 45000, 2500, 5000 );

        mExoPlayer = ExoPlayerFactory.newInstance( renderers, trackSelector, loadControl );

        /*mExoPlayer = ExoPlayerFactory
                .newSimpleInstance( this, trackSelector, loadControl, null, SimpleExoPlayer.EXTENSION_RENDERER_MODE_ON );*/

        mExoPlayer.addListener( new ExoPlayerCallbacks() );
        mExoPlayer.setPlayWhenReady( false );
    }

    public synchronized void prepareAudioSource( Uri uri ) throws IOException
    {
        if ( mMediaPlayer != null )
        {
            mState = STATE_PREPARING;
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource( SlimPlayerApplication.getInstance(), uri );
            mMediaPlayer.prepare();
        }
        else if ( mVitamioPlayer != null )
        {
            mState = STATE_PREPARING;
            mVitamioPlayer.stop();
            mVitamioPlayer.reset();
            mVitamioPlayer.setDataSource( SlimPlayerApplication.getInstance(),  uri );
            mVitamioPlayer.prepare();
        }
        else if ( mExoPlayer != null )
        {
            MediaSource mediaSource;

            mediaSource = new ExtractorMediaSource( uri, mDataSourceFactory, mExtractorsFactory, null, null );

            mState = STATE_PREPARING;
            mExoPlayer.stop();
            mExoPlayer.seekTo( 0L );
            mExoPlayer.prepare( mediaSource, true, true );
            mExoPlayer.setPlayWhenReady( false );

        }
    }

    public synchronized void play()
    {
        if ( mMediaPlayer != null )
        {
            mMediaPlayer.start();
            mState = STATE_PLAYING;
        }
        else if ( mVitamioPlayer != null )
        {
            mVitamioPlayer.start();
            mState = STATE_PLAYING;
        }
        else if ( mExoPlayer != null )
        {
            mExoPlayer.setPlayWhenReady( true );
            mState = STATE_PLAYING;
        }
    }

    public synchronized void pause()
    {
        if ( mMediaPlayer != null )
        {
            mMediaPlayer.pause();
        }
        else if ( mVitamioPlayer != null )
        {
            mVitamioPlayer.pause();
        }
        else if ( mExoPlayer != null )
        {
            mExoPlayer.setPlayWhenReady( false );
        }
    }

    public synchronized void stop()
    {
        if ( mMediaPlayer != null )
        {
            mMediaPlayer.stop();
        }
        else if ( mVitamioPlayer != null )
        {
            mVitamioPlayer.stop();
        }
        else if ( mExoPlayer != null )
        {
            mExoPlayer.stop();
        }
    }

    public synchronized long getCurrentPosition()
    {
        if ( mMediaPlayer != null )
        {
            return mMediaPlayer.getCurrentPosition();
        }
        else if ( mVitamioPlayer != null )
        {
            return mVitamioPlayer.getCurrentPosition();
        }
        else if ( mExoPlayer != null )
        {
            return mExoPlayer.getCurrentPosition();
        }

        return -1;
    }

    public synchronized long getDuration()
    {
        if ( mMediaPlayer != null )
        {
            return mMediaPlayer.getDuration();
        }
        else if ( mVitamioPlayer != null )
        {
            return mVitamioPlayer.getDuration();
        }
        else if ( mExoPlayer != null )
        {
            return mExoPlayer.getDuration();
        }

        return -1;
    }

    public synchronized void seekTo( long position )
    {
        if ( mMediaPlayer != null )
        {
            mMediaPlayer.seekTo( (int) position );
        }
        else if ( mVitamioPlayer != null )
        {
            mVitamioPlayer.seekTo( position );
        }
        else if ( mExoPlayer != null )
        {
            mExoPlayer.seekTo( position );
        }
    }

    public synchronized void release()
    {
        if ( mMediaPlayer != null )
        {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if ( mVitamioPlayer != null )
        {
            mVitamioPlayer.stop();
            mVitamioPlayer.release();
            mVitamioPlayer = null;
        }
        if ( mExoPlayer != null )
        {
            mExoPlayer.setPlayWhenReady( false );
            mExoPlayer.stop();
            mExoPlayer.release();
            mExoPlayer = null;
            mDataSourceFactory = null;
            mExtractorsFactory = null;
        }
    }


    public void setCallbacksListener( PlayerCallbacks listener )
    {
        this.mListener = listener;
    }

    public void setBufferReceiver( CustomMediaCodecAudioRenderer.BufferReceiver bufferReceiver )
    {
        if ( mExoPlayer != null && mCustomAudioRenderer != null )
        {
            mCustomAudioRenderer.setBufferReceiver( bufferReceiver );
        }
    }

    public void enableBufferProcessing()
    {
        if ( mExoPlayer != null && mCustomAudioRenderer != null )
        {
            mCustomAudioRenderer.enableBufferProcessing();
        }
    }

    public void disableBufferProcessing()
    {
        if ( mExoPlayer != null && mCustomAudioRenderer != null )
        {
            mCustomAudioRenderer.disableBufferProcessing();
        }
    }




    public interface PlayerCallbacks
    {
        void onCompletion();
    }



    private class MediaPlayerCallbacks implements android.media.MediaPlayer.OnCompletionListener
    {
        @Override
        public void onCompletion( android.media.MediaPlayer mp )
        {
            mState = STATE_STOPPED;
            if ( mListener != null )
                mListener.onCompletion();
        }
    }

    private class VitamioPlayerCallbacks implements MediaPlayer.OnCompletionListener
    {
        @Override
        public void onCompletion( MediaPlayer mp )
        {
            mState = STATE_STOPPED;
            if ( mListener != null )
                mListener.onCompletion();
        }
    }

    private class ExoPlayerCallbacks implements ExoPlayer.EventListener
    {
        @Override
        public void onLoadingChanged( boolean isLoading )
        {

        }

        @Override
        public void onTimelineChanged( Timeline timeline, Object manifest )
        {

        }

        @Override
        public void onTracksChanged( TrackGroupArray trackGroups, TrackSelectionArray trackSelections )
        {

        }

        @Override
        public void onPlayerStateChanged( boolean playWhenReady, int playbackState )
        {
            synchronized ( Player.this )
            {
                if ( playbackState == ExoPlayer.STATE_ENDED && playWhenReady && mState == STATE_PLAYING )
                {
                    mState = STATE_STOPPED;

                    if ( mListener != null )
                    {
                        Log.v(TAG, "calling onCompletionListener");
                        mListener.onCompletion();
                    }
                }
            }


        }

        @Override
        public void onPlayerError( ExoPlaybackException error )
        {
            error.printStackTrace();
        }

        @Override
        public void onPositionDiscontinuity()
        {

        }
    }







}
