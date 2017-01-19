package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ContentResolver;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Miroslav on 16.1.2017..
 */

public class MusicProvider {
    private final String TAG = getClass().getSimpleName();

    //private List<MediaMetadataCompat> mMusicMetadataList;

    private Map<DoubleKey,List<MediaMetadataCompat>> mMusicMetadataMap;

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
        mMusicMetadataMap = new HashMap<>();
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

    public List<MediaMetadataCompat> loadMedia(String source, String parameter)
    {
        Bundle bundle;
        MediaMetadataCompat mediaMetadata;
        MediaMetadataCompat.Builder metadataBuilder;
        String mediaUriStr;
        List<MediaMetadataCompat> mediaMetadataList;
        DoubleKey doubleKey = new DoubleKey(source, parameter);

        if (mMusicMetadataMap.containsKey(doubleKey))
        {
            mediaMetadataList = mMusicMetadataMap.get(doubleKey);

            if (mediaMetadataList != null)
                return mediaMetadataList;
        }

        if (parameter == null)
            bundle = ScreenBundles.getBundleForMainScreen(source);
        else
            bundle = ScreenBundles.getBundleForSubScreen(source, parameter);

        if (bundle == null)
            return null;


        Cursor cursor = Utils.queryMedia(bundle);

        if (cursor == null)
        {
            Log.w(TAG,"Cursor is null");
            return null;
        }

        mediaMetadataList = new ArrayList<>(cursor.getCount());

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

                mediaMetadataList.add(mediaMetadata);
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

                mediaMetadataList.add(mediaMetadata);

            }
        }


        cursor.close();



        mMusicMetadataMap.put(doubleKey, mediaMetadataList);

        return mediaMetadataList;

    }

    public void invalidateAllData()
    {
        mMusicMetadataMap = new HashMap<>();
    }

    public void invalidateDataAndNotify(String source, String parameter)
    {
        Bundle bundle = new Bundle();
        bundle.putString(ScreenBundles.PARAMETER_KEY,parameter);

        mMusicMetadataMap.remove(new DoubleKey(source, parameter));
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
