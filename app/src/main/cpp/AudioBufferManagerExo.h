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
#include "AudioBufferManager.h"
#include <stdlib.h>
#include "BufferWrap.h"






class AudioBufferManagerExo : public AudioBufferManager
{


private:
    int mTargetSamples;
    int mTargetTimeSpan;

    std::list<BufferWrap*> mBufferWrapList;
    std::list<Buffer*>     mFreeBufferList;

    Buffer * mResultBuffer;

    jlong mLastCurrentTimeUs = 0;

    /**
     * Used to make sure reset() and some other delete operations don't go at same time
     */
    std::mutex mResetLock;
    std::mutex mDestructorLock;

    /**
     * Indicates whether destructor has been called
     */
    bool mReleased = false;

    bool mConstructed = false;



public:

    /**
     * @param targetSamples - number of samples we try to have at screen at single moment (draw)
     * @param targetTimeSpan - time in miliseconds we try to represent in single moment
     */
    AudioBufferManagerExo( int targetSamples, int targetTimeSpan );

    virtual ~AudioBufferManagerExo();

    virtual void processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );

    /**
     * Takes the most significant samples in aomount required to satisfy targetSamples and targetTimeSpan
     *
     * @param buffer - buffer with samples
     * @param pcmFrameSize - how many bytes is signle sample
     * @param sampleRate - how many samples represent 1 second
     */
    Buffer * createMonoSamples( Buffer * buffer, jint pcmFrameSize, jint sampleRate );

    /**
     * Retrieves used buffer so we don't allocate new one
     */
    Buffer * getFreeByteBuffer( int minimalCapacity );

    /**
     * Deletes all buffer wraps whose presentation time is stale in regard to current play position
     * Also feeds used buffer for later retrieval using getFreebyteBuffer()
     */
    void deleteStaleBufferWraps();

    virtual Buffer * getSamples();

    /**
     * Deletes all buffer wraps, to allow new ones to be shown ( used after seek is detected )
     */
    void reset();



};


#endif //SLIMPLAYER_AUDIOBUFFERMANAGER2_H
