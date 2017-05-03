//
// Created by miroslav on 30.04.17..
//

#ifndef SLIMPLAYER_AUDIOBUFFERMANAGER_H
#define SLIMPLAYER_AUDIOBUFFERMANAGER_H

#include <jni.h>

class Buffer
{
private:

    //Some buffers are allocated on java side, and they will be cleaned there, so here we keep track only of those that are created here
    bool needsDelete;

public:
    jbyte * buffer = nullptr;
    int len;
    int cap;

    Buffer( int cap );

    Buffer( jbyte * buffer, int len, int cap );

    ~Buffer();

};


class AudioBufferManager
{
public:



    virtual ~AudioBufferManager() {}

    virtual void processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs ) = 0;

    virtual Buffer * getSamples() = 0;
};


#endif //SLIMPLAYER_AUDIOBUFFERMANAGER_H
