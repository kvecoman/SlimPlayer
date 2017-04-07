//
// Created by miroslav on 01.04.17..
//

#ifndef SLIMPLAYER_VISUALIZERVIEW_H
#define SLIMPLAYER_VISUALIZERVIEW_H

#include <jni.h>
#include <android/log.h>
#include "SlimShared.h"
#include "Jrect.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_initNative
        ( JNIEnv * env, jobject thiz );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_releaseNative
        ( JNIEnv * env, jobject thiz );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_calculateWaveformData
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint samplesCount );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_VisualizerView_calculateCurvePoints
        ( JNIEnv * env, jobject thiz, jobject samplesBuffer, jint curvePointsCount );


#ifdef __cplusplus
}
#endif

#endif //SLIMPLAYER_VISUALIZERVIEW_H
