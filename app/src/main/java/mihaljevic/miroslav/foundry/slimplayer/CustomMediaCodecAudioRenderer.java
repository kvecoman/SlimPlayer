package mihaljevic.miroslav.foundry.slimplayer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
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
    private byte[] dbgDummyMonoSamples = { 25,67, 35, 100, 111, 1, 36, 74, 45, 55, 12, 19, 66, 69 };
    private long dbgOldTime;

    private long timesProcessOutputBufferCalled = 0;


    private int mOldBufferIndex = -1;

    @C.PcmEncoding
    private int mOutputPcmEncoding;

    //private String mOutputMimeType;

    private int mOutputChanelCount;
    private int mOutputSampleRate;

    private int mPcmFrameSize;



    protected final String TAG = getClass().getSimpleName();


    private AudioBufferManager mAudioBufferManager;


    public CustomMediaCodecAudioRenderer( MediaCodecSelector mediaCodecSelector, AudioBufferManager audioBufferManager/*, int targetSamples, int targetTimeSpan */)
    {
        super( mediaCodecSelector );

        mAudioBufferManager     = audioBufferManager;
    }

    public void setAudioBufferManager( AudioBufferManager audioBufferManager )
    {
        mAudioBufferManager = audioBufferManager;
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
        //mOutputMimeType     = outputFormat.getString    ( MediaFormat.KEY_MIME );
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

        /*Log.d( TAG, "Current position time:" + positionUs );

        if ( positionUs - dbgOldTime > 100000 )
        {
            int x = 0;
        }*/

        //TODO - enable/disable callback method for this

        //Make sure we have fresh buffer and also the one that won't be skipped
        if ( mOldBufferIndex != bufferIndex && !shouldSkip && mAudioBufferManager != null )
        {
            //processBuffer( buffer, bufferPresentationTimeUs, positionUs );

            mAudioBufferManager.onProcessBuffer2( buffer, bufferPresentationTimeUs, mPcmFrameSize, mOutputSampleRate );

            mOldBufferIndex = bufferIndex;
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



}
