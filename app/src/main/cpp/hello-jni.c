//
// Created by miroslav on 28.03.17..
//

#include <string.h>
#include <jni.h>


JNIEXPORT jstring JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_TestPlayerActivity_helloFromTheOtherSide( JNIEnv* env,
                                                  jobject thiz )
{
    return (*env)->NewStringUTF(env, "hello from C side");

}

