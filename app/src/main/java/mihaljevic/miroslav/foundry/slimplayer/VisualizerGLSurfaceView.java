package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by miroslav on 16.04.17..
 */


public class VisualizerGLSurfaceView extends GLSurfaceView
{
    //private boolean mEnabled = false;

    private VisualizerGLRenderer mRenderer;

    /*private int mWidth;
    private int mHeight;*/

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



    /*@Override
    protected void onSizeChanged( int w, int h, int oldw, int oldh )
    {
        super.onSizeChanged( w, h, oldw, oldh );

        if ( w <= 0 || h <= 0 )
            return;

        mWidth = w;
        mHeight = h;

        //surfaceChanged( getHolder(), PixelFormat.TRANSLUCENT, w, h );
    }*/


    public VisualizerGLRenderer getRenderer()
    {
        return mRenderer;
    }

    /*public void enable()
    {
        mEnabled = true;
        onResume();
        mRenderer.enable();
        //TODO - this probably needs to be turned on
        surfaceChanged( getHolder(), PixelFormat.TRANSLUCENT, mWidth, mHeight );
    }*/

    /*public void disable()
    {
        mEnabled = false;
        mRenderer.disable();
        onPause();

    }*/

    public void release()
    {
        //disable();
        mRenderer.release();
    }
}
