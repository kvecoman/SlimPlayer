package mihaljevic.miroslav.foundry.slimplayer;

import android.graphics.Path;
import android.graphics.PointF;

/**
 * Created by miroslav on 26.03.17..
 */

public class CurveAnimator
{
    public PointF[] CUBIC_BEZIER_EASE_IN_OUT =  {
            new PointF( 0, 0 ),
            new PointF( 0.35f, 0 ),
            new PointF( 0.65f, 1 ),
            new PointF( 1, 1 )
    };

    public PointF[] CUBIC_BEZIER_EASE_IN =  {
            new PointF( 0, 0 ),
            new PointF( 0.35f, 0 ),
            new PointF( 1, 1 ),
            new PointF( 1, 1 )
    };

    public PointF[] CUBIC_BEZIER_LINEAR =  {
            new PointF( 0, 0 ),
            new PointF( 0, 0 ),
            new PointF( 1, 1 ),
            new PointF( 1, 1 )
    };

    //Number of points in curve
    private int mPointsCount = 0;

    //Length of animation in frames
    private int mFramesCount = 0;

    //Current active frame
    private int mFrameIndex = 0;

    //Start points
    private PointF[] mStartPoints;

    //Calculated mid points between start and end points for current frame
    private PointF[] mCurrentPoints;

    //End points
    private PointF[] mEndPoints;

    //Is the current animation cycle done
    private boolean mDone = true;



    public CurveAnimator( int pointCount, int frameCount )
    {
        mPointsCount    = pointCount;
        mFramesCount    = frameCount;
        mCurrentPoints  = new PointF[ pointCount ];
    }


    public boolean isDone()
    {
        return mDone;
    }

    public void addPoints( PointF[] points )
    {
        if ( points.length != mPointsCount )
            throw new IllegalArgumentException( "Length of end points is not same as expected number of points" );


        mEndPoints = points;


        if ( mStartPoints == null )
            mStartPoints = points;
        else
            mStartPoints = mCurrentPoints;

        mFrameIndex = 0;

        mDone = false;
    }

    public Path getCurveForCurrentFrame()
    {
        if ( mDone )
            return null;


        float   animationPercentage;
        float   bezierPercentage;
        float   midX;
        float   midY;
        PointF  startPoint;
        PointF  endPoint;
        PointF  midPoint;
        Path    curvePath;


        animationPercentage = ( float ) mFrameIndex / ( float ) mFramesCount;

        //This percentage follows bezier curve that we defined
        bezierPercentage = percentageFromCubicBezier( animationPercentage, CUBIC_BEZIER_LINEAR );

        for ( int i = 0; i < mPointsCount; i++ )
        {
            startPoint  = mStartPoints[ i ];
            endPoint    = mEndPoints[ i ];

            //Thanks to DZone for those two lines ( https://dzone.com/articles/how-find-point-coordinates )
            //Calculate mid points accordingly by bezier curve
            midX = ( 1 - bezierPercentage ) * startPoint.x + bezierPercentage * endPoint.x;
            midY = ( 1 - bezierPercentage ) * startPoint.y + bezierPercentage * endPoint.y;

            midPoint = new PointF( midX, midY );

            mCurrentPoints[ i ] = midPoint;
        }


        curvePath = curveFromPoints( mCurrentPoints );

        //If we have come to the 100% of animation cycle, we set the DONE flag up
        if ( animationPercentage >= 1 )
            mDone = true;

        //Move to the next frame
        mFrameIndex++;

        return curvePath;

    }

    //Calculates value effect for current state x ( x is between 0 ( 0% ) and 1 ( 100% ) )
    private float percentageFromCubicBezier( float x, PointF[] bezierPoints )
    {
        float result;

        result =    ( float )
                (
                        ( bezierPoints[ 0 ].y * ( 1 - Math.pow( x, 3 ) ) ) +
                                ( 3 * bezierPoints[ 1 ].y * ( 1 - Math.pow( x, 2 ) ) * x ) +
                                ( 3 * bezierPoints[ 2 ].y * ( 1 - x ) * Math.pow( x, 2 ) ) +
                                ( bezierPoints[ 3 ].y * Math.pow( x, 3 ) )
                );

        return result;
    }


    private Path curveFromPoints( PointF[] curvePoints )
    {
        Path    curvePath;

        PointF start;
        PointF ctrl1;
        PointF ctrl2;
        PointF end;


        curvePath   = new Path();
        start       = new PointF(  );
        ctrl1       = new PointF(  );
        ctrl2       = new PointF(  );
        end         = new PointF(  );


        curvePath.moveTo        ( curvePoints[ 0 ].x, curvePoints[ 0 ].y );
        curvePath.setFillType   ( Path.FillType.EVEN_ODD );


        for ( int i = 1; i < curvePoints.length; i++ )
        {

            start.x = curvePoints[ i - 1 ].x;
            start.y = curvePoints[ i - 1 ].y;

            end.x = curvePoints[ i ].x;
            end.y = curvePoints[ i ].y;

            ctrl1.x = start.x - ( ( start.x - end.x ) / 2 );
            ctrl1.y = start.y;

            ctrl2.x = ctrl1.x;
            ctrl2.y = end.y;

            curvePath.cubicTo( ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, end.x, end.y );
        }

        return curvePath;

    }


}