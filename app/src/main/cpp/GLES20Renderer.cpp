//
// Created by miroslav on 06.04.17..
//

#include "GLES20Renderer.h"



JNIEXPORT jlong JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan/*, jint strokeWidth*/ )
{

        jlong instancePtr;

        instancePtr = ( ( jlong )( new GLES20Renderer( curvePointsCount,  transitionFrames,  targetSamplesCount,  targetTimeSpan/*, strokeWidth */) ) );

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
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNVG
        ( JNIEnv * env, jobject thiz, jlong objPtr, jint width, jint height, jfloat density )
{
        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->initNVG( width, height, density );

}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseNVG ( JNIEnv * env, jobject thiz, jlong objPtr )
{
    GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;

    instance->releaseNVG();
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

        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "processBuffer() - capacity is %i for instance %i", capacity,instance->mInstance );

        buffer = new Buffer( bufferPtr, samplesCount, capacity );

        if ( /*instance->mEnabled &&*/ instance->mConstructed && !instance->mDeleted && instance->mAudioBufferManager != nullptr )
            instance->mAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );

}



JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz, jlong objPtr, jint drawOffset )
{

        GLES20Renderer * instance;

        instance = (GLES20Renderer*)objPtr;

        instance->render( drawOffset );

}

/*JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_enable
        ( JNIEnv * env, jobject thiz, jlong objPtr )
{
    GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;

    //instance->enable();
}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_disable
        ( JNIEnv * env, jobject thiz, jlong objPtr )
{
    GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;

    //instance->disable();
}*/





GLES20Renderer::GLES20Renderer( jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan/*, jint strokeWidth*/ )
{
        mConstructorLock.lock();

        mInstance               = sInstanceNumber++;

        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "GLES20Renderer() - constructor for instance %i", mInstance );

        mCurvePointsCount       = curvePointsCount;

        mCurvePoints            = new Point[ curvePointsCount ];

        mCurveAnimator          = new CurveAnimator( curvePointsCount, transitionFrames );

        mWaveformPoints         = new Point[ targetSamplesCount ];

        mAudioBufferManager     = new AudioBufferManager( targetSamplesCount, targetTimeSpan, mInstance );

        //mStrokeWidth            = strokeWidth;


        mConstructed = true;

        mConstructorLock.unlock();

}

GLES20Renderer::~GLES20Renderer()
{
        //sNVGCreateLock.lock();
        //mNVGContextLock.lock();

        mConstructorLock.lock();

        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "~GLES20Renderer() - destructor for instance %i", mInstance );



        mDeleted = true;

        if ( !mConstructed )
        {
            mConstructorLock.unlock();
            return;
        }


        delete[] mCurvePoints;
        delete[] mWaveformPoints;

        delete mCurveAnimator;
        delete mAudioBufferManager;

        //releaseNVG();


        mConstructorLock.unlock();
        //mNVGContextLock.unlock();
        //sNVGCreateLock.unlock();
}

void GLES20Renderer::initNVG( int width, int height, float density )
{


         if ( mDeleted || /*!mEnabled ||*/ !mConstructed )
            return;

        //sNVGCreateLock.lock();
        //mNVGContextLock.lock();

        mConstructorLock.lock();
        __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "initNVG() for instance %i", mInstance );

        mDensity = density;

        mGLESReleased = true;

        if ( mNVGCtx != nullptr )
            nvgDeleteGLES2( mNVGCtx );






        mNVGCtx = nvgCreateGLES2( NVG_DEBUG );

        mGLESReleased = false;

        mWidth  = width;
        mHeight = height;

        glColorMask( 1, 1, 1, 1 );
        glClearColor( 0, 0, 0, 0 );
        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );

        mConstructorLock.unlock();
        //mNVGContextLock.unlock();
        //sNVGCreateLock.unlock();
}


void GLES20Renderer::releaseNVG()
{



    //mNVGContextLock.lock();

    mConstructorLock.lock();
    __android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "releaseNVG() for instance %i", mInstance );

    mGLESReleased = true;

    if ( mNVGCtx != nullptr )
        nvgDeleteGLES2( mNVGCtx );

    mConstructorLock.unlock();

    //mNVGContextLock.unlock();
}




