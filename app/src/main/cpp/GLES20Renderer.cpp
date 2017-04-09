//
// Created by miroslav on 06.04.17..
//

#include "GLES20Renderer.h"

//TODO - everything up until now should maybe be put in single class VisualizerSurface which extends GLSurface

static jclass       class_GLES20Renderer;


static jclass       class_ByteBuffer;
static jmethodID    methodID_ByteBuffer_limit;

static jclass       class_PointF;
static jmethodID    methodID_PointF_constructor;

static struct NVGcontext * sNVGCtx;

static jint sWidth;
static jint sHeight;

static jint sSamplesCount;

static jint sCurvePointsCount;
static Point * sCurvePoints;
static Point * sWaveformPoints;

static CurveAnimator * sCurveAnimator;

static AudioBufferManager2 * sAudioBufferManager;

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan )
{
        class_GLES20Renderer                = env->FindClass( "mihaljevic/miroslav/foundry/slimplayer/GLES20Renderer" );

        class_PointF                        = env->FindClass( "android/graphics/PointF" );
        methodID_PointF_constructor         = env->GetMethodID( class_PointF, "<init>", "(FF)V" );

        class_ByteBuffer                    = env->FindClass( "java/nio/ByteBuffer" );
        methodID_ByteBuffer_limit           = env->GetMethodID( class_ByteBuffer, "limit", "()I" );

        class_ByteBuffer            = ( jclass ) env->NewGlobalRef( class_ByteBuffer );
        class_GLES20Renderer        = ( jclass ) env->NewGlobalRef( class_GLES20Renderer );
        class_PointF                = ( jclass ) env->NewGlobalRef( class_PointF );

        sCurvePointsCount = curvePointsCount;
        sCurvePoints = new Point[ curvePointsCount ];

        sCurveAnimator = new CurveAnimator( curvePointsCount, transitionFrames );

        sWaveformPoints = new Point[ targetSamplesCount ];

        sAudioBufferManager = new AudioBufferManager2( targetSamplesCount, targetTimeSpan );
}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_releaseNative
        ( JNIEnv * env, jobject thiz )
{
        env->DeleteGlobalRef( class_GLES20Renderer);

        env->DeleteGlobalRef( class_ByteBuffer );

        env->DeleteGlobalRef( class_PointF );
}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_initGLES
        ( JNIEnv * env, jobject thiz, jint width, jint height )
{
        if ( sNVGCtx != nullptr )
                nvgDeleteGLES2( sNVGCtx );

        //TODO - see which flags are best to use
        sNVGCtx = nvgCreateGLES2( NVG_STENCIL_STROKES | NVG_DEBUG );

        sWidth = width;
        sHeight = height;

        glClearColor( 0.85, 0.85, 0.85, 0 );

}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_releaseGLES
        ( JNIEnv * env, jobject thiz )
{
        nvgDeleteGLES2( sNVGCtx );
}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_processBuffer
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
        Buffer * buffer;

        jbyte * bufferPtr;
        jint capacity;

        bufferPtr       = ( jbyte* )env->GetDirectBufferAddress( samplesBuffer );
        capacity        = env->GetDirectBufferCapacity( samplesBuffer );

        buffer = new Buffer( bufferPtr, samplesCount, capacity );

        sAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );

}



JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_render
        ( JNIEnv * env, jobject thiz )
{

        Buffer * buffer;
        int samplesCount;


        buffer = sAudioBufferManager->getSamples();

        if ( buffer == nullptr )
                return;

        samplesCount = buffer->len;

        sSamplesCount = samplesCount;

        //Here the samples are absoluted
        calculateCurvePoints( env, buffer);

        //This needs to be called after the samples are absoluted
        calculateWaveformPoints( env, buffer );

        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );

        nvgBeginFrame( sNVGCtx, 200, 200, 1 );

        drawWaveform( sNVGCtx );

        if ( sCurveAnimator->isDone() )
                sCurveAnimator->addPoints( sCurvePoints );

        sCurveAnimator->calculateNextFrame();

        sCurveAnimator->drawCurrentFrameCurve( sNVGCtx );



        nvgEndFrame( sNVGCtx );

}


void drawWaveform(NVGcontext * nvgContext )
{
        nvgBeginPath( nvgContext );

        nvgMoveTo( nvgContext, sWaveformPoints[0].x, sWaveformPoints[0].y );

        for ( int i = 1; i < sSamplesCount; i++ )
        {
                nvgLineTo( nvgContext, sWaveformPoints[i].x, sWaveformPoints[i].y );
        }


        //TODO - stroke parameters somewhero elso, they also need to be moved from CurveAnimator::DrawCurrentCurve or something
        nvgStrokeColor( nvgContext, nvgRGBA( 54, 194, 249, 255 ) );
        nvgStrokeWidth( nvgContext, 5 );
        nvgStroke( nvgContext );
}


void calculateWaveformPoints( JNIEnv * env, Buffer * buffer )
{
        int samplesCount;

        jfloat x;
        jfloat y;

        Jrect rect( 0, 0, sWidth, sHeight / 2 );
        Point * point;

        samplesCount = buffer->len;



        for ( jint i = 0; i < samplesCount; i++ )
        {
                x = rect.getWitdth() * i / ( samplesCount - 1 );
                y = rect.getHeight() / 2 + ( ( jbyte ) ( buffer->buffer[ i ] + 128 ) ) * ( rect.getHeight() / 2 ) / 128;

                point = &sWaveformPoints[i];

                point->x = x;
                point->y = y;
        }

}


void calculateCurvePoints( JNIEnv * env, Buffer * buffer )
{
        jint        pointDistance;
        jfloat      scaledHeight;
        jfloat      scaling;
        int         samplesCount;


        jint x;
        jint y;


        jfloat maxSectorHeight;
        jint sectorSize;

        Jrect rect(0, sHeight / 2, sWidth, sHeight);


        samplesCount = buffer->len;


        pointDistance = rect.getWitdth() / ( sCurvePointsCount - 1 );



        absoluteSamples( buffer );



        scaling = ( jfloat ) rect.getHeight() / ( jfloat )128;




        sectorSize = samplesCount / sCurvePointsCount;

        for ( int i = 0; i < sCurvePointsCount; i++ )
        {
                maxSectorHeight = findMaxByte( buffer, i * sectorSize, i * sectorSize + sectorSize );

                scaledHeight = scaling * maxSectorHeight;

                x = i * pointDistance;
                //y = scaledHeight; This is when we use full screen space
                y = rect.getHeight() + scaledHeight;


                sCurvePoints[ i ].x = x;
                sCurvePoints[ i ].y = y;

        }



        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "calculateCurvePoints() EXIT");

}

jbyte findMaxByte( Buffer * buffer, int start, int end )
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

void absoluteSamples( Buffer * buffer )
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


