//
// Created by miroslav on 06.04.17..
//

#include "GLES20Renderer.h"


static jclass       class_GLES20Renderer;
static jfieldID     fieldID_GLES20Renderer_curvePoints;
static jfieldID     fieldID_GLES20Renderer_samplesCount;
static jfieldID     fieldID_GLES20Renderer_waveformPoints;
static jfieldID     fieldID_GLES20Renderer_waveformPointsFloat;


static jclass       class_ByteBuffer;
static jmethodID    methodID_ByteBuffer_limit;

static jclass       class_PointF;
static jmethodID    methodID_PointF_constructor;

static struct NVGcontext * sNVGCtx;

static jint sWidth;
static jint sHeight;

static jint sCurvePointsCount;
static Point * sCurvePoints;

static CurveAnimator * sCurveAnimator;

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames )
{
        class_GLES20Renderer                            = env->FindClass( "mihaljevic/miroslav/foundry/slimplayer/GLES20Renderer" );
        fieldID_GLES20Renderer_curvePoints              = env->GetFieldID( class_GLES20Renderer, "mCurvePoints","[Landroid/graphics/PointF;" );
        fieldID_GLES20Renderer_samplesCount             = env->GetFieldID( class_GLES20Renderer, "mSamplesCount", "I" );
        fieldID_GLES20Renderer_waveformPoints           = env->GetFieldID( class_GLES20Renderer, "mWaveformPoints", "[Landroid/graphics/PointF;" );
        fieldID_GLES20Renderer_waveformPointsFloat      = env->GetFieldID( class_GLES20Renderer, "mWaveformPointsFloat", "[F" );

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
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_drawCurve
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer )
{
        //Point * curvePoints;

        /*Point  start;
        Point  ctrl1;
        Point  ctrl2;
        Point  end;*/

        calculateCurvePoints( env, samplesBuffer);

        glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );

        nvgBeginFrame( sNVGCtx, 200, 200, 1 );

        if ( sCurveAnimator->isDone() )
                sCurveAnimator->addPoints( sCurvePoints );

        sCurveAnimator->calculateNextFrame();

        sCurveAnimator->drawCurrentFrameCurve( sNVGCtx );

        

        /*nvgBeginPath( sNVGCtx );
        

        nvgMoveTo( sNVGCtx, sCurvePoints[0].x, sCurvePoints[0].y );

        for ( int i = 1; i < sCurvePointsCount; i++ )
        {
                start.x = sCurvePoints[ i - 1 ].x;
                start.y = sCurvePoints[ i - 1 ].y;

                end.x = sCurvePoints[ i ].x;
                end.y = sCurvePoints[ i ].y;

                ctrl1.x = start.x - ( ( start.x - end.x ) / 2 );
                ctrl1.y = start.y;

                ctrl2.x = ctrl1.x;
                ctrl2.y = end.y;
                
                nvgBezierTo( sNVGCtx, ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, end.x, end.y );
        }

        nvgStrokeColor( sNVGCtx, nvgRGBA( 54, 194, 249, 255 ) );
        nvgStrokeWidth( sNVGCtx, 10 );
        nvgStroke( sNVGCtx );*/

        /*nvgBeginPath( sNVGCtx );
        nvgRect( sNVGCtx, 20, 20, 100, 100 );
        nvgFillColor( sNVGCtx, nvgRGBA( 255, 192, 0, 255 ) );
        nvgFill( sNVGCtx );*/

        nvgEndFrame( sNVGCtx );

}


void calculateCurvePoints( JNIEnv * env, jobject samplesBuffer )
{
        jint        pointDistance;
        jfloat      scaledHeight;
        jfloat      scaling;


        jint x;
        jint y;

        jbyte * samplesBufferPtr;
        jint    samplesCount;

        jfloat maxSectorHeight;
        jint sectorSize;

        //Point  curvePoints[ sCurvePointsCount ]; //TODO - optimize, to static variable



        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "calculateCurvePoints()");


        samplesCount        = env->CallIntMethod( samplesBuffer, methodID_ByteBuffer_limit );
        samplesBufferPtr    = (jbyte*)env->GetDirectBufferAddress( samplesBuffer );


        pointDistance = sWidth / ( sCurvePointsCount - 1 );



        absoluteSamples( samplesBufferPtr, samplesCount );



        scaling = ( jfloat ) sHeight / ( jfloat )128;




        sectorSize = samplesCount / sCurvePointsCount;

        for ( int i = 0; i < sCurvePointsCount; i++ )
        {
                maxSectorHeight = findMaxByte( samplesBufferPtr, i * sectorSize, i * sectorSize + sectorSize );

                scaledHeight = scaling * maxSectorHeight;

                x = i * pointDistance;
                //y = sHeight + scaledHeight;
                y = scaledHeight;


                sCurvePoints[ i ].x = x;
                sCurvePoints[ i ].y = y;

        }



        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "calculateCurvePoints() EXIT");

}

jbyte findMaxByte( jbyte * buffer, int start, int end )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "findMaxBytes()");


        jbyte max = -128;

        for ( int i = start; i < end; i++ )
        {
                if ( buffer[i] > max )
                        max = buffer[i];
        }

        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "findMaxBytes DONE()");

        return max;
}

void absoluteSamples( jbyte * bufferPtr, jint count )
{
        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "absoluteSamples()");

        jbyte absolutedSample;

        for ( int i = 0; i < count; i++ )
        {
                absolutedSample = ( jbyte ) abs( bufferPtr[i] );
                bufferPtr[i]    = absolutedSample;
        }

        //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "absoluteSamples() - DONE");
}


