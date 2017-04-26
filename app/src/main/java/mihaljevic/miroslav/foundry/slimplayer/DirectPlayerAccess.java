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
    CustomMediaCodecAudioRenderer   audioRenderer;
    ExoPlayer                       exoPlayer;
    //VisualizerGLRenderer            visualizerGLRenderer;

    VisualizerGLSurfaceView         activeVisualizer;

    public DirectPlayerAccess( CustomMediaCodecAudioRenderer audioRenderer, ExoPlayer exoPlayer/*, VisualizerGLRenderer visualizerGLRenderer*/ )
    {
        this.audioRenderer          = audioRenderer;
        this.exoPlayer              = exoPlayer;
        /*this.visualizerGLRenderer   = visualizerGLRenderer;*/
    }

    public boolean isNotNull()
    {
        return audioRenderer != null && exoPlayer != null/* && visualizerGLRenderer != null*/;
    }

    public void setActiveVisualizer( VisualizerGLSurfaceView visualizer )
    {
        //stopActiveVisualizer();

        activeVisualizer = visualizer;

        audioRenderer.setBufferReceiver( activeVisualizer.getRenderer() );
    }

    public void enableActiveVisualizer()
    {
        if ( audioRenderer != null )
            audioRenderer.enableBufferProcessing();

        if ( activeVisualizer != null )
        {
            activeVisualizer.getRenderer().enable();
            activeVisualizer.getRenderer().disableClear();

            //Enable in case we are paused
            //activeVisualizer.onResume();
        }
    }

    public void disableActiveVisualizer()
    {
        if ( audioRenderer != null )
            audioRenderer.disableBufferProcessing();

        if ( activeVisualizer != null )
        {
            activeVisualizer.getRenderer().enableClear();
            activeVisualizer.getRenderer().disable();
        }
    }

}
