package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by miroslav on 16.04.17..
 */

//TODO - continue here - things are mostly okay except forr phantom calls on totally crazy instance numbers for getSamples()... Now handle rotation and processor consumption

public class VisualizerGLSurfaceView extends GLSurfaceView
{
    private boolean mEnabled = false;

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
        setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY );
        onPause();
    }



    /*@Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        super.onSizeChanged( w, h, oldw, oldh );

        if ( w <= 0 || h <= 0 )
            return;

        surfaceChanged( getHolder(), PixelFormat.TRANSLUCENT, w, h );
    }*/



    public VisualizerGLRenderer getRenderer()
    {
        return mRenderer;
    }

    public void enable()
    {
        mEnabled = true;
        onResume();
        mRenderer.enable();
    }

    public void disable()
    {
        mEnabled = false;
        mRenderer.disable();
        onPause();

    }

    public void release()
    {
        mEnabled = false;
        mRenderer.disable();

        onResume();
        queueEvent( new Runnable()
        {
            @Override
            public void run()
            {
                mRenderer.release();
            }
        } );


    }
}
