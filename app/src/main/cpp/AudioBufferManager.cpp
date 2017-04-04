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
static jfieldID     fieldID_AudioBufferManager_audioRenderer;
static jfieldID     fieldID_AudioBufferManager_bufferWrapList;
static jfieldID     fieldID_AudioBufferManager_resultByteBuffer;

static jclass       class_LinkedList;
static jmethodID    methodID_LinkedList_size;
static jmethodID    methodID_LinkedList_get;
static jmethodID    methodID_LinkedList_remove;

static jclass       class_List;
static jmethodID    methodID_List_size;
static jmethodID    methodID_List_get;
static jmethodID    methodID_List_remove;
static jmethodID    methodID_List_isEmpty;
static jmethodID    methodID_List_add;

static jclass       class_MediaCodecAudioRenderer;
static jmethodID    methodID_MediaCodecAudioRenderer_getPositionUs;

static jclass       class_BufferWrap;
static jfieldID     fieldID_BufferWrap_presentationTimeUs;
static jfieldID     fieldID_BufferWrap_buffer;

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
    fieldID_AudioBufferManager_freeBufferList   = env->GetFieldID( class_AudioBufferManager, "mFreeBufferList", "Ljava/util/List;");
    fieldID_AudioBufferManager_audioRenderer    = env->GetFieldID( class_AudioBufferManager, "mAudioRenderer", "Lcom/google/android/exoplayer2/audio/MediaCodecAudioRenderer;" );
    fieldID_AudioBufferManager_bufferWrapList   = env->GetFieldID( class_AudioBufferManager, "mBufferWrapList", "Ljava/util/List;" );
    fieldID_AudioBufferManager_resultByteBuffer = env->GetFieldID( class_AudioBufferManager, "mResultByteBuffer", "Ljava/nio/ByteBuffer;" );

    class_LinkedList            = env->FindClass( "java/util/LinkedList" );
    methodID_LinkedList_size    = env->GetMethodID( class_LinkedList, "size", "()I" );
    methodID_LinkedList_get     = env->GetMethodID( class_LinkedList, "get", "(I)Ljava/lang/Object;" );
    methodID_LinkedList_remove  = env->GetMethodID( class_LinkedList, "remove", "(I)Ljava/lang/Object;" );

    class_List                  = env->FindClass( "java/util/List" );
    methodID_List_size          = env->GetMethodID( class_List, "size", "()I" );
    methodID_List_get           = env->GetMethodID( class_List, "get", "(I)Ljava/lang/Object;" );
    methodID_List_remove        = env->GetMethodID( class_List, "remove", "(I)Ljava/lang/Object;" );
    methodID_List_isEmpty       = env->GetMethodID( class_List, "isEmpty", "()Z" );
    methodID_List_add           = env->GetMethodID( class_List, "add", "(Ljava/lang/Object;)Z" );

    class_MediaCodecAudioRenderer                   = env->FindClass( "com/google/android/exoplayer2/audio/MediaCodecAudioRenderer" );
    methodID_MediaCodecAudioRenderer_getPositionUs  = env->GetMethodID( class_MediaCodecAudioRenderer, "getPositionUs", "()J" );

    class_BufferWrap                        = env->FindClass( "mihaljevic/miroslav/foundry/slimplayer/AudioBufferManager$BufferWrap" );
    fieldID_BufferWrap_presentationTimeUs   = env->GetFieldID( class_BufferWrap, "presentationTimeUs", "J" );
    fieldID_BufferWrap_buffer               = env->GetFieldID( class_BufferWrap, "buffer", "Ljava/nio/ByteBuffer;" );



    class_ByteBuffer                = ( jclass ) env->NewGlobalRef( class_ByteBuffer );
    class_AudioBufferManager        = ( jclass ) env->NewGlobalRef( class_AudioBufferManager );
    class_LinkedList                = ( jclass ) env->NewGlobalRef( class_LinkedList );
    class_List                      = ( jclass ) env->NewGlobalRef( class_List );
    class_MediaCodecAudioRenderer   = ( jclass ) env->NewGlobalRef( class_MediaCodecAudioRenderer );
    class_BufferWrap                = ( jclass ) env->NewGlobalRef( class_BufferWrap );

}

