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


    private int mOldBufferIndex = -1;

    @C.PcmEncoding
    private int mOutputPcmEncoding;


    private int mOutputChanelCount;
    private int mOutputSampleRate;

    private int mPcmFrameSize;

    private BufferReceiver mBufferReceiver;

    private boolean mEnabled = true;



    protected final String TAG = getClass().getSimpleName();



    public CustomMediaCodecAudioRenderer( MediaCodecSelector mediaCodecSelector )
    {
        super( mediaCodecSelector );
    }

    public void setBufferProcessing( boolean bufferProcessing )
    {
        mEnabled = bufferProcessing;
    }

    public void enableBufferProcessing()
    {
        mEnabled = true;
    }

    public void disableBufferProcessing()
    {
        mEnabled = false;
    }

    public boolean isBufferProcessing()
    {
        return mEnabled;
    }



    public void setBufferReceiver( BufferReceiver bufferReceiver )
    {
        mBufferReceiver = bufferReceiver;
    }

    public BufferReceiver getBufferReceiver()
    {
        return mBufferReceiver;
    }



    //test with AAC - EDIT: aparently AAC doesn't show up by android database
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


        mOutputChanelCount  = outputFormat.getInteger   ( MediaFormat.KEY_CHANNEL_COUNT );
        mOutputSampleRate   = outputFormat.getInteger   ( MediaFormat.KEY_SAMPLE_RATE );

        //Calculate pcmFrameSize (how many bytes it takes)
        switch( mOutputPcmEncoding )
        {
            case C.ENCODING_PCM_8BIT:
                mPcmFrameSize = mOutputChanelCount;
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
        if ( mEnabled && mOldBufferIndex != bufferIndex && !shouldSkip && mBufferReceiver != null )
        {

            mBufferReceiver.processBuffer( buffer, buffer.limit(), bufferPresentationTimeUs, mPcmFrameSize, mOutputSampleRate, positionUs );

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

    interface BufferReceiver
    {
       void processBuffer( ByteBuffer samplesBuffer, int samplesCount, long presentationTimeUs, int pcmFrameSize, int sampleRate, long currentTimeUs );
    }



}
