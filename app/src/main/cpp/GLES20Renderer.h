//
// Created by miroslav on 06.04.17..
//

#ifndef SLIMPLAYER_GLES20RENDERER_H
#define SLIMPLAYER_GLES20RENDERER_H

/*
//#define NANOVG_GLES2
#ifndef NANOVG_GLES2_IMPLEMENTATION
#define NANOVG_GLES2_IMPLEMENTATION
#endif //NANOVG_GLES2_IMPLEMENTATION

#ifndef GLFW_INCLUDE_ES2
#define GLFW_INCLUDE_ES2
#endif //GLFW_INCLUDE_ES2



#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "nanovg/nanovg.h"
#include "nanovg/nanovg_gl.h"
#include "nanovg/nanovg_gl_utils.h"*/
#include <android/log.h>
#include <mutex>
#include "Shared.h"
#include "Rect.h"
#include "Point.h"
#include "CurveAnimator.h"
#include "AudioBufferManager.h"
#include "AudioBufferManagerExo.h"
#include "AudioBufferManagerMedia.h"
#include "DrawParams.h"
#include "DrawWaveformExtension.h"



#ifdef __cplusplus
extern "C" {
#endif


/*JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_enable
        ( JNIEnv * env, jobject thiz, jlong objPtr );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerGLRenderer_disable
        ( JNIEnv * env, jobject thiz, jlong objPtr );*/





/*
 * command to help find errors
 * adb logcat | ndk-stack -sym /home/miroslav/Documents/SlimPlayer/app/build/intermediates/cmake/debug/obj/armeabi/
 *
 */

//static std::mutex sNVGCreateLock;



class GLES20Renderer /*: public DrawWaveformExtension*/
{
public:

    const NVGcolor  STROKE_COLOR = nvgRGBA( 54, 194, 249, 255 );
    const int       STROKE_WIDTH = 10;

    struct NVGcontext * mNVGCtx = nullptr;

     jint mCurvePointsCount;

     Point * mCurvePoints = nullptr;


     CurveAnimator * mCurveAnimator = nullptr;

     AudioBufferManager * mAudioBufferManager = nullptr;


    bool mDeleted = false;

    bool mGLESReleased = true;

    bool mConstructed = false;

    //std::mutex mConstructorLock;

    //Screen density ratio
    float mDensity = 1.0;

    //Holder for sceen size, draw color, stroke width and draw offset
    DrawParams * mDrawParams;





    GLES20Renderer( jint curvePointsCount, jint transitionFrames, AudioBufferManager * audioBufferManager );

    ~GLES20Renderer();

    void initNVG( int width, int height, float density );

    void releaseNVG();

    void render( int drawOffset );

    void drawCurve( Buffer * samplesBuffer );

    void calculateCurvePoints( Buffer * buffer );

    jbyte findMaxByte( Buffer * buffer, int start, int end );

    //void absoluteSamples( Buffer * buffer );


};



#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_GLES20RENDERER_H

