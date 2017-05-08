package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import mihaljevic.miroslav.foundry.slimplayer.activities.MainActivity;

import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertArrayEquals;
import static mihaljevic.miroslav.foundry.slimplayer.TestData.*;

/**
 * Created by miroslav on 09.03.17..
 */

@RunWith( AndroidJUnit4.class )
public class UtilsTest
{
    private CountDownLatch signal = new CountDownLatch( 1 );

    @Rule
    public ActivityTestRule<MainActivity > mActivityRule =
            new ActivityTestRule< MainActivity >( MainActivity.class );

    @Test
    public void testPlaylistContainsAudioID_positive()
    {
        boolean     result;
        Bundle      bundle;
        Cursor      playlistCursor;

        bundle = ScreenBundles.getBundleForSubScreen( Const.PLAYLISTS_SCREEN, String.valueOf( TEST_PLAYLIST_ID ) );

        playlistCursor = Utils.queryMedia( bundle );

        result = Utils.playlistContainsAudioID( playlistCursor, Long.valueOf( EXPECTED_AUDIO_ID ) );

        assertTrue( result );
    }

    @Test
    public void testPlaylistContainsAudioID_negative()
    {
        boolean     result;
        Bundle      bundle;
        Cursor      playlistCursor;

        bundle = ScreenBundles.getBundleForSubScreen( Const.PLAYLISTS_SCREEN, String.valueOf( TEST_PLAYLIST_ID ) );

        playlistCursor = Utils.queryMedia( bundle );

        result = Utils.playlistContainsAudioID( playlistCursor, -278 );

        playlistCursor.close();

        assertFalse( result );
    }

    @Test
    public void testCheckIfPlaylistExist_positive()
    {
        boolean result;

        result = Utils.checkIfPlaylistExist( TEST_PLAYLIST );

        assertTrue( result );
    }

    @Test
    public void testCheckIfPlaylistExist_negative()
    {
        boolean result;

        result = Utils.checkIfPlaylistExist( "HJEDKJDJGVNOSJJGSIJDHGFISHJFGNSKJNGFJS" );

        assertFalse( result );
    }

    @Test
    public void testCreatePlaylist()
    {

        boolean playlistCreated;

        Utils.createPlaylist( CREATED_TEST_PLAYLIST );

        playlistCreated = Utils.checkIfPlaylistExist( CREATED_TEST_PLAYLIST );

        deleteCreatedPlaylist();

        assertTrue( playlistCreated );

    }

    private void deleteCreatedPlaylist()
    {
        ContentResolver     resolver;

        resolver = InstrumentationRegistry.getContext().getContentResolver();

        resolver.delete(    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                            MediaStore.Audio.Playlists.NAME + "='" + CREATED_TEST_PLAYLIST + "'",
                            null );
    }

    @Test
    public void testInsertIntoPlaylist()
    {
        ArrayList<String>   songIDs;
        long                playlistID;
        Cursor              playlistIDCursor;
        Cursor              playlistCursor;
        Bundle              bundle;
        ContentResolver     resolver;
        boolean             inserted;

        songIDs = new ArrayList<>(  );
        songIDs.add( EXPECTED_AUDIO_ID );

        resolver = InstrumentationRegistry.getContext().getContentResolver();

        Utils.createPlaylist( CREATED_TEST_PLAYLIST );

        playlistIDCursor = resolver.query( MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        new String [] { MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME },
                        MediaStore.Audio.Playlists.NAME + "='" + CREATED_TEST_PLAYLIST + "'",
                        null,null );

        playlistIDCursor.moveToFirst();

        playlistID = playlistIDCursor.getLong( playlistIDCursor.getColumnIndex( MediaStore.Audio.Playlists._ID ) );

        playlistIDCursor.close();

        Utils.insertIntoPlaylist( songIDs, playlistID );

        bundle = ScreenBundles.getBundleForSubScreen( Const.PLAYLISTS_SCREEN, String.valueOf( playlistID ) );

        playlistCursor = Utils.queryMedia( bundle );

        inserted = Utils.playlistContainsAudioID( playlistCursor, Long.valueOf( EXPECTED_AUDIO_ID ) );

        playlistCursor.close();

        deleteCreatedPlaylist();

        assertTrue( inserted );

    }


