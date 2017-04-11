//
// Created by miroslav on 06.04.17..
//

#ifndef SLIMPLAYER_GLES20RENDERER_H
#define SLIMPLAYER_GLES20RENDERER_H

//#define NANOVG_GLES2
#define NANOVG_GLES2_IMPLEMENTATION
#define GLFW_INCLUDE_ES2

#include <jni.h>
#include <android/log.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "nanovg/nanovg.h"
#include "nanovg/nanovg_gl.h"
#include "nanovg/nanovg_gl_utils.h"
#include "Jrect.h"
#include "Point.h"
#include "CurveAnimator.h"
#include "AudioBufferManager.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseNative
        ( JNIEnv * env, jobject thiz );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initGLES
        ( JNIEnv * env, jobject thiz, int width, int height, jfloat clearRed, jfloat clearGreen, jfloat clearBlue );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseGLES
        ( JNIEnv * env, jobject thiz );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBuffer
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz );




void drawWaveform(NVGcontext * nvgContext );

void calculateWaveformPoints( Buffer * buffer );

void calculateCurvePoints( Buffer * buffer );

jbyte findMaxByte( Buffer * buffer, int start, int end );

void absoluteSamples( Buffer * buffer );



#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_GLES20RENDERER_H

