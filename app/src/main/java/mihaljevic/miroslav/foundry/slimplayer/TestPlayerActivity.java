package mihaljevic.miroslav.foundry.slimplayer;

import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
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




    /*static{
        System.loadLibrary( "hello-jnicpp" );
    }



    public native String helloFromTheOtherSide();*/






    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test_player );

        button = ( Button ) findViewById( R.id.button );
        button.setOnClickListener( this );

        mVisualizerView = ( VisualizerView ) findViewById( R.id.visualizer );

        initExoPlayer();

        mAudioBufferManager = new AudioBufferManager( mAudioRenderer, VISUALIZATION_SAMPLES, VISUALIZATION_TIME_SPAN );

        mVisualizerView.setAudioBufferManager( mAudioBufferManager );

        ( ( CustomMediaCodecAudioRenderer ) mAudioRenderer ).setAudioBufferManager( mAudioBufferManager );

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        exoPlayer.release();

        mVisualizerView.disableUpdate();

        mVisualizerView = null;

        mAudioBufferManager.release();


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
        extractorsFactory = new AudioExtractorsFactory();

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




    @Override
    public void onClick( View v )
    {
        String buttonText;

        buttonText = ( String ) button.getText();

        if ( buttonText.equals( "Play" ) )
        {
            exoPlayer.setPlayWhenReady( true );
            button.setText( "Pause" );
            mVisualizerView.enableUpdate();


        }
        else
        {
            exoPlayer.setPlayWhenReady( false );
            button.setText( "Play" );
            mVisualizerView.disableUpdate();

        }

        //Utils.toastShort( helloFromTheOtherSide() );
    }




    public static class AudioExtractorsFactory implements ExtractorsFactory
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
    }




}
