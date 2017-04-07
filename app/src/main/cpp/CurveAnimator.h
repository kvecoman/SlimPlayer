//
// Created by miroslav on 07.04.17..
//

#ifndef SLIMPLAYER_CURVEANIMATOR_H
#define SLIMPLAYER_CURVEANIMATOR_H

#include "Point.h"
#include "nanovg/nanovg.h"
#include <math.h>
#include <android/log.h>

class CurveAnimator
{
private:
    //Number of points in curve
    int mPointsCount = 0;

    //Length of animation in frames
    int mFramesCount = 0;

    //Current active frame
    int mFrameIndex = 0;

    //Start points
    Point * mStartPoints;

    //Calculated mid points between start and end points for current frame
    Point * mCurrentPoints;

    //End points
    Point * mEndPoints;

    //Is the current animation cycle done
    bool mDone = true;

    //Are we adding points for first time
    bool mFirstAdd = true;

public:
    Point CUBIC_BEZIER_EASE_IN_OUT[4] =  {
            Point( 0, 0 ),
            Point( 0.35f, 0 ),
            Point( 0.65f, 1 ),
            Point( 1, 1 )
    };

    Point CUBIC_BEZIER_EASE_IN[4] =  {
            Point( 0, 0 ),
            Point( 0.35f, 0 ),
            Point( 1, 1 ),
            Point( 1, 1 )
    };

    Point CUBIC_BEZIER_LINEAR[4] =  {
            Point( 0, 0 ),
            Point( 0, 0 ),
            Point( 1, 1 ),
            Point( 1, 1 )
    };



    CurveAnimator( int pointCount, int frameCount );

    bool isDone();

    void addPoints( Point points[] );

    void calculateNextFrame();

    //Calculates value effect for current state x ( x is between 0 ( 0% ) and 1 ( 100% ) )
    float percentageFromCubicBezier( float x, Point bezierPoints[] );


    void drawCurrentFrameCurve( NVGcontext * nvgContext );
    
    
};


#endif //SLIMPLAYER_CURVEANIMATOR_H
