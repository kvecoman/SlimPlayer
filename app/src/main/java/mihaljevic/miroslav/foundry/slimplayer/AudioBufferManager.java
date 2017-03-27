package mihaljevic.miroslav.foundry.slimplayer;

import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by miroslav on 25.03.17..
 *
 * Used to handle passing sample buffers around to components that need it for visualization of audio
 */

public class AudioBufferManager
{
    protected final String TAG = getClass().getSimpleName();

    private MediaCodecAudioRenderer mAudioRenderer;


    private int mTargetSamples;
    private int mTargetTimeSpan;

    private LinkedList< BufferWrap > mBufferWrapList;






    public AudioBufferManager( MediaCodecAudioRenderer audioRenderer,  int targetSamples, int targetTimeSpan )
    {
        mBufferWrapList = new LinkedList<>();
        mAudioRenderer  = audioRenderer;

        mTargetSamples  = targetSamples;
        mTargetTimeSpan = targetTimeSpan;

        mBufferWrapList = new LinkedList<>(  );


    }

    public void onProcessBuffer2( ByteBuffer byteBuffer, long presentationTimeUs, int pcmFrameSize, int sampleRate )
    {
        int     originalSamplesCount;
        float   representedTime;
        int     monoSamplesCount;
        int     sampleJump;

        BufferWrap bufferWrap;

        //TODO - new system of passing samples work, but the bug with renderer time still persists

        //Log.d( TAG, "onProcessBuffer2()" );

        originalSamplesCount = ( byteBuffer.limit() - byteBuffer.position() ) / pcmFrameSize;

        if ( originalSamplesCount == 0 )
            return;

        representedTime = ( float ) originalSamplesCount / ( float ) sampleRate * 1000f;

        monoSamplesCount = ( int )( representedTime / ( float ) mTargetTimeSpan * ( float ) mTargetSamples );




        //Bellow we test extreme max and min cases
        if ( monoSamplesCount < 2 )
            monoSamplesCount = 2;

        if ( mTargetSamples > originalSamplesCount || monoSamplesCount > originalSamplesCount )
            monoSamplesCount = originalSamplesCount;


        sampleJump = originalSamplesCount / monoSamplesCount;

        bufferWrap = new BufferWrap( null, presentationTimeUs );
        bufferWrap.buffer = new Byte[ monoSamplesCount ];


        for ( int i = 0; i < monoSamplesCount; i++ )
        {
            //Obtain the most significant sample (last one in frame) every "sampleJump" samples
            bufferWrap.buffer[ i ] = byteBuffer.get( ( i * pcmFrameSize * sampleJump ) + ( pcmFrameSize - 1 ) );
        }

        synchronized ( this )
        {
            mBufferWrapList.addLast( bufferWrap );
        }



    }


    private synchronized void deleteStaleBufferWraps()
    {
        long currentTimeUs;

        currentTimeUs = mAudioRenderer.getPositionUs();

        Log.d( TAG, "deleteStaleBufferWraps() - current time us: " + currentTimeUs );

        while ( !mBufferWrapList.isEmpty() && mBufferWrapList.getFirst().presentationTimeUs < currentTimeUs )
            mBufferWrapList.removeFirst();
    }

    public synchronized BufferWrap getSamples()
    {
        BufferWrap bufferWrap;
        BufferWrap resultBufferWrap;
        int samplesCount;
        int buffersCount;


        samplesCount = 0;
        buffersCount = 0;

        //Log.d( TAG, "getSamples()" );

        deleteStaleBufferWraps();

        if ( mBufferWrapList.isEmpty() )
            return null;

        resultBufferWrap        = new BufferWrap( null, -1 );
        resultBufferWrap.buffer = new Byte[ mTargetSamples ];

        while ( buffersCount < mBufferWrapList.size() && samplesCount + mBufferWrapList.get( buffersCount ).buffer.length <= mTargetSamples )
        {
            bufferWrap = mBufferWrapList.get( buffersCount );

            for ( int i = 0; i < bufferWrap.buffer.length; i++ )
            {
                resultBufferWrap.buffer[ samplesCount + i ] = bufferWrap.buffer[ i ];
            }


            samplesCount += bufferWrap.buffer.length;
            buffersCount++;
        }

        resultBufferWrap.end = samplesCount - 1;

        return resultBufferWrap;

    }



    public static class BufferWrap
    {
        long presentationTimeUs;
        Byte[] buffer;
        int end = -1;

        public BufferWrap( Byte[] buffer, long presentationTimeUs )
        {
            this.presentationTimeUs = presentationTimeUs;
            this.buffer = buffer;
        }
    }


}
