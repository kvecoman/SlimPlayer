package mihaljevic.miroslav.foundry.slimplayer;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.*;
import static mihaljevic.miroslav.foundry.slimplayer.TestData.*;

/**
 * Created by miroslav on 06.03.17..
 */

@RunWith(AndroidJUnit4.class)
public class MusicProviderTest
{



    private static MusicProvider musicProvider;

    @BeforeClass
    public static void beforeClass()
    {
        musicProvider = MusicProvider.getInstance();
    }

    @Test
    public void testLoadMedia_BasicPositive()
    {
        List<MediaBrowserCompat.MediaItem>  mediaItemList;

        mediaItemList = musicProvider.loadMedia( TEST_SOURCE, TEST_PARAMETER );

        assertEquals( 530, mediaItemList.size() );

    }

    @Test
    public void testMediaFromFile_BasicPositive()
    {
        Uri                             fileUri;
        MediaBrowserCompat.MediaItem    mediaItem;

        fileUri = Uri.fromFile( new File( TEST_MUSIC_PATH ) );


        mediaItem = musicProvider.mediaFromFile( fileUri.toString() );

        assertEquals( true, mediaItem.isPlayable() );
        assertEquals( EXPECTED_TITLE, mediaItem.getDescription().getTitle() );
    }

    @Test
    public void testGetMetadata_positiveUsingLoadMedia()
    {
        List<MediaBrowserCompat.MediaItem>  mediaItemList;
        MediaMetadataCompat                 metadata;
        String                              title;

        mediaItemList = musicProvider.loadMedia( TEST_SOURCE, TEST_PARAMETER );

        //NOTE - this part also tests metadata getting capacity, not good to have multiple stuff in single test
        for ( MediaBrowserCompat.MediaItem mediaItem : mediaItemList )
        {
            title = ( String )mediaItem.getDescription().getTitle();

            if ( TextUtils.equals( title, EXPECTED_TITLE ) )
            {
                metadata = musicProvider.getMetadata( mediaItem.getMediaId() );

                assertEquals( EXPECTED_TITLE, metadata.getString( MediaMetadataCompat.METADATA_KEY_TITLE ) );
                assertEquals( EXPECTED_ARTIST, metadata.getString( MediaMetadataCompat.METADATA_KEY_ARTIST ) );
                assertEquals( EXPECTED_ALBUM, metadata.getString( MediaMetadataCompat.METADATA_KEY_ALBUM ) );

                return;
            }
        }

        fail();
    }

    @Test
    public void testGetMetadata_positiveUsingmMediaFromFile()
    {
        Uri                             fileUri;
        MediaBrowserCompat.MediaItem    mediaItem;
        MediaMetadataCompat             metadata;


        fileUri = Uri.fromFile( new File( TEST_MUSIC_PATH ) );


        mediaItem = musicProvider.mediaFromFile( fileUri.toString() );

        metadata = musicProvider.getMetadata( mediaItem.getMediaId() );

        assertEquals( mediaItem.getDescription().getTitle(), metadata.getString( MediaMetadataCompat.METADATA_KEY_TITLE ) );

        assertEquals( EXPECTED_TITLE, metadata.getString( MediaMetadataCompat.METADATA_KEY_TITLE ) );
        assertEquals( EXPECTED_ARTIST, metadata.getString( MediaMetadataCompat.METADATA_KEY_ARTIST ) );
        assertEquals( EXPECTED_ALBUM, metadata.getString( MediaMetadataCompat.METADATA_KEY_ALBUM ) );
    }

    @Test
    public void testInvalidateAllData()
    {
        Uri                             fileUri;
        MediaBrowserCompat.MediaItem    mediaItem;
        MediaMetadataCompat             metadata;

        musicProvider.loadMedia( TEST_SOURCE, TEST_PARAMETER );

        fileUri = Uri.fromFile( new File( TEST_MUSIC_PATH ) );


        mediaItem = musicProvider.mediaFromFile( fileUri.toString() );

        musicProvider.invalidateAllData();

        metadata = musicProvider.getMetadata( mediaItem.getMediaId() );

        assertNull( metadata );



    }
}
