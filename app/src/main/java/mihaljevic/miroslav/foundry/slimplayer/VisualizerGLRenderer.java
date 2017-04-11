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

    //GL ES surface width and height
    private int mWidth;
    private int mHeight;





    //NATIVE METHODS *****************************************************************************************************

    static
    {
        System.loadLibrary( "visualizer" );
    }

    private native void initNative( int curvePointsCount, int transitionFrames, int targetSamplesCount, int targetTimeSpan );

    private native void releaseNative();

    private native void initGLES( int width, int height, float red, float green, float blue );

    private native void releaseGLES();

    private native void render( );

    public native void processBuffer( ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );

    //**************************************************************************************************************************






    public VisualizerGLRenderer()
    {
        initNative( CURVE_POINTS, TRANSITION_FRAMES, DEFAULT_TARGET_SAMPLES_TO_SHOW, DEFAULT_TARGET_TIME_SPAN );
    }


    /**
     *
     * @param curvePoints           - amount of points the curve will be created from
     * @param transitionFrames      - number of frames for curve to reach last updated points from its current shape
     * @param targetSamplesToShow   - amount of samples that show targeted time span
     * @param targetTimeSpan        - amount of time we see at every moment on screen in curve or waveform
     */
    public VisualizerGLRenderer( int curvePoints, int transitionFrames, int targetSamplesToShow, int targetTimeSpan )
    {
        initNative( curvePoints, transitionFrames, targetSamplesToShow, targetTimeSpan );
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
        Float red;
        Float green;
        Float blue;

        red     = 0f;
        green   = 0f;
        blue    = 0f;

        mWidth  = width;
        mHeight = height;

        //Utils.calculateColorForGL( , red, green, blue );

        initGLES( width, height, 0.985f, 0.985f, 0.985f );
    }

    @Override
    public void onDrawFrame( GL10 gl )
    {
        render(  );
    }



}
