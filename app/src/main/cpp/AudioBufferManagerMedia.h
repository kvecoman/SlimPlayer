//
// Created by miroslav on 02.05.17..
//

#ifndef SLIMPLAYER_AUDIOBUFFERMANAGERMEDIA_H
#define SLIMPLAYER_AUDIOBUFFERMANAGERMEDIA_H


#include "AudioBufferManager.h"
#include <mutex>
#include <stdlib.h>

class AudioBufferManagerMedia : public AudioBufferManager
{
private:
    int mTargetSamples;

    Buffer * mResultBuffer;

    bool mConstructed = false;

    bool mReleased = false;

    std::mutex mLock;


public:
    AudioBufferManagerMedia( int targetSamples );

    virtual ~AudioBufferManagerMedia();

    virtual void processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs );

    virtual Buffer * getSamples();
};


#endif //SLIMPLAYER_AUDIOBUFFERMANAGERMEDIA_H