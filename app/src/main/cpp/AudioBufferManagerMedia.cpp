//
// Created by miroslav on 02.05.17..
//

#include "AudioBufferManagerMedia.h"

AudioBufferManagerMedia::AudioBufferManagerMedia( int targetSamples )
{
    mLock.lock();

    mTargetSamples = targetSamples;

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
    int originalSamplesCount;
    int monoSamplesCount;
    int sampleJump;

    /*unsigned int unsignedSample;
    int sample;*/


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
        /*unsignedSample = (jboolean)buffer->buffer[ ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) ];

        sample = ((int)unsignedSample) - 127;

        sample = abs( sample );

        mResultBuffer->buffer[i] = (jbyte)sample;*/



        //Optimized version
        //Here we take the unsigned sample and limit it to 8 bit signed format (to 127)
        mResultBuffer->buffer[i] = (jbyte)abs( ( ( int )(jboolean)buffer->buffer[ ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) ] ) - 127 );
    }

    //mResultBuffer->len = monoSamplesCount;


    //mLock.unlock();

}

Buffer * AudioBufferManagerMedia::getSamples()
{
    return mResultBuffer;
}
