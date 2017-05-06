# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-rtti -fno-exceptions -Wall -Werror")
set(CMAKE_VERBOSE_MAKEFILE ON)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.



    add_library( visualizer

               SHARED

               AudioBufferManager.h
               AudioBufferManagerExo.h
               AudioBufferManagerExo.cpp
               AudioBufferManagerMedia.h
               AudioBufferManagerMedia.cpp
               Buffer.h
               Buffer.cpp
               BufferWrap.h
               BufferWrap.cpp
               CurveAnimator.h
               CurveAnimator.cpp
               DrawParams.h
               DrawParams.cpp
               DrawWaveformExtension.h
               DrawWaveformExtension.cpp
               GLES20Renderer.h
               GLES20Renderer.cpp
               GLES20RendererJNILayer.h
               GLES20RendererJNILayer.cpp
               Rect.h
               Rect.cpp
               Point.h
               Point.cpp
               Shared.h
               nanovg/fontstash.h
               nanovg/nanovg.c
               nanovg/nanovg.h
               nanovg/nanovg_gl.h
               nanovg/nanovg_gl_utils.h
               nanovg/stb_image.h
               nanovg/stb_truetype.h )





  target_link_libraries( visualizer android log GLESv2 EGL )

# Specifies a path to native header files.
#include_directories( **relative_path_without_quotes** )
