//
// Created by miroslav on 05.05.17..
//

#include "GLES20RendererJNILayer.h"



JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan/*, jint strokeWidth*/, jboolean exoAudioBufferManager )
{

    //jlong instancePtr;

    if ( exoAudioBufferManager )
        sAudioBufferManager = new AudioBufferManagerExo( targetSamplesCount, targetTimeSpan );
    else
        sAudioBufferManager = new AudioBufferManagerMedia( targetSamplesCount );

    sRenderer = new GLES20Renderer( curvePointsCount,  transitionFrames, sAudioBufferManager );

    /*instancePtr = ( ( jlong )( new GLES20Renderer( curvePointsCount,  transitionFrames, sAudioBufferManager ) ) );

    return instancePtr;*/

}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_deleteNativeInstance
        ( JNIEnv * env, jobject thiz )
{
    /*GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;*/

    delete sRenderer;
    delete sAudioBufferManager;
}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNVG
        ( JNIEnv * env, jobject thiz, jint width, jint height, jfloat density )
{
    /*GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;

    instance->initNVG(  );*/

    sRenderer->initNVG( width, height, density );

}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseNVG ( JNIEnv * env, jobject thiz )
{
    /*GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;

    instance->releaseNVG();*/

    sRenderer->releaseNVG();
}



JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBufferNative
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
    Buffer * buffer;

    //GLES20Renderer * instance;

    jbyte * bufferPtr;
    jint capacity;

    if ( sAudioBufferManager == nullptr )
        return;

    //instance = (GLES20Renderer*)objPtr;

    bufferPtr       = ( jbyte* )env->GetDirectBufferAddress( samplesBuffer );
    capacity        = env->GetDirectBufferCapacity( samplesBuffer );

    //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "processBuffer() - capacity is %i for instance %i", capacity,instance->mInstance );

    buffer = new Buffer( bufferPtr, samplesCount, capacity );

    /*if ( instance->mConstructed && !instance->mDeleted && instance->mAudioBufferManager != nullptr )
        instance->mAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );*/


    sAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );


}


//RAW ARRAY VERSION
JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBufferArrayNative
        ( JNIEnv * env, jobject thiz, jarray samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
    Buffer * buffer;

    //GLES20Renderer * instance;

    jbyte * bufferPtr;
    jint capacity;
    jboolean isCopy;

    if ( sAudioBufferManager == nullptr )
        return;

    //instance = (GLES20Renderer*)objPtr;

    capacity        = env->GetArrayLength( samplesBuffer );
    bufferPtr       = (jbyte*)env->GetPrimitiveArrayCritical( samplesBuffer, &isCopy );


    //__android_log_print( ANDROID_LOG_VERBOSE, "GLES20Renderer", "processBuffer() - capacity is %i for instance %i", capacity,instance->mInstance );

    buffer = new Buffer( bufferPtr, samplesCount, capacity );

    /*if ( instance->mConstructed && !instance->mDeleted && instance->mAudioBufferManager != nullptr )
        instance->mAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );*/

    sAudioBufferManager->processBuffer( buffer, presentationTimeUs, pcmFrameSize, sampleRate, currentTimeUs );

    env->ReleasePrimitiveArrayCritical( samplesBuffer, bufferPtr, 0 );
    delete buffer;

}



JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz, jint drawOffset )
{

    /*GLES20Renderer * instance;

    instance = (GLES20Renderer*)objPtr;

    instance->render( drawOffset );*/

    sRenderer->render( drawOffset );

}





/*JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initExoAudioBufferManager
        ( JNIEnv * env, jobject thiz, jint targetSamplesCount, jint targetTimeSpan )
{
    sAudioBufferManager = new AudioBufferManagerExo( targetSamplesCount, targetTimeSpan );
}

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initMediaAudioBufferManager
        ( JNIEnv * env, jobject thiz,   jint targetSamplesCount )
{
    sAudioBufferManager = new AudioBufferManagerMedia( targetSamplesCount );
}*/