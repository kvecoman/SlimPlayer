//
// Created by miroslav on 05.05.17..
//

#ifndef SLIMPLAYER_BUFFERWRAP_H
#define SLIMPLAYER_BUFFERWRAP_H


#include <jni.h>
#include "Buffer.h"

/**
 * BufferWrap is used to store buffer and its presentationTime property, so we know when to display it
 */
class BufferWrap
{
public:
    /**
     * Time when this samples are supposed to be shown
     */
    jlong presentationTimeUs;

    Buffer * buffer;



    BufferWrap( Buffer * buffer, jlong presentationTimeUs );

    ~BufferWrap();
};


#endif //SLIMPLAYER_BUFFERWRAP_H
