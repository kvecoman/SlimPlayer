package mihaljevic.miroslav.foundry.slimplayer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

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

public class VisualizerGLRenderer implements GLSurfaceView.Renderer, CustomMediaCodecAudioRenderer.BufferReceiver
{

    protected final String TAG = getClass().getSimpleName();

    private static final int CURVE_POINTS = 8; //DEFAULT IS 8

    private static final int TRANSITION_FRAMES = 8;  //10 is a nice value, it is synced, yet nicely slow and smooth, DEFAULT IS 8

    private static final int DEFAULT_TARGET_SAMPLES_TO_SHOW = 200; //DEFAULT 200, number of samples being shown at screen every moment

    private static final int DEFAULT_TARGET_TIME_SPAN = 500; //DEFAULT 500, in ms

    //private static final int DEFAULT_STROKE_WIDTH = 10;




    //GL ES surface width and height
    private int mWidth;
    private int mHeight;

    private boolean mEnabled = false;

    private long mNativeInstancePtr = 0;

    private boolean mReleased = false;

    //Used to create scroll illusion
    private int mDrawOffset = 0;

    private boolean mClear = false;






    //NATIVE METHODS *****************************************************************************************************

    static
    {
        System.loadLibrary( "visualizer" );
    }

    private native long initNative( int curvePointsCount, int transitionFrames, int targetSamplesCount, int targetTimeSpan/*, int strokeWidth*/ );

    private native void deleteNativeInstance( long objPtr );

    private native void initNVG( long objPtr, int width, int height, float density );

    private native void releaseNVG( long objPtr );

    private native void render( long objPtr, int drawOffset );

    public native void processBuffer( long objPtr, ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );

    /*private native void enable( long objPtr );

    private native void disable( long objPtr );*/

    //**************************************************************************************************************************






    public VisualizerGLRenderer()
    {
        mNativeInstancePtr = initNative( CURVE_POINTS, TRANSITION_FRAMES, DEFAULT_TARGET_SAMPLES_TO_SHOW, DEFAULT_TARGET_TIME_SPAN/*, DEFAULT_STROKE_WIDTH*/ );
    }


    /**
     *
     * @param curvePoints           - amount of points the curve will be created from
     * @param transitionFrames      - number of frames for curve to reach last updated points from its current shape
     * @param targetSamplesToShow   - amount of samples that show targeted time span
     * @param targetTimeSpan        - amount of time we see at every moment on screen in curve or waveform
     */
    public VisualizerGLRenderer( int curvePoints, int transitionFrames, int targetSamplesToShow, int targetTimeSpan/*, int strokeWidth*/ )
    {
        mNativeInstancePtr = initNative( curvePoints, transitionFrames, targetSamplesToShow, targetTimeSpan/*, strokeWidth*/ );
    }

    @Override
    public void onSurfaceCreated( GL10 gl_notUsed, EGLConfig config )
    {

    }


    @Override
    public void onSurfaceChanged( GL10 gl, int width, int height )
    {

        float density;

        if ( /*!mEnabled ||*/ mReleased || ( mWidth == width && mHeight == height ) )
            return;

        mWidth  = width;
        mHeight = height;

        density = SlimPlayerApplication.getInstance().getResources().getDisplayMetrics().density;

        initNVG( mNativeInstancePtr, width, height, density );

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
            render( mNativeInstancePtr, mDrawOffset );
        /*else
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT );*/

        GLES20.glClearColor(0, 0, 0, 0);
        //GLES20.glClearColor(230f / 255f, 207f / 255f, 0f, 0.5f);
    }

    /**
     * This can be called on non GL thread
     */
    public void release()
    {
        mReleased = true;

        deleteNativeInstance( mNativeInstancePtr );
    }

    /**
     * Needs to be called on GL thread
     */
    public void releaseNVG()
    {
        releaseNVG( mNativeInstancePtr );
    }


    /*public void setAcceptSamples( boolean accept )
    {
        this.mEnabled = accept;
    }*/

    public void enable()
    {
        mEnabled = true;
        //enable( mNativeInstancePtr );
    }

    public void disable()
    {
        mEnabled = false;
        //disable( mNativeInstancePtr );
    }

    public void enableClear()
    {
        mClear = true;
    }

    public void disableClear()
    {
        mClear = false;
    }

    public void setDrawOffset( int drawOffset )
    {
        mDrawOffset = drawOffset;
    }

    @Override
    public void processBuffer( ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs )
    {
        if ( !mReleased && mEnabled )
            processBuffer( mNativeInstancePtr, samplesBuffer, samplesCount, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );
    }
}
