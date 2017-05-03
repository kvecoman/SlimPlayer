package mihaljevic.miroslav.foundry.slimplayer;

import android.opengl.GLSurfaceView;

import com.google.android.exoplayer2.ExoPlayer;

/**
 * Created by miroslav on 11.04.17..
 *
 * Class which allows direct access to some media player architecture components for performance and ease of use
 */

public class DirectPlayerAccess
{
    /*CustomMediaCodecAudioRenderer   audioRenderer;
    ExoPlayer                       exoPlayer;*/
    Player mPlayer;
    //VisualizerGLRenderer            visualizerGLRenderer;

    VisualizerGLSurfaceView         activeVisualizer;

    public DirectPlayerAccess( Player player )
    {
        mPlayer = player;
        /*this.visualizerGLRenderer   = visualizerGLRenderer;*/
    }

    public boolean isNotNull()
    {
        return mPlayer != null;
    }

    public void setActiveVisualizer( VisualizerGLSurfaceView visualizer )
    {
        //stopActiveVisualizer();

        activeVisualizer = visualizer;

        mPlayer.setBufferReceiver( activeVisualizer.getRenderer() );
    }

    public void enableActiveVisualizer()
    {
        if ( mPlayer != null )
            mPlayer.enableBufferProcessing();

        if ( activeVisualizer != null )
        {
            activeVisualizer.getRenderer().enable();
            activeVisualizer.getRenderer().disableClear();

            //Enable in case we are paused
            //activeVisualizer.onResume(); //TODO - maybe this can be commented, it is not used for screen rotation, and not for page switching
        }
    }

    public void disableActiveVisualizer()
    {
        if ( mPlayer != null )
            mPlayer.disableBufferProcessing();

        if ( activeVisualizer != null )
        {
            activeVisualizer.getRenderer().enableClear();
            activeVisualizer.getRenderer().disable();
        }
    }


    public long getCurrentPlayerPosition()
    {
        return mPlayer.getCurrentPosition();
    }

}
