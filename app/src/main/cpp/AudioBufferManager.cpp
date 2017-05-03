//
// Created by miroslav on 30.04.17..
//

#include "AudioBufferManager.h"

/**
 * Replica of java's ByteBuffer class, has similar fields, used to transfer samples around
 */
Buffer::Buffer( int cap )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "Buffer", "Buffer(int) - constructor" );

    this->buffer = new jbyte[ cap ];
    this->cap = cap;
    this->len = cap;

    needsDelete = true;
}

Buffer::Buffer( jbyte * buffer, int len, int cap )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "Buffer", "Buffer(byte, int, int) - constructor" );

    this->buffer    = buffer;
    this->cap       = cap;
    this->len       = len;

    needsDelete = false;
}

Buffer::~Buffer()
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "Buffer", "~Buffer() - destructor, needsDelete: %i, length: %i, capacity %i", needsDelete, len, cap );

    if ( needsDelete )
        delete [] buffer;
}