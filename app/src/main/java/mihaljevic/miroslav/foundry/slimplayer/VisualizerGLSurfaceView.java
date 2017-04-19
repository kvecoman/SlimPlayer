package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by miroslav on 16.04.17..
 */



public class VisualizerGLSurfaceView extends GLSurfaceView
{
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
        mRenderer = new VisualizerGLRenderer();

        setDebugFlags( DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS );
        getHolder().setFormat( PixelFormat.TRANSLUCENT );
        setEGLConfigChooser( 8, 8, 8, 8, 16, 0 );
        setEGLContextClientVersion( 2 );
        setPreserveEGLContextOnPause( true ); //TODO - handle this preservation on pause???
        setZOrderOnTop( true );

        setRenderer( mRenderer );
        setRenderMode( GLSurfaceView.RENDERMODE_WHEN_DIRTY );
        onPause();
    }



    @Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        super.onSizeChanged( w, h, oldw, oldh );

        if ( w <= 0 || h <= 0 )
            return;

        surfaceChanged( getHolder(), PixelFormat.TRANSLUCENT, w, h );
    }



    public VisualizerGLRenderer getRenderer()
    {
        return mRenderer;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        mRenderer.setEnabled( true );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        mRenderer.setEnabled( false );
    }

    public void release()
    {
        /*mRenderer.scheduleRelease();
        surfaceChanged( getHolder(), PixelFormat.TRANSLUCENT, 0, 0 );*/

        onResume();
        //setRenderMode( RENDERMODE_WHEN_DIRTY );
        //mRenderer.setEnabled( false );
        //mRenderer.release();
        //requestRender();
        queueEvent( new Runnable()
        {
            @Override
            public void run()
            {
                mRenderer.release(); //TODO - continue here - not working
            }
        } );
        //onPause();




    }
}
