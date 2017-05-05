//
// Created by miroslav on 05.05.17..
//

#include "BufferWrap.h"


BufferWrap::BufferWrap( Buffer * buffer, jlong presentationTimeUs )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "BufferWrap", "BufferWrap() - constructor" );

    this->buffer                = buffer;
    this->presentationTimeUs    = presentationTimeUs;
}

BufferWrap::~BufferWrap()
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "BufferWrap", "~BufferWrap() - destructor" );

    if ( buffer != nullptr )
        delete buffer;
}