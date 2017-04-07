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

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_releaseNative
        ( JNIEnv * env, jobject thiz );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_initGLES
        ( JNIEnv * env, jobject thiz, int width, int height );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_releaseGLES
        ( JNIEnv * env, jobject thiz );



JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_GLES20Renderer_drawCurve
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer );

void calculateCurvePoints( JNIEnv * env, jobject samplesBuffer );

jbyte findMaxByte( jbyte * buffer, int start, int end );

void absoluteSamples( jbyte * bufferPtr, jint count );



#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_GLES20RENDERER_H

