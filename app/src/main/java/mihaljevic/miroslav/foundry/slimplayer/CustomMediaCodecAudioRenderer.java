package mihaljevic.miroslav.foundry.slimplayer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.util.MimeTypes;

import java.nio.ByteBuffer;


/**
 * Created by miroslav on 13.03.17..
 *
 * Used to expose byte buffer for visualization purposes
 *
 * @author Miroslav Mihaljević
 */

public class CustomMediaCodecAudioRenderer extends MediaCodecAudioRenderer
{
    private long timesProcessOutputBufferCalled = 0;


    private int oldBufferIndex = -1;

    @C.PcmEncoding
    private int mOutputPcmEncoding;

    private String mOutputMimeType;

    private int mOutputChanelCount;
    private int mOutputSampleRate;

    private int mPcmFrameSize;

    private int mTargetSamples;
    private int mTargetTimeSpan;


    protected final String TAG = getClass().getSimpleName();

    private OutputBufferListener mOutputBufferListener;


    public CustomMediaCodecAudioRenderer( MediaCodecSelector mediaCodecSelector, OutputBufferListener outputBufferListener, int targetSamples, int targetTimeSpan )
    {
        super( mediaCodecSelector );

        mOutputBufferListener   = outputBufferListener;
        mTargetSamples          = targetSamples;
        mTargetTimeSpan         = targetTimeSpan;
    }



    public void setOutputBufferListener( OutputBufferListener outputBufferListener )
    {
        mOutputBufferListener = outputBufferListener;
    }



    //TODO - test with AAC
    @Override
    protected void onInputFormatChanged( Format newFormat ) throws ExoPlaybackException
    {
        super.onInputFormatChanged( newFormat );

        Log.d( TAG, "inputFormatChanged: " + newFormat );


        //Here we make predictions about output audio encoding
        if ( TextUtils.equals( newFormat.sampleMimeType, MimeTypes.AUDIO_RAW ) )
        {
            //AUDIO_RAW is pcm encoded audio, and as such we can just take its encoding

            mOutputPcmEncoding = newFormat.pcmEncoding;
        }
        else
        {
            //For anything else, we expect codec to output it in 16 bit PCM
            mOutputPcmEncoding = C.ENCODING_PCM_16BIT;
        }
    }

    @Override
    @TargetApi( Build.VERSION_CODES.JELLY_BEAN)
    protected void onOutputFormatChanged( MediaCodec codec, MediaFormat outputFormat )
    {
        super.onOutputFormatChanged( codec, outputFormat );

        Log.d( TAG, "outputFormatChanged: " + outputFormat);

        //Get format info
        mOutputMimeType     = outputFormat.getString    ( MediaFormat.KEY_MIME );
        mOutputChanelCount  = outputFormat.getInteger   ( MediaFormat.KEY_CHANNEL_COUNT );
        mOutputSampleRate   = outputFormat.getInteger   ( MediaFormat.KEY_SAMPLE_RATE );

        //Calculate pcmFrameSize (how many bytes it takes)
        switch( mOutputPcmEncoding )
        {
            case C.ENCODING_PCM_8BIT:
                mPcmFrameSize = 1 * mOutputChanelCount;
                break;
            case C.ENCODING_PCM_16BIT:
                mPcmFrameSize = 2 * mOutputChanelCount;
                break;
            case C.ENCODING_PCM_24BIT:
                mPcmFrameSize = 3 * mOutputChanelCount;
                break;
            case C.ENCODING_PCM_32BIT:
                mPcmFrameSize = 4 * mOutputChanelCount;
                break;
            default:
                mPcmFrameSize = 2 * mOutputChanelCount;
        }

    }

    @Override
    protected boolean processOutputBuffer( long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs, boolean shouldSkip ) throws ExoPlaybackException
    {
        boolean fullyProcessed;


        //Make sure we have fresh buffer and also the one that won't be skipped
        if ( oldBufferIndex != bufferIndex && !shouldSkip )
        {
            processBuffer( buffer, bufferPresentationTimeUs );

            oldBufferIndex = bufferIndex;
        }


        fullyProcessed = super.processOutputBuffer( positionUs,
                                                    elapsedRealtimeUs,
                                                    codec,
                                                    buffer,
                                                    bufferIndex,
                                                    bufferFlags,
                                                    bufferPresentationTimeUs,
                                                    shouldSkip );

        return fullyProcessed;
    }

    private void processBuffer( final ByteBuffer byteBuffer, final long bufferPresentationTimeUs )
    {
        byte[] buffer;

        buffer = acquirePartialMonoSamples( byteBuffer );
        absolute( buffer );

        if ( mOutputBufferListener != null )
        {
            mOutputBufferListener.onOutputBuffer( buffer, bufferPresentationTimeUs );
        }
    }

