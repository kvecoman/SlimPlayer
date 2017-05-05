package mihaljevic.miroslav.foundry.slimplayer;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
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
import java.io.IOException;

public class TestPlayerActivity /*extends AppCompatActivity implements Button.OnClickListener*/
{

/*
    protected final String TAG = getClass().getSimpleName();

    public static final String TEST_SONG_PATH           = "/storage/sdcard0/Samsung/Music/RELJA X COBY X STOJA - SAMO JAKO (OFFICIAL VIDEO) 4K.mp3";
    public static final String TEST_SONG_PATH_API_23    = "/storage/15FC-0502/Jelena Vuckovic feat DJ Vujo_91 - Led.mp3";



    private Button button;

    private MediaPlayer mMediaPlayer;

    private io.vov.vitamio.MediaPlayer mVitamioPlayer;




    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_test_player );

        if ( !io.vov.vitamio.LibsChecker.checkVitamioLibs(this) )
            return;

        button = ( Button ) findViewById( R.id.button );
        button.setOnClickListener( this );



        mVitamioPlayer = new io.vov.vitamio.MediaPlayer( this, true );
        try
        {
            mVitamioPlayer.setDataSource( this,  Uri.fromFile( new File( TEST_SONG_PATH ) ) );
            mVitamioPlayer.prepare();
            mVitamioPlayer.start();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }



    }


    @Override
    public void onClick( View v )
    {

        if ( mMediaPlayer != null )
        {
            if ( mMediaPlayer.isPlaying() )
                mMediaPlayer.stop();
            else
                mMediaPlayer.start();
        }
        else if ( mVitamioPlayer != null )
        {
            if ( mVitamioPlayer.isPlaying() )
                mVitamioPlayer.stop();
            else
                mVitamioPlayer.start();
        }
    }


*/

}
