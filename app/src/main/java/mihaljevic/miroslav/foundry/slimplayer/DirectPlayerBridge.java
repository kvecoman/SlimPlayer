package mihaljevic.miroslav.foundry.slimplayer;

/**
 * Created by miroslav on 11.04.17..
 *
 * Class which allows direct access to some media player architecture components for performance and ease of use
 * Mostly used for visualizer purposes
 *
 * @author Miroslav Mihaljević
 */



public class DirectPlayerBridge
{
    /*CustomMediaCodecAudioRenderer   audioRenderer;
    ExoPlayer                       exoPlayer;*/
    public Player player;
    //VisualizerGLRenderer            visualizerGLRenderer;

    public VisualizerGLSurfaceView         activeVisualizer;

    public DirectPlayerBridge( Player player )
    {
        this.player = player;
        /*this.visualizerGLRenderer   = visualizerGLRenderer;*/
    }

    public boolean isNotNull()
    {
        return player != null;
    }

    public void setActiveVisualizer( VisualizerGLSurfaceView visualizer )
    {
        //stopActiveVisualizer();

        activeVisualizer = visualizer;

        player.setBufferReceiver( activeVisualizer.getRenderer() );
    }

    public void enableActiveVisualizer()
    {
        if ( player != null )
            player.enableBufferProcessing();

        if ( activeVisualizer != null )
        {
            activeVisualizer.getRenderer().enable();
            activeVisualizer.getRenderer().disableClear();
        }
    }

    public void disableActiveVisualizer()
    {
        if ( player != null )
            player.disableBufferProcessing();

        if ( activeVisualizer != null )
        {
            activeVisualizer.getRenderer().enableClear();
            activeVisualizer.getRenderer().disable();
        }
    }


    public long getCurrentPlayerPosition()
    {
        return player.getCurrentPosition();
    }

}
