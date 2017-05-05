//
// Created by miroslav on 07.04.17..
//

#include "CurveAnimator.h"


CurveAnimator::CurveAnimator( int pointCount, int frameCount )
{
    mPointsCount    = pointCount;
    mFramesCount    = frameCount;
    mCurrentPoints  = new Point[ pointCount ];
    mStartPoints    = new Point[ pointCount ];
    mEndPoints      = new Point[ pointCount ];
}

CurveAnimator::~CurveAnimator()
{
    delete [] mCurrentPoints;
    delete [] mStartPoints;
    delete [] mEndPoints;
}

void CurveAnimator::copyPoints( Point * src, Point * dest, int count )
{

    if ( src == nullptr || dest == nullptr )
        return;

    for ( int i = 0; i < count; i++ )
    {
        dest[i].x = src[i].x;
        dest[i].y = src[i].y;
    }
}


bool CurveAnimator::isDone()
{
    return mDone;
}


void CurveAnimator::addPoints( Point points[] )
{

    //NOTE - here we will always assume that we will receive correct number of points


    if ( points == nullptr )
        return;



    copyPoints( points, mEndPoints, mPointsCount );

    if ( mFirstAdd )
    {
        mFirstAdd = false;
        copyPoints( points, mStartPoints, mPointsCount );
    }
    else
    {
        copyPoints( mCurrentPoints, mStartPoints, mPointsCount );
    }


    mFrameIndex = 0;

    mDone = false;
}

void CurveAnimator::calculateNextFrame()
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "CurveAnimator", "calculateNextFrame()" );

    if ( mDone )
        return;


    float   animationPercentage;
    float   bezierPercentage;
    float   midX;
    float   midY;
    Point  startPoint;
    Point  endPoint;
    Point  midPoint;


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

        midPoint.x = midX;
        midPoint.y = midY;

        mCurrentPoints[ i ] = midPoint;
    }

    //If we have come to the 100% of animation cycle, we set the DONE flag up
    if ( animationPercentage >= 1 )
        mDone = true;

    //Move to the next frame
    mFrameIndex++;



}


/**
 * Calculates value effect for current state x ( x is between 0 ( 0% ) and 1 ( 100% ) )
 */
float CurveAnimator::percentageFromCubicBezier( float x, Point bezierPoints[] )
{

    float result;

    result =    ( float )
            (
                    ( bezierPoints[ 0 ].y * ( 1 - pow( x, 3 ) ) ) +
                    ( 3 * bezierPoints[ 1 ].y * ( 1 - pow( x, 2 ) ) * x ) +
                    ( 3 * bezierPoints[ 2 ].y * ( 1 - x ) * pow( x, 2 ) ) +
                    ( bezierPoints[ 3 ].y * pow( x, 3 ) )
            );

    return result;
}


void CurveAnimator::drawCurrentFrameCurve( NVGcontext * nvgContext, DrawParams * drawParams )
{

    Point start;
    Point ctrl1;
    Point ctrl2;
    Point end;


    nvgBeginPath( nvgContext );


    nvgMoveTo( nvgContext, mCurrentPoints[0].x + drawParams->drawOffset, mCurrentPoints[0].y );

    for ( int i = 1; i < mPointsCount; i++ )
    {
        start.x = mCurrentPoints[ i - 1 ].x;
        start.y = mCurrentPoints[ i - 1 ].y;

        start.x += drawParams->drawOffset;

        end.x = mCurrentPoints[ i ].x;
        end.y = mCurrentPoints[ i ].y;

        end.x += drawParams->drawOffset;

        ctrl1.x = start.x - ( ( start.x - end.x ) / 2 );
        ctrl1.y = start.y;

        ctrl2.x = ctrl1.x;
        ctrl2.y = end.y;

        nvgBezierTo( nvgContext, ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, end.x, end.y );
    }

    nvgStrokeColor( nvgContext, drawParams->strokeColor );
    nvgStrokeWidth( nvgContext, drawParams->strokeWidth );
    nvgStroke( nvgContext );

}
