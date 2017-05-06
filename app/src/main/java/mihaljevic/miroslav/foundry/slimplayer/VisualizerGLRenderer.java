package mihaljevic.miroslav.foundry.slimplayer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by miroslav on 04.04.17..
 *
 * Renderer responsible for drawing visualization and its related operations
 *
 * @author Miroslav Mihaljević
 *
 */

public class VisualizerGLRenderer implements GLSurfaceView.Renderer, Player.BufferReceiver
{

    protected final String TAG = getClass().getSimpleName();

    public static final int DEFAULT_CURVE_POINTS = 8; //DEFAULT IS 8

    public static final int DEFAULT_TRANSITION_FRAMES = 8;  //10 is a nice value, it is synced, yet nicely slow and smooth, DEFAULT IS 8

    public static final int DEFAULT_TARGET_SAMPLES_TO_SHOW = 200; //DEFAULT 200, number of samples being shown at screen every moment

    public static final int DEFAULT_TARGET_TIME_SPAN = 500; //DEFAULT 500, in ms

    //private static final int DEFAULT_STROKE_WIDTH = 10;





    //GL ES surface width and height
    private int mWidth;
    private int mHeight;

    private boolean mEnabled = false;


    private boolean mReleased = false;

    //Used to create scroll illusion
    private int mDrawOffset = 0;

    private boolean mClear = false;

    private boolean mBufferProcessingEnabled = false;



    //TODO - optimize visualizer renderer


    //NATIVE METHODS *****************************************************************************************************

    static
    {
        System.loadLibrary( "visualizer" );
    }

    private native void initNative( int curvePointsCount, int transitionFrames, int targetSamplesCount, int targetTimeSpan, boolean exoAudioBufferManager );

    /**
     * Needs to be called on GL thread
     */
    private native void deleteNativeInstance( );

    private native void initNVG( int width, int height, float density );

    /**
     * Needs to be called on GL thread
     */
    public native void releaseNVG( );

    private native void render( int drawOffset );

    public native void processBufferNative( ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );

    public native void processBufferArrayNative( byte[] samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );

    
    /*public native void initExoAudioBufferManager( int targetSamplesCount, int targetTimeSpan );

    public native void initMediaAudioBufferManager( int targetSamplesCount );*/


    //**************************************************************************************************************************


    /**
     *
     * @param exoAudioBufferManager - whether to use AudioBufferManager intended for exo player (should be false if using default android media player and its visualizer)
     */
    public VisualizerGLRenderer( boolean exoAudioBufferManager )
    {
       initNative( DEFAULT_CURVE_POINTS, DEFAULT_TRANSITION_FRAMES, DEFAULT_TARGET_SAMPLES_TO_SHOW, DEFAULT_TARGET_TIME_SPAN, exoAudioBufferManager );

        mBufferProcessingEnabled = true;
    }


    /**
     *
     * @param curvePoints           - amount of points the curve will be created from
     * @param transitionFrames      - number of frames for curve to reach last updated points from its current shape
     * @param targetSamplesToShow   - amount of samples that show targeted time span
     * @param targetTimeSpan        - amount of time we see at every moment on screen in curve or waveform
     */
    /*public VisualizerGLRenderer( int curvePoints, int transitionFrames, int targetSamplesToShow, int targetTimeSpan, boolean exoAudioBufferManager )
    {
        mNativeInstancePtr = initNative( curvePoints, transitionFrames, targetSamplesToShow, targetTimeSpan, exoAudioBufferManager );
    }*/

    @Override
    public void onSurfaceCreated( GL10 gl_notUsed, EGLConfig config )
    {

    }


    @Override
    public void onSurfaceChanged( GL10 gl, int width, int height )
    {

        float density;

        if ( /*!mEnabled ||*/ mReleased || ( mWidth == width && mHeight == height ) )
        {
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT );
            return;
        }

        mWidth  = width;
        mHeight = height;

        density = SlimPlayerApplication.getInstance().getResources().getDisplayMetrics().density;

        initNVG( width, height, density );

        //SlimPlayerApplication.getInstance().getResources().getDisplayMetrics().densityDpi;

    }

    @Override
    public void onDrawFrame( GL10 gl )
    {

        if ( mClear )
        {
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT );
            return;
        }

        if ( mEnabled )
            render( mDrawOffset );
        /*else
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT );*/

        GLES20.glClearColor(0, 0, 0, 0);
        //GLES20.glClearColor(230f / 255f, 207f / 255f, 0f, 0.5f);
    }

    /**
     * This also needs to be called on GL thread
     */
    public void release()
    {
        Log.v( TAG,"release()" );
        mReleased = true;

        deleteNativeInstance(  );
    }





    public void enable()
    {
        mEnabled = true;
    }

    public void disable()
    {
        mEnabled = false;
    }

    public void enableClear()
    {
        mClear = true;
    }

    public void disableClear()
    {
        mClear = false;
    }

    public void enableBufferProcessing() { mBufferProcessingEnabled = true;}

    public void disableBufferProcessing() { mBufferProcessingEnabled = false; }

    public void setDrawOffset( int drawOffset )
    {
        mDrawOffset = drawOffset;
    }

    @Override
    public void processBuffer( ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs )
    {
        if ( !mReleased && mBufferProcessingEnabled )
            processBufferNative( samplesBuffer, samplesCount, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );
    }

    @Override
    public void processBufferArray( byte[] samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs )
    {
        if ( !mReleased && mBufferProcessingEnabled )
            processBufferArrayNative( samplesBuffer, samplesCount, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );
    }
}
