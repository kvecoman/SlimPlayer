//
// Created by miroslav on 08.04.17..
//


#include "AudioBufferManagerExo.h"







/**
 * BufferWrap is used to store buffer and its presentationTime property, so we know when to display it
 */
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





AudioBufferManagerExo::AudioBufferManagerExo( int targetSamples, int targetTimeSpan, int instance )
{
    mDestructorLock.lock();

    mInstance = instance;

    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "AudioBufferManagerExo() - constructor for instance %i", mInstance );

    mTargetSamples = targetSamples;
    mTargetTimeSpan = targetTimeSpan;

    mResultBuffer = new Buffer( targetSamples );

    mConstructed = true;

    mDestructorLock.unlock();


}


AudioBufferManagerExo::~AudioBufferManagerExo()
{


    mReleased = true;

    mResetLock.lock();
    mDestructorLock.lock();
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "AudioBufferManagerExoExo() - destructor for instance %i", mInstance );

    if ( mResultBuffer != nullptr )
        delete  mResultBuffer;

    for (std::list<BufferWrap*>::const_iterator iterator = mBufferWrapList.begin(), end = mBufferWrapList.end(); iterator != end; ++iterator)
    {
        delete *iterator;
    }

    for (std::list<Buffer*>::const_iterator iterator = mFreeBufferList.begin(), end = mFreeBufferList.end(); iterator != end; ++iterator)
    {
        delete *iterator;
    }

    mBufferWrapList.clear();
    mFreeBufferList.clear();


    mDestructorLock.unlock();
    mResetLock.unlock();
}

/**
 * Here we take buffer, take significant mono samples from it and store them for later use (display)
 */
void AudioBufferManagerExo::processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{



    /*if ( !mEnabled )
        return;*/

    mDestructorLock.lock();



    if ( mReleased || !mConstructed )
    {
        mDestructorLock.unlock();
        return;
    }

    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "processBuffer() for instance %i", mInstance );

    //If the seek has happened clear the list of current buffer wraps
    if ( currentTimeUs < mLastCurrentTimeUs )
        reset();


    BufferWrap *    bufferWrap = nullptr;
    Buffer *        monoBuffer = nullptr;


    monoBuffer = createMonoSamples( buffer, pcmFrameSize, sampleRate );

    if ( monoBuffer == nullptr )
    {
        mDestructorLock.unlock();
        return;
    }


    bufferWrap = new BufferWrap( monoBuffer, presentationTimeUs );

    mBufferWrapList.push_back( bufferWrap );







    mLastCurrentTimeUs = currentTimeUs;


    mDestructorLock.unlock();
}

/**
 * Function that takes in original samples buffer (containing hundreds or thousands of samples) and
 * only take the most significant mono samples
 */
Buffer * AudioBufferManagerExo::createMonoSamples( Buffer * buffer, jint pcmFrameSize, jint sampleRate )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "createMonoSamples()" );
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "createMonoSamples() for instance %i", mInstance );

    int     originalSamplesCount;
    float   representedTime;
    int     monoSamplesCount;
    int     sampleJump;

    //jbyte           monoSample;
    Buffer *        newBuffer;


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
        /*monoSample =

        newBuffer->buffer[i] = monoSample;*/

        //Optimized version
        newBuffer->buffer[i] = static_cast<jbyte>( abs( buffer->buffer[ ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) ] ) );
    }

    return newBuffer;
}

/**
 * Method used to find already allocated buffer that is not in use
 */
Buffer * AudioBufferManagerExo::getFreeByteBuffer( int minimalCapacity )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "getFreeByteBuffer()" );
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "getFreeByteBuffer() for instance %i", mInstance );

    Buffer * freeBuffer = nullptr;

    for (std::list<Buffer*>::const_iterator iterator = mFreeBufferList.begin(), end = mFreeBufferList.end(); iterator != end; ++iterator)
    {
        freeBuffer = *iterator;

        if ( freeBuffer->cap <= minimalCapacity  )
        {
            mFreeBufferList.remove( freeBuffer );

            freeBuffer->len = minimalCapacity;

            return freeBuffer;
        }
    }

    return nullptr;
}



/**
 * Here we delete obsolete sample buffers (if the player has already played past them)
 */
void AudioBufferManagerExo::deleteStaleBufferWraps()
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "deleteStaleBufferWraps()" );
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "deleteStaleBufferWraps() for instance %i", mInstance );


    long            currentTimeUs;
    BufferWrap *    bufferWrap = nullptr;
    Buffer *        buffer = nullptr;


    currentTimeUs = mLastCurrentTimeUs;



    mBufferWrapList.begin();

    while (  !mBufferWrapList.empty() &&  mBufferWrapList.front()->presentationTimeUs < currentTimeUs )
    {
        bufferWrap = mBufferWrapList.front();
        mBufferWrapList.remove( bufferWrap );

        buffer = bufferWrap->buffer;
        mFreeBufferList.push_back( buffer );

        //We decouple buffer and bufferWrap so we can delete bufferWrap without corrupting memory
        bufferWrap->buffer = nullptr;
        delete bufferWrap;

    }



}




/**
 * Get target amount of samples for current playback time
 */
Buffer * AudioBufferManagerExo::getSamples()
{


    /*if ( !mEnabled )
        return nullptr;*/



    mDestructorLock.lock();
    mResetLock.lock();


    if ( mReleased || !mConstructed )
    {
        mResetLock.unlock();
        mDestructorLock.unlock();

        return nullptr;
    }

    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "getSamples() for instance %i", mInstance );

    BufferWrap * bufferWrap = nullptr;
    int samplesCount;
    int buffersCount;




    samplesCount = 0;
    buffersCount = 0;


    deleteStaleBufferWraps();

    if ( mBufferWrapList.empty() )
    {
        mResetLock.unlock();
        mDestructorLock.unlock();
        return nullptr;
    }



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


    mResetLock.unlock();
    mDestructorLock.unlock();

    return mResultBuffer;
}

void AudioBufferManagerExo::reset()
{


    mResetLock.lock();

    if ( mReleased || !mConstructed || mBufferWrapList.size() <= 0 )
    {
        mResetLock.unlock();
        return;
    }

    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManagerExo", "reset()  for instance %i", mInstance );

    for (std::list<BufferWrap*>::const_iterator iterator = mBufferWrapList.begin(), end = mBufferWrapList.end(); iterator != end; ++iterator)
    {
            delete *iterator;
    }

    mBufferWrapList.clear();
    mLastCurrentTimeUs = 0;

    mResetLock.unlock();
}


/*void AudioBufferManagerExo::enable()
{
    mEnabled = true;
}*/


/*void AudioBufferManagerExo::disable()
{
    mEnabled = false;
}*/
