package mihaljevic.miroslav.foundry.slimplayer;

import android.Manifest;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
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


import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Created by miroslav on 28.04.17..
 *
 * Front end for different players that might be used internally ( like android MediaPlayer, ExoPlayer or Vitamio framework )
 *
 * @author Miroslav Mihaljević
 */



public class Player implements Visualizer.OnDataCaptureListener
{
    protected final String TAG = getClass().getSimpleName();

    //NOTE - these values need to be in sync with values used in settings
    public static final int PLAYER_MEDIA_PLAYER = 1;
    public static final int PLAYER_VITAMIO_PLAYER = 2; //Dropped support for this one because it requires activity for init and doesn't have better performance than exo player
    public static final int PLAYER_EXO_PLAYER = 3;

    public static final int STATE_NONE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_STOPPED = 3;


    private int mState = STATE_NONE;

    private PlayerCallbacks mListener;

    private android.media.MediaPlayer                       mMediaPlayer;
    //private MediaPlayer                                     mVitamioPlayer;
    private Visualizer                                      mVisualizer;
    private BufferReceiver    mVisualizerBufferReceiver;

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
                Log.i(TAG, "Initializing android media player");
                mMediaPlayer = new android.media.MediaPlayer();
                mMediaPlayer.setOnCompletionListener( new MediaPlayerCallbacks() );
                initMediaVisualizer();
                break;
            /*case PLAYER_VITAMIO_PLAYER:
                Log.i(TAG, "Initializing vitamio framework player");
                if ( !io.vov.vitamio.LibsChecker.checkVitamioLibs( SlimPlayerApplication.getInstance() ) ) //This requires activity :(, droping support for vitamio
                    return;
                mVitamioPlayer = new MediaPlayer( SlimPlayerApplication.getInstance(), true );
                mVitamioPlayer.setOnCompletionListener( new VitamioPlayerCallbacks() );
                mVisualizer = new Visualizer( 0  );
                initMediaVisualizer();
                break;*/
            case PLAYER_EXO_PLAYER:
                Log.i(TAG, "Initializing exo player");
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

    private synchronized void initMediaVisualizer()
    {
        if ( !Utils.checkPermission( Manifest.permission.RECORD_AUDIO ) || !Utils.isVisualizerEnabled() || mVisualizer != null )
            return;


        mVisualizer = new Visualizer( mMediaPlayer.getAudioSessionId() );

        //TODO - see whats up with scaling mode, it is always "as played" on Galaxy S2
        mVisualizer.setCaptureSize( Visualizer.getCaptureSizeRange()[1] );
        if ( Build.VERSION.SDK_INT >= 16 )
            mVisualizer.setScalingMode( Visualizer.SCALING_MODE_NORMALIZED );

        mVisualizer.setDataCaptureListener( this, Visualizer.getMaxCaptureRate(), true, false );
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

            if ( mVisualizer != null )
            {
                mVisualizer.setEnabled( false );
                mVisualizer.release();
            }

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

    public void setBufferReceiver( BufferReceiver bufferReceiver )
    {
        if ( mMediaPlayer != null )
        {
            mVisualizerBufferReceiver = bufferReceiver;
        }
        else if ( mExoPlayer != null && mCustomAudioRenderer != null )
        {
            mCustomAudioRenderer.setBufferReceiver( bufferReceiver );
        }
    }

    public void enableBufferProcessing()
    {
        if ( mMediaPlayer != null && mVisualizer != null )
        {
            mVisualizer.setEnabled( true );
        }
        else if ( mExoPlayer != null && mCustomAudioRenderer != null )
        {
            mCustomAudioRenderer.enableBufferProcessing();
        }
    }

    public void disableBufferProcessing()
    {
        if ( mMediaPlayer != null && mVisualizer != null )
        {
            mVisualizer.setEnabled( false );
        }
        else if ( mExoPlayer != null && mCustomAudioRenderer != null )
        {
            mCustomAudioRenderer.disableBufferProcessing();
        }
    }


    @Override
    public void onFftDataCapture( Visualizer visualizer, byte[] fft, int samplingRate )
    {

    }

    @Override
    public void onWaveFormDataCapture( Visualizer visualizer, byte[] waveform, int samplingRate )
    {
        if ( mVisualizerBufferReceiver != null )
            mVisualizerBufferReceiver.processBufferArray(  waveform, waveform.length, -1, 1, samplingRate, -1 );
    }

    interface BufferReceiver
    {
        void processBuffer( ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );

        void processBufferArray( byte[] samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );
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
