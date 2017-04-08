package mihaljevic.miroslav.foundry.slimplayer;

import android.graphics.PointF;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by miroslav on 04.04.17..
 */

public class GLES20Renderer implements GLSurfaceView.Renderer
{



    private int mWidth;
    private int mHeight;



    private AudioBufferManager mAudioBufferManager;

    protected final String TAG = getClass().getSimpleName();

    private static final int DEFAULT_TARGET_SAMPLES_TO_SHOW = 200;

    private static final int DEFAULT_FPS = 60;

    private static final int TRANSITION_FRAMES = 3;

    //TODO - transition frames and curve_points should be settable from outside

    //Points that will be calculated from this much sectors of waveform
    private static final int CURVE_POINTS = 8;


    private ByteBuffer  mSamplesBuffer;





    static
    {
        System.loadLibrary( "visualizer" );
    }

    private native void initNative( int curvePointsCount, int transitionFrames, int targetSamplesCount );

    private native void releaseNative();

    private native void initGLES( int width, int height );

    private native void releaseGLES();



    private native void render( ByteBuffer samplesBuffer, int samplesCount );












    public GLES20Renderer()
    {

        initNative( CURVE_POINTS, TRANSITION_FRAMES, DEFAULT_TARGET_SAMPLES_TO_SHOW );

    }

    public void release()
    {
        releaseGLES();
        releaseNative();
    }

    @Override
    public void onSurfaceCreated( GL10 gl_notUsed, EGLConfig config )
    {

    }

    @Override
    public void onSurfaceChanged( GL10 gl, int width, int height )
    {
        mWidth = width;
        mHeight = height;

        initGLES( width, height );
    }

    @Override
    public void onDrawFrame( GL10 gl )
    {


        boolean samplesAcquired;

        samplesAcquired = acquireSamples();

        if ( !samplesAcquired )
            return;


        render( mSamplesBuffer, mSamplesBuffer.limit() );
    }




    private boolean acquireSamples()
    {
        if ( mAudioBufferManager == null )
            return false;


        mSamplesBuffer  = mAudioBufferManager.getSamplesJava(); //NOTE - JAVA version is faster


        if ( mSamplesBuffer == null )
            return false;


        return true;
    }



    public void setAudioBufferManager( AudioBufferManager audioBufferManager )
    {
        mAudioBufferManager = audioBufferManager;
    }



}
