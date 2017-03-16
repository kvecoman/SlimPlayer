package mihaljevic.miroslav.foundry.slimplayer;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Created by Miroslav on 16.1.2017..
 *
 * Singleton class that loads music metadata items, thread safe
 *
 * Heavily inspired by android music service sample
 *
 * @author Miroslav Mihaljević
 */

public class MusicProvider {
    private final String TAG = getClass().getSimpleName();

    private static final int CACHED_ITEMS_CAPACITY = 7;
    private static final int CACHE_CLEAR_INTERVAL = 2; //Cache clear interval in minutes


    private IntervalLRUCache<String, List<MediaMetadataCompat>> mMediaListsCache;

    //Its not really a cache, but if list from music items cache is deleted then metadatas can also be garbage collected
    private WeakMap<String, MediaMetadataCompat> mMusicMetadataCache;




    private MediaBrowserServiceCompat mListener;


    private static MusicProvider sInstance;

    public synchronized static MusicProvider getInstance()
    {
        if ( sInstance == null )
        {
            sInstance = new MusicProvider();
        }

        return sInstance;
    }


    private MusicProvider()
    {
        mMediaListsCache    = new IntervalLRUCache<>(CACHED_ITEMS_CAPACITY, CACHE_CLEAR_INTERVAL);
        mMusicMetadataCache = new WeakMap<>(  );

    }

    public List<MediaBrowserCompat.MediaItem> loadMedia( String source, String parameter )
    {
        List<MediaMetadataCompat>           metadataList;
        List<MediaBrowserCompat.MediaItem>  mediaItems;

        metadataList    = loadMetadataList( source, parameter );
        mediaItems      = mediaItemsFromMetadata( metadataList, Utils.isSongs( source, parameter ) ?  MediaBrowserCompat.MediaItem.FLAG_PLAYABLE :  MediaBrowserCompat.MediaItem.FLAG_BROWSABLE );

        return mediaItems;
    }


    public List<MediaBrowserCompat.MediaItem> mediaItemsFromMetadata( List<MediaMetadataCompat> metadataList, @MediaBrowserCompat.MediaItem.Flags int flag )
    {

        MediaBrowserCompat.MediaItem        mediaItem;
        List<MediaBrowserCompat.MediaItem>  mediaItemsList;

        //Something wrong went with loading metadata, return null
        if ( metadataList == null )
            return null;


        mediaItemsList = new ArrayList<>( metadataList.size() );


        for (MediaMetadataCompat metadata : metadataList)
        {

            //Get media item
            mediaItem = new MediaBrowserCompat.MediaItem( metadata.getDescription(), flag );

            mediaItemsList.add( mediaItem );
        }


        return mediaItemsList;
    }


