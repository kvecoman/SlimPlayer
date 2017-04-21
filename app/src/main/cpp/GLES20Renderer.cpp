//
// Created by miroslav on 06.04.17..
//

#include "GLES20Renderer.h"



JNIEXPORT jlong JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan, jint strokeWidth )
{

        jlong instancePtr;

        instancePtr = ( ( jlong )( new GLES20Renderer( curvePointsCount,  transitionFrames,  targetSamplesCount,  targetTimeSpan, strokeWidth ) ) );

        return instancePtr;
        
}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_deleteNativeInstance
        ( JNIEnv * env, jobject thiz, jlong objPtr )
{
        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        delete instance;
}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initGLES
        ( JNIEnv * env, jobject thiz, jlong objPtr, jint width, jint height )
{
        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->initGLES( width, height );

}



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





GLES20Renderer::GLES20Renderer( jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan, jint strokeWidth )
{
        mInstance               = sInstanceNumber++;

        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "GLES20Renderer() - constructor for instance %i", mInstance );

        mCurvePointsCount       = curvePointsCount;

        mCurvePoints            = new Point[ curvePointsCount ];

        mCurveAnimator          = new CurveAnimator( curvePointsCount, transitionFrames, strokeWidth );

        mWaveformPoints         = new Point[ targetSamplesCount ];

        mAudioBufferManager     = new AudioBufferManager( targetSamplesCount, targetTimeSpan, mInstance );

        mStrokeWidth            = strokeWidth;



}

GLES20Renderer::~GLES20Renderer()
{
        //sNVGCreateLock.lock();
        //mNVGContextLock.lock();

        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "~GLES20Renderer() - destructor for instance %i", mInstance );

        mReleased = true;

        delete[] mCurvePoints;
        delete[] mWaveformPoints;

        delete mCurveAnimator;
        delete mAudioBufferManager;

        if ( mNVGCtx != nullptr )
                nvgDeleteGLES2( mNVGCtx );


        //mNVGContextLock.unlock();
        //sNVGCreateLock.unlock();
}

void GLES20Renderer::initGLES( int width, int height )
{
        //sNVGCreateLock.lock();
        //mNVGContextLock.lock();


        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "initGLES() for instance %i", mInstance );
        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "width: %i, height: %i", width, height );


        if ( mNVGCtx != nullptr )
                nvgDeleteGLES2( mNVGCtx );

        mNVGCtx = nvgCreateGLES2( NVG_DEBUG );

        mWidth = width;
        mHeight = height;

        glColorMask( 1, 1, 1, 1 );
        glClearColor( 0, 0, 0, 0 );
        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );


        //mNVGContextLock.unlock();
        //sNVGCreateLock.unlock();
}



void GLES20Renderer::render()
{

        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "render() for instance %i", mInstance );

        //mNVGContextLock.lock();

        Buffer * buffer;
        int samplesCount;

        if ( mNVGCtx == nullptr || mReleased )
        {
            //mNVGContextLock.unlock();
            return;
        }


        buffer = mAudioBufferManager->getSamples();

        if ( buffer == nullptr )
        {
                //mNVGContextLock.unlock();
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

        //mNVGContextLock.unlock();
}




void GLES20Renderer::drawWaveform(NVGcontext * nvgContext )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "drawWaveform() for instance %i", mInstance );

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
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "calculateWaveformPoints() for instance %i", mInstance );

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
                y = rect.getHeight() - ( ( jint )( scaling * buffer->buffer[i] ) );

                point = &mWaveformPoints[i];

                point->x = x;
                point->y = y;
        }

}


void GLES20Renderer::calculateCurvePoints( Buffer * buffer )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "calculateCurvePoints() for instance %i", mInstance );

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
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "findMaxBytes() for instance %i", mInstance );


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

        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "absoluteSamples() for instance %i", mInstance );

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


