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
#include <vector>
#include <mutex>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNative
        ( JNIEnv * env, jobject thiz, jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan, jint strokeWidth );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_deleteNativeInstance
        ( JNIEnv * env, jobject thiz, jlong objPtr  );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_initNVG
        ( JNIEnv * env, jobject thiz, jlong objPtr, int width, int height );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_releaseNVG ( JNIEnv * env, jobject thiz, jlong objPtr );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_processBuffer
        ( JNIEnv * env, jobject thiz, jlong objPtr, jobject samplesBuffer, jint samplesCount, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_render
        ( JNIEnv * env, jobject thiz, jlong objPtr );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_enable
        ( JNIEnv * env, jobject thiz, jlong objPtr );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_disable
        ( JNIEnv * env, jobject thiz, jlong objPtr );





/*
 * command to help find errors
 * adb logcat | ndk-stack -sym /home/miroslav/Documents/SlimPlayer/app/build/intermediates/cmake/debug/obj/armeabi/
 *
 */

//static std::mutex sNVGCreateLock;

static int sInstanceNumber = 0;

class GLES20Renderer
{
public:



    struct NVGcontext * mNVGCtx = nullptr;

     jint mWidth;
     jint mHeight;

     jint mSamplesCount;

     jint mCurvePointsCount;
     Point * mCurvePoints;
     Point * mWaveformPoints;

     CurveAnimator * mCurveAnimator;

     AudioBufferManager * mAudioBufferManager;

    jint mStrokeWidth;

    //std::mutex mNVGContextLock;

    int mInstance = -1;

    bool mDeleted = false;

    bool mGLESReleased = true;

    //bool mEnabled = false;

    bool mConstructed = false;

    std::mutex mConstructorLock;





    GLES20Renderer( jint curvePointsCount, jint transitionFrames, jint targetSamplesCount, jint targetTimeSpan, jint strokeWidth );

    ~GLES20Renderer();

    void initNVG( int width, int height );

    void releaseNVG();

    void render();

    void drawWaveform(NVGcontext * nvgContext );

    void calculateWaveformPoints( Buffer * buffer );

    void calculateCurvePoints( Buffer * buffer );

    jbyte findMaxByte( Buffer * buffer, int start, int end );

    void absoluteSamples( Buffer * buffer );

    //void enable();

    //void disable();
};



#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_GLES20RENDERER_H

