//
// Created by miroslav on 08.04.17..
//

#include "AudioBufferManager.h"


/**
 * Replica of java's ByteBuffer class, has similar fields, used to transfer samples around
 */
Buffer::Buffer( int cap )
{
        this->buffer = new jbyte[ cap ];
        this->cap = cap;
        this->len = cap;

        needsDelete = true;
}

Buffer::Buffer( jbyte * buffer, int len, int cap )
{
    this->buffer    = buffer;
    this->cap       = cap;
    this->len       = len;

    needsDelete = false;
}

Buffer::~Buffer()
{
    __android_log_print( ANDROID_LOG_VERBOSE, "Buffer", "~Buffer() - destructor" );

    if ( needsDelete )
        delete [] buffer;
}




/**
 * BufferWrap is used to store buffer and its presentationTime property, so we know when to display it
 */
BufferWrap::BufferWrap( Buffer * buffer, jlong presentationTimeUs )
{
    this->buffer                = buffer;
    this->presentationTimeUs    = presentationTimeUs;
}

BufferWrap::~BufferWrap()
{
    __android_log_print( ANDROID_LOG_VERBOSE, "BufferWrap", "~BufferWrap() - destructor" );

    if ( buffer != nullptr )
        delete buffer;
}





AudioBufferManager::AudioBufferManager( int targetSamples, int targetTimeSpan )
{
    mTargetSamples = targetSamples;
    mTargetTimeSpan = targetTimeSpan;

    mResultBuffer = new Buffer( targetSamples );
}

AudioBufferManager::~AudioBufferManager()
{
    mDeleteLock.lock();
    mDestructorLock.lock();

    delete  mResultBuffer;

    for (std::list<BufferWrap*>::const_iterator iterator = mBufferWrapList.begin(), end = mBufferWrapList.end(); iterator != end; ++iterator)
    {
        delete *iterator;
    }

    for (std::list<Buffer*>::const_iterator iterator = mFreeBufferList.begin(), end = mFreeBufferList.end(); iterator != end; ++iterator)
    {
        delete *iterator;
    }

    mDeleteLock.unlock();
    mDestructorLock.unlock();
}

/**
 * Here we take buffer, take significant mono samples from it and store them for later use (display)
 */
void AudioBufferManager::processBuffer( Buffer * buffer, jlong presentationTimeUs, jint pcmFrameSize, jint sampleRate, jlong currentTimeUs )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManager", "processBuffer()" );

    mDestructorLock.lock();

    BufferWrap *    bufferWrap;
    Buffer *        monoBuffer;

    //__android_log_print( ANDROID_LOG_DEBUG, "AudioBufferManager", "currentTimeUs: %lld", currentTimeUs );
    //__android_log_print( ANDROID_LOG_DEBUG, "AudioBufferManager", "presentationTimeUs: %lld", presentationTimeUs );

    //mProviderLock.lock();

    monoBuffer = createMonoSamples( buffer, pcmFrameSize, sampleRate );

    if ( monoBuffer == nullptr )
    {
        mDestructorLock.unlock();
        return;
    }


    bufferWrap = new BufferWrap( monoBuffer, presentationTimeUs );

    mBufferWrapList.push_back( bufferWrap );

    //mProviderLock.unlock();



    //If the seek has happened clear the list of current buffer wraps - EDIT: seeking problem solved in delete stale buffer wraps while loop
    /*if ( currentTimeUs < mLastCurrentTimeUs )
        reset();*/

    mLastCurrentTimeUs = currentTimeUs;

    //__android_log_print( ANDROID_LOG_DEBUG, "AudioBufferManager", "%lld stored in buffer wrap list", presentationTimeUs  );

    mDestructorLock.unlock();
}

/**
 * Function that takes in original samples buffer (containing hundreds or thousands of samples) and
 * only take the most significant mono samples
 */
