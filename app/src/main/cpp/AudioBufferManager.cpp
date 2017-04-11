//
// Created by miroslav on 08.04.17..
//

#include "AudioBufferManager.h"


/**
 * Replica of java's ByteBuffer class, has similiar fields, used to trnasfer samples around
 */
Buffer::Buffer( int cap )
{
        this->buffer = new jbyte[ cap ];
        this->cap = cap;
        this->len = cap;
}

Buffer::Buffer( jbyte * buffer, int len, int cap )
{
    this->buffer    = buffer;
    this->cap       = cap;
    this->len       = len;
}




/**
 * BufferWrap is used to store buffer and its presentationTime property, so we know when to display it
 */
BufferWrap::BufferWrap( Buffer * buffer, jlong presentationTimeUs )
{
    this->buffer                = buffer;
    this->presentationTimeUs    = presentationTimeUs;
}





AudioBufferManager2::AudioBufferManager2( int targetSamples, int targetTimeSpan )
{
    mTargetSamples = targetSamples;
    mTargetTimeSpan = targetTimeSpan;

    mResultBuffer = new Buffer( targetSamples );
}

/**
 * Here we take buffer, take significant mono samples from it and store them for later use (display)
 */
void AudioBufferManager2::processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
    BufferWrap *    bufferWrap;
    Buffer *        newBuffer;

    newBuffer = createMonoSamples( buffer, pcmFrameSize, sampleRate );

    if ( newBuffer == nullptr )
        return;

    bufferWrap = new BufferWrap( newBuffer, presentationTimeUs );

    mBufferWrapList.push_back( bufferWrap );

    mLastCurrentTimeUs = currentTimeUs;
}

/**
 * Function that takes in original samples buffer (containing hundreds or thousands of smaples) and
 * only take the most significant mono samples
 */
Buffer * AudioBufferManager2::createMonoSamples( Buffer * buffer, jint pcmFrameSize, jint sampleRate )
{
    int     originalSamplesCount;
    float   representedTime;
    int     monoSamplesCount;
    int     sampleJump;

    jbyte        monoSample;
    Buffer *  newBuffer;


    originalSamplesCount = buffer->len  / pcmFrameSize;

    if ( originalSamplesCount == 0 )
        return nullptr;

    representedTime = ( float ) originalSamplesCount / ( float ) sampleRate * 1000;

    monoSamplesCount = ( int )( representedTime / ( float ) mTargetTimeSpan * ( float ) mTargetSamples );


    //Bellow we test extreme max and min cases
    if ( monoSamplesCount < 2 )
        monoSamplesCount = 2;

    if ( mTargetSamples > originalSamplesCount || monoSamplesCount > originalSamplesCount )
        monoSamplesCount = originalSamplesCount;


    sampleJump = originalSamplesCount / monoSamplesCount;

    newBuffer = getFreeByteBuffer( monoSamplesCount );

    if ( newBuffer == nullptr )
    {
        newBuffer = new Buffer( monoSamplesCount );
    }


    for ( int i = 0; i < monoSamplesCount; i++ )
    {
        //Obtain the most significant sample (last one in frame) every "sampleJump" samples
        monoSample = buffer->buffer[ ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) ];

        newBuffer->buffer[i] = monoSample;
    }

    return newBuffer;
}

/**
 * Here we delete obsolete sample buffers (if the player has already played past them)
 */
void AudioBufferManager2::deleteStaleBufferWraps()
{
    long currentTimeUs;
    BufferWrap * bufferWrap;

    currentTimeUs = mLastCurrentTimeUs;

    while ( !mBufferWrapList.empty() && mBufferWrapList.front()->presentationTimeUs < currentTimeUs )
    {
        bufferWrap = mBufferWrapList.front();

        mFreeBufferList.push_back( bufferWrap->buffer );

        mBufferWrapList.pop_front();

    }
}


/**
 * Method used to find already allocated buffer that is not in use
 */
Buffer * AudioBufferManager2::getFreeByteBuffer( int targetCapacity )
{
    Buffer * freeBuffer;

    for (std::list<Buffer*>::const_iterator iterator = mFreeBufferList.begin(), end = mFreeBufferList.end(); iterator != end; ++iterator)
    {
        freeBuffer = *iterator;

        if ( freeBuffer->cap <= targetCapacity  )
        {
            mFreeBufferList.remove( freeBuffer );

            freeBuffer->len = targetCapacity;

            return freeBuffer;
        }
    }


    return nullptr;
}

/**
 * Get target amount of samples for current playback time
 */
Buffer * AudioBufferManager2::getSamples()
{
    BufferWrap * bufferWrap;
    int samplesCount;
    int buffersCount;



    samplesCount = 0;
    buffersCount = 0;


    deleteStaleBufferWraps();

    if ( mBufferWrapList.empty() )
        return nullptr;


    mResultBuffer->len = mTargetSamples;

    std::list<BufferWrap*>::const_iterator iterator = mBufferWrapList.begin();

    while ( buffersCount < mBufferWrapList.size() && samplesCount + (*iterator)->buffer->len <= mTargetSamples )
    {
        bufferWrap = *iterator;

        for ( int i = 0; i < bufferWrap->buffer->len; i++ )
        {
            mResultBuffer->buffer[samplesCount + i] = bufferWrap->buffer->buffer[ i ];
        }


        samplesCount += bufferWrap->buffer->len;
        buffersCount++;
        iterator++;
    }


    mResultBuffer->len = samplesCount;

    return mResultBuffer;
}