    public synchronized List<MediaMetadataCompat> loadMetadataList(String source, String parameter)
    {
        Bundle                              cursorBundle;
        Cursor                              cursor;
        List<MediaMetadataCompat>           metadataList;
        MediaMetadataCompat                 mediaMetadata;
        MediaMetadataCompat.Builder         metadataBuilder;
        Uri                                 mediaUri;
        String                              mediaUriStr;
        String                              mediaID;
        boolean                             tryCache;
        String                              parentKey;

        if ( Build.VERSION.SDK_INT >= 16 &&  !Utils.checkPermission(  android.Manifest.permission.READ_EXTERNAL_STORAGE ) )
            return null;


        parentKey = Utils.createParentString( source, parameter );

        //Try to retrieve cached list if it exists
        metadataList = mMediaListsCache.get( parentKey );

        //If we have anything cached then return it
        if ( metadataList != null )
            return metadataList;


        //Retrieve appropriate cursorBundle for cursor
        if (parameter == null)
            cursorBundle = ScreenBundles.getBundleForMainScreen(source);
        else
            cursorBundle = ScreenBundles.getBundleForSubScreen(source, parameter);

        if (cursorBundle == null)
            return null;

        cursor = Utils.queryMedia( cursorBundle );

        if ( cursor == null )
            return null;

        metadataList = new ArrayList<>( cursor.getCount() );

        //Here we try to predict is there even a point in looking at cache for metadata
        if ( cursor.getCount() > mMusicMetadataCache.size() )
            tryCache = false;
        else
            tryCache = true;

        if ( Utils.isSongs( source, parameter ) )
        {

            //If we are loading songs
            while ( cursor.moveToNext() )
            {
                mediaID = cursor.getString( 0 );

                //Here we try to check if we have metadata cached
                if ( tryCache )
                {
                    mediaMetadata = mMusicMetadataCache.get( mediaID );

                    if (mediaMetadata != null)
                    {
                        //We have a hit, no need to read cursor, so we just continue
                        metadataList.add( mediaMetadata );
                        continue;
                    }
                }


                //We don't have hit from cache, so we have to load metadata from cursor
                metadataBuilder = new MediaMetadataCompat.Builder();


                mediaUri    = Uri.parse( cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.DATA ) ) );
                mediaUriStr = mediaUri.toString();

                metadataBuilder
                        .putString  ( MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaID )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_TITLE, cursor.getString( 1 ) )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_ALBUM, cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM ) ) )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_ARTIST, cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST ) ) )
                        .putLong    ( MediaMetadataCompat.METADATA_KEY_DURATION, cursor.getLong( cursor.getColumnIndex( MediaStore.Audio.Media.DURATION ) ) )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUriStr );

                mediaMetadata = metadataBuilder.build();

                metadataList.add( mediaMetadata );

                mMusicMetadataCache.put( mediaID, mediaMetadata );
            }

        }
        else
        {
            //We are loading categories
            while (cursor.moveToNext())
            {
                mediaID = cursor.getString( 0 );

                metadataBuilder = new MediaMetadataCompat.Builder();

                metadataBuilder .putString( MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaID )
                                .putString( MediaMetadataCompat.METADATA_KEY_TITLE, cursor.getString(1) );

                mediaMetadata = metadataBuilder.build();

                metadataList.add( mediaMetadata );
            }

        }

        cursor.close();

        //Cache this list for later retrieval
        mMediaListsCache.put( parentKey, metadataList );

        return metadataList;

    }


    public synchronized MediaBrowserCompat.MediaItem mediaFromFile(String fileUriString)
    {
        MediaMetadataCompat             metadata;
        MediaMetadataCompat.Builder     metadataBuilder;
        MediaMetadataRetriever          retriever;
        Uri                             fileUri;
        MediaBrowserCompat.MediaItem    mediaItem;
        List<MediaMetadataCompat>       mediaMetadataList;

        if ( Build.VERSION.SDK_INT >= 16 && !Utils.checkPermission( android.Manifest.permission.READ_EXTERNAL_STORAGE ))
            return null;

        metadataBuilder     = new MediaMetadataCompat.Builder(  );
        retriever           = new MediaMetadataRetriever();
        fileUri             = Uri.parse( fileUriString );
        mediaMetadataList   = new ArrayList<>( 1 );


        if ( fileUri == null )
            return null;

        retriever.setDataSource( fileUri.getPath() );

        metadataBuilder .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, fileUriString)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_TITLE ))
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ALBUM ))
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ARTIST ))
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION )))
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, fileUriString);


        metadata = metadataBuilder.build();

        mediaMetadataList.add( metadata );

        mediaItem = new MediaBrowserCompat.MediaItem(metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);


        mMediaListsCache.put    ( mediaItem.getMediaId(), mediaMetadataList );
        mMusicMetadataCache.put ( mediaItem.getMediaId(), metadata );

        return mediaItem;
    }


    public synchronized MediaMetadataCompat getMetadata(String mediaID)
    {
        return mMusicMetadataCache.get( mediaID );
    }




    public synchronized void invalidateAllData()
    {
        mMediaListsCache.removeAll();
        mMusicMetadataCache.clear();
    }

    //Discard only one list and notify media service to make a call to load list again
    public synchronized void invalidateDataAndNotify(String source, String parameter)
    {
        String parentString;


        parentString = Utils.createParentString( source, parameter );

        mMediaListsCache.remove( parentString );

        synchronized ( this )
        {
            if ( mListener != null )
                mListener.notifyChildrenChanged( parentString );
        }
    }

    public synchronized void registerDataListener(MediaBrowserServiceCompat service)
    {
        mListener = service;
    }

    public synchronized void unregisterDataListener()
    {
        mListener = null;
    }



    //DEBUG function - it counts number of weak references with null value
    /*public int countNullReferences()
    {
        int timesProcessOutputBufferCalled;

        timesProcessOutputBufferCalled = 0;

        for (WeakReference<MediaMetadataCompat> ref : mMusicMetadataCache.values())
        {
            if (ref.get() == null)
                timesProcessOutputBufferCalled++;
        }

        return timesProcessOutputBufferCalled;
    }*/


}