void GLES20Renderer::render( int drawOffset )
{



        //mNVGContextLock.lock();
        mConstructorLock.lock();

        if ( mNVGCtx == nullptr || mDeleted || mGLESReleased /*|| !mEnabled*/ )
        {
            //mNVGContextLock.unlock();
            mConstructorLock.unlock();
            return;
        }

        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "render() for instance %i", mInstance );



        Buffer * buffer;
        int samplesCount;

        mDrawOffset = drawOffset;


        buffer = mAudioBufferManager->getSamples();

        if ( buffer == nullptr )
        {
                //mNVGContextLock.unlock();
                mConstructorLock.unlock();
                return;
        }


        samplesCount    = buffer->len;
        mSamplesCount   = samplesCount;

        //Here the samples are absoluted
        absoluteSamples( buffer );

        //calculateWaveformPoints(  buffer );

        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );


        nvgBeginFrame( mNVGCtx, mWidth, mHeight, mDensity );

        //drawWaveform( mNVGCtx );


        if ( mCurveAnimator->isDone() )
        {
                calculateCurvePoints( buffer);
                mCurveAnimator->addPoints( mCurvePoints );
        }


        mCurveAnimator->calculateNextFrame();
        mCurveAnimator->drawCurrentFrameCurve( mNVGCtx, mDrawOffset, &STROKE_COLOR, STROKE_WIDTH );



        /*nvgRect( mNVGCtx, 0, 0, mWidth, mHeight );
        nvgFillColor( mNVGCtx, nvgRGBA( 230, 207, 0, 128 ) );
        nvgFill( mNVGCtx );*/


        nvgEndFrame( mNVGCtx );

        mConstructorLock.unlock();

        //mNVGContextLock.unlock();
}




void GLES20Renderer::drawWaveform(NVGcontext * nvgContext )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "drawWaveform() for instance %i", mInstance );

        if ( nvgContext == nullptr )
            return;

        nvgBeginPath( nvgContext );

        nvgMoveTo( nvgContext, mWaveformPoints[0].x + mDrawOffset, mWaveformPoints[0].y );

        for ( int i = 1; i < mSamplesCount; i++ )
        {
                nvgLineTo( nvgContext, mWaveformPoints[i].x + mDrawOffset, mWaveformPoints[i].y );
        }


        nvgStrokeColor( nvgContext, STROKE_COLOR );
        nvgStrokeWidth( nvgContext, 2 );
        nvgStroke( nvgContext );
}


void GLES20Renderer::calculateWaveformPoints( Buffer * buffer )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "calculateWaveformPoints() for instance %i", mInstance );

        if ( buffer == nullptr )
            return;

        int samplesCount;

        jfloat x;
        jfloat y;

        jfloat scaling;

        jint strokeOffset;

        strokeOffset = STROKE_WIDTH;

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

        Jrect rect(0, /*( mHeight / 2 ) +*/ yOffset, mWidth, mHeight - yOffset);


        samplesCount = buffer->len;


        pointDistance = rect.getWitdth() / ( mCurvePointsCount - 1 );


        scaling = ( ( jfloat ) rect.getHeight() ) / ( ( jfloat )127 );




        sectorSize = samplesCount / mCurvePointsCount;

        for ( int i = 0; i < mCurvePointsCount; i++ )
        {
                maxSectorHeight = findMaxByte( buffer, i * sectorSize, i * sectorSize + sectorSize );

                scaledHeight = scaling * ( (float)maxSectorHeight );

                x = i * pointDistance;
                //y = scaledHeight; This is when we use full screen space
                //y = rect.getHeight() + scaledHeight; When we use half surface but draw towards bottom
                y = rect.getHeight() - scaledHeight + rect.starty;


                /*if ( maxSectorHeight > 110 )
                {
                    __android_log_print( ANDROID_LOG_INFO, "GLES20Renderer", "mWidth: %i, mHeight: %i\n rect width: %i, rect height: %i\n maxSectorHeight: %i, scaling: %f, scaledHeight: %f\n rect top: %i, y: %i",
                                                                                mWidth, mHeight, rect.getWitdth(), rect.getHeight(), maxSectorHeight, scaling, scaledHeight, yOffset, y );
                }*/

                mCurvePoints[ i ].x = x;
                mCurvePoints[ i ].y = y;

        }



        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "calculateCurvePoints() EXIT");

}

jbyte GLES20Renderer::findMaxByte( Buffer * buffer, int start, int end )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "findMaxBytes()");
        //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "findMaxBytes() for instance %i", mInstance );

        if ( buffer == nullptr )
            return 0;


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

        if ( buffer == nullptr )
            return;

        count = buffer->len;

        for ( int i = 0; i < count; i++ )
        {
                absolutedSample = ( jbyte ) abs( buffer->buffer[i] );
                buffer->buffer[i]     = absolutedSample;
        }

        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "absoluteSamples() - DONE");
}



/*void GLES20Renderer::enable()
{
    mConstructorLock.lock();

    mEnabled = true;

    if ( mAudioBufferManager != nullptr )
        mAudioBufferManager->enable();

    mConstructorLock.unlock();
}*/

/*void GLES20Renderer::disable()
{
    mConstructorLock.lock();

    mEnabled = false;

    if ( mAudioBufferManager != nullptr )
        mAudioBufferManager->disable();

    mConstructorLock.unlock();
}*/
