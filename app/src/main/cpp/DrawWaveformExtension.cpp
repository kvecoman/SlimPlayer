//
// Created by miroslav on 05.05.17..
//

#include "DrawWaveformExtension.h"
#include "Rect.h"

DrawWaveformExtension::~DrawWaveformExtension()
{
    if ( mWaveformPoints != nullptr )
        delete mWaveformPoints;
}

void DrawWaveformExtension::initWaveformPoints( int targetSamplesCount )
{
    if ( mWaveformPoints != nullptr )
        delete mWaveformPoints;

    mWaveformPoints = new Point[ targetSamplesCount ];
}

void DrawWaveformExtension::drawWaveform( NVGcontext * nvgContext, Buffer * samplesBuffer, DrawParams * drawParams )
{

    if ( nvgContext == nullptr )
        return;

    calculateWaveformPoints( samplesBuffer, drawParams );

    nvgBeginPath( nvgContext );

    nvgMoveTo( nvgContext, mWaveformPoints[0].x + drawParams->drawOffset, mWaveformPoints[0].y );

    for ( int i = 1; i < samplesBuffer->len; i++ )
    {
        nvgLineTo( nvgContext, mWaveformPoints[i].x + drawParams->drawOffset, mWaveformPoints[i].y );
    }


    nvgStrokeColor( nvgContext, drawParams->strokeColor );
    nvgStrokeWidth( nvgContext, 2 );
    nvgStroke( nvgContext );
}


void DrawWaveformExtension::calculateWaveformPoints( Buffer * buffer, DrawParams * drawParams )
{

    if ( buffer == nullptr )
        return;

    int samplesCount;
    jfloat scaling;
    Rect rect( 0, 0, drawParams->screenWidth, drawParams->screenHeight  );


    samplesCount    = buffer->len;
    scaling         = ( jfloat ) rect.getHeight() / ( jfloat ) 127;

    for ( jint i = 0; i < samplesCount; i++ )
    {
        //Optimized version
        mWaveformPoints[i].x = rect.getWitdth() * i / ( samplesCount - 1 );
        mWaveformPoints[i].y = rect.getHeight() - ( ( jint )( scaling * buffer->buffer[i] ) );

    }

}