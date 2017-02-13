package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Miroslav on 23.11.2016..
 *
 * Static class, contains utility functions used in other components
 *
 * @author Miroslav Mihaljević
 */
public final class Utils {

    protected static final String TAG = Utils.class.getSimpleName();

    private static SlimPlayerApplication sAppContext = SlimPlayerApplication.getInstance();

    private Utils(){}



    //Helper function to set and calculate height of list view (assuming all rows are same)
    public static int calculateListHeight(ListView lv)
    {
        int height;
        ListAdapter adapter = lv.getAdapter();
        int count = adapter.getCount();

        if (adapter.getCount() <= 0)
            return 0;

        View item =  adapter.getView(0,null,lv);

        item.measure(0,0);

        height = item.getMeasuredHeight() * count;
        height += lv.getDividerHeight() * (count - 1);

        return height;
    }

    //Check if AUDIO_ID already exists in playlist
    public static boolean playlistCheckForDuplicate(Cursor playlistCursor, long id)
    {
        if (playlistCursor == null)
            return false;


        for (int i = 0; i < playlistCursor.getCount(); i++)
        {
            playlistCursor.moveToPosition(i);

            if (playlistCursor.getLong(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)) == id)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean checkIfPlaylistExist(String playlistName)
    {
        //Check if playlist with that name already exists
        Cursor playlistsCursor = sAppContext.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                                        new String [] {MediaStore.Audio.Playlists._ID,MediaStore.Audio.Playlists.NAME},null,null,null);
        for (int i = 0;i < playlistsCursor.getCount();i++)
        {
            playlistsCursor.moveToPosition(i);
            if (playlistName.equals(playlistsCursor.getString(playlistsCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))))
            {
                playlistsCursor.close();
                return true;
            }
        }
        playlistsCursor.close();
        return false;
    }

    //Creates playlist and returns id
    public static long createPlaylist( String playlistName)
    {
        //Create content values with new playlist info
        ContentValues inserts = new ContentValues();
        inserts.put(MediaStore.Audio.Playlists.NAME, playlistName);
        inserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        inserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        //Insert new playlist values
        Uri uri = sAppContext.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,inserts);

        //Get ID of newly created playlist
        Cursor c = sAppContext.getContentResolver().query(uri, new String[]{MediaStore.Audio.Playlists._ID},null,null,null);
        c.moveToFirst();
        long pId = c.getLong(0);
        c.close();

        return pId;
    }

    //Returns number of inserted items
    public static int insertIntoPlaylist( List<String> IDs, long playlistId)
    {
        //long [] IDs = data.getLongArrayExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY);
        //long playlistId = ;
        Uri playlistUri = MediaStore.Audio.Playlists.Members.getContentUri("external",playlistId);
        List<ContentValues> valuesList = new ArrayList<>();

        Cursor playlistCursor = sAppContext.getContentResolver().query(playlistUri,new String[]{MediaStore.Audio.Playlists.Members._ID, MediaStore.Audio.Playlists.Members.AUDIO_ID},null,null,null);
        int songCount = playlistCursor.getCount();

        ContentValues values;

        long id;
        for(String audio_id : IDs)
        {
            id = Long.parseLong(audio_id);
            if (!Utils.playlistCheckForDuplicate(playlistCursor,id)) {

                values = new ContentValues();
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id);
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, valuesList.size() + songCount);
                valuesList.add(values);
            }
        }

        ContentResolver resolver = sAppContext.getContentResolver();
        ContentValues[] valuesArray = new ContentValues[valuesList.size()];
        valuesList.toArray(valuesArray);
        int result = resolver.bulkInsert(playlistUri, valuesArray);
        resolver.notifyChange(playlistUri,null);

        return result;

    }

    public static int deleteFromList(List<MediaBrowserCompat.MediaItem> mediaItems, Uri uri, SparseBooleanArray checkedPositions, String id_field)
    {
        int position;
        String id;
        int deletedCount = 0;

        if (mediaItems == null)
            return  0;

        for (int i = 0;i < checkedPositions.size();i++)
        {
            position = checkedPositions.keyAt(i);

            if (position >= 0 && position < mediaItems.size() && checkedPositions.get(position))
            {
                id = mediaItems.get(position).getMediaId();
                deletedCount += sAppContext.getContentResolver().delete(  uri,
                        id_field + "=?",
                        new String  [] {id});
            }

        }

        return deletedCount;

    }

    //Bundle contains data with keys from ScreenBundles
    public static Cursor queryMedia(Bundle args)
    {
        Log.v(TAG,"queryMedia()");

        if (args == null)
            return null;

        //Create query that will fetch songs that we need for this screen
        Uri uri = Uri.parse(args.getString( Const.URI_KEY));
        String [] projection = args.getStringArray( Const.PROJECTION_KEY);
        String selection = args.getString( Const.SELECTION_KEY);
        String [] selectionArgs = args.getStringArray( Const.SELECTION_ARGS_KEY);
        String sortOrder = args.getString( Const.SORT_ORDER_KEY);

        return sAppContext.getContentResolver().query(uri,projection,selection,selectionArgs,sortOrder);
    }

    //Method that detects empty genres and stores that list into preferences
    public static int deleteEmptyGenres()
    {
        ContentResolver resolver = sAppContext.getContentResolver();
        int count = 0;
        Cursor genresCursor = resolver.query( MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                new String[]{ MediaStore.Audio.Genres._ID }, null, null, null );

        if ( genresCursor == null )
            return 0;

        int id;
        Cursor cursor;
        genresCursor.moveToFirst();
        do
        {
            id = genresCursor.getInt( 0 );
            cursor = resolver.query( MediaStore.Audio.Genres.Members.getContentUri( "external", id ),
                    new String[]{ MediaStore.Audio.Genres.Members._ID }, null, null, null );
            if ( cursor.getCount() == 0 )
            {
                //Here we delete if genre is empty
                resolver.delete( MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, MediaStore.Audio.Genres._ID + "=" + id, null );
                count++;
            }
            cursor.close();
        } while ( genresCursor.moveToNext() );

        genresCursor.close();

        return count;

    }



    //Checks if two strings are equal even if they are null
    //NOTE - Alternative is provided in TextUtils
    /*public static boolean equalsIncludingNull(String str1, String str2)
    {

        return (str1 == null && str2 == null)  //If both strings are null then they are same
                || (str1!= null && str2!=null && str1.equals(str2)); //If it comes to here then both strings must not be null and they must match with equals() to return that they are same
    }*/


    //Gets display name either for playlist or genre or artist etc.
    public static String getDisplayName( String source, String parameter)
    {

        String displayName;
        Cursor cursor;
        ContentResolver resolver;
        Uri uri;
        String [] projection;
        String selection;

        displayName = null;
        cursor = null;
        resolver = sAppContext.getContentResolver();

        if (source == null)
            return null;

        switch (source)
        {
            case Const.ALL_SCREEN:

                displayName = sAppContext.getString(R.string.all_songs_screen_title);
                break;
            case Const.PLAYLISTS_SCREEN:

                uri =           MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                projection =    new String[]{MediaStore.Audio.Playlists.NAME};
                selection =     MediaStore.Audio.Playlists._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if (cursor == null || cursor.getCount() == 0)
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString(0);
                break;
            case Const.ALBUMS_SCREEN:

                uri =           MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
                projection =    new String[]{MediaStore.Audio.Albums.ALBUM};
                selection =     MediaStore.Audio.Albums._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if (cursor == null || cursor.getCount() == 0)
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString(0);
                break;
            case Const.ARTISTS_SCREEN:

                uri =           MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
                projection =    new String[]{MediaStore.Audio.Artists.ARTIST};
                selection =     MediaStore.Audio.Artists._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if (cursor == null || cursor.getCount() == 0)
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString(0);
                break;
            case Const.GENRES_SCREEN:

                uri =           MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
                projection =    new String[]{MediaStore.Audio.Genres.NAME};
                selection =     MediaStore.Audio.Genres._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if (cursor == null || cursor.getCount() == 0)
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString(0);
                break;
            case Const.FILE_URI_KEY:
                displayName = sAppContext.getString(R.string.music_title);
                break;
        }

        if (cursor != null && !cursor.isClosed())
            cursor.close();


        return displayName;
    }



    public static void toastShort(String text)
    {
        Toast.makeText(SlimPlayerApplication.getInstance(),text,Toast.LENGTH_SHORT).show();
    }

    public static void toastLong(String text)
    {
        Toast.makeText(SlimPlayerApplication.getInstance(),text,Toast.LENGTH_LONG).show();
    }

    public static boolean isSourceDifferent(String source1, String parameter1, String source2, String parameter2)
    {
        boolean different;

        //Here we check if the source and parameters are same, but if both sources are null then we take them as they are different
        different = !(  TextUtils.equals( source1, source2 ) &&
                        TextUtils.equals( parameter1, parameter2 )
                        && source1 != null );                               //This actually checks if both sources are null(because first line would fail if they are not same)
                                                                            // ...and if they are, "different" will be true

        return different;
    }

    public static MediaBrowserCompat.MediaItem mediaFromFile(String fileUriString)
    {
        MediaMetadataCompat metadata;
        MediaMetadataCompat.Builder metadataBuilder;
        MediaMetadataRetriever retriever;
        Uri fileUri;
        MediaBrowserCompat.MediaItem mediaItem;

        metadataBuilder = new MediaMetadataCompat.Builder(  );
        retriever = new MediaMetadataRetriever();
        fileUri = Uri.parse( fileUriString );


        if (fileUri == null)
            return null;

        retriever.setDataSource( fileUri.getPath() );

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "0")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_TITLE ))
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ALBUM ))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_ARTIST ))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Long.parseLong(retriever.extractMetadata( MediaMetadataRetriever.METADATA_KEY_DURATION )))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, fileUriString);


        metadata = metadataBuilder.build();

        //Get media item with metadata bundled in its media description object
        mediaItem = MusicProvider.getInstance().bundleMetadata( metadata );

        return mediaItem;
    }

    public static String createParentString( String source, String parameter )
    {
        if (source == null || source.length() == 0)
            return null;

        if (parameter == null)
            return source;

        return source + ":" + parameter;
    }


}
