//
// Created by miroslav on 30.04.17..
//

#ifndef SLIMPLAYER_AUDIOBUFFERMANAGER_H
#define SLIMPLAYER_AUDIOBUFFERMANAGER_H


#include "Buffer.h"


/**
 * Abstract base class used to provide interface for
 * taking in samples, processing them and preparing them for output
 */
class AudioBufferManager
{
public:



    virtual ~AudioBufferManager() {}

    /**
     * Used to take in samples and store them for latter or immediately prepare them for use
     */
    virtual void processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs ) = 0;

    /**
     * Get currently prepared samples for display use
     */
    virtual Buffer * getSamples() = 0;
};


#endif //SLIMPLAYER_AUDIOBUFFERMANAGER_H
