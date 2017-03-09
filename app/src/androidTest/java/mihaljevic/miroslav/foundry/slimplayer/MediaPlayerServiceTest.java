package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static mihaljevic.miroslav.foundry.slimplayer.TestData.*;


/**
 * Created by miroslav on 07.03.17..
 */

@RunWith( AndroidJUnit4.class )
public class MediaPlayerServiceTest
{

    private CountDownLatch signal = new CountDownLatch( 1 );
    private boolean tested = false;

    private MediaBrowserCompat      mediaBrowser;
    private MediaControllerCompat   mediaController;

    @Before
    public void before()
    {
        tested = false;
        signal = new CountDownLatch( 1 );
    }


    //************************************************************************************************
    @Test
    public void testPlayFromMediaID() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testPlayFromMediaIDConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

    }


    private class testPlayFromMediaIDConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testPlayFromMediaIDControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }




        }
    }

    private class testPlayFromMediaIDControllerCallbacks extends MediaControllerCompat.Callback
    {

        @Override
        public void onQueueChanged( List< MediaSessionCompat.QueueItem > queue )
        {
            super.onQueueChanged( queue );

            MediaSessionCompat.QueueItem queueItem;

            queueItem   = queue.get( EXPECTED_POSITION );

            assertEquals( EXPECTED_COUNT, queue.size() );
            assertEquals( EXPECTED_TITLE, queueItem.getDescription().getTitle() );

            mediaBrowser.disconnect();

            signal.countDown();

        }
    }

    //********************************************************************************************************
    @Test
    public void testStop() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testStopConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testStopConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testStopControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testStopControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean stopCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING )
            {
                mediaController.getTransportControls().stop();
                stopCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_STOPPED && stopCalled )
            {
                int queueCount;

                queueCount = mediaController.getQueue().size();

                assertEquals( EXPECTED_COUNT, queueCount );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }

    //********************************************************************************************************
    @Test
    public void testPause() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testPauseConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testPauseConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testPauseControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testPauseControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean pauseCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING )
            {
                mediaController.getTransportControls().pause();
                pauseCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && pauseCalled )
            {
                int queueCount;
                int position;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }




    //********************************************************************************************************
    @Test
    public void testResume() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testResumeConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testResumeConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testResumeControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testResumeControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean pauseCalled = false;
        private boolean resumeCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && !pauseCalled )
            {
                mediaController.getTransportControls().pause();
                pauseCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && pauseCalled )
            {
                mediaController.getTransportControls().play();
                resumeCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && resumeCalled )
            {
                int queueCount;
                int position;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }

    //********************************************************************************************************
    @Test
    public void testSkipToPrevious() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testSkipToPreviousConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testSkipToPreviousConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testSkipToPreviousControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION + 1 );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testSkipToPreviousControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean skipCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && !skipCalled )
            {
                mediaController.getTransportControls().skipToPrevious();
                skipCalled = true;
            }

            else if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && skipCalled )
            {
                int queueCount;
                int position;
                MediaSessionCompat.QueueItem queueItem;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();
                queueItem = mediaController.getQueue().get( position );

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );
                assertEquals( EXPECTED_TITLE, queueItem.getDescription().getTitle() );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }


    //********************************************************************************************************
    @Test
    public void testSkipToNext() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testSkipToNextConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testSkipToNextConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testSkipToNextControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION - 1 );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testSkipToNextControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean skipCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && !skipCalled )
            {
                mediaController.getTransportControls().skipToNext();
                skipCalled = true;
            }

            else if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && skipCalled )
            {
                int queueCount;
                int position;
                MediaSessionCompat.QueueItem queueItem;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();
                queueItem = mediaController.getQueue().get( position );

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );
                assertEquals( EXPECTED_TITLE, queueItem.getDescription().getTitle() );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }


    //********************************************************************************************************
    @Test
    public void testSkipToQueueItem() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testSkipToQueueItemConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testSkipToQueueItemConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testSkipToQueueItemControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, 0 );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testSkipToQueueItemControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean skipCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && !skipCalled )
            {
                mediaController.getTransportControls().skipToQueueItem( EXPECTED_POSITION );
                skipCalled = true;
            }

            else if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && skipCalled )
            {
                int queueCount;
                int position;
                MediaSessionCompat.QueueItem queueItem;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();
                queueItem = mediaController.getQueue().get( position );

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );
                assertEquals( EXPECTED_TITLE, queueItem.getDescription().getTitle() );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }

    //********************************************************************************************************
    @Test
    public void testSeekTo() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testSeekToConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testSeekToConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testSeekToControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testSeekToControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean seekToCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && !seekToCalled )
            {
                mediaController.getTransportControls().pause();
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && !seekToCalled)
            {
                mediaController.getTransportControls().seekTo( EXPECTED_SEEK );
                seekToCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && seekToCalled)
            {
                int queueCount;
                int position;
                MediaSessionCompat.QueueItem queueItem;
                int seek;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();
                queueItem = mediaController.getQueue().get( position );
                seek = (int)state.getPosition();

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );
                assertEquals( EXPECTED_TITLE, queueItem.getDescription().getTitle() );
                assertEquals( EXPECTED_SEEK, seek );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }

    //********************************************************************************************************
    @Test
    public void testFastForward() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testFastForwardConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testFastForwardConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testFastForwardControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testFastForwardControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean seekToCalled = false;
        private boolean fastForwardCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && !seekToCalled )
            {
                mediaController.getTransportControls().pause();
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && !seekToCalled )
            {
                mediaController.getTransportControls().seekTo( EXPECTED_SEEK );
                seekToCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && seekToCalled && !fastForwardCalled)
            {
                mediaController.getTransportControls().fastForward();
                fastForwardCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && fastForwardCalled )
            {
                int queueCount;
                int position;
                MediaSessionCompat.QueueItem queueItem;
                int seek;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();
                queueItem = mediaController.getQueue().get( position );
                seek = (int)state.getPosition();

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );
                assertEquals( EXPECTED_TITLE, queueItem.getDescription().getTitle() );
                assertEquals( EXPECTED_SEEK + MediaPlayerService.FF_SPEED, seek );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }

    //********************************************************************************************************
    @Test
    public void testRewind() throws InterruptedException
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                mediaBrowser = new MediaBrowserCompat( InstrumentationRegistry.getTargetContext(),
                        MediaPlayerService.COMPONENT_NAME,
                        new testRewindConnectionCallbacks(),
                        null );

                mediaBrowser.connect();
            }
        } );

        signal.await();

        assertTrue( tested );
    }

    private class testRewindConnectionCallbacks extends MediaBrowserCompat.ConnectionCallback
    {
        @Override
        public void onConnected()
        {
            super.onConnected();
            Bundle extras;

            try
            {
                mediaController = new MediaControllerCompat( InstrumentationRegistry.getTargetContext(), mediaBrowser.getSessionToken() );
                mediaController.registerCallback( new testRewindControllerCallbacks() );

                extras = new Bundle();
                extras.putString( Const.SOURCE_KEY, Const.GENRES_SCREEN );
                extras.putString( Const.PARAMETER_KEY, "16" );
                extras.putString( Const.DISPLAY_NAME, "Oldies" );
                extras.putInt   ( Const.POSITION_KEY, EXPECTED_POSITION );

                mediaController.getTransportControls().playFromMediaId( Const.UNKNOWN, extras );
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }

        }
    }

    private class testRewindControllerCallbacks extends MediaControllerCompat.Callback
    {

        private boolean seekToCalled = false;
        private boolean rewindCalled = false;


        @Override
        public void onPlaybackStateChanged( PlaybackStateCompat state )
        {
            super.onPlaybackStateChanged( state );

            if ( state.getState() == PlaybackStateCompat.STATE_PLAYING && !seekToCalled )
            {
                mediaController.getTransportControls().pause();
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && !seekToCalled )
            {
                mediaController.getTransportControls().seekTo( EXPECTED_SEEK );
                seekToCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && seekToCalled && !rewindCalled )
            {
                mediaController.getTransportControls().rewind();
                rewindCalled = true;
            }
            else if ( state.getState() == PlaybackStateCompat.STATE_PAUSED && rewindCalled )
            {
                int queueCount;
                int position;
                MediaSessionCompat.QueueItem queueItem;
                int seek;

                queueCount = mediaController.getQueue().size();
                position = (int)state.getActiveQueueItemId();
                queueItem = mediaController.getQueue().get( position );
                seek = (int)state.getPosition();

                assertEquals( EXPECTED_COUNT, queueCount );
                assertEquals( EXPECTED_POSITION, position );
                assertEquals( EXPECTED_TITLE, queueItem.getDescription().getTitle() );
                assertEquals( EXPECTED_SEEK - MediaPlayerService.FF_SPEED, seek );

                tested = true;

                mediaBrowser.disconnect();

                signal.countDown();
            }
        }
    }
}
