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


static AudioBufferManager * sAudioBufferManager = nullptr;
static GLES20Renderer *     sRenderer = nullptr;


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan, jboolean exoAudioBufferManager );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_deleteNativeInstance
        ( JNIEnv * env, jobject thiz );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNVG
        ( JNIEnv * env, jobject thiz, jint width, jint height, jfloat density );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseNVG ( JNIEnv * env, jobject thiz );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBufferNative
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBufferArrayNative
        ( JNIEnv * env, jobject thiz, jarray samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz, jint drawOffset );





/*JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initExoAudioBufferManager
        ( JNIEnv * env, jobject thiz, jint targetSamplesCount, jint targetTimeSpan );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initMediaAudioBufferManager
        ( JNIEnv * env, jobject thiz,jint targetSamplesCount );*/


#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_GLES20RENDERERJNILAYER_H
