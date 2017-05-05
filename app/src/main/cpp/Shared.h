//
// Created by miroslav on 12.04.17..
//

#ifndef SLIMPLAYER_SHARED_H
#define SLIMPLAYER_SHARED_H

/**
 * Outside place to define all NanoVG related stuff except one declared in GLES20Renderer.cpp
 * ( it must be in cpp, not here, linker errors otherwise )
 */

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
#include "nanovg/nanovg_gl_utils.h"

#endif //SLIMPLAYER_SHARED_H
