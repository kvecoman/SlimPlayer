package mihaljevic.miroslav.foundry.slimplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import java.io.File;

public class TestPlayerActivity extends AppCompatActivity implements Button.OnClickListener
{


    protected final String TAG = getClass().getSimpleName();

    public static final String TEST_SONG_PATH           = "/storage/sdcard0/Samsung/Music/RELJA POPOVIC - LOM (OFFICIAL VIDEO).mp3";
    public static final String TEST_SONG_PATH_API_23    = "/storage/15FC-0502/Jelena Vuckovic feat DJ Vujo_91 - Led.mp3";



    private Button button;

    private ExoPlayer exoPlayer;


    MediaCodecAudioRenderer mAudioRenderer;




    private GLSurfaceView mGLSurfaceView;

    private VisualizerGLRenderer mVisualizerGLRenderer;






    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test_player );

        button = ( Button ) findViewById( R.id.button );
        button.setOnClickListener( this );

        initExoPlayer();




        if ( hasGLES20() )
        {
            mVisualizerGLRenderer = new VisualizerGLRenderer();

            mGLSurfaceView = new GLSurfaceView( this );
            //mGLSurfaceView = ( GLSurfaceView ) findViewById( R.id.visualizer );

            mGLSurfaceView.setEGLConfigChooser( 8, 8, 8, 8, 16, 0 );
            mGLSurfaceView.setEGLContextClientVersion( 2 );
            mGLSurfaceView.setPreserveEGLContextOnPause( true ); //TODO - handle this preservation on pause???
            mGLSurfaceView.setRenderer( mVisualizerGLRenderer );
            mGLSurfaceView.setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY );
        }
        else
        {
            Log.w( TAG, "GLES 2.0 not supported" );
        }



        ( ( CustomMediaCodecAudioRenderer ) mAudioRenderer ).setBufferReceiver( mVisualizerGLRenderer );

        //GLES2.0 code
        RelativeLayout.LayoutParams   layoutParams;
        ViewGroup                       viewGroup;

        layoutParams = new RelativeLayout.LayoutParams( 200, 200 );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE );

        viewGroup = ( ViewGroup ) findViewById( R.id.activity_test_player );

        viewGroup.addView( mGLSurfaceView, 0, layoutParams );

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


        mVisualizerGLRenderer.release();

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
        extractorsFactory = new DefaultExtractorsFactory();

        mediaCodecSelector = MediaCodecSelector.DEFAULT;


        audioRenderer   = new CustomMediaCodecAudioRenderer( mediaCodecSelector );
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
            ( ( CustomMediaCodecAudioRenderer ) mAudioRenderer ).setBufferProcessing( true );
            mGLSurfaceView.setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        }
        else
        {
            button.setText( "Play" );
            ( ( CustomMediaCodecAudioRenderer ) mAudioRenderer ).setBufferProcessing( false );
            mGLSurfaceView.setRenderMode( GLSurfaceView.RENDERMODE_WHEN_DIRTY );

        }

    }




}
