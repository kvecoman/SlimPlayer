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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Miroslav on 16.1.2017..
 *
 * Singleton class that loads
 */

public class MusicProvider {
    private final String TAG = getClass().getSimpleName();



    //private List<MediaMetadataCompat> mMusicMetadataList;

    private Map<DoubleKey,List<MediaBrowserCompat.MediaItem>> mMusicItemsMap;

    //private Map<String, String> mCursorMetadataKeys;

    private State mState = State.NOT_READY;

    private MediaBrowserServiceCompat mListener;

    public enum State {
        NOT_READY, LOADING, READY;
    }

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
        mMusicItemsMap = new HashMap<>();
        /*mCursorMetadataKeys = new HashMap<>();
        buildCursorMetadataKeys();*/
    }

    /*private void buildCursorMetadataKeys()
    {
        mCursorMetadataKeys.put(MediaStore.Audio.Media._ID, MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        mCursorMetadataKeys.put(MediaStore.Audio.Playlists._ID, MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        mCursorMetadataKeys.put(MediaStore.Audio.Genres._ID, MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        mCursorMetadataKeys.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        mCursorMetadataKeys.put(MediaStore.Audio.Playlists.Members.TITLE, MediaMetadataCompat.METADATA_KEY_TITLE);
        mCursorMetadataKeys.put(MediaStore.Audio.Playlists.Members.ARTIST, MediaMetadataCompat.METADATA_KEY_ARTIST);
        mCursorMetadataKeys.put(MediaStore.Audio.Playlists.Members.ALBUM, MediaMetadataCompat.METADATA_KEY_ALBUM);
        mCursorMetadataKeys.put(MediaStore.Audio.Genres.NAME, MediaMetadataCompat.METADATA_KEY_GENRE);

    }*/


    /*public void loadMedia()
    {
        Log.v(TAG,"loadMedia()");
        mState = State.LOADING;

        mMusicMetadataList = new ArrayList<>();
        MediaBrowserCompat.MediaItem item;
        MediaDescriptionCompat description;
        MediaMetadataCompat metadata;
        String mediaUriStr;
        SlimMetadata slimMetadata;
        String albumId;
        String artistId;

        ContentResolver contentResolver = SlimPlayerApplication.getInstance().getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String [] projection = new String [] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" + ScreenBundles.addDirectoryCheckSQL() + ")";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = contentResolver.query(uri,projection,selection,null,sortOrder);

        if (cursor == null)
        {
            Log.w(TAG,"Cursor is null");
            return;
        }

        while (cursor.moveToNext())
        {
            mediaUriStr = Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))).toString();

            metadata = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)))
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)))
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)))
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)))
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)))
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,mediaUriStr)
                        .build();

            albumId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            artistId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));

            slimMetadata = new SlimMetadata(metadata,albumId,artistId);

            mMusicMetadataList.add(slimMetadata);
        }

        cursor.close();

        mState = State.READY;
    }*/

    public List<MediaBrowserCompat.MediaItem> loadMedia(String source, String parameter)
    {
        Bundle                              cursorBundle;
        MediaMetadataCompat                 mediaMetadata;
        MediaMetadataCompat.Builder         metadataBuilder;
        String                              mediaUriStr;
        MediaBrowserCompat.MediaItem        mediaItem;
        List<MediaBrowserCompat.MediaItem>  mediaItemsList;
        DoubleKey                           doubleKey;

        doubleKey = new DoubleKey(source, parameter);

        //Return existing list if we have it cached
        if (mMusicItemsMap.containsKey(doubleKey))
        {
            mediaItemsList = mMusicItemsMap.get(doubleKey);

            if (mediaItemsList != null)
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

        mediaItemsList = new ArrayList<>(cursor.getCount());

        //Check whether we load songs or categories and build metadata for it from cursor
        if (source.equals(Const.ALL_SCREEN) || parameter != null)
        {
            //If we are loading songs
            while (cursor.moveToNext())
            {
                metadataBuilder = new MediaMetadataCompat.Builder();

                mediaUriStr = Uri.parse(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))).toString();

                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,cursor.getString(0))
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,cursor.getString(1))
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)))
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)))
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)))
                                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,mediaUriStr);

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
        mMusicItemsMap.put(doubleKey, mediaItemsList);

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

    //Discard all cached lists so that we load fresh data when media service calls
    public void invalidateAllData()
    {
        mMusicItemsMap = new HashMap<>();
    }

    //Discard only one list and notify media service to make a call to load list again
    public void invalidateDataAndNotify(String source, String parameter)
    {
        Bundle bundle = new Bundle();
        bundle.putString( Const.PARAMETER_KEY,parameter);

        mMusicItemsMap.remove(new DoubleKey(source, parameter));
        if (mListener != null)
            mListener.notifyChildrenChanged(source, bundle);
    }

    public void registerDataListener(MediaBrowserServiceCompat service)
    {
        mListener = service;
    }

    public void unregisterDataListener()
    {
        mListener = null;
    }

    /*public Set<SlimMetadata> getMusicMetadata() {

        if (mState != State.READY)
            return null;

        return mMusicMetadataList;
    }*/
}
