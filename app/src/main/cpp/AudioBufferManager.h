//
// Created by miroslav on 08.04.17..
//

#ifndef SLIMPLAYER_AUDIOBUFFERMANAGER2_H
#define SLIMPLAYER_AUDIOBUFFERMANAGER2_H

#include <jni.h>
#include <list>
#include <string>
#include <android/log.h>
#include "Shared.h"
#include <mutex>
#include "SynchronizedLinkedList.h"

class Buffer
{
private:

    //Some buffers are allocated on java side, and they will be cleaned there, so here we keep track only of those that are created here
    bool needsDelete;

public:
    jbyte * buffer;
    int len;
    int cap;

    Buffer( int cap );

    Buffer( jbyte * buffer, int len, int cap );

    ~Buffer();

};

class BufferWrap
{
public:
    jlong presentationTimeUs;
    Buffer * buffer;

    BufferWrap( Buffer * buffer, jlong presentationTimeUs );

    ~BufferWrap();
};

class AudioBufferManager
{


private:
    int mTargetSamples;
    int mTargetTimeSpan;

    std::list<BufferWrap*> mBufferWrapList;
    //SynchronizedLinkedList<BufferWrap> mBufferWrapList;
    std::list<Buffer*>     mFreeBufferList;
    //SynchronizedLinkedList<Buffer> mFreeBufferList;


    Buffer * mResultBuffer;

    jlong mLastCurrentTimeUs;

    std::mutex mDeleteLock; //Used to make sure reset() and some other delete operations don't go at same time
    std::mutex mDestructorLock;

    //BufferWrap * mCurrentActiveBuffer;

    //std::mutex mProviderLock;
    //std::mutex mConsumerLock;
    //std::mutex mLock;


public:

    AudioBufferManager( int targetSamples, int targetTimeSpan );

    ~AudioBufferManager();

    void processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );

    Buffer * createMonoSamples( Buffer * buffer, jint pcmFrameSize, jint sampleRate );

    Buffer * getFreeByteBuffer( int minimalCapacity );

    void deleteStaleBufferWraps();

    Buffer * getSamples();

    void reset();

};


#endif //SLIMPLAYER_AUDIOBUFFERMANAGER2_H
