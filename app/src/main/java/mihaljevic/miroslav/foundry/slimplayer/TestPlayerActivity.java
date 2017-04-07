package mihaljevic.miroslav.foundry.slimplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.v4.app.ActivityManagerCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestPlayerActivity extends AppCompatActivity implements Button.OnClickListener
{

    private static boolean dbgFullscreenGLES = false;

    protected final String TAG = getClass().getSimpleName();

    public static final String TEST_SONG_PATH           = "/storage/sdcard0/Samsung/Music/Jelena VuÄŤkoviÄ‡ feat DJ Vujo_91 - Led.mp3";
    public static final String TEST_SONG_PATH_API_23    = "/storage/15FC-0502/Jelena Vuckovic feat DJ Vujo_91 - Led.mp3";

    //This number of samples represent below defined time span
    public static final int VISUALIZATION_SAMPLES = 200;

    //How much time does visualization represent ( in ms )
    public static final int VISUALIZATION_TIME_SPAN = 500;


    private Button button;

    private ExoPlayer exoPlayer;

    private VisualizerView  mVisualizerView;

    MediaCodecAudioRenderer mAudioRenderer;

    private AudioBufferManager mAudioBufferManager;



    private GLSurfaceView mGLSurfaceView;

    private GLES20Renderer mGLES20Renderer;






    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test_player );

        button = ( Button ) findViewById( R.id.button );
        button.setOnClickListener( this );

        initExoPlayer();

        mAudioBufferManager = new AudioBufferManager( mAudioRenderer, VISUALIZATION_SAMPLES, VISUALIZATION_TIME_SPAN );

        /*mVisualizerView = ( VisualizerView ) findViewById( R.id.visualizer );
        mVisualizerView.setAudioBufferManager( mAudioBufferManager );*/



        if ( hasGLES20() )
        {
            mGLES20Renderer = new GLES20Renderer();
            mGLES20Renderer.setAudioBufferManager( mAudioBufferManager );

            mGLSurfaceView = new GLSurfaceView( this );
            mGLSurfaceView.setEGLConfigChooser( 8, 8, 8, 8, 16, 0 );
            mGLSurfaceView.setEGLContextClientVersion( 2 );
            mGLSurfaceView.setPreserveEGLContextOnPause( true );
            mGLSurfaceView.setRenderer( mGLES20Renderer );
            mGLSurfaceView.setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY );
        }
        else
        {
            Log.w( TAG, "GLES 2.0 not supported" );
        }



        ( ( CustomMediaCodecAudioRenderer ) mAudioRenderer ).setAudioBufferManager( mAudioBufferManager );

        //GLES2.0 code
        RelativeLayout.LayoutParams layoutParams;
        ViewGroup viewGroup;

        layoutParams = new RelativeLayout.LayoutParams( 200, 200 );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE );

        viewGroup = ( ViewGroup ) findViewById( R.id.activity_test_player );

        viewGroup.addView( mGLSurfaceView, 1, layoutParams);

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mGLSurfaceView.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        exoPlayer.release();

        /*mVisualizerView.disableUpdate();
        mVisualizerView.release();
        mVisualizerView = null;*/

        mAudioBufferManager.release();

        mGLES20Renderer.release();

    }



    public void initExoPlayer()
    {
        TrackSelector       trackSelector;
        LoadControl         loadControl;
        MediaSource         mediaSource;
        String              filePath;
        File                file;
        Uri                 fileURI;
        DataSource.Factory  dataSourceFactory;
        ExtractorsFactory   extractorsFactory;
        Renderer            audioRenderer;
        Renderer[]          renderers;
        MediaCodecSelector  mediaCodecSelector;

        filePath    = Build.VERSION.SDK_INT >= 22 ? TEST_SONG_PATH_API_23 : TEST_SONG_PATH;
        file        = new File( filePath );
        fileURI     = Uri.fromFile( file );

        dataSourceFactory = new FileDataSourceFactory();
        //extractorsFactory = new AudioExtractorsFactory();
        extractorsFactory = new DefaultExtractorsFactory();

        mediaCodecSelector = MediaCodecSelector.DEFAULT;


        audioRenderer   = new CustomMediaCodecAudioRenderer( mediaCodecSelector, null );
        renderers       = new Renderer[] { audioRenderer };

        trackSelector   = new DefaultTrackSelector();
        loadControl     = new DefaultLoadControl();
        mediaSource     = new ExtractorMediaSource( fileURI, dataSourceFactory, extractorsFactory, null, null );

        exoPlayer = ExoPlayerFactory.newInstance( renderers, trackSelector, loadControl );

        exoPlayer.setPlayWhenReady  ( false );
        exoPlayer.prepare           ( mediaSource );

        mAudioRenderer = ( MediaCodecAudioRenderer ) audioRenderer;
    }


    private boolean hasGLES20()
    {
        ActivityManager     activityManager;
        ConfigurationInfo   configurationInfo;

        activityManager = ( ActivityManager ) getSystemService( Context.ACTIVITY_SERVICE );

        configurationInfo = activityManager.getDeviceConfigurationInfo();

        return configurationInfo.reqGlEsVersion >= 0x20000;

    }

    private boolean playPause()
    {
        boolean playing;

        playing = exoPlayer.getPlayWhenReady();

        exoPlayer.setPlayWhenReady( !playing );

        return !playing;

    }



    @Override
    public void onClick( View v )
    {
        boolean playing;

        playing = playPause();

        if ( playing )
        {
            button.setText( "Pause" );
            //mVisualizerView.enableUpdate();
        }
        else
        {
            button.setText( "Play" );
            //mVisualizerView.disableUpdate();
        }

    }




    /*public static class AudioExtractorsFactory implements ExtractorsFactory
    {
        private static List< Class< ? extends Extractor > > audioExtractorClasses;

        public AudioExtractorsFactory()
        {
            synchronized ( AudioExtractorsFactory.class )
            {

                if ( audioExtractorClasses == null )
                {
                    List< Class< ? extends Extractor > > extractorClasses = new ArrayList<>();

                    try
                    {
                        extractorClasses.add(
                                Class.forName( "com.google.android.exoplayer2.extractor.mp3.Mp3Extractor" )
                                     .asSubclass( Extractor.class ) );
                    } catch ( ClassNotFoundException e )
                    {
                        // Extractor not found.
                    }

                    try
                    {
                        extractorClasses.add(
                                Class.forName( "com.google.android.exoplayer2.extractor.ts.Ac3Extractor" )
                                     .asSubclass( Extractor.class ) );
                    } catch ( ClassNotFoundException e )
                    {
                        // Extractor not found.
                    }

                    try
                    {
                        extractorClasses.add(
                                Class.forName( "com.google.android.exoplayer2.extractor.ogg.OggExtractor" )
                                     .asSubclass( Extractor.class ) );
                    } catch ( ClassNotFoundException e )
                    {
                        // Extractor not found.
                    }

                    try
                    {
                        extractorClasses.add(
                                Class.forName( "com.google.android.exoplayer2.extractor.wav.WavExtractor" )
                                     .asSubclass( Extractor.class ) );
                    } catch ( ClassNotFoundException e )
                    {
                        // Extractor not found.
                    }

                    audioExtractorClasses = extractorClasses;
                }
            }
        }

        @Override
        public Extractor[] createExtractors()
        {
            Extractor[] extractors;

            extractors = new Extractor[ audioExtractorClasses.size() ];

            for ( int i = 0; i < extractors.length; i++ )
            {
                try
                {
                    extractors[ i ] = audioExtractorClasses.get( i ).getConstructor().newInstance();
                }
                catch ( Exception e )
                {
                    // Should never happen.
                    throw new IllegalStateException( "Unexpected error creating default extractor", e );
                }
            }
            return extractors;
        }
    }*/




}