Buffer * AudioBufferManager::createMonoSamples( Buffer * buffer, jint pcmFrameSize, jint sampleRate )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManager", "createMonoSamples()" );

    int     originalSamplesCount;
    float   representedTime;
    int     monoSamplesCount;
    int     sampleJump;

    jbyte           monoSample;
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
        monoSample = buffer->buffer[ ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) ];

        newBuffer->buffer[i] = monoSample;
    }

    return newBuffer;
}

/**
 * Method used to find already allocated buffer that is not in use
 */
Buffer * AudioBufferManager::getFreeByteBuffer( int minimalCapacity )
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManager", "getFreeByteBuffer()" );

    Buffer * freeBuffer;

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

    //mLock.lock();

    /*mFreeBufferList.begin();

    freeBuffer = mFreeBufferList.currentItem();

    while ( freeBuffer != nullptr )
    {

        if ( freeBuffer->cap <= minimalCapacity )
        {
            mFreeBufferList.removeCurrent();

            freeBuffer->len = minimalCapacity;

            mLock.unlock();
            return freeBuffer;
        }

        mFreeBufferList.next();
        freeBuffer = mFreeBufferList.currentItem();
    }*/

    //mLock.unlock();
    return nullptr;
}



/**
 * Here we delete obsolete sample buffers (if the player has already played past them)
 */
void AudioBufferManager::deleteStaleBufferWraps()
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManager", "deleteStaleBufferWraps()" );

    long            currentTimeUs;
    BufferWrap *    bufferWrap;
    Buffer *        buffer;
    //long            timeDifference;

    currentTimeUs = mLastCurrentTimeUs;

    //mLock.lock();

    mBufferWrapList.begin();

    //timeDifference =  mBufferWrapList.front()->presentationTimeUs - currentTimeUs;

    mDeleteLock.lock();

    while (  !mBufferWrapList.empty() &&  mBufferWrapList.front()->presentationTimeUs < currentTimeUs )
    {
        bufferWrap = mBufferWrapList.front();
        mBufferWrapList.remove( bufferWrap );

        buffer = bufferWrap->buffer;
        mFreeBufferList.push_back( buffer );

        //We decouple buffer and bufferWrap so we can delete bufferWrap without corrupting memory
        bufferWrap->buffer = nullptr;
        delete bufferWrap;

        //timeDifference =  mBufferWrapList.front()->presentationTimeUs - currentTimeUs;
    }

    mDeleteLock.unlock();

    //mLock.unlock();
}




/**
 * Get target amount of samples for current playback time
 */
Buffer * AudioBufferManager::getSamples()
{
    //__android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManager", "getSamples()" );

    mDestructorLock.lock();

    BufferWrap * bufferWrap;
    int samplesCount;
    int buffersCount;


    //mConsumerLock.lock();

    samplesCount = 0;
    buffersCount = 0;


    deleteStaleBufferWraps();

    if ( mBufferWrapList.empty() )
    {
        //mConsumerLock.unlock();
        mDestructorLock.unlock();
        return nullptr;
    }



    mResultBuffer->len = mTargetSamples;

    std::list<BufferWrap*>::const_iterator iterator = mBufferWrapList.begin();

    //mLock.lock();

    //mBufferWrapList.begin();

    mDeleteLock.lock();

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

    mDeleteLock.unlock();

    //mLock.unlock();


    mResultBuffer->len = samplesCount;

    //mConsumerLock.unlock();

    mDestructorLock.unlock();

    return mResultBuffer;
}

void AudioBufferManager::reset()
{
    __android_log_print( ANDROID_LOG_VERBOSE, "AudioBufferManager", "reset()" );

    mDeleteLock.lock();

    for (std::list<BufferWrap*>::const_iterator iterator = mBufferWrapList.begin(), end = mBufferWrapList.end(); iterator != end; ++iterator)
    {
        delete *iterator;
    }

    mBufferWrapList.clear();
    mLastCurrentTimeUs = 0;

    mDeleteLock.unlock();


}
