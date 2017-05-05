//
// Created by miroslav on 06.04.17..
//

#include "GLES20Renderer.h"

#include "nanovg/nanovg_gl.h"







GLES20Renderer::GLES20Renderer( jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan/*, jint strokeWidth*/,jboolean exoAudioBufferManager )
{
        mConstructorLock.lock();

        mCurvePointsCount           = curvePointsCount;

        mCurvePoints                = new Point[ curvePointsCount ];

        mCurveAnimator              = new CurveAnimator( curvePointsCount, transitionFrames );

        //Comment out if we are drawing waveform
        //initWaveformPoints( targetSamplesCount );

        if ( exoAudioBufferManager )
            mAudioBufferManager     = new AudioBufferManagerExo( targetSamplesCount, targetTimeSpan );
        else
            mAudioBufferManager     = new AudioBufferManagerMedia( targetSamplesCount, targetTimeSpan );

        mDrawParams                 = new DrawParams( -1, -1, STROKE_WIDTH, 0, STROKE_COLOR );

        mConstructed = true;

        mConstructorLock.unlock();

}

GLES20Renderer::~GLES20Renderer()
{

        mConstructorLock.lock();


        mDeleted = true;

        if ( !mConstructed )
        {
            mConstructorLock.unlock();
            return;
        }


        delete[] mCurvePoints;


        delete mCurveAnimator;
        delete mAudioBufferManager;

        delete mDrawParams;


        mConstructorLock.unlock();

}

void GLES20Renderer::initNVG( int width, int height, float density )
{


         if ( mDeleted || !mConstructed )
            return;



        mConstructorLock.lock();

        mDensity = density;

        mGLESReleased = true;

        if ( mNVGCtx != nullptr )
            nvgDeleteGLES2( mNVGCtx );




        mNVGCtx = nvgCreateGLES2( NVG_DEBUG );

        mGLESReleased = false;

        mDrawParams->screenWidth    = width;
        mDrawParams->screenHeight   = height;

        glColorMask( 1, 1, 1, 1 );
        glClearColor( 0, 0, 0, 0 );
        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );

        mConstructorLock.unlock();
}


void GLES20Renderer::releaseNVG()
{

    mConstructorLock.lock();


    mGLESReleased = true;

    if ( mNVGCtx != nullptr )
        nvgDeleteGLES2( mNVGCtx );

    mConstructorLock.unlock();

}




void GLES20Renderer::render( int drawOffset )
{


        mConstructorLock.lock();

        if ( mNVGCtx == nullptr || mDeleted || mGLESReleased )
        {
            mConstructorLock.unlock();
            return;
        }




        Buffer * samplesBuffer;

        mDrawParams->drawOffset = drawOffset;


        samplesBuffer = mAudioBufferManager->getSamples();

        if ( samplesBuffer == nullptr )
        {
                mConstructorLock.unlock();
                return;
        }



        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );


        nvgBeginFrame( mNVGCtx, mDrawParams->screenWidth, mDrawParams->screenHeight, mDensity );

        //drawWaveform( mNVGCtx, samplesBuffer, mDrawParams );


        if ( mCurveAnimator->isDone() )
        {
                calculateCurvePoints( samplesBuffer);
                mCurveAnimator->addPoints( mCurvePoints );
        }


        mCurveAnimator->calculateNextFrame();
        mCurveAnimator->drawCurrentFrameCurve( mNVGCtx, mDrawParams );




        nvgEndFrame( mNVGCtx );

        mConstructorLock.unlock();

}







void GLES20Renderer::calculateCurvePoints( Buffer * buffer )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "calculateCurvePoints() for instance %i", mInstance );

        if ( buffer == nullptr )
            return;

        jint        pointDistance;
        jfloat      scaledHeight;
        jfloat      scaling;
        int         samplesCount;


        jint x;
        jint y;


        jint maxSectorHeight;
        jint sectorSize;

        jint yOffset;

        //With stroke offset we make sure that strokes are'nt drawn outside of canvas
        yOffset = ( (double)STROKE_WIDTH ) / 2.0;

        Jrect rect( 0, yOffset, mDrawParams->screenWidth, mDrawParams->screenHeight - yOffset );


        samplesCount = buffer->len;


        pointDistance = rect.getWitdth() / ( mCurvePointsCount - 1 );


        scaling = ( ( jfloat ) rect.getHeight() ) / ( ( jfloat )127 );




        sectorSize = samplesCount / mCurvePointsCount;

        for ( int i = 0; i < mCurvePointsCount; i++ )
        {
                maxSectorHeight = findMaxByte( buffer, i * sectorSize, i * sectorSize + sectorSize );

                scaledHeight = scaling * ( (float)maxSectorHeight );

                x = i * pointDistance;
                y = rect.getHeight() - scaledHeight + rect.starty;



                mCurvePoints[ i ].x = x;
                mCurvePoints[ i ].y = y;

        }

}

jbyte GLES20Renderer::findMaxByte( Buffer * buffer, int start, int end )
{

        if ( buffer == nullptr )
            return 0;


        jbyte max = -128;

        for ( int i = start; i < end; i++ )
        {
                if ( buffer->buffer[i] > max )
                        max = buffer->buffer[i];
        }



        return max;
}

/*void GLES20Renderer::absoluteSamples( Buffer * buffer )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "absoluteSamples()");

        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "absoluteSamples() for instance %i", mInstance );

        jbyte   absolutedSample;
        int     count;

        if ( buffer == nullptr )
            return;

        count = buffer->len;

        for ( int i = 0; i < count; i++ )
        {
                absolutedSample = ( jbyte ) abs( buffer->buffer[i] );
                buffer->buffer[i]     = absolutedSample;
        }

        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "absoluteSamples() - DONE");
}*/
