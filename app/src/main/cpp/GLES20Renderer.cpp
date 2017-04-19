//
// Created by miroslav on 06.04.17..
//

#include "GLES20Renderer.h"

//static std::vector<GLES20RendererData> sClassData;

//static struct NVGcontext * mNVGCtx;

/*static jint sWidth;
static jint sHeight;

static jint sSamplesCount;

static jint sCurvePointsCount;
static Point * sCurvePoints;
static Point * sWaveformPoints;

static CurveAnimator * sCurveAnimator;

static AudioBufferManager * sAudioBufferManager;*/



JNIEXPORT jlong JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan, jint strokeWidth )
{

        jlong instancePtr;

        instancePtr = ( ( jlong )( new GLES20Renderer( curvePointsCount,  transitionFrames,  targetSamplesCount,  targetTimeSpan, strokeWidth ) ) );

        return instancePtr;
        
}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseNative
        ( JNIEnv * env, jobject thiz, jlong objPtr )
{
        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->releaseNative();

        //instance->mScheduledForDelete = true;

        delete instance;
}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initGLES
        ( JNIEnv * env, jobject thiz, jlong objPtr, jint width, jint height, jfloat clearRed, jfloat clearGreen, jfloat clearBlue  )
{
        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->initGLES( width, height, clearRed,  clearGreen, clearBlue );

}

/*JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseGLES
        ( JNIEnv * env, jobject thiz, jlong objPtr )
{
        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->releaseGLES();

}*/

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBuffer
        ( JNIEnv * env, jobject thiz, jlong objPtr, jobject samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
        Buffer * buffer;

        GLES20Renderer * instance;

        jbyte * bufferPtr;
        jint capacity;

        instance = (GLES20Renderer*)objPtr;

        bufferPtr       = ( jbyte* )env->GetDirectBufferAddress( samplesBuffer );
        capacity        = env->GetDirectBufferCapacity( samplesBuffer );

        buffer = new Buffer( bufferPtr, samplesCount, capacity );

        instance->mAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );


}



JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz, jlong objPtr )
{

        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->render();



}



JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_reset
        ( JNIEnv * env, jobject thiz, jlong objPtr )
{
        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->reset();
}


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_deleteNVGContexts
        ( JNIEnv * env, jobject thiz )
{
        sNVGCreateLock.lock();

        for (std::list<NVGcontext*>::const_iterator iterator = sNVGDeleteList.begin(), end = sNVGDeleteList.end(); iterator != end; ++iterator)
        {
                if ( *iterator != nullptr )
                        nvgDeleteGLES2( *iterator );

                sNVGDeleteList.remove( *iterator );
        }

        sNVGCreateLock.unlock();
}













GLES20Renderer::GLES20Renderer( jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan, jint strokeWidth )
{
        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "GLES20Renderer() - constructor" );

        mCurvePointsCount       = curvePointsCount;

        mCurvePoints            = new Point[ curvePointsCount ];

        mCurveAnimator          = new CurveAnimator( curvePointsCount, transitionFrames, strokeWidth );

        mWaveformPoints         = new Point[ targetSamplesCount ];

        mAudioBufferManager     = new AudioBufferManager( targetSamplesCount, targetTimeSpan );

        mStrokeWidth            = strokeWidth;

}

GLES20Renderer::~GLES20Renderer()
{
        sNVGCreateLock.lock();

        mNVGContextLock.lock();

        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "~GLES20Renderer() - destructor" );

        delete[] mCurvePoints;
        delete[] mWaveformPoints;

        delete mCurveAnimator;

        delete mAudioBufferManager;



        //TODO - continue here - make this work, the new method of deleting later doesn't work ( try makking minialist use of nanoVG and see if you can delete it then in demo activity)
        if ( mNVGCtx != nullptr )
                nvgDeleteGLES2( mNVGCtx );

        /*if ( mNVGCtx != nullptr )
                sNVGDeleteList.push_back( mNVGCtx );*/

        mNVGContextLock.unlock();

        sNVGCreateLock.unlock();
}

void GLES20Renderer::releaseNative()
{

}

void GLES20Renderer::initGLES( int width, int height, jfloat clearRed, jfloat clearGreen, jfloat clearBlue )
{
        sNVGCreateLock.lock();

        mNVGContextLock.lock();

        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "initGLES()" );

        //TODO - see which flags are best to use
        if ( mNVGCtx != nullptr )
                nvgDeleteGLES2( mNVGCtx );

        /*if ( mNVGCtx != nullptr )
                sNVGDeleteList.push_back( mNVGCtx );*/


        mNVGCtx = nvgCreateGLES2( 0/*NVG_STENCIL_STROKES | NVG_DEBUG*/ );


        mWidth = width;
        mHeight = height;

        glColorMask( 1, 1, 1, 1 );
        //glClearColor( clearRed, clearGreen, clearBlue, 0 );
        glClearColor( 0, 0, 0, 0 );
        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );

        mNVGContextLock.unlock();

        sNVGCreateLock.unlock();
}


