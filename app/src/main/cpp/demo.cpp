//
// Created by miroslav on 19.04.17..
//

#ifndef SLIMPLAYER_DEMO_H
#define SLIMPLAYER_DEMO_H

#define NANOVG_GLES2_IMPLEMENTATION

#include <jni.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "nanovg/nanovg.h"
#include "nanovg/nanovg_gl.h"
#include "nanovg/nanovg_gl_utils.h"
#include <stdlib.h>
#include <time.h>

#ifdef __cplusplus
extern "C" {
#endif

static NVGcontext * sNVGCtx;

static int sWidth;
static int sHeight;

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_DemoActivity_00024Renderer_createNVGContext
        ( JNIEnv * env, jobject thiz, jint width, jint height  )
{
    if ( sNVGCtx == nullptr )
        sNVGCtx = nvgCreateGLES2( 0 );

    sWidth  = width;
    sHeight = height;

    glClearColor( 0.5, 0.5, 0.5, 0.5 );
    glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );
}


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_DemoActivity_00024Renderer_draw
        ( JNIEnv * env, jobject thiz )
{
    int x;
    int y;

    if ( sNVGCtx == nullptr )
        return;

    srand (time(NULL));

    x = rand() % sWidth;
    y = rand() % sHeight;

    glClear( GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT );

    nvgBeginFrame( sNVGCtx, sWidth, sHeight, 1 );

    nvgCircle( sNVGCtx, x, y, 20 );
    nvgFillColor( sNVGCtx, nvgRGBA( 230, 207, 0, 255 ) );
    nvgFill( sNVGCtx );


    nvgEndFrame( sNVGCtx );
}



JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_DemoActivity_00024Renderer_deleteNVGContext
        ( JNIEnv * env, jobject thiz  )
{
    if ( sNVGCtx != nullptr )
        nvgDeleteGLES2( sNVGCtx );
}




#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_DEMO_H