    private void processBufferAsync( final ByteBuffer byteBuffer, final long bufferPresentationTimeUs )
    {

        Log.v( TAG, "processBufferAsync()" );

        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground( Void... params )
            {

                processBuffer( byteBuffer, bufferPresentationTimeUs );

                return null;
            }


        }.execute(  );
    }

    private byte[] acquirePartialMonoSamples( ByteBuffer byteBuffer )
    {
        byte[]  partialBuffer;
        int     numberOfSamples;
        float   representedTime;
        int     monoPartialSamples;
        int     sampleJump;


        //TODO - averaging of samples

        numberOfSamples = ( byteBuffer.limit() - byteBuffer.position() ) / mPcmFrameSize;

        if ( numberOfSamples == 0 )
            return null;

        representedTime = ( float ) numberOfSamples / ( float ) mOutputSampleRate * 1000f;

        monoPartialSamples = ( int )( representedTime / ( float ) mTargetTimeSpan * ( float ) mTargetSamples );

        //Bellow we test extreme max and min cases

        if ( monoPartialSamples < 2 )
            monoPartialSamples = 2;

        if ( mTargetSamples > numberOfSamples || monoPartialSamples > numberOfSamples )
            monoPartialSamples = numberOfSamples;


        partialBuffer = new byte[ monoPartialSamples ];

        sampleJump = numberOfSamples / monoPartialSamples;

        for ( int i = 0; i < monoPartialSamples; i++ )
        {
            partialBuffer[ i ] = byteBuffer.get( ( i * mPcmFrameSize * sampleJump ) + ( mPcmFrameSize - 1 ) );
        }

        return partialBuffer;
    }

    private void absolute( byte[] buffer )
    {
        for ( int i = 0; i < buffer.length; i++ )
        {
            buffer[ i ] = ( byte ) Math.abs( ( short ) buffer[i] );
        }
    }


    /*private byte[] resampleToPCM16Bit( ByteBuffer byteBuffer )
    {
        byte[]  resampledBuffer;
        int     originalSize;
        int     newSize;
        int     newIndex;

        if ( byteBuffer == null || byteBuffer.limit() == 0 )
            return null;

        originalSize    = byteBuffer.limit();
        newSize         = -1;
        newIndex        = 0;

        switch ( mOutputPcmEncoding )
        {
            case C.ENCODING_PCM_8BIT:
                newSize = originalSize * 2;
                break;
            case C.ENCODING_PCM_24BIT:
                newSize = ( originalSize / 3 ) * 2;
                break;
            case C.ENCODING_PCM_32BIT:
                newSize =  originalSize / 2;
                break;
        }

        if ( newSize <= 0 )
            return null;

        resampledBuffer = new byte[ newSize ];

        switch ( mOutputPcmEncoding )
        {
            case C.ENCODING_PCM_8BIT:
                for ( int i = 0; i < originalSize; i++ )
                {
                    resampledBuffer[ i * 2 ]        = ( byte ) 0;
                    resampledBuffer[ i * 2 + 1 ]    = ( byte ) ( ( byteBuffer.get( i ) & 0xFF ) - 128 );
                }
                break;
            case C.ENCODING_PCM_24BIT:
                for ( int i = 0; i < originalSize; i += 3 )
                {
                    resampledBuffer[ newIndex ]     = byteBuffer.get( i + 1 );
                    resampledBuffer[ newIndex + 1 ] = byteBuffer.get( i + 2 );
                    newIndex += 2;
                }
                break;
            case C.ENCODING_PCM_32BIT:
                for ( int i = 0; i < originalSize; i += 4 )
                {
                    resampledBuffer[ newIndex ]     = byteBuffer.get( i + 2 );
                    resampledBuffer[ newIndex + 1 ] = byteBuffer.get( i + 3 );
                    newIndex += 2;
                }
                break;
            default:
                return null;
        }

        return resampledBuffer;

    }

    private byte[] resampleToPCM8Bit( ByteBuffer byteBuffer )
    {
        byte[]  resampledBuffer;
        int     originalSize;
        int     newSize;
        int     newIndex;

        if ( byteBuffer == null || byteBuffer.limit() == 0 )
            return null;

        originalSize    = byteBuffer.limit();
        newSize         = -1;
        newIndex        = 0;

        switch ( mOutputPcmEncoding )
        {
            case C.ENCODING_PCM_16BIT:
                newSize = originalSize / 2;
                break;
            case C.ENCODING_PCM_24BIT:
                newSize = originalSize / 3 ;
                break;
            case C.ENCODING_PCM_32BIT:
                newSize =  originalSize / 4;
                break;
        }

        if ( newSize <= 0 )
            return null;

        resampledBuffer = new byte[ newSize ];

        switch ( mOutputPcmEncoding )
        {
            case C.ENCODING_PCM_16BIT:
                for ( int i = 0; i < originalSize; i += 2 )
                {
                    resampledBuffer[ newIndex ] = byteBuffer.get( i + 1 );
                    newIndex++;
                }
                break;
            case C.ENCODING_PCM_24BIT:
                for ( int i = 0; i < originalSize; i += 3 )
                {
                    resampledBuffer[ newIndex ] = byteBuffer.get( i + 2 );
                    newIndex++;
                }
                break;
            case C.ENCODING_PCM_32BIT:
                for ( int i = 0; i < originalSize; i += 4 )
                {
                    resampledBuffer[ newIndex ] = byteBuffer.get( i + 3 );
                    newIndex++;
                }
                break;
            default:
                return null;
        }

        return resampledBuffer;

    }*/

    public interface OutputBufferListener
    {
        void onOutputBuffer( byte[] buffer, long bufferPresentationTimeUs );
    }


}
