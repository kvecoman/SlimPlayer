//
// Created by miroslav on 02.05.17..
//

#include "AudioBufferManagerMedia.h"

AudioBufferManagerMedia::AudioBufferManagerMedia( int targetSamples, int targetTimeSpan )
{
    mLock.lock();

    mTargetSamples = targetSamples;
    mTargetTimeSpan = targetTimeSpan;

    mResultBuffer = new Buffer( targetSamples );

    mConstructed = true;

    mLock.unlock();
}

AudioBufferManagerMedia::~AudioBufferManagerMedia()
{
    mLock.lock();

    mReleased = true;

    delete mResultBuffer;

    mLock.unlock();
}

void AudioBufferManagerMedia::processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
    int     originalSamplesCount;
    int     monoSamplesCount;
    int     sampleJump;


    if ( mReleased || !mConstructed )
    {
        return;
    }


    //mLock.lock();


    originalSamplesCount = buffer->len  / pcmFrameSize;

    if ( originalSamplesCount == 0 )
    {
        //mLock.unlock();
        return;
    }


    monoSamplesCount = mTargetSamples;

    //Bellow we test extreme max and min cases
    /*if ( monoSamplesCount < 2 )
        monoSamplesCount = 2;*/

    if ( mTargetSamples > originalSamplesCount || monoSamplesCount > originalSamplesCount )
        monoSamplesCount = originalSamplesCount;

    sampleJump = originalSamplesCount / monoSamplesCount;

    for ( int i = 0; i < monoSamplesCount; i++ )
    {
        //Optimized version
        //mResultBuffer->buffer[i] = buffer->buffer[ ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) ];

        //Here we take the unsigned sample and limit it to 8 bit signed format (to 127)
        mResultBuffer->buffer[i] = static_cast<jbyte>( ( double( static_cast<jboolean> ( buffer->buffer[ ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) ] ) ) / 255.0 ) * 127.0 );
    }

    //mResultBuffer->len = monoSamplesCount;


    //mLock.unlock();

}

Buffer * AudioBufferManagerMedia::getSamples()
{
    return mResultBuffer;
}
