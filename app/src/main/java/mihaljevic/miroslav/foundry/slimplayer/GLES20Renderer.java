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






    public static final float VERTICES[] = new float[]
                                        {   10.0f, 200f, 0.0f,
                                            10.0f, 100f, 0.0f,
                                            100f, 100f, 0.0f };

    public static final short DRAW_ORDER_INDICES[] = new short[]{ 0, 1, 2 };

    private float[] mProjectionMatrix           = new float[16];
    private float[] mViewMatrix                 = new float[16];
    private float[] mProjectionAndViewMatrix    = new float[16];

    public FloatBuffer mVerticesBuffer;
    public ShortBuffer mDrawOrderIndicesBuffer;

    private int mSolidColorProgram;

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

    private PointF[] mCurvePoints;

    private ByteBuffer  mSamplesBuffer;
    private int         mSamplesCount = 1;

    private PointF[]    mWaveformPoints;
    private float[]     mWaveformPointsFloat;


    //Provides curve every frame as needed
    private CurveAnimator mCurveAnimator;


    static{
        System.loadLibrary( "visualizer" );
    }

    private native void initNative( int curvePointsCount, int transitionFrames );

    private native void releaseNative();

    private native void initGLES( int width, int height );

    private native void releaseGLES();

    /*private native void calculateWaveformData( ByteBuffer samplesBuffer, int samplesCount );

    private native void calculateCurvePoints( ByteBuffer samplesBuffer, int curvePointsCount );*/

    private native void drawCurve( ByteBuffer smaplesBuffer );


    public void setSamplesToShow( int targetSamplesCount )
    {
        if ( targetSamplesCount <= 0 )
            return;

        //mTargetSamplesToShow = targetSamplesCount;

        mWaveformPoints = new PointF[ targetSamplesCount ];

        for ( int i = 0; i < mWaveformPoints.length; i++ )
        {
            mWaveformPoints[ i ] = new PointF();
        }

        mWaveformPointsFloat = new float[ ( targetSamplesCount - 1 ) * 4 ];
    }


    private boolean acquireSamples()
    {
        if ( mAudioBufferManager == null )
            return false;


        mSamplesBuffer  = mAudioBufferManager.getSamplesJava(); //NOTE - JAVA version is faster


        if ( mSamplesBuffer == null )
            return false;

        mSamplesCount = mSamplesBuffer.limit();

        return true;
    }


















    public GLES20Renderer()
    {
        setSamplesToShow( DEFAULT_TARGET_SAMPLES_TO_SHOW );

        mCurvePoints = new PointF[ CURVE_POINTS ];

        mCurveAnimator = new CurveAnimator( CURVE_POINTS, TRANSITION_FRAMES );

        //Init points to something
        for ( int i = 0; i < CURVE_POINTS; i++ )
        {
            mCurvePoints[i] = new PointF( 0, 0 );
        }

        Log.d(TAG, "initNative should be called now");
        initNative( CURVE_POINTS, TRANSITION_FRAMES );

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

        initMatrices( width, height );
        initShaders();
        calculateTriangleVertices();

        initGLES( width, height );
    }

    @Override
    public void onDrawFrame( GL10 gl )
    {
        //render( mProjectionAndViewMatrix );

        boolean samplesAcquired;

        samplesAcquired = acquireSamples();

        if ( !samplesAcquired )
            return;

        //This is called first, because samples are apsoluted inside and that will be shown on raw waveform data when it is drawn
        //calculateCurvePointsJava( mSamplesBuffer, CURVE_POINTS );


        //calculateWaveformDataJava( mSamplesBuffer, mSamplesCount );

        //Log.d(TAG, "drawCurve should be called now");
        drawCurve( mSamplesBuffer );
    }







    public void setAudioBufferManager( AudioBufferManager audioBufferManager )
    {
        mAudioBufferManager = audioBufferManager;
    }




    private void render( float[] m )
    {
        int positionHandle;
        int mtrxHandle;

        //Log.v( TAG, "render()" );


        GLES20.glClear( GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT );


        positionHandle = GLES20.glGetAttribLocation ( mSolidColorProgram, "vPosition" );
        GLES20.glEnableVertexAttribArray            ( positionHandle );
        GLES20.glVertexAttribPointer                ( positionHandle, 3, GLES20.GL_FLOAT, false, 0, mVerticesBuffer );



        mtrxHandle = GLES20.glGetUniformLocation( mSolidColorProgram, "uMVPMatrix" );
        GLES20.glUniformMatrix4fv               ( mtrxHandle, 1, false, m, 0 );


        GLES20.glDrawElements               ( GLES20.GL_TRIANGLES, DRAW_ORDER_INDICES.length, GLES20.GL_UNSIGNED_SHORT, mDrawOrderIndicesBuffer );
        GLES20.glDisableVertexAttribArray   ( positionHandle );

    }

    private void initMatrices( int width, int height )
    {
        for ( int i = 0; i < 16; i++ )
        {
            mProjectionMatrix[i] = 0f;
            mViewMatrix[i] = 0f;
            mProjectionAndViewMatrix[i] = 0f;
        }

        Matrix.orthoM( mProjectionMatrix, 0, 0f, width, 0f, height, 0, 50 );

        Matrix.setLookAtM( mViewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0f );

        Matrix.multiplyMM( mProjectionAndViewMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0 );
    }

    private void initShaders()
    {
        int vertexShader;
        int fragmentShader;


        GLES20.glClearColor( 0.5f, 0.5f, 0.5f, 0.5f );

        vertexShader    = Shaders.loadShader( GLES20.GL_VERTEX_SHADER, Shaders.vs_SolidColor );
        fragmentShader  = Shaders.loadShader( GLES20.GL_FRAGMENT_SHADER, Shaders.fs_SolidColor );

        mSolidColorProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader   ( mSolidColorProgram, vertexShader );
        GLES20.glAttachShader   ( mSolidColorProgram, fragmentShader );
        GLES20.glLinkProgram    ( mSolidColorProgram );

        GLES20.glUseProgram( mSolidColorProgram );
    }

    private void calculateTriangleVertices()
    {
        ByteBuffer buffer1;
        ByteBuffer buffer2;

        buffer1 = ByteBuffer.allocateDirect( VERTICES.length * 4 );
        buffer1.order( ByteOrder.nativeOrder() );

        mVerticesBuffer = buffer1.asFloatBuffer();
        mVerticesBuffer.put( VERTICES );
        mVerticesBuffer.position( 0 );

        buffer2 = ByteBuffer.allocateDirect( DRAW_ORDER_INDICES.length * 2 );
        buffer2.order( ByteOrder.nativeOrder() );

        mDrawOrderIndicesBuffer = buffer2.asShortBuffer();
        mDrawOrderIndicesBuffer.put( DRAW_ORDER_INDICES );
        mDrawOrderIndicesBuffer.position( 0 );
    }


    static class Shaders
    {

        public static final String vs_SolidColor =
                "uniform    mat4        uMVPMatrix;" +
                        "attribute  vec4        vPosition;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "}";

        public static final String fs_SolidColor =
                "precision mediump float;" +
                        "void main() {" +
                        "  gl_FragColor = vec4(0,0,0.5,1);" +
                        "}";

        public static int loadShader( int type, String shaderCode )
        {
            int shader = GLES20.glCreateShader( type );

            GLES20.glShaderSource( shader, shaderCode );
            GLES20.glCompileShader( shader );

            return shader;
        }

    }











    //UNUSED JAVA IMPLEMENTATION OF CODE (NATIVE C++ used) ***************************************************************************************************************

    private void calculateWaveformDataJava(ByteBuffer samplesBuffer, int samplesCount)
    {
        //For normal waveform data we use upper half of canvas rectangle
        //mCanvasRect.set( 0, 0, getWidth(), getHeight() / 2 );

        Rect rect;

        rect = new Rect( 0, 0, mWidth, mHeight / 2  );

        float x;
        float y;
        float oldx;
        float oldy;


        /*for ( int i = 0; i < samplesCount; i++ )
        {
            mWaveformPoints[ i ].x = rect.width() * i / ( samplesCount - 1 );
            mWaveformPoints[ i ].y = rect.height() / 2 + ( ( byte ) ( samplesBuffer.get( i ) + 128 ) ) * ( rect.height() / 2 ) / 128;
        }*/

        oldx = 0;
        oldy = rect.height() / 2 + ( ( byte ) ( samplesBuffer.get( 0 ) + 128 ) ) * ( rect.height() / 2 ) / 128;

        for ( int i = 0; i <  ( samplesCount - 1 ); i++ )
        {
            /*mWaveformPointsFloat[ i * 4 ]       = mWaveformPoints[ i ].x;
            mWaveformPointsFloat[ i * 4 + 1 ]   = mWaveformPoints[ i ].y;
            mWaveformPointsFloat[ i * 4 + 2 ]   = mWaveformPoints[ i + 1 ].x;
            mWaveformPointsFloat[ i * 4 + 3 ]   = mWaveformPoints[ i + 1 ].y;*/

            x = rect.width() * (i + 1) / ( samplesCount - 1 );
            y = rect.height() / 2 + ( ( byte ) ( samplesBuffer.get( i + 1 ) + 128 ) ) * ( rect.height() / 2 ) / 128;

            mWaveformPointsFloat[ i * 4 ]       = oldx;
            mWaveformPointsFloat[ i * 4 + 1 ]   = oldy;
            mWaveformPointsFloat[ i * 4 + 2 ]   = x;
            mWaveformPointsFloat[ i * 4 + 3 ]   = y;

            oldx = x;
            oldy = y;
        }
    }

    private void calculateCurvePointsJava( ByteBuffer samplesBuffer, int curvePointsCount )
    {

        Rect    curveRect;
        int     pointDistance;
        float   scaledHeight;
        float   scaling;
        int     sectorSize;
        byte    maxSectorHeight;


        pointDistance   = mWidth / ( curvePointsCount - 1 );

        //For debug purposes we only take lower half
        curveRect       = new Rect( 0, mHeight / 2, mWidth, mHeight  );

        absoluteSamples( samplesBuffer );

        //curveHeights = calculateCurvePointHeights( samplesBuffer, curvePointsCount );

        scaling = ( float ) curveRect.height() / 128f;

        sectorSize      = ( samplesBuffer.limit() ) / curvePointsCount;

        for ( int i = 0; i < curvePointsCount; i++ )
        {
            maxSectorHeight = findMaxByte( samplesBuffer, i * sectorSize, i * sectorSize + sectorSize );

            scaledHeight = scaling * maxSectorHeight;

            mCurvePoints[ i ] = new PointF( i * pointDistance, curveRect.height() + scaledHeight );
        }

    }

    private void absoluteSamples( ByteBuffer samplesBuffer )
    {
        byte absolutedSample;

        for ( int i = 0; i < samplesBuffer.limit(); i++ )
        {
            absolutedSample = ( byte ) Math.abs(  samplesBuffer.get( i ) );
            samplesBuffer.put( i, absolutedSample );
        }
    }


    private byte findMaxByte( ByteBuffer buffer, int start, int end )
    {
        byte max = Byte.MIN_VALUE;

        for ( int i = start; i < end; i++ )
        {
            if ( buffer.get( i ) > max )
                max = buffer.get( i );
        }

        return max;
    }




}
