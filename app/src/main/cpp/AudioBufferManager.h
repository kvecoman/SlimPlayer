//
// Created by miroslav on 29.03.17..
//

#ifndef SLIMPLAYER_AUDIOBUFFERMANAGER_H
#define SLIMPLAYER_AUDIOBUFFERMANAGER_H

#include <jni.h>
#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_init
        ( JNIEnv * env, jobject thiz );

JNIEXPORT void JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_destroy
        ( JNIEnv * env, jobject thiz );

JNIEXPORT jobject JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_createMonoSamples( JNIEnv * env, jobject thiz, jobject byteBuffer, jint pcmFrameSize, jint sampleRate );

jobject getFreeByteBuffer( JNIEnv * env, jobject * thiz, jint targetCapacity );

void deleteStaleBufferWraps( JNIEnv * env, jobject * thiz );

JNIEXPORT jobject JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_getSamples( JNIEnv * env, jobject thiz );

#ifdef __cplusplus
}
#endif
#endif //SLIMPLAYER_AUDIOBUFFERMANAGER_H
