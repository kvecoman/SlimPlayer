package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by miroslav on 11.03.17..
 *
 * Inspired by android-er.blogspot.com
 */

public class VisualizerView extends View
{
    protected final String TAG = getClass().getSimpleName();

    private int dbgOnDrawCalled = 0;

    private byte[]  mBytes;
    private float[] mPoints;
    private Rect    mRect = new Rect();
    private Paint   mForePaint = new Paint();

    private ByteBuffer  mByteBuffer;
    private int         mFrameSize;

    private LinkedBlockingQueue<BufferWrap > mBufferWrapQueue;

    private LinkedList<BufferWrap > mBufferWrapList = new LinkedList<>();

    private int mBuffersToShow;
    private int mSamplesToShow;

    public VisualizerView( Context context )
    {
        super( context );
        init();
    }

    public VisualizerView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
        init();
    }

    public VisualizerView( Context context, AttributeSet attrs, int defStyleAttr )
    {
        super( context, attrs, defStyleAttr );
        init();
    }

    private void init()
    {
        mBytes = null;
        mForePaint.setStrokeWidth   ( 1f );
        mForePaint.setAntiAlias     ( true );
        mForePaint.setColor         ( Color.rgb( 0, 128, 255 ) );

        mBufferWrapQueue = new LinkedBlockingQueue<>(  );
    }

    public void updateVisualizer( byte[] monoSamples, long presentationTimeUs, long currentTimeUs )
    {
        BufferWrap bufferWrap;

        if ( monoSamples == null )
            return;

        bufferWrap = new BufferWrap( monoSamples, presentationTimeUs );

        mBufferWrapList.addLast( bufferWrap );

        bufferWrap = null;

        //Remove all bufers that are before current position (these are not needed anymore)
        while ( !mBufferWrapList.isEmpty() && mBufferWrapList.getFirst().presentationTimeUs < currentTimeUs )
            mBufferWrapList.removeFirst();

        if ( mBufferWrapList.isEmpty() )
            return;

        bufferWrap = mBufferWrapList.getFirst();

        //Check the distance form current position to first bufferWrap after it is not too big
        //TODO - this to some constant, like maximum displacement time or something
        if ( Math.abs( bufferWrap.presentationTimeUs - currentTimeUs) > 200000 )
            return;


        mBuffersToShow = 0;
        mSamplesToShow = 0;

        //Calculate how many buffers we will show and to which amount of samples it will come to
        while ( mBuffersToShow < mBufferWrapList.size() && mSamplesToShow + mBufferWrapList.get( mBuffersToShow ).buffer.length <= TestPlayerActivity.VISUALIZATION_SAMPLES )
        {
            mSamplesToShow += mBufferWrapList.get( mBuffersToShow ).buffer.length;
            mBuffersToShow++;
        }

        invalidate();
    }

    public void updateVisualizer ( ByteBuffer byteBuffer, int frameSize )
    {
        mByteBuffer = byteBuffer;
        mFrameSize = frameSize;
        invalidate();
    }

    @Override
    protected void onDraw( Canvas canvas )
    {
        dbgOnDrawCalled++;
        Log.v(TAG, dbgOnDrawCalled + " onDraw()");


        super.onDraw( canvas );

        byte[] buffer;
        int newIndex;
        int count;

        if ( mByteBuffer != null)
        {
            int size;

            size = ( mByteBuffer.limit() - mByteBuffer.position() ) / mFrameSize;

            if ( mPoints == null || mPoints.length < size * 4 )
            {
                mPoints = new float[ size * 4 ];
            }

            mRect.set( 0, 0, getWidth(), getHeight() );

            for( int i = 0; i < size - 1; i++ )
            {
                mPoints[ i * 4 ]        = mRect.width() * i / ( size - 1 );
                mPoints[ i * 4 + 1 ]    = mRect.height() / 2 + ( ( byte ) ( mByteBuffer.get( i * mFrameSize ) + 128 ) ) * ( mRect.height() / 2 ) / 128;
                mPoints[ i * 4 + 2 ]    = mRect.width() * ( i + 1 ) / ( size - 1 );
                mPoints[ i * 4 + 3 ]    = mRect.height() / 2 + ( ( byte ) ( mByteBuffer.get( ( i + 1 ) * mFrameSize ) + 128 ) ) * ( mRect.height() / 2 ) / 128;
            }

            canvas.drawLines( mPoints, mForePaint );
        }
        else
        {
            if ( mBufferWrapList == null || mBufferWrapList.size() == 0 || mSamplesToShow == 0 || mBuffersToShow == 0 )
                return;

            count = mSamplesToShow - mBuffersToShow;

            if ( mPoints == null || mPoints.length != count * 4 )
            {
                mPoints = new float[ count * 4 ];
            }

            mRect.set( 0, 0, getWidth(), getHeight() );

            newIndex = 0;

            for ( int i = 0;i < mBuffersToShow; i++ )
            {
                buffer = mBufferWrapList.get( i ).buffer;

                for ( int k = 0; k < buffer.length - 1; k++ )
                {
                    mPoints[ newIndex * 4 ]        = mRect.width() * ( newIndex ) / ( count - 1 );
                    mPoints[ newIndex * 4 + 1 ]    = mRect.height() / 2 + ( ( byte ) ( buffer[k] + 128 ) ) * ( mRect.height() / 2 ) / 128;
                    mPoints[ newIndex * 4 + 2 ]    = mRect.width() * ( newIndex + 1 ) / ( count - 1 );
                    mPoints[ newIndex * 4 + 3 ]    = mRect.height() / 2 + ( ( byte ) ( buffer[k + 1] + 128 ) ) * ( mRect.height() / 2 ) / 128;

                    newIndex++;
                }
            }

            /*for( int i = 0; i < mSamplesToShow - 1; i++ )
            {
                mPoints[ i * 4 ]        = mRect.width() * i / ( mBytes.length - 1 );
                mPoints[ i * 4 + 1 ]    = mRect.height() / 2 + ( ( byte ) ( mBytes[i] + 128 ) ) * ( mRect.height() / 2 ) / 128;
                mPoints[ i * 4 + 2 ]    = mRect.width() * ( i + 1 ) / ( mBytes.length - 1 );
                mPoints[ i * 4 + 3 ]    = mRect.height() / 2 + ( ( byte ) ( mBytes[i + 1] + 128 ) ) * ( mRect.height() / 2 ) / 128;
            }*/

            canvas.drawLines( mPoints, mForePaint );
        }
    }

    private class BufferWrap
    {
        long presentationTimeUs;
        byte[] buffer;

        public BufferWrap( byte[] buffer, long presentationTimeUs )
        {
            this.presentationTimeUs = presentationTimeUs;
            this.buffer = buffer;
        }
    }
}
