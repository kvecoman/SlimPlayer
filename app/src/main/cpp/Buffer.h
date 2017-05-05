//
// Created by miroslav on 05.05.17..
//

#ifndef SLIMPLAYER_BUFFER_H
#define SLIMPLAYER_BUFFER_H


#include <jni.h>

/**
 * Replica of java's ByteBuffer class, has similar fields, used to transfer samples around
 */
class Buffer
{
private:

    /**
     * Keeps track whether buffer is allocated on native side and
     * if it needs to be deleted here
     */
    bool needsDelete;

public:
    jbyte * buffer = nullptr;
    int len;
    int cap;

    Buffer( int cap );

    Buffer( jbyte * buffer, int len, int cap );

    ~Buffer();

};


#endif //SLIMPLAYER_BUFFER_H
