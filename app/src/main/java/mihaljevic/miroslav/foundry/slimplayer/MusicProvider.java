package mihaljevic.miroslav.foundry.slimplayer;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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


    private LRUCache<String, List<MediaBrowserCompat.MediaItem>> mMusicItemsCache;


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


    private MusicProvider(){
        mMusicItemsCache = new LRUCache<>(CACHED_ITEMS_CAPACITY);
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

        //doubleKey = new DoubleKey(source, parameter);
        parentKey = Utils.createParentString( source, parameter );

        //Try to retrieve cached list if it exists
        mediaItemsList = mMusicItemsCache.get(parentKey);

        //Return existing list if we have it cached
        if (mediaItemsList != null)
            return mediaItemsList;


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

        mediaItemsList = new ArrayList<>(cursor.getCount());

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
                mediaItem = bundleMetadata( mediaMetadata );


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

                mediaItemsList.add(mediaItem);

            }
        }


        cursor.close();

        //Cache this list for later retrieval
        mMusicItemsCache.put(parentKey, mediaItemsList);

        return mediaItemsList;

    }

    public MediaBrowserCompat.MediaItem bundleMetadata(MediaMetadataCompat metadata)
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
    }


    public void invalidateAllData()
    {
        mMusicItemsCache.removeAll();
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


}
