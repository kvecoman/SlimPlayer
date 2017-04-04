//
// Created by miroslav on 01.04.17..
//

#include "VisualizerView.h"

static jclass       class_VisualizerView;
static jfieldID     fieldID_VisualizerView_curvePoints;
static jfieldID     fieldID_VisualizerView_samplesCount;
static jfieldID     fieldID_VisualizerView_waveformPoints;
static jfieldID     fieldID_VisualizerView_waveformPointsFloat;
static jmethodID    methodID_VisualizerView_getWidth;
static jmethodID    methodID_VisualizerView_getHeight;

static jclass       class_PointF;
static jfieldID     fieldID_PointF_x;
static jfieldID     fieldID_PointF_y;
static jmethodID    methodID_PointF_constructor;

static jclass       class_ByteBuffer;
static jmethodID    methodID_ByteBuffer_limit;



JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_init
        ( JNIEnv * env, jobject thiz )
{
    class_VisualizerView                            = env->FindClass( "mihaljevic/miroslav/foundry/slimplayer/VisualizerView" );
    fieldID_VisualizerView_curvePoints              = env->GetFieldID( class_VisualizerView, "mCurvePoints","[Landroid/graphics/PointF;" );
    fieldID_VisualizerView_samplesCount             = env->GetFieldID( class_VisualizerView, "mSamplesCount", "I" );
    fieldID_VisualizerView_waveformPoints           = env->GetFieldID( class_VisualizerView, "mWaveformPoints", "[Landroid/graphics/PointF;" );
    fieldID_VisualizerView_waveformPointsFloat      = env->GetFieldID( class_VisualizerView, "mWaveformPointsFloat", "[F" );
    methodID_VisualizerView_getWidth                = env->GetMethodID( class_VisualizerView, "getWidth", "()I" );
    methodID_VisualizerView_getHeight               = env->GetMethodID( class_VisualizerView, "getHeight", "()I" );

    class_PointF                        = env->FindClass( "android/graphics/PointF" );
    fieldID_PointF_x                    = env->GetFieldID( class_PointF, "x", "F" );
    fieldID_PointF_y                    = env->GetFieldID( class_PointF, "y", "F" );
    methodID_PointF_constructor         = env->GetMethodID( class_PointF, "<init>", "(FF)V" );

    class_ByteBuffer                    = env->FindClass( "java/nio/ByteBuffer" );
    methodID_ByteBuffer_limit           = env->GetMethodID( class_ByteBuffer, "limit", "()I" );

    class_ByteBuffer            = ( jclass ) env->NewGlobalRef( class_ByteBuffer );
    class_VisualizerView        = ( jclass ) env->NewGlobalRef( class_VisualizerView );
    class_PointF                = ( jclass ) env->NewGlobalRef( class_PointF );
}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_destroy
        ( JNIEnv * env, jobject thiz )
{
    env->DeleteGlobalRef( class_VisualizerView );

    env->DeleteGlobalRef( class_ByteBuffer );

    env->DeleteGlobalRef( class_PointF );
}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_calculateWaveformData
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint samplesCount )
{
    Jrect * rect;

    jint width;
    jint height;

    /*jfloat x;
    jfloat y;
    jobject point;*/

    //jobjectArray    waveformPoints;
    jfloatArray     waveformPointsFloat;
    jfloat *        waveformPointsFloatPtr;

    jbyte * bufferPtr;

    jfloat x;
    jfloat y;
    jfloat oldx;
    jfloat oldy;


    width = env->CallIntMethod( thiz, methodID_VisualizerView_getWidth );
    height = env->CallIntMethod( thiz, methodID_VisualizerView_getHeight );

    //waveformPoints      = ( jobjectArray ) env->GetObjectField( thiz, fieldID_VisualizerView_waveformPoints );
    waveformPointsFloat = ( jfloatArray ) env->GetObjectField( thiz, fieldID_VisualizerView_waveformPointsFloat );

    waveformPointsFloatPtr = env->GetFloatArrayElements( waveformPointsFloat, 0 );

    bufferPtr = ( jbyte* )env->GetDirectBufferAddress( samplesBuffer );

    rect = new Jrect( 0, 0, width, height / 2 );

    /*for ( jint i = 0; i < samplesCount; i++ )
    {
        x = rect->getWitdth() * i / ( samplesCount - 1 );
        y = rect->getHeight() / 2 + ( ( jbyte ) ( bufferPtr[ i ] + 128 ) ) * ( rect->getHeight() / 2 ) / 128;

        point = env->GetObjectArrayElement( waveformPoints, i );

        env->SetFloatField( point, fieldID_PointF_x, x );
        env->SetFloatField( point, fieldID_PointF_y, y );

        env->DeleteLocalRef( point );
    }*/

    oldx = 0;
    oldy = rect->getHeight() / 2 + ( ( jbyte ) ( bufferPtr[ 0 ] + 128 ) ) * ( rect->getHeight() / 2 ) / 128;

    for ( jint i = 0; i < ( samplesCount - 1 ) ; i++ )
    {
        /*point = env->GetObjectArrayElement( waveformPoints, i );

        waveformPointsFloatPtr[ i * 4]      = env->GetFloatField( point, fieldID_PointF_x );
        waveformPointsFloatPtr[ i * 4 + 1]  = env->GetFloatField( point, fieldID_PointF_y );

        env->DeleteLocalRef( point );

        point = env->GetObjectArrayElement( waveformPoints, i + 1 );

        waveformPointsFloatPtr[ i * 4 + 2]  = env->GetFloatField( point, fieldID_PointF_x );
        waveformPointsFloatPtr[ i * 4 + 3]  = env->GetFloatField( point, fieldID_PointF_y );

        env->DeleteLocalRef( point );*/

        x = rect->getWitdth() * ( i + 1 ) / ( samplesCount - 1 );
        y = rect->getHeight() / 2 + ( ( jbyte ) ( bufferPtr[ i ] + 128 ) ) * ( rect->getHeight() / 2 ) / 128;

        waveformPointsFloatPtr[ i * 4]      = oldx;
        waveformPointsFloatPtr[ i * 4 + 1]  = oldy;
        waveformPointsFloatPtr[ i * 4 + 2]  = x;
        waveformPointsFloatPtr[ i * 4 + 3]  = y;

        oldx = x;
        oldy = y;
    }

    env->ReleaseFloatArrayElements( waveformPointsFloat, waveformPointsFloatPtr, 0 );


}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_calculateCurvePoints
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint curvePointsCount )
{
    Jrect *     curveRect;
    jint        pointDistance;
    jfloat      scaledHeight;
    jfloat      scaling;
    //jfloat *    curveHeights;

    jobject curvePointsRaw;
    jobjectArray * curvePoints;
    jobject point;

    jint width;
    jint height;

    jfloat x;
    jfloat y;

    jbyte * samplesBufferPtr;
    jint    samplesCount;

    jfloat maxSectorHeight;
    jint sectorSize;



    //__android_log_print( ANDROID_LOG_VERBOSE, "VisualizerView", "calculateCurvePoints()");


    samplesCount        = env->CallIntMethod( samplesBuffer, methodID_ByteBuffer_limit );
    samplesBufferPtr    = (jbyte*)env->GetDirectBufferAddress( samplesBuffer );


    width = env->CallIntMethod( thiz, methodID_VisualizerView_getWidth );
    height = env->CallIntMethod( thiz, methodID_VisualizerView_getHeight );

    pointDistance = width / ( curvePointsCount - 1 );

    curveRect = new Jrect( 0, height / 2, width, height );

    absoluteSamples( samplesBufferPtr, samplesCount );

    //curveHeights = calculateCurvePointHeights( samplesBufferPtr, samplesCount, curvePointsCount );

    scaling = ( jfloat ) curveRect->getHeight() / ( jfloat )128;


    curvePointsRaw  = env->GetObjectField( thiz, fieldID_VisualizerView_curvePoints );
    curvePoints     = (jobjectArray*)&curvePointsRaw;

    sectorSize = samplesCount / curvePointsCount;

    for ( int i = 0; i < curvePointsCount; i++ )
    {
        maxSectorHeight = findMaxByte( samplesBufferPtr, i * sectorSize, i * sectorSize + sectorSize );

        scaledHeight = scaling * maxSectorHeight;

        x = i * pointDistance;
        y = curveRect->getHeight() + scaledHeight;

        point = env->NewObject( class_PointF, methodID_PointF_constructor, x, y );

        env->SetObjectArrayElement( *curvePoints, i, point );
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