void GLES20Renderer::render()
{
        //THis needs tobe outside of lock
        /*if ( mScheduledForDelete )
        {
                delete this;
                return;
        }*/

        mNVGContextLock.lock();

        Buffer * buffer;
        int samplesCount;




        buffer = mAudioBufferManager->getSamples();

        if ( buffer == nullptr )
        {
                mNVGContextLock.unlock();
                return;
        }


        samplesCount    = buffer->len;
        mSamplesCount   = samplesCount;

        //Here the samples are absoluted
        absoluteSamples( buffer );


        calculateWaveformPoints(  buffer );

        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );

        //TODO - see if you need different pixel ratio for HIDP devices
        nvgBeginFrame( mNVGCtx, mWidth, mHeight, 1 );

        drawWaveform( mNVGCtx );


        if ( mCurveAnimator->isDone() )
        {
                calculateCurvePoints( buffer);
                mCurveAnimator->addPoints( mCurvePoints );
        }


        mCurveAnimator->calculateNextFrame();

        mCurveAnimator->drawCurrentFrameCurve( mNVGCtx );


        nvgEndFrame( mNVGCtx );

        mNVGContextLock.unlock();
}

void GLES20Renderer::reset()
{
        //TODO - remove this
        /*mAudioBufferManager->reset();
        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );*/

}


void GLES20Renderer::drawWaveform(NVGcontext * nvgContext )
{
        nvgBeginPath( nvgContext );

        nvgMoveTo( nvgContext, mWaveformPoints[0].x, mWaveformPoints[0].y );

        for ( int i = 1; i < mSamplesCount; i++ )
        {
                nvgLineTo( nvgContext, mWaveformPoints[i].x, mWaveformPoints[i].y );
        }


        //TODO - stroke parameters somewhero elso, they also need to be moved from CurveAnimator::DrawCurrentCurve or something
        nvgStrokeColor( nvgContext, nvgRGBA( 54, 194, 249, 255 ) );
        nvgStrokeWidth( nvgContext, 2 );
        nvgStroke( nvgContext );
}


void GLES20Renderer::calculateWaveformPoints( Buffer * buffer )
{
        int samplesCount;

        jfloat x;
        jfloat y;

        jfloat scaling;

        jint strokeOffset;

        strokeOffset = mStrokeWidth;

        Jrect rect( 0, ( 0 + strokeOffset ), mWidth, ( mHeight / 2 ) - strokeOffset );
        Point * point;

        samplesCount = buffer->len;

        scaling = ( jfloat ) rect.getHeight() / ( jfloat ) 128;

        for ( jint i = 0; i < samplesCount; i++ )
        {
                x = rect.getWitdth() * i / ( samplesCount - 1 );
                //y = rect.getHeight() / 2 + ( ( jbyte ) ( buffer->buffer[ i ] + 128 ) ) * ( rect.getHeight() / 2 ) / 128;
                y = rect.getHeight() - ( ( jint )( scaling * buffer->buffer[i] ) );

                point = &mWaveformPoints[i];

                point->x = x;
                point->y = y;
        }

}


void GLES20Renderer::calculateCurvePoints( Buffer * buffer )
{
        jint        pointDistance;
        jfloat      scaledHeight;
        jfloat      scaling;
        int         samplesCount;


        jint x;
        jint y;


        jfloat maxSectorHeight;
        jint sectorSize;

        jint strokeOffset;

        //With stroke offset we make sure that strokes are'nt drawn outside of canvas
        strokeOffset = mStrokeWidth;

        Jrect rect(0, ( mHeight / 2 ) + strokeOffset, mWidth, mHeight - strokeOffset);


        samplesCount = buffer->len;


        pointDistance = rect.getWitdth() / ( mCurvePointsCount - 1 );


        scaling = ( jfloat ) rect.getHeight() / ( jfloat )128;




        sectorSize = samplesCount / mCurvePointsCount;

        for ( int i = 0; i < mCurvePointsCount; i++ )
        {
                maxSectorHeight = findMaxByte( buffer, i * sectorSize, i * sectorSize + sectorSize );

                scaledHeight = scaling * maxSectorHeight;

                x = i * pointDistance;
                //y = scaledHeight; This is when we use full screen space
                //y = rect.getHeight() + scaledHeight; When we use half surface but draw towards bottom
                y = mHeight - scaledHeight;


                mCurvePoints[ i ].x = x;
                mCurvePoints[ i ].y = y;

        }



        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "calculateCurvePoints() EXIT");

}

jbyte GLES20Renderer::findMaxByte( Buffer * buffer, int start, int end )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "findMaxBytes()");


        jbyte max = -128;

        for ( int i = start; i < end; i++ )
        {
                if ( buffer->buffer[i] > max )
                        max = buffer->buffer[i];
        }

        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "findMaxBytes DONE()");

        return max;
}

void GLES20Renderer::absoluteSamples( Buffer * buffer )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "absoluteSamples()");

        jbyte   absolutedSample;
        int     count;

        count = buffer->len;

        for ( int i = 0; i < count; i++ )
        {
                absolutedSample = ( jbyte ) abs( buffer->buffer[i] );
                buffer->buffer[i]     = absolutedSample;
        }

        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "absoluteSamples() - DONE");
}


