package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.nio.ByteBuffer;

/**
 * Created by miroslav on 11.03.17..
 *
 * Inspired by android-er.blogspot.com
 */

public class VisualizerView extends View
{
    protected final String TAG = getClass().getSimpleName();

    private static final int DEFAULT_TARGET_SAMPLES_TO_SHOW = 200;

    private static final int DEFAULT_FPS = 60;

    private static final int TRANSITION_FRAMES = 3;

    //TODO - transition frames and curve_points should be settable from outside

    //Points that will be calculated from N sectors of waveform
    private static final int CURVE_POINTS = 8;

    private int dbgLinePos = 0;



    //private int dbgOnDrawCalled = 0;

    //Connecting point between audio renderer and visualizer, it transfers obtained samples to here
    private AudioBufferManager mAudioBufferManager;

    //Height values of curve points
    //private float[] mCurveHeights;

    private PointF[] mCurvePoints;




    private ByteBuffer  mSamplesBuffer;
    private int         mSamplesCount = 1;

    private PointF[]    mWaveformPoints;
    private float[]     mWaveformPointsFloat;
    //private Rect        mCanvasRect = new Rect();
    private Paint       mForePaint = new Paint();



    //How much samples we try to show in one frame
    //private int mTargetSamplesToShow;

    //Provides curve every frame as needed
    private CurveAnimator mCurveAnimator;


    //Whether we are drawing visualizations or not
    private boolean     mUpdateEnabled = false;

    //Delay between calling of update handler
    private long        mUpdateDelayMs;

    //Handler used as timer to time next draw of visualization
    private Handler     mUpdateHandler = new Handler(  );

    //Runnable where we call update methods for visualization to update
    private Runnable    mUpdateRunable = new Runnable()
    {
        @Override
        public void run()
        {
            if ( !mUpdateEnabled )
                return;

            updateVisualizer();
            mUpdateHandler.postDelayed( this, mUpdateDelayMs );
        }
    };



    static{
        System.loadLibrary( "visualizer" );
    }

    private native void initNative();

    private native void releaseNative();

    private native void calculateWaveformData( ByteBuffer samplesBuffer, int samplesCount );

    private native void calculateCurvePoints( ByteBuffer samplesBuffer, int curvePointsCount );






    public VisualizerView( Context context )
    {
        super( context );
        init();
    }

    public VisualizerView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        init();
    }

    public VisualizerView( Context context, AttributeSet attrs, int defStyleAttr )
    {
        super( context, attrs, defStyleAttr );
        init();
    }

    private void init()
    {
        mForePaint.setStrokeWidth   ( 1f );
        mForePaint.setAntiAlias     ( true );
        mForePaint.setColor         ( Color.rgb( 0, 128, 255 ) );
        mForePaint.setStyle         ( Paint.Style.STROKE );

        setSamplesToShow( DEFAULT_TARGET_SAMPLES_TO_SHOW );

        setFps( DEFAULT_FPS );

        mCurveAnimator = new CurveAnimator( CURVE_POINTS, TRANSITION_FRAMES );


        mCurvePoints = new PointF[ CURVE_POINTS ];

        //Init points to something
        for ( int i = 0; i < CURVE_POINTS; i++ )
        {
            mCurvePoints[i] = new PointF( 0, 0 );
        }

        initNative();

    }

    public void release()
    {
        releaseNative();
    }



    public void setFps( int fps )
    {
        mUpdateDelayMs = 1000 / fps;
    }

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

    public void setAudioBufferManager( AudioBufferManager audioBufferManager )
    {
        mAudioBufferManager = audioBufferManager;
    }

    public void enableUpdate()
    {
        if ( mUpdateEnabled )
            return;

        mUpdateHandler.post( mUpdateRunable );

        mUpdateEnabled = true;
    }

    public void disableUpdate()
    {
        mUpdateEnabled = false;
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


    long dbgStartTime;
    long dbgEndTime;
    long dbgTimeSum = 0;
    long dbgCalls = 0;
    double dbgAverageTime = 0;

    //dbgStartTime    = System.currentTimeMillis();
    //CODE TO MEASURE GOES HERE
    //dbgEndTime      = System.currentTimeMillis();

    //dbgTimeSum += dbgEndTime - dbgStartTime;
    //dbgCalls++;

    //dbgAverageTime = ( double )dbgTimeSum / ( double )dbgCalls;
    //Log.d( TAG, "calculateWaveformData() average time: "  + String.format( "%.06f", dbgAverageTime ) + " ms after " + dbgCalls + " calls");

    public void updateVisualizer()
    {

        boolean samplesAcquired;

        samplesAcquired = acquireSamples();

        if ( !samplesAcquired )
            return;

        //This is called first, because samples are apsoluted inside and that will be shown on raw waveform data when it is drawn
        calculateCurvePoints( mSamplesBuffer, CURVE_POINTS );


        calculateWaveformData( mSamplesBuffer, mSamplesCount );


        invalidate();


    }


    private void drawWaveformData( Canvas canvas )
    {
        canvas.drawLines( mWaveformPointsFloat, 0, (mSamplesCount - 1) * 4 , mForePaint );
    }



    private void drawCurve( Canvas canvas )
    {

        Path curvePath;

        //TODO - points don't need to be calculated every frame/update
        if ( mCurveAnimator.isDone() )
            mCurveAnimator.addPoints( mCurvePoints );

        curvePath = mCurveAnimator.getCurveForCurrentFrame();


        canvas.drawPath( curvePath, mForePaint );
    }



    @Override
    protected void onDraw( Canvas canvas )
    {
        super.onDraw( canvas );


        drawWaveformData( canvas );

        drawCurve( canvas );

        //DEBUG STUFF
        canvas.drawLine( dbgLinePos, 0, dbgLinePos, getHeight(), mForePaint );
        dbgLinePos += getWidth() / DEFAULT_FPS;
        dbgLinePos %= getWidth();


    }



    //UNUSED JAVA IMPLEMENTATION OF CODE (NATIVE C++ used) ***************************************************************************************************************

    private void calculateWaveformDataJava(ByteBuffer samplesBuffer, int samplesCount)
    {
        //For normal waveform data we use upper half of canvas rectangle
        //mCanvasRect.set( 0, 0, getWidth(), getHeight() / 2 );

        Rect rect;

        rect = new Rect( 0, 0, getWidth(), getHeight() / 2  );

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
        //float[] curveHeights;
        int     sectorSize;
        byte    maxSectorHeight;


        pointDistance   = getWidth() / ( curvePointsCount - 1 );

        //For debug purposes we only take lower half
        curveRect       = new Rect( 0, getHeight() / 2, getWidth(), getHeight()  );

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