JNIEXPORT void JNICALL
Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_destroy
        ( JNIEnv * env, jobject thiz )
{
    env->DeleteGlobalRef( class_ByteBuffer );
    env->DeleteGlobalRef( class_AudioBufferManager );
    env->DeleteGlobalRef( class_LinkedList );
    env->DeleteGlobalRef( class_List );
    env->DeleteGlobalRef( class_MediaCodecAudioRenderer );
    env->DeleteGlobalRef( class_BufferWrap );
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

    size = env->CallIntMethod( freeBufferList, methodID_List_size );

    if ( size <= 0 )
        return nullptr;


    for ( jint i = 0; i < size; i++ )
    {
        freeBuffer = env->CallObjectMethod( freeBufferList, methodID_List_get, i );

        capacity = env->CallIntMethod( freeBuffer, methodID_ByteBuffer_capacity  );

        if ( capacity <= targetCapacity )
        {
            env->CallObjectMethod( freeBufferList, methodID_List_remove, i );

            env->CallObjectMethod( freeBuffer, methodID_ByteBuffer_setLimit, targetCapacity );

            return freeBuffer;

        }
    }

    return nullptr;
}


void deleteStaleBufferWraps( JNIEnv * env, jobject * thiz )
{
    jlong   currentTimeUs;
    jobject bufferWrap;

    jobject audioRenderer;
    jobject bufferWrapList;
    jobject freeBufferList;
    jobject buffer;

    audioRenderer   = env->GetObjectField( *thiz, fieldID_AudioBufferManager_audioRenderer );
    bufferWrapList  = env->GetObjectField( *thiz, fieldID_AudioBufferManager_bufferWrapList );
    freeBufferList  = env->GetObjectField( *thiz, fieldID_AudioBufferManager_freeBufferList );

    currentTimeUs = env->CallLongMethod( audioRenderer, methodID_MediaCodecAudioRenderer_getPositionUs );

    while ( !env->CallBooleanMethod( bufferWrapList, methodID_List_isEmpty ) && env->GetLongField( env->CallObjectMethod( bufferWrapList, methodID_List_get, 0 ), fieldID_BufferWrap_presentationTimeUs ) < currentTimeUs )
    {
        bufferWrap = env->CallObjectMethod( bufferWrapList, methodID_List_remove, 0 );

        buffer = env->GetObjectField( bufferWrap, fieldID_BufferWrap_buffer );

        env->CallBooleanMethod( freeBufferList, methodID_List_add, buffer );
    }


}

JNIEXPORT jobject JNICALL
        Java_mihaljevic_miroslav_foundry_slimplayer_AudioBufferManager_getSamples( JNIEnv * env, jobject thiz )
{
    jobject bufferWrap;
    jint samplesCount;
    jint buffersCount;

    jobject bufferWrapList;
    jobject resultByteBuffer;
    jint    targetSamples;

    jint limit;

    jbyte * resultBufferPtr;
    jbyte * bufferPtr;

    samplesCount = 0;
    buffersCount = 0;

    bufferWrapList      = env->GetObjectField( thiz, fieldID_AudioBufferManager_bufferWrapList );
    resultByteBuffer    = env->GetObjectField( thiz, fieldID_AudioBufferManager_resultByteBuffer );
    targetSamples       = env->GetIntField( thiz, fieldID_AudioBufferManager_targetSamples);

    deleteStaleBufferWraps( env, &thiz );

    if ( env->CallBooleanMethod( bufferWrapList, methodID_List_isEmpty ) )
        return nullptr;

    env->CallObjectMethod( resultByteBuffer, methodID_ByteBuffer_setLimit, targetSamples );

    resultBufferPtr = ( jbyte* )env->GetDirectBufferAddress( resultByteBuffer );



    while ( buffersCount < env->CallIntMethod( bufferWrapList, methodID_List_size ) && samplesCount + env->CallIntMethod( env->GetObjectField( env->CallObjectMethod( bufferWrapList, methodID_List_get, buffersCount ), fieldID_BufferWrap_buffer ), methodID_ByteBuffer_limit ) <= targetSamples )
    {
        bufferWrap = env->CallObjectMethod( bufferWrapList, methodID_List_get, buffersCount );

        limit = env->CallIntMethod( env->GetObjectField( bufferWrap, fieldID_BufferWrap_buffer ), methodID_ByteBuffer_limit );

        bufferPtr = ( jbyte* )env->GetDirectBufferAddress( env->GetObjectField( bufferWrap, fieldID_BufferWrap_buffer ) );

        for ( int i = 0; i < limit; i++ )
        {
            resultBufferPtr[ samplesCount + i ] = bufferPtr[ i ];
        }

        samplesCount += limit;
        buffersCount++;

    }

    return resultByteBuffer;
}