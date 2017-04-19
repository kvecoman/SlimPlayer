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

    public void switchVisualizationRenderer( VisualizerGLRenderer renderer )
    {
        CustomMediaCodecAudioRenderer.BufferReceiver    receiver;
        VisualizerGLRenderer                            oldRenderer;

        if ( audioRenderer == null )
            return;

        receiver = audioRenderer.getBufferReceiver();

        if ( receiver instanceof VisualizerGLRenderer )
        {
            oldRenderer = ( VisualizerGLRenderer ) receiver;

            if ( renderer == oldRenderer )
                return;

            oldRenderer.setEnabled( false );
            //oldRenderer.reset();
        }

        audioRenderer.setBufferReceiver( renderer );
    }

    public void stopActiveVisualizerRenderer()
    {
        CustomMediaCodecAudioRenderer.BufferReceiver    receiver;
        VisualizerGLRenderer                            renderer;

        if ( audioRenderer == null )
            return;

        receiver = audioRenderer.getBufferReceiver();

        if ( receiver instanceof VisualizerGLRenderer )
        {
            renderer = ( VisualizerGLRenderer ) receiver;

            renderer.setEnabled( false );
        }

        audioRenderer.setBufferReceiver( null );

    }
}
