package mihaljevic.miroslav.foundry.slimplayer;

import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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


    private LRUCache<String, List<MediaHolder>> mMusicItemsCache;

    //Its not really a cache, but if list from music items cache is deleted then metadatas can also be garbage collected
    //private Map<String, WeakReference<MediaMetadataCompat>> mMetadataCache;

    //private WeakHashMap<MediaDescriptionCompat, MediaMetadataCompat> mMetadataCache2;

    private WeakMap<String, MediaMetadataCompat> mMetadataCache3;


    private MediaBrowserServiceCompat mListener;


    private static MusicProvider sInstance;

    public static MusicProvider getInstance()
    {
        if (sInstance == null)
        {
            sInstance = new MusicProvider();
        }

        return sInstance;
    }


    private MusicProvider()
    {
        mMusicItemsCache = new LRUCache<>(CACHED_ITEMS_CAPACITY);
        //mMetadataCache = new HashMap<>(  );
        //mMetadataCache2 = new WeakHashMap<>(  );
        mMetadataCache3 = new WeakMap<>(  );
    }



    public List<MediaBrowserCompat.MediaItem> loadMedia(String source, String parameter)
    {
        Bundle                              cursorBundle;
        MediaMetadataCompat                 mediaMetadata;
        MediaMetadataCompat.Builder         metadataBuilder;
        Uri                                 mediaUri;
        String                              mediaUriStr;
        MediaBrowserCompat.MediaItem        mediaItem;
        List<MediaBrowserCompat.MediaItem>  mediaItemsList;
        String                              parentKey;
        MediaHolder                         mediaHolder;
        List<MediaHolder>                   mediaHolderList;

        //doubleKey = new DoubleKey(source, parameter);
        parentKey = Utils.createParentString( source, parameter );

        //Try to retrieve cached list if it exists
        mediaItemsList = itemsFromHolders( mMusicItemsCache.get(parentKey) );

        //Return existing list if we have it cached
        if (mediaItemsList != null)
        {
            int x = 1;
            return mediaItemsList;
        }


        //Retrieve appropriate cursorBundle for cursor
        if (parameter == null)
            cursorBundle = ScreenBundles.getBundleForMainScreen(source);
        else
            cursorBundle = ScreenBundles.getBundleForSubScreen(source, parameter);

        if (cursorBundle == null)
            return null;

        //Load cursor using parameters from cursorBundle
        Cursor cursor = Utils.queryMedia(cursorBundle);

        if (cursor == null)
        {
            Log.w(TAG,"Cursor is null");
            return null;
        }

        mediaItemsList = new ArrayList<>( cursor.getCount() );
        mediaHolderList = new ArrayList<>( cursor.getCount() );

        //Check whether we load songs or categories and build metadata for it from cursor
        if (source.equals(Const.ALL_SCREEN) || parameter != null)
        {
            //If we are loading songs
            while (cursor.moveToNext())
            {
                metadataBuilder = new MediaMetadataCompat.Builder();


                mediaUri = Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                mediaUriStr = mediaUri.toString();

                metadataBuilder
                        .putString  ( MediaMetadataCompat.METADATA_KEY_MEDIA_ID,    cursor.getString( 0 ) )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_TITLE,       cursor.getString( 1 ) )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_ALBUM,       cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ALBUM ) ) )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_ARTIST,      cursor.getString( cursor.getColumnIndex( MediaStore.Audio.Media.ARTIST ) ) )
                        .putLong    ( MediaMetadataCompat.METADATA_KEY_DURATION,    cursor.getLong( cursor.getColumnIndex( MediaStore.Audio.Media.DURATION ) ) )
                        .putString  ( MediaMetadataCompat.METADATA_KEY_MEDIA_URI,   mediaUriStr );

                mediaMetadata = metadataBuilder.build();



                //Get media item with description that has bundled media metadata object in it
                mediaItem = new MediaBrowserCompat.MediaItem( mediaMetadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE ) /*bundleMetadata( mediaMetadata )*/;

                mediaHolder = new MediaHolder( mediaItem, mediaMetadata );

                //mMetadataCache.put( mediaItem.getMediaId(), new WeakReference<>( mediaMetadata ) );
                //mMetadataCache2.put( mediaItem.getDescription(), mediaMetadata );
                mMetadataCache3.put( mediaItem.getMediaId(), mediaMetadata );
                //TODO - continue here - what can you put as key here that isn't referenced by metadata (or create your own collection with weak values)

                mediaHolderList.add( mediaHolder );

                mediaItemsList.add(mediaItem);
            }
        }
        else
        {
            //We are loading categories
            while (cursor.moveToNext())
            {
                metadataBuilder = new MediaMetadataCompat.Builder();

                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,cursor.getString(0))
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE,cursor.getString(1));

                mediaMetadata = metadataBuilder.build();

                mediaItem = new MediaBrowserCompat.MediaItem(mediaMetadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);

                mediaHolder = new MediaHolder( mediaItem, mediaMetadata );

                //mMetadataCache.put( mediaItem.getMediaId(), new WeakReference<>( mediaMetadata ) );
                //mMetadataCache2.put( mediaItem.getDescription(), mediaMetadata );
                mMetadataCache3.put( mediaItem.getMediaId(), mediaMetadata );

                mediaHolderList.add( mediaHolder );

                mediaItemsList.add(mediaItem);

            }
        }


        cursor.close();

        //mediaItemsList = itemsFromHolders( mediaHolderList );

        //Cache this list for later retrieval
        mMusicItemsCache.put(parentKey, mediaHolderList);

        return mediaItemsList;

    }

    /*public MediaBrowserCompat.MediaItem bundleMetadata(MediaMetadataCompat metadata)
    {
        MediaBrowserCompat.MediaItem        mediaItem;
        Bundle                              descriptionBundle;
        MediaDescriptionCompat              mediaDescription;
        MediaDescriptionCompat.Builder      descriptionBuilder;

        //We use media description's cursorBundle as means to carry around mediaMetadata
        descriptionBundle = new Bundle();
        descriptionBundle.putParcelable( Const.METADATA_KEY, metadata  );


        descriptionBuilder = new MediaDescriptionCompat.Builder();
        descriptionBuilder  .setMediaId  ( metadata.getString( MediaMetadataCompat.METADATA_KEY_MEDIA_ID ) )
                            .setTitle   ( metadata.getString( MediaMetadataCompat.METADATA_KEY_TITLE ) )
                            .setMediaUri( Uri.parse( metadata.getString( MediaMetadataCompat.METADATA_KEY_MEDIA_URI ) ) )
                            .setExtras  ( descriptionBundle );

        mediaDescription = descriptionBuilder.build();

        mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

        return mediaItem;
    }*/

    public synchronized MediaBrowserCompat.MediaItem mediaFromFile(String fileUriString)
    {
        MediaMetadataCompat metadata;
        MediaMetadataCompat.Builder metadataBuilder;
        MediaMetadataRetriever retriever;
        Uri fileUri;
        MediaBrowserCompat.MediaItem mediaItem;
        List<MediaHolder> mediaHolderList;
        MediaHolder mediaHolder;

        metadataBuilder = new MediaMetadataCompat.Builder(  );
        retriever = new MediaMetadataRetriever();
        fileUri = Uri.parse( fileUriString );
        mediaHolderList = new ArrayList<>( 1 );


        if (fileUri == null)
            return null;

        retriever.setDataSource( fileUri.getPath() );

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, fileUriString)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_TITLE ))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ALBUM ))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ARTIST ))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION )))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, fileUriString);


        metadata = metadataBuilder.build();

        //Get media item with metadata bundled in its media description object
        //mediaItem = MusicProvider.getInstance().bundleMetadata( metadata );

        mediaItem = new MediaBrowserCompat.MediaItem(metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

        mediaHolder = new MediaHolder( mediaItem, metadata );

        mediaHolderList.add( mediaHolder );

        mMusicItemsCache.put( mediaItem.getMediaId(), mediaHolderList );

        //mMetadataCache.put( mediaItem.getMediaId(), new WeakReference<>( metadata ) );
        //mMetadataCache2.put( mediaItem.getDescription(), metadata );
        mMetadataCache3.put( mediaItem.getMediaId(), metadata );


        return mediaItem;
    }

    //TODO - change parameter back to mediaID
    public MediaMetadataCompat getMetadata(MediaDescriptionCompat description)
    {
        MediaMetadataCompat metadata;
        WeakReference<MediaMetadataCompat> weakReference;

        /*weakReference = mMetadataCache.get( mediaId );

        if (weakReference == null)
            return null;

        metadata = weakReference.get();

        if (metadata == null)
            mMetadataCache.remove( mediaId );*/

        //metadata = mMetadataCache2.get( description );

        metadata = mMetadataCache3.get( description.getMediaId() );

        return metadata;
    }

    private List<MediaBrowserCompat.MediaItem> itemsFromHolders(List<MediaHolder> mediaHolderList)
    {
        if (mediaHolderList == null || mediaHolderList.size() == 0)
            return null;

        List<MediaBrowserCompat.MediaItem> itemsList;

        itemsList = new ArrayList<>( mediaHolderList.size() );

        for (MediaHolder holder : mediaHolderList)
        {
            itemsList.add( holder.mediaItem );
        }

        return itemsList;
    }


    public synchronized void invalidateAllData()
    {
        mMusicItemsCache.removeAll();
        //mMetadataCache.clear();
        //mMetadataCache2.clear();
        mMetadataCache3.clear();
    }

    //Discard only one list and notify media service to make a call to load list again
    public void invalidateDataAndNotify(String source, String parameter)
    {
        String parentString;


        parentString = Utils.createParentString( source, parameter );

        mMusicItemsCache.remove(parentString);

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

    private class MediaHolder
    {
        MediaBrowserCompat.MediaItem mediaItem;

        MediaMetadataCompat metadata;

        public MediaHolder( MediaBrowserCompat.MediaItem mediaItem, MediaMetadataCompat metadata )
        {
            this.mediaItem = mediaItem;
            this.metadata = metadata;
        }
    }


    //DEBUG function - it counts number of weak references with null value
    /*public int countNullReferences()
    {
        int count;

        count = 0;

        for (WeakReference<MediaMetadataCompat> ref : mMetadataCache3.values())
        {
            if (ref.get() == null)
                count++;
        }

        return count;
    }*/


}
