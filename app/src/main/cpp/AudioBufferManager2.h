//
// Created by miroslav on 08.04.17..
//

#ifndef SLIMPLAYER_AUDIOBUFFERMANAGER2_H
#define SLIMPLAYER_AUDIOBUFFERMANAGER2_H

#include <jni.h>
#include <list>

class Buffer
{
public:
    jbyte * buffer;
    int len;
    int cap;

    Buffer( int cap );

    Buffer( jbyte * buffer, int len, int cap );



};

class BufferWrap
{
public:
    jlong presentationTimeUs;
    Buffer * buffer;

    BufferWrap( Buffer * buffer, jlong presentationTimeUs );
};

class AudioBufferManager2
{


private:
    int mTargetSamples;
    int mTargetTimeSpan;

    std::list<BufferWrap*> mBufferWrapList;
    std::list<Buffer*>     mFreeBufferList;

    Buffer * mResultBuffer;

    jlong mLastCurrentTimeUs;


public:

    AudioBufferManager2( int targetSamples, int targetTimeSpan );

    void processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );

    Buffer * createMonoSamples( Buffer * buffer, jint pcmFrameSize, jint sampleRate );

    void deleteStaleBufferWraps();

    Buffer * getFreeByteBuffer( int targetCapacity );

    Buffer * getSamples();

};


#endif //SLIMPLAYER_AUDIOBUFFERMANAGER2_H
