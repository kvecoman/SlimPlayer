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
import android.util.Log;
import android.view.View;

import java.util.List;

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



    private int dbgOnDrawCalled = 0;

    //Connecting point between audio renderer and visualizer, it transfers obtained samples to here
    private AudioBufferManager mAudioBufferManager;

    //Height values of curve points
    private float[] mCurveHeights;

    //private byte[]      mSamples;

    private AudioBufferManager.BufferWrap mSamplesBuffer;

    private PointF[]    mWaveformPoints;
    private float[]     mWaveformPointsFloat;
    private Rect        mCanvasRect = new Rect();
    private Paint       mForePaint = new Paint();



    //How much samples we try to show in one frame
    private int mTargetSamplesToShow;

    //Provides curve a every frame as needed
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

        mCurveHeights = new float[ CURVE_POINTS ];

    }



    public void setFps( int fps )
    {
        mUpdateDelayMs = 1000 / fps;
    }

    public void setSamplesToShow( int targetSamplesCount )
    {
        if ( targetSamplesCount <= 0 )
            return;

        mTargetSamplesToShow = targetSamplesCount;

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

    private void absoluteSamples()
    {
        for ( int i = 0; i <= mSamplesBuffer.end; i++ )
        {
            mSamplesBuffer.buffer[ i ] = ( byte ) Math.abs(  mSamplesBuffer.buffer[i] );
        }
    }


    private byte findMaxByte( AudioBufferManager.BufferWrap bufferWrap, int start, int end )
    {
        byte max = Byte.MIN_VALUE;

        for ( int i = start; i < end; i++ )
        {
            if ( bufferWrap.buffer[i] > max )
                max = bufferWrap.buffer[i];
        }

        return max;
    }

    //Samples should be absoluted before calling this method
    private void calculateCurvePointHeights()
    {
        int     sectorSize;
        byte    maxSample;

        sectorSize = ( mSamplesBuffer.end + 1 ) / CURVE_POINTS;

        for ( int i = 0; i < CURVE_POINTS; i++ )
        {
            maxSample = findMaxByte( mSamplesBuffer, i * sectorSize, i * sectorSize + sectorSize );
            mCurveHeights[i] = ( float ) maxSample;
        }
    }




    public void updateVisualizer()
    {

        if ( mAudioBufferManager == null )
            return;

        mSamplesBuffer = mAudioBufferManager.getSamples();

        if ( mSamplesBuffer == null )
            return;

        absoluteSamples();

        calculateCurvePointHeights();

        calculateWaveformData(  );

        invalidate();


    }


    private PointF[] calculateCurvePointsFromHeights()
    {
        PointF[] curvePoints;
        Rect    curveRect;
        int     pointDistance;
        float   scaledHeight;
        float   scaling;

        curvePoints     = new PointF[ CURVE_POINTS ];
        pointDistance   = getWidth() / (CURVE_POINTS - 1);

        //For debug purposes we only take lower half
        curveRect       = new Rect( 0, getHeight() / 2, getWidth(), getHeight()  );

        scaling = ( float ) curveRect.height() / 128f;

        for ( int i = 0; i < CURVE_POINTS; i++ )
        {
            scaledHeight = scaling * mCurveHeights[ i ];

            curvePoints[ i ] = new PointF( i * pointDistance, curveRect.height() + scaledHeight );
        }

        return curvePoints;
    }

    private void calculateWaveformData()
    {
        int samplesCount;

        //For normal waveform data we use upper half of canvas rectangle
        mCanvasRect.set( 0, 0, getWidth(), getHeight() / 2 );


        samplesCount = mSamplesBuffer.end + 1;

        for ( int i = 0; i <= mSamplesBuffer.end; i++ )
        {
            mWaveformPoints[ i ].x = mCanvasRect.width() * i / ( samplesCount - 1 );
            mWaveformPoints[ i ].y = mCanvasRect.height() / 2 + ( ( byte ) ( mSamplesBuffer.buffer[ i ] + 128 ) ) * ( mCanvasRect.height() / 2 ) / 128;
        }



    }


    private void drawWaveformData( Canvas canvas )
    {
        int count;


        count = mWaveformPoints.length;


        for ( int i = 0; i < ( ( count - 1 ) ); i++ )
        {
            mWaveformPointsFloat[ i * 4 ]       = mWaveformPoints[ i ].x;
            mWaveformPointsFloat[ i * 4 + 1 ]   = mWaveformPoints[ i ].y;
            mWaveformPointsFloat[ i * 4 + 2 ]   = mWaveformPoints[ i + 1 ].x;
            mWaveformPointsFloat[ i * 4 + 3 ]   = mWaveformPoints[ i + 1 ].y;
        }

        canvas.drawLines( mWaveformPointsFloat, 0, (count - 1) * 4 , mForePaint );
    }

    private void drawCurve( Canvas canvas )
    {
        PointF[]    curvePoints;
        Path        curvePath;

        curvePoints = calculateCurvePointsFromHeights();

        if ( mCurveAnimator.isDone() )
            mCurveAnimator.addPoints( curvePoints );

        curvePath = mCurveAnimator.getCurveForCurrentFrame();


        canvas.drawPath( curvePath, mForePaint );
    }



    @Override
    protected void onDraw( Canvas canvas )
    {
        super.onDraw( canvas );



        dbgOnDrawCalled++;
        //Log.v(TAG, dbgOnDrawCalled + " onDraw()");

        //Report whether we have hardware acceleration
        /*if ( dbgOnDrawCalled == 1 )
        {
            String hardwareAccelerationStatus;

            hardwareAccelerationStatus = "Hardware acceleration: ";

            hardwareAccelerationStatus += canvas.isHardwareAccelerated() ? "ON" : "OFF";

            Log.i( TAG, hardwareAccelerationStatus );
        }*/


        drawWaveformData( canvas );

        drawCurve( canvas );

        //DEBUG STUFF
        canvas.drawLine( dbgLinePos, 0, dbgLinePos, getHeight(), mForePaint );
        dbgLinePos += getWidth() / DEFAULT_FPS;
        dbgLinePos %= getWidth();


    }

}
