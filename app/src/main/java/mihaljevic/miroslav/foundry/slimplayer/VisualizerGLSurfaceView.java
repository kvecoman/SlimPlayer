package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Created by miroslav on 16.04.17..
 *
 * View on which visualization will be drawn
 *
 * @author Miroslav Mihaljević
 */


public class VisualizerGLSurfaceView extends GLSurfaceView
{
    private final String TAG = getClass().getSimpleName();

    private VisualizerGLRenderer mRenderer;


    public VisualizerGLSurfaceView( Context context )
    {
        super( context );
        init();
    }

    public VisualizerGLSurfaceView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        init();
    }
    
    private void init()
    {
        if ( isInEditMode() )
            return;



        setDebugFlags( DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS );
        getHolder().setFormat( PixelFormat.TRANSLUCENT );
        setEGLConfigChooser( 8, 8, 8, 8, 16, 0 );
        setEGLContextClientVersion( 2 );
        setPreserveEGLContextOnPause( true );
        setZOrderOnTop( true );

        //queueEvent( new InitRunnable() );

        mRenderer = new VisualizerGLRenderer( SlimPlayerApplication.getInstance().getSelectedPlayerEngine() == Player.PLAYER_EXO_PLAYER );

        //NOTE - GL thread is created in setRenderer()
        setRenderer( mRenderer );
        setRenderMode( GLSurfaceView.RENDERMODE_WHEN_DIRTY );
        onPause();
    }


    public VisualizerGLRenderer getRenderer()
    {
        return mRenderer;
    }


    public void release()
    {
        Log.v( TAG,"release()" );
        onResume();
        mRenderer.disable();
        mRenderer.disableBufferProcessing();
        queueEvent( new ReleaseRunnable() );
    }

    @Override
    protected void onDraw( Canvas canvas )
    {
        if ( isInEditMode() )
            return;

        super.onDraw( canvas );
    }


    /**
     * Runnable that cleans up native instance of renderer, supposed to be run on GL thread
     */
    private class ReleaseRunnable implements Runnable
    {
        @Override
        public void run()
        {
            mRenderer.releaseNVG();
            mRenderer.release();
        }
    }
}
