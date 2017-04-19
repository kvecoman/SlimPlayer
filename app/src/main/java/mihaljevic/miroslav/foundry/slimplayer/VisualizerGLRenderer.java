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

    private static final int CURVE_POINTS = 8;

    private static final int TRANSITION_FRAMES = 8;  //10 is a nice value, it is synced, yet nicely slow and smooth

    private static final int DEFAULT_TARGET_SAMPLES_TO_SHOW = 200;

    private static final int DEFAULT_TARGET_TIME_SPAN = 500;

    private static final int DEFAULT_STROKE_WIDTH = 10;

    //private static int sInstanceCount = 0;



    //private int mInstance;

    //GL ES surface width and height
    private int mWidth;
    private int mHeight;

    private boolean mEnabled = false;

    private boolean mCleared = false;

    private long mNativeInstancePtr = 0;

    private boolean mReleased = false;

    //private boolean mScheduledRelease = false;





    //NATIVE METHODS *****************************************************************************************************

    static
    {
        System.loadLibrary( "visualizer" );
    }

    private native long initNative( int curvePointsCount, int transitionFrames, int targetSamplesCount, int targetTimeSpan, int strokeWidth );

    private native void releaseNative( long objPtr );

    private native void initGLES( long objPtr, int width, int height, float red, float green, float blue );

    //private native void releaseGLES( long objPtr );

    private native void render( long objPtr );

    public native void processBuffer( long objPtr, ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );

    private native void reset( long objPtr );

    public native void deleteNVGContexts();

    //**************************************************************************************************************************






    public VisualizerGLRenderer()
    {
        /*mInstance = sInstanceCount;
        sInstanceCount++;*/

        mNativeInstancePtr = initNative( CURVE_POINTS, TRANSITION_FRAMES, DEFAULT_TARGET_SAMPLES_TO_SHOW, DEFAULT_TARGET_TIME_SPAN, DEFAULT_STROKE_WIDTH );
    }


    /**
     *
     * @param curvePoints           - amount of points the curve will be created from
     * @param transitionFrames      - number of frames for curve to reach last updated points from its current shape
     * @param targetSamplesToShow   - amount of samples that show targeted time span
     * @param targetTimeSpan        - amount of time we see at every moment on screen in curve or waveform
     */
    public VisualizerGLRenderer( int curvePoints, int transitionFrames, int targetSamplesToShow, int targetTimeSpan, int strokeWidth )
    {
        //mInstance = sInstanceCount;
        //sInstanceCount++;

        mNativeInstancePtr = initNative( curvePoints, transitionFrames, targetSamplesToShow, targetTimeSpan, strokeWidth );
    }

    /*public void scheduleRelease()
    {
        mScheduledRelease = true;
    }*/

    /**
     * It seems it needs to be called on GL thread
     */
    public void release()
    {
        //releaseGLES( mNativeInstancePtr );
        releaseNative( mNativeInstancePtr );

        mReleased = true;

        //sInstanceCount--;
    }

    @Override
    public void onSurfaceCreated( GL10 gl_notUsed, EGLConfig config )
    {

    }


    @Override
    public void onSurfaceChanged( GL10 gl, int width, int height )
    {
        /*if ( mScheduledRelease && !mReleased )
        {
            release();
            return;
        }*/

        /*Float red;
        Float green;
        Float blue;

        red     = 0f;
        green   = 0f;
        blue    = 0f;*/

        mWidth  = width;
        mHeight = height;

        //Utils.calculateColorForGL( , red, green, blue );

        //TODO - remove colors
        initGLES( mNativeInstancePtr, width, height, 0.785f, 0.985f, 0.985f );
    }

    @Override
    public void onDrawFrame( GL10 gl )
    {

        if ( mCleared )
            return;

        if ( mEnabled )
            render( mNativeInstancePtr );
        else
        {
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT );
            mCleared = true;
        }

        GLES20.glClearColor(0, 0, 0, 0);
    }

    public boolean isEnabled()
    {
        return mEnabled;
    }

    public void setEnabled( boolean enabled )
    {
        this.mEnabled = enabled;
        mCleared = false;
    }

    //TODO - remove this whole reset thing
    public void reset()
    {
        reset( mNativeInstancePtr );
    }

    @Override
    public void processBuffer( ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs )
    {
        if ( !mReleased && mEnabled )
            processBuffer( mNativeInstancePtr, samplesBuffer, samplesCount, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );
    }
}
