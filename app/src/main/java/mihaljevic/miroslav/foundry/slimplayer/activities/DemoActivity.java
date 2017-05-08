package mihaljevic.miroslav.foundry.slimplayer.activities;

import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import mihaljevic.miroslav.foundry.slimplayer.R;

public class DemoActivity extends AppCompatActivity implements View.OnClickListener
{

    private GLSurfaceView mGLSurfaceView;
    private Renderer mRenderer;

    private boolean mCreated;
    private boolean mDestroyed;

    private Handler mRenderHandler = new Handler(  );
    private Runnable mRenderRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if ( !mCreated || mDestroyed || mRenderer == null )
                return;

            mGLSurfaceView.requestRender();

            mGLSurfaceView.postDelayed( this, 2000 );
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_demo );

        RelativeLayout.LayoutParams     layoutParams;

        mGLSurfaceView  = new GLSurfaceView( this );


        mGLSurfaceView.getHolder().setFormat( PixelFormat.TRANSLUCENT );
        mGLSurfaceView.setEGLConfigChooser( 8, 8, 8, 8, 16, 0 );
        mGLSurfaceView.setEGLContextClientVersion( 2 );
        mGLSurfaceView.setPreserveEGLContextOnPause( true );
        mGLSurfaceView.setZOrderOnTop( true );

        mRenderer = new Renderer();
        mGLSurfaceView.setRenderer( mRenderer );
        mGLSurfaceView.setRenderMode( GLSurfaceView.RENDERMODE_WHEN_DIRTY );
        mGLSurfaceView.onPause();

        mGLSurfaceView.setOnClickListener( this );

        layoutParams = new RelativeLayout.LayoutParams( 50, 200 );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE );
        layoutParams.addRule( RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE );

        if ( Build.VERSION.SDK_INT >= 17 )
        {
            layoutParams.addRule( RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE );
            layoutParams.addRule( RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE );
        }

        ( ( ViewGroup ) findViewById( R.id.activity_demo ) ).addView( mGLSurfaceView, 0, layoutParams );
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if ( mGLSurfaceView != null )
            mGLSurfaceView.onPause();

        if ( !mDestroyed && mRenderer != null )
            mRenderer.deleteNVGContext();
    }

    @Override
    public void onClick( View v )
    {
        if ( !mCreated && !mDestroyed )
        {

            mGLSurfaceView.onResume();
            mCreated = true;
            //mRenderHandler.post( mRenderRunnable );
            mGLSurfaceView.setRenderMode( GLSurfaceView.RENDERMODE_CONTINUOUSLY );
        }
        else if ( mCreated && !mDestroyed )
        {
            mGLSurfaceView.onPause();
            mDestroyed = true;

            mGLSurfaceView.queueEvent( new Runnable()
            {
                @Override
                public void run()
                {
                    mRenderer.deleteNVGContext();
                }
            } );
        }
    }

    private static class Renderer implements GLSurfaceView.Renderer
    {

        static
        {
            System.loadLibrary( "demo" );
        }

        private int mWidth;
        private int mHeight;



        //********************************************************

        private native void createNVGContext( int width, int height );

        public native void draw();

        public native void deleteNVGContext();



        //********************************************************


        @Override
        public void onSurfaceCreated( GL10 gl, EGLConfig config )
        {

        }

        @Override
        public void onSurfaceChanged( GL10 gl, int width, int height )
        {
            mWidth = width;
            mHeight = height;

            createNVGContext( mWidth, mHeight );
        }

        @Override
        public void onDrawFrame( GL10 gl )
        {
            draw();
        }
    }
}
