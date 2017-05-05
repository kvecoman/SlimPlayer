//
// Created by miroslav on 05.05.17..
//

#include "GLES20RendererJNILayer.h"



JNIEXPORT jlong JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan/*, jint strokeWidth*/, jboolean exoAudioBufferManager )
{

    jlong instancePtr;

    instancePtr = ( ( jlong )( new GLES20Renderer( curvePointsCount,  transitionFrames,  targetSamplesCount,  targetTimeSpan/*, strokeWidth */, exoAudioBufferManager) ) );

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


//RAW ARRAY VERSION
JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBufferArray
        ( JNIEnv * env, jobject thiz, jlong objPtr, jarray samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
    Buffer * buffer;

    GLES20Renderer * instance;

    jbyte * bufferPtr;
    jint capacity;
    jboolean isCopy;

    instance = (GLES20Renderer*)objPtr;

    capacity        = env->GetArrayLength( samplesBuffer );
    bufferPtr       = (jbyte*)env->GetPrimitiveArrayCritical( samplesBuffer, &isCopy );


    //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "processBuffer() - capacity is %i for instance %i", capacity,instance->mInstance );

    buffer = new Buffer( bufferPtr, samplesCount, capacity );

    if ( /*instance->mEnabled &&*/ instance->mConstructed && !instance->mDeleted && instance->mAudioBufferManager != nullptr )
        instance->mAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );

    env->ReleasePrimitiveArrayCritical( samplesBuffer, bufferPtr, 0 );
    delete buffer;

}



JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz, jlong objPtr, jint drawOffset )
{

    GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;

    instance->render( drawOffset );

}