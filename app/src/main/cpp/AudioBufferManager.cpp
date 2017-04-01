//
// Created by miroslav on 29.03.17..
//

#include "AudioBufferManager.h"

static jclass       class_ByteBuffer;
static jmethodID    methodID_ByteBuffer_limit;
static jmethodID    methodID_ByteBuffer_setLimit;
static jmethodID    methodID_ByteBuffer_position;
static jmethodID    methodID_ByteBuffer_capacity;

static jclass       class_AudioBufferManager;
static jfieldID     fieldID_AudioBufferManager_targetTimeSpan;
static jfieldID     fieldID_AudioBufferManager_targetSamples;
static jfieldID     fieldID_AudioBufferManager_freeBufferList;

static jclass       class_LinkedList;
static jmethodID    methodID_LinkedList_size;
static jmethodID    methodID_LinkedList_get;
static jmethodID    methodID_LinkedList_remove;

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_init
        ( JNIEnv * env, jobject thiz )
{
    class_ByteBuffer                = env->FindClass( "java/nio/ByteBuffer" );
    methodID_ByteBuffer_limit       = env->GetMethodID( class_ByteBuffer, "limit", "()I" );
    methodID_ByteBuffer_setLimit    = env->GetMethodID( class_ByteBuffer, "limit", "(I)Ljava/nio/Buffer;" );
    methodID_ByteBuffer_position    = env->GetMethodID( class_ByteBuffer, "position", "()I" );
    methodID_ByteBuffer_capacity    = env->GetMethodID( class_ByteBuffer, "capacity", "()I" );

    class_AudioBufferManager                    = env->FindClass( "mihaljevic/miroslav/foundry/slimplayer/AudioBufferManager" );
    fieldID_AudioBufferManager_targetTimeSpan   = env->GetFieldID( class_AudioBufferManager, "mTargetTimeSpan", "I" );
    fieldID_AudioBufferManager_targetSamples    = env->GetFieldID( class_AudioBufferManager, "mTargetSamples", "I" );
    fieldID_AudioBufferManager_freeBufferList   = env->GetFieldID(class_AudioBufferManager, "mFreeBufferList", "Ljava/util/LinkedList;");

    class_LinkedList            = env->FindClass( "java/util/LinkedList" );
    methodID_LinkedList_size    = env->GetMethodID( class_LinkedList, "size", "()I" );
    methodID_LinkedList_get     = env->GetMethodID( class_LinkedList, "get", "(I)Ljava/lang/Object;" );
    methodID_LinkedList_remove  = env->GetMethodID( class_LinkedList, "remove", "(I)Ljava/lang/Object;" );


    class_ByteBuffer            = ( jclass ) env->NewGlobalRef( class_ByteBuffer );
    class_AudioBufferManager    = ( jclass ) env->NewGlobalRef( class_AudioBufferManager );
    class_LinkedList            = ( jclass ) env->NewGlobalRef( class_LinkedList );

}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_destroy
        ( JNIEnv * env, jobject thiz )
{
    env->DeleteGlobalRef( class_ByteBuffer );

    env->DeleteGlobalRef( class_AudioBufferManager );

    env->DeleteGlobalRef( class_LinkedList );
}


JNIEXPORT jobject JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_createMonoSamples
        ( JNIEnv * env, jobject thiz, jobject byteBuffer, jint pcmFrameSize, jint sampleRate )
{
    jint    originalSamplesCount;
    jfloat  representedTime;
    jint    monoSamplesCount;
    jint    sampleJump;

    jbyte   monoSample;
    jobject newBuffer;

    jint byteBufferLimit;
    jint byteBufferPosition;
    jint targetTimeSpan;
    jint targetSamples;

    jint    significantByteIndex;

    jbyte * byteBufferDataPtr;
    void *  newBufferDataPtr;



    byteBufferLimit     = env->CallIntMethod( byteBuffer, methodID_ByteBuffer_limit );
    byteBufferPosition  = env->CallIntMethod( byteBuffer, methodID_ByteBuffer_position );


    //getFreeByteBufferMethodID   = env->GetMethodID( class_AudioBufferManager, "getFreeByteBuffer", "(I)Ljava/nio/ByteBuffer;" );


    targetTimeSpan  = env->GetIntField( thiz, fieldID_AudioBufferManager_targetTimeSpan );
    targetSamples   = env->GetIntField( thiz, fieldID_AudioBufferManager_targetSamples );

    originalSamplesCount = (byteBufferLimit - byteBufferPosition) / pcmFrameSize;

    if ( originalSamplesCount == 0 )
        return nullptr;

    representedTime = ( jfloat ) originalSamplesCount / ( jfloat ) sampleRate * ( jfloat ) 1000;

    monoSamplesCount = ( jint ) (representedTime / ( jfloat ) targetTimeSpan *
                                 ( jfloat ) targetSamples);

    //Bellow we test extreme max and min cases
    if ( monoSamplesCount < 2 )
        monoSamplesCount = 2;

    if ( targetSamples > originalSamplesCount || monoSamplesCount > originalSamplesCount )
        monoSamplesCount = originalSamplesCount;

    sampleJump = originalSamplesCount / monoSamplesCount;

    //newBuffer = env->CallObjectMethod( thiz, getFreeByteBufferMethodID, monoSamplesCount );
    newBuffer = getFreeByteBuffer( env, &thiz, monoSamplesCount );

    if ( newBuffer == nullptr )
    {
        newBufferDataPtr = new jbyte[monoSamplesCount];
        newBuffer = env->NewDirectByteBuffer( newBufferDataPtr, monoSamplesCount );
        __android_log_print( ANDROID_LOG_DEBUG, "AudioBufferManager", "Allocating new buffer");
    }
    else
    {
        newBufferDataPtr = (jbyte*)env->GetDirectBufferAddress( newBuffer );
    }


    byteBufferDataPtr = (jbyte*)env->GetDirectBufferAddress( byteBuffer );

    for ( int i = 0; i < monoSamplesCount; i++ )
    {
        significantByteIndex = ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 );

        monoSample = byteBufferDataPtr[ significantByteIndex ];

        ( ( jbyte* )newBufferDataPtr )[i] = monoSample;
    }

    return newBuffer;
}

jobject getFreeByteBuffer( JNIEnv * env, jobject * thiz, jint targetCapacity )
{
    jobject freeBuffer;

    jint size;
    jint capacity;

    jobject freeBufferList;


    freeBufferList = env->GetObjectField( *thiz, fieldID_AudioBufferManager_freeBufferList );

    size = env->CallIntMethod( freeBufferList, methodID_LinkedList_size );

    if ( size <= 0 )
        return nullptr;


    for ( jint i = 0; i < size; i++ )
    {
        freeBuffer = env->CallObjectMethod( freeBufferList, methodID_LinkedList_get, i );

        capacity = env->CallIntMethod( freeBuffer, methodID_ByteBuffer_capacity  );

        if ( capacity <= targetCapacity )
        {
            env->CallObjectMethod( freeBufferList, methodID_LinkedList_remove, i );

            env->CallObjectMethod( freeBuffer, methodID_ByteBuffer_setLimit, targetCapacity );

            return freeBuffer;

        }
    }

    return nullptr;
}