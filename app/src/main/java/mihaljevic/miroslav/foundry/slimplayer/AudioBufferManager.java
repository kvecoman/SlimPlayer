package mihaljevic.miroslav.foundry.slimplayer;

import android.util.Log;

import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.Buffer;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by miroslav on 25.03.17..
 *
 * Used to handle passing sample buffers around to components that need it for visualization of audio
 */

public class AudioBufferManager
{
    protected final String TAG = getClass().getSimpleName();

    //Lock when both mBufferWrapList and mFreeBufferList are in use
    //private final String BOTH_LISTS_LOCK = "both_lists_lock";

    private MediaCodecAudioRenderer mAudioRenderer;


    private final int mTargetSamples;
    private final int mTargetTimeSpan;

    /*private LinkedList< BufferWrap > mBufferWrapList;
    private LinkedList< ByteBuffer > mFreeBufferList;*/
    private List< BufferWrap > mBufferWrapList;
    private List< ByteBuffer > mFreeBufferList;

    //Memory allocated for samples that are retreived when getSamples is called
    private ByteBuffer mResultByteBuffer;

    /*private long dbgStartTime;
    private long dbgEndTime;
    private double dbgAverageTime = 0;*/

    /*static
    {
        System.loadLibrary( "visualizer" );

    }*/


    /*private static native void init();

    private static native void destroy();

    private native ByteBuffer createMonoSamples( ByteBuffer byteBuffer, int pcmFrameSize, int sampleRate );*/





    public AudioBufferManager( MediaCodecAudioRenderer audioRenderer,  int targetSamples, int targetTimeSpan )
    {

        mAudioRenderer  = audioRenderer;

        mTargetSamples  = targetSamples;
        mTargetTimeSpan = targetTimeSpan;

        /*mBufferWrapList = new LinkedList<>(  );
        mFreeBufferList = new LinkedList<>(  );*/

        mBufferWrapList = Collections.synchronizedList( new LinkedList<BufferWrap>(  ) );
        mFreeBufferList = Collections.synchronizedList( new LinkedList<ByteBuffer>(  ) );;

        mResultByteBuffer = ByteBuffer.allocateDirect( mTargetSamples );



        //init();


    }

    public void onProcessBuffer2( ByteBuffer byteBuffer, long presentationTimeUs, int pcmFrameSize, int sampleRate )
    {
        BufferWrap  bufferWrap;
        ByteBuffer  newBuffer;

        //ByteBuffer javaBuffer = createMonoSamplesJava( byteBuffer, pcmFrameSize, sampleRate );
        //dbgStartTime    = System.currentTimeMillis();
        newBuffer       = createMonoSamplesJava( byteBuffer, pcmFrameSize, sampleRate );
        //dbgEndTime      = System.currentTimeMillis();

        //dbgAverageTime = ( ( dbgAverageTime + ( double )( dbgEndTime - dbgStartTime ) ) ) / 2f;


        //Log.d( TAG, "createMonoSamples() average time: "  + String.format( "%.06f", dbgAverageTime ) + " ms");

        if ( newBuffer == null )
            return;

        bufferWrap = new BufferWrap( newBuffer, presentationTimeUs );

        //mBufferWrapList.addLast( bufferWrap );

        mBufferWrapList.add( bufferWrap );



    }

    private ByteBuffer createMonoSamplesJava( ByteBuffer byteBuffer, int pcmFrameSize, int sampleRate )
    {
        int     originalSamplesCount;
        float   representedTime;
        int     monoSamplesCount;
        int     sampleJump;

        byte        monoSample;
        ByteBuffer  newBuffer;


        originalSamplesCount = ( byteBuffer.limit() - byteBuffer.position() ) / pcmFrameSize;

        if ( originalSamplesCount == 0 )
            return null;

        representedTime = ( float ) originalSamplesCount / ( float ) sampleRate * 1000f;

        monoSamplesCount = ( int )( representedTime / ( float ) mTargetTimeSpan * ( float ) mTargetSamples );


        //Bellow we test extreme max and min cases
        if ( monoSamplesCount < 2 )
            monoSamplesCount = 2;

        if ( mTargetSamples > originalSamplesCount || monoSamplesCount > originalSamplesCount )
            monoSamplesCount = originalSamplesCount;


        sampleJump = originalSamplesCount / monoSamplesCount;

        newBuffer = getFreeByteBuffer( monoSamplesCount );

        if ( newBuffer == null )
        {
            newBuffer = ByteBuffer.allocateDirect( monoSamplesCount );
        }


        for ( int i = 0; i < monoSamplesCount; i++ )
        {
            //Obtain the most significant sample (last one in frame) every "sampleJump" samples
            monoSample = byteBuffer.get( ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) );

            newBuffer.put( i, monoSample );
        }

        return newBuffer;
    }

    private ByteBuffer getFreeByteBuffer( int targetCapacity )
    {
        ByteBuffer freeBuffer;

        for ( int i = 0; i < mFreeBufferList.size(); i++ )
        {
            freeBuffer = mFreeBufferList.get( i );


            if ( freeBuffer.capacity() <= targetCapacity  )
            {
                mFreeBufferList.remove( i );

                freeBuffer.limit( targetCapacity );

                return freeBuffer;
            }
        }

        return null;
    }





    private void deleteStaleBufferWraps()
    {
        long currentTimeUs;
        BufferWrap bufferWrap;

        currentTimeUs = mAudioRenderer.getPositionUs();

        //Log.d( TAG, "deleteStaleBufferWraps() - current time us: " + currentTimeUs );

        /*while ( !mBufferWrapList.isEmpty() && mBufferWrapList.getFirst().presentationTimeUs < currentTimeUs )
        {
            bufferWrap = mBufferWrapList.pollFirst();

            mFreeBufferList.addLast( bufferWrap.buffer );
        }*/

        while ( !mBufferWrapList.isEmpty() && mBufferWrapList.get(0).presentationTimeUs < currentTimeUs )
        {
            bufferWrap = mBufferWrapList.remove( 0 );

            mFreeBufferList.add( bufferWrap.buffer );
        }
    }

    public ByteBuffer getSamples()
    {
        BufferWrap bufferWrap;
        //BufferWrap resultBufferWrap;
        int samplesCount;
        int buffersCount;


        samplesCount = 0;
        buffersCount = 0;


        deleteStaleBufferWraps();

        if ( mBufferWrapList.isEmpty() )
            return null;


        mResultByteBuffer.limit( mTargetSamples );

        while ( buffersCount < mBufferWrapList.size() && samplesCount + mBufferWrapList.get( buffersCount ).buffer.limit() <= mTargetSamples )
        {
            bufferWrap = mBufferWrapList.get( buffersCount );

            for ( int i = 0; i < bufferWrap.buffer.limit(); i++ )
            {
                mResultByteBuffer.put( samplesCount + i, bufferWrap.buffer.get( i ) );
            }


            samplesCount += bufferWrap.buffer.limit();
            buffersCount++;
        }


        mResultByteBuffer.limit( samplesCount );

        return mResultByteBuffer;

    }




    public void release()
    {
        /*destroy();*/
    }



    public static class BufferWrap
    {
        long presentationTimeUs;
        ByteBuffer buffer;

        public BufferWrap( ByteBuffer buffer, long presentationTimeUs )
        {
            this.presentationTimeUs = presentationTimeUs;
            this.buffer = buffer;
        }
    }




}
