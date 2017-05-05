//
// Created by miroslav on 05.05.17..
//

#ifndef SLIMPLAYER_GLES20RENDERERJNILAYER_H
#define SLIMPLAYER_GLES20RENDERERJNILAYER_H

#include <jni.h>
#include "GLES20Renderer.h"


#ifdef __cplusplus
extern "C" {
#endif

/**
 * This is communication layer between java GLES20Renderer instance and native c++ GLES20Renderer instance
 */

JNIEXPORT jlong JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan/*, jint strokeWidth*/, jboolean exoAudioBufferManager );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_deleteNativeInstance
        ( JNIEnv * env, jobject thiz, jlong objPtr  );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNVG
        ( JNIEnv * env, jobject thiz, jlong objPtr, jint width, jint height, jfloat density );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseNVG ( JNIEnv * env, jobject thiz, jlong objPtr );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBuffer
        ( JNIEnv * env, jobject thiz, jlong objPtr, jobject samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBufferArray
        ( JNIEnv * env, jobject thiz, jlong objPtr, jarray samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz, jlong objPtr, jint drawOffset );


#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_GLES20RENDERERJNILAYER_H
