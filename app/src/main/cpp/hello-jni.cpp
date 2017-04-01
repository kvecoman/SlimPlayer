#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_TestPlayerActivity_helloFromTheOtherSide( JNIEnv * env, jobject thiz )
{
    return env->NewStringUTF( "Hello from the other siiiiiideeeeee!!!!!!" );
}