    @Test
    public void testDeleteFromList()
    {
        List<MediaBrowserCompat.MediaItem>  mediaItems;
        MusicProvider                       musicProvider;
        Uri                                 listURI;
        HashSet<Integer>                    selection;
        int                                 targetPosition;
        boolean                             deleted;

        Utils.createPlaylist( CREATED_TEST_PLAYLIST );

        selection       = new HashSet<>( 1 );
        musicProvider   = MusicProvider.getInstance();
        listURI         = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        mediaItems      = musicProvider.loadMedia( Const.PLAYLISTS_SCREEN, null );

        targetPosition = -1;

        for (int i = 0; i < mediaItems.size();i++)
        {
            if ( mediaItems.get( i ).getDescription().getTitle().equals( CREATED_TEST_PLAYLIST ) )
                targetPosition = i;
        }

        selection.add( targetPosition );


        Utils.deleteFromList( mediaItems, listURI, selection, MediaStore.Audio.Playlists._ID );

        deleted = !Utils.checkIfPlaylistExist( CREATED_TEST_PLAYLIST );

        assertTrue( deleted );

    }

    @Test
    public void testQueryMedia()
    {
        Bundle bundle;
        Cursor cursor;
        String title;
        String artist;
        String album;

        bundle = ScreenBundles.getBundleForSubScreen( TEST_SOURCE, TEST_PARAMETER );
        cursor = Utils.queryMedia( bundle );

        cursor.moveToPosition( EXPECTED_POSITION );

        title   = cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.TITLE ) );
        artist  = cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST ) );
        album   = cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM ) );

        cursor.close();

        assertEquals( EXPECTED_TITLE, title );
        assertEquals( EXPECTED_ARTIST, artist );
        assertEquals( EXPECTED_ALBUM, album );

    }

    @Test
    public void testGetDisplayName()
    {
        String displayName;

        displayName = Utils.getDisplayName( TEST_SOURCE, TEST_PARAMETER );

        assertEquals( EXPECTED_DISPLAY_NAME, displayName );
    }

    @Test
    public void testToastShort() throws InterruptedException
    {

        signal = new CountDownLatch( 1 );


        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                Utils.toastShort( TEST_STRING );

                signal.countDown();
            }
        } );

        signal.await();

        TestUtils.isToastMessageDisplayed( TEST_STRING );
    }

    @Test
    public void testToastLong() throws InterruptedException
    {

        signal = new CountDownLatch( 1 );


        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                Utils.toastLong( TEST_STRING );

                signal.countDown();
            }
        } );

        signal.await();

        TestUtils.isToastMessageDisplayed( TEST_STRING );
    }


    @Test
    public void testCreateParentString()
    {
        String parentString;

        parentString = Utils.createParentString( TEST_SOURCE, TEST_PARAMETER );

        assertEquals( EXPECTED_PARENT_STRING, parentString );
    }

    @Test
    public void testSplitParentString()
    {
        String [] split;

        split = Utils.splitParentString( EXPECTED_PARENT_STRING );

        assertEquals( TEST_SOURCE, split[0] );
        assertEquals( TEST_PARAMETER, split[1] );
    }

    @Test
    public void testGetByIndex()
    {
        TreeMap<String, String> treeMap;
        String string;

        treeMap = new TreeMap<>(  );

        treeMap.put( "1", "First" );
        treeMap.put( "2", "Second" );
        treeMap.put( "3", "Third" );

        string = ( String )Utils.getByIndex( treeMap, 1 );

        assertEquals( "Second", string );

    }

    @Test
    public void testAlphabetSort()
    {
        String [] strArray = new String[] { "B", "A", "C" };

        Arrays.sort(strArray, new Utils.alphabetSort() );

        assertArrayEquals( new String[] {"A", "B", "C"}, strArray );

    }



    //NOTE - this doesn't work
    /*@Test
    public void testShowMessageOKCancel() throws InterruptedException
    {


        signal = new CountDownLatch( 1 );


        InstrumentationRegistry.getInstrumentation().runOnMainSync( new Runnable()
        {
            @Override
            public void run()
            {
                Utils.showMessageOKCancel( InstrumentationRegistry.getTargetContext(), TEST_STRING, null, null );

                signal.countDown();
            }
        } );

        signal.await();

        onView(withText(TEST_STRING)).check(matches(isDisplayed()));
    }*/

    @Test
    public void testIsSongs_positive()
    {
        boolean isSongs;

        isSongs = Utils.isSongs( TEST_SOURCE, TEST_PARAMETER );

        assertTrue( isSongs );
    }

    @Test
    public void testIsSongs_negative()
    {
        boolean isSongs;

        isSongs = Utils.isSongs( TEST_SOURCE, null );

        assertFalse( isSongs );
    }



}
