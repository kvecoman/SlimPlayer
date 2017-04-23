package mihaljevic.miroslav.foundry.slimplayer;

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

    public void stopActiveVisualizer()
    {
        /*if ( activeVisualizer != null )
        {
            activeVisualizer.disable();
        }*/

        audioRenderer.setBufferReceiver( null );

        /*if ( activeVisualizer != null )
        {
            activeVisualizer.queueEvent( new Runnable()
            {
                @Override
                public void run()
                {
                    activeVisualizer.getRenderer().releaseNVG();
                }
            } );

            activeVisualizer.onPause();
        }*/

    }

    public void enableActiveVisualizer()
    {
        if ( audioRenderer != null )
            audioRenderer.enableBufferProcessing();

        if ( activeVisualizer != null )
        {
            activeVisualizer.getRenderer().enable();

            //Enable in case we are paused
            activeVisualizer.onResume();
        }
    }

    public void disableActiveVisualizer()
    {
        if ( audioRenderer != null )
            audioRenderer.disableBufferProcessing();

        if ( activeVisualizer != null )
            activeVisualizer.getRenderer().disable();
    }

}
