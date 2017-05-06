package mihaljevic.miroslav.foundry.slimplayer;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

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
    public static int calculateListHeight( ListView listView )
    {
        int         height;
        ListAdapter adapter;
        int         count;
        View        item;


        adapter = listView.getAdapter();
        count   = adapter.getCount();

        if (count <= 0)
            return 0;

        item = adapter.getView(0, null, listView);


        item.measure( 0, 0 );

        height = item.getMeasuredHeight() * count;
        height += listView.getDividerHeight() * ( count - 1 );

        return height;
    }

    //Check if AUDIO_ID already exists in playlist
    public static boolean playlistContainsAudioID( Cursor playlistCursor, long targetID )
    {
        if (playlistCursor == null)
            return false;

        long currentID;


        for (int i = 0; i < playlistCursor.getCount(); i++)
        {
            playlistCursor.moveToPosition( i );

            currentID = playlistCursor.getLong( playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID ) );

            if ( currentID == targetID )
            {
                return true;
            }
        }

        return false;
    }

    public static boolean checkIfPlaylistExist( String playlistName )
    {
        if ( Build.VERSION.SDK_INT >= 16 && !checkPermission( Manifest.permission.READ_EXTERNAL_STORAGE ) )
            return false;

        if ( playlistName == null )
            return false;

        //Check if playlist with that name already exists
        Cursor      playlistsCursor;
        String []   projection;
        String      currentPlaylistName;

        projection = new String [] { MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME };

        playlistsCursor = sAppContext.getContentResolver().query(   MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                                                    projection,
                                                                    null,null,null );
        if ( playlistsCursor == null )
            return false;

        while ( playlistsCursor.moveToNext() )
        {
            currentPlaylistName = playlistsCursor.getString( playlistsCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME ) );

            if ( playlistName.equals(currentPlaylistName) )
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
        if ( Build.VERSION.SDK_INT >= 16 && !checkPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE ))
            return -1;

        ContentValues   inserts;
        Uri             insertedPlaylistUri;
        long            playlistID;

        //Create content values with new playlist info
        inserts = new ContentValues();
        inserts.put(MediaStore.Audio.Playlists.NAME, playlistName);
        inserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        inserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        //Insert new playlist values
        insertedPlaylistUri = sAppContext.getContentResolver().insert( MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, inserts );


        if ( insertedPlaylistUri == null )
            return -1;

        playlistID = Long.valueOf( insertedPlaylistUri.getLastPathSegment() );


        return playlistID;
    }

    //Returns number of inserted items
    public static int insertIntoPlaylist( List<String> songIDs, long playlistId)
    {

        Uri             playlistUri;
        String []       playlistCursorProjection;
        Cursor          playlistCursor;
        int             existingSongsCount;
        long            songID;
        ContentResolver resolver;
        ContentValues   singleValues;
        int             valuesCount;
        ContentValues[] valuesArray;
        ContentValues[] trimmedValuesArray;
        int             addedSongsCount;


        if ( songIDs == null || songIDs.size() == 0 || playlistId < 0 )
            return 0;


        resolver                    = sAppContext.getContentResolver();
        playlistUri                 = MediaStore.Audio.Playlists.Members.getContentUri("external",playlistId);
        playlistCursorProjection    = new String[] {    MediaStore.Audio.Playlists.Members._ID,
                                                        MediaStore.Audio.Playlists.Members.AUDIO_ID };
        playlistCursor = resolver.query(playlistUri,
                                        playlistCursorProjection,
                                        null,
                                        null,
                                        null);

        if ( playlistCursor == null )
            return 0;

        existingSongsCount = playlistCursor.getCount();

        valuesArray = new ContentValues[songIDs.size()];
        valuesCount = 0;

        for(String songIDStr : songIDs)
        {
            songID = Long.parseLong(songIDStr);

            if ( !Utils.playlistContainsAudioID( playlistCursor, songID ) )
            {
                //If we don't have this ID in playlist then prepare it to be added

                singleValues = new ContentValues();
                singleValues.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, songID);
                singleValues.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, valuesCount + existingSongsCount);

                valuesArray[valuesCount] = singleValues;
                valuesCount++;
            }
        }


        trimmedValuesArray = Arrays.copyOf( valuesArray, valuesCount );

        addedSongsCount = resolver.bulkInsert(playlistUri, trimmedValuesArray);

        resolver.notifyChange(playlistUri,null);

        return addedSongsCount;

    }

    public static int deleteFromList( List<MediaBrowserCompat.MediaItem> mediaItems, Uri listURI, HashSet<Integer> selectedPositions, String idField)
    {
        //int     position;
        String  mediaID;
        int     deletedCount;
        //boolean isItemChecked;

        deletedCount = 0;

        if (mediaItems == null)
            return  0;

        //for (int i = 0; i < checkedPositions.size(); i++)
        for (Integer position : selectedPositions )
        {
            /*position        = checkedPositions.keyAt( i );
            isItemChecked   = checkedPositions.valueAt( i );*/

            if ( position >= 0 && position < mediaItems.size() )
            {
                mediaID = mediaItems.get(position).getMediaId();

                deletedCount += sAppContext.getContentResolver().delete(    listURI,
                                                                            idField + "=?",
                                                                            new String  [] {mediaID});
            }

        }

        return deletedCount;

    }

    //Bundle contains data with keys from ScreenBundles
    public static Cursor queryMedia( Bundle args )
    {
        Log.v(TAG,"queryMedia()");

        if (args == null)
            return null;

        Uri         uri;
        String []   projection;
        String      selection;
        String []   selectionArgs;
        String      sortOrder;

        //Create query that will fetch songs that we need for this screen
        uri             = Uri.parse(args.getString  ( Const.URI_KEY));
        projection      = args.getStringArray       ( Const.PROJECTION_KEY);
        selection       = args.getString            ( Const.SELECTION_KEY);
        selectionArgs   = args.getStringArray       ( Const.SELECTION_ARGS_KEY);
        sortOrder       = args.getString            ( Const.SORT_ORDER_KEY);

        return sAppContext.getContentResolver().query(  uri,
                                                        projection,
                                                        selection,
                                                        selectionArgs,
                                                        sortOrder );
    }

    //Method that detects empty genres and stores that list into preferences
    public static int deleteEmptyGenres()
    {
        if ( Build.VERSION.SDK_INT >= 16 && !checkPermission( Manifest.permission.READ_EXTERNAL_STORAGE ))
        {
            Log.i(TAG, "No READ_EXTERNAL_STORAGE permission to delete genres");
            return 0;
        }

        ContentResolver resolver;
        int             count;
        Cursor          genresCursor;
        int             id;
        Cursor          cursor;

        resolver    = sAppContext.getContentResolver();
        count       = 0;

        genresCursor = resolver.query(  MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                                        new String[]{ MediaStore.Audio.Genres._ID },
                                        null, null, null );

        if ( genresCursor == null )
            return 0;


        while ( genresCursor.moveToNext() )
        {
            id = genresCursor.getInt( 0 );
            cursor = resolver.query(    MediaStore.Audio.Genres.Members.getContentUri( "external", id ),
                                        new String[]{ MediaStore.Audio.Genres.Members._ID },
                                        null, null, null );
            if (cursor == null)
                continue;

            if (cursor.getCount() == 0 )
            {
                //Here we delete if genre is empty
                resolver.delete( MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, MediaStore.Audio.Genres._ID + "=" + id, null );
                count++;
            }

            cursor.close();

        }

        genresCursor.close();

        return count;

    }





    //Gets display name either for playlist or genre or artist etc.
    public static String getDisplayName( String source, String parameter)
    {

        String          displayName;
        Cursor          cursor;
        ContentResolver resolver;
        Uri             uri;
        String []       projection;
        String          selection;

        displayName = null;
        cursor      = null;
        resolver    = sAppContext.getContentResolver();

        if ( source == null)
            return null;

        switch ( source )
        {
            case Const.ALL_SCREEN:

                displayName = sAppContext.getString(R.string.all_songs_screen_title);
                break;
            case Const.PLAYLISTS_SCREEN:

                uri         = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
                projection  = new String[]{ MediaStore.Audio.Playlists.NAME };
                selection   = MediaStore.Audio.Playlists._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if ( cursor == null || cursor.getCount() == 0)
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString( 0 );
                break;
            case Const.ALBUMS_SCREEN:

                uri         = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
                projection  = new String[]{MediaStore.Audio.Albums.ALBUM};
                selection   = MediaStore.Audio.Albums._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if ( cursor == null || cursor.getCount() == 0 )
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString( 0 );
                break;
            case Const.ARTISTS_SCREEN:

                uri         = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
                projection  = new String[]{ MediaStore.Audio.Artists.ARTIST };
                selection   = MediaStore.Audio.Artists._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if ( cursor == null || cursor.getCount() == 0 )
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString(0);
                break;
            case Const.GENRES_SCREEN:

                uri         = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
                projection  = new String[]{MediaStore.Audio.Genres.NAME};
                selection   = MediaStore.Audio.Genres._ID + "=" + parameter;

                cursor = resolver.query( uri, projection, selection, null, null );

                if ( cursor == null || cursor.getCount() == 0 )
                    return null;

                cursor.moveToFirst();
                displayName = cursor.getString(0);
                break;
            case Const.FILE_URI_KEY:
                displayName = sAppContext.getString(R.string.music_title);
                break;
        }

        if ( cursor != null && !cursor.isClosed() )
            cursor.close();


        return displayName;
    }



    public static void toastShort( String text )
    {

        Toast.makeText( SlimPlayerApplication.getInstance(), text, Toast.LENGTH_SHORT ).show();
    }

    public static void toastLong( String text )
    {
        Toast.makeText( SlimPlayerApplication.getInstance(), text, Toast.LENGTH_LONG ).show();
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


    //Creates joined string with source and parameter
    public static String createParentString( String source, String parameter )
    {
        if ( source == null || source.length() == 0 )
            return null;

        if ( parameter == null )
            return source;

        return source + ":" + parameter;
    }

    //Splits parent string into source and parameter
    public static String[] splitParentString( String parentString )
    {
        String []   result;
        String []   split;
        String      source;
        String      parameter;

        result= new String[2];

        split = parentString.split( "\\:" );

        if ( split.length < 1 || split.length > 2 )
            return result;
        else if ( split.length == 1 )
        {
            source      = split[ 0 ];
            parameter   = null;
        }
        else
        {
            source      = split[ 0 ];
            parameter   = split[ 1 ];
        }

        result[0] = source;
        result[1] = parameter;

        return result;
    }




    public static Object getByIndex( TreeMap map, int index)
    {
        return map.get( (map.keySet().toArray())[index] );
    }

    public static class alphabetSort implements Comparator<String>
    {
        @Override
        public int compare( String o1, String o2 )
        {
            return o1.toLowerCase().compareTo( o2.toLowerCase() );
        }
    }

    //Checks permission for whole app
    public static boolean checkPermission( String permission)
    {
        return ContextCompat.checkSelfPermission( SlimPlayerApplication.getInstance(), permission ) == PackageManager.PERMISSION_GRANTED;
    }

    public static void askPermission ( final Activity activity, final String permission, String explanation,final int requestCode )
    {
        askPermission( activity, permission, explanation, requestCode, null );
    }


    public static void askPermission( final Activity activity, final String permission, String explanation,final int requestCode, DialogInterface.OnClickListener cancelListener )
    {
        if ( Build.VERSION.SDK_INT >= 16 &&  !checkPermission( permission ) )
        {
            if ( ActivityCompat.shouldShowRequestPermissionRationale( activity, permission ) )
            {
                showMessageOKCancel(activity, explanation, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick( DialogInterface dialog, int which )
                    {
                        ActivityCompat.requestPermissions( activity, new String[]{permission}, requestCode );
                    }
                },
                cancelListener);
            }
            else
            {
                ActivityCompat.requestPermissions( activity, new String[] { permission }, requestCode );
            }

        }
    }

    public static void showMessageOKCancel( Context context, String message, DialogInterface.OnClickListener okListener )
    {
        showMessageOKCancel( context, message, okListener, null );
    }


    public static void showMessageOKCancel( Context context, String message, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener )
    {
        new AlertDialog.Builder( context )
                        .setMessage( message )
                        .setPositiveButton( context.getString( R.string.OK ), okListener )
                        .setNegativeButton( context.getString( R.string.Cancel ), cancelListener )
                        .create()
                        .show();
    }

    /*public static <T> boolean existsInArray( T[] array, T target )
    {
        if ( array == null || array.length == 0 || target == null)
            return false;

        for ( T object : array )
        {
            if ( object.equals( target ) )
                return true;
        }

        return false;
    }*/

    public static boolean isSongs( String source, String parameter )
    {
        if ( source == null )
            return false;


        return source.equals( Const.ALL_SCREEN ) || parameter != null;
    }


    public static void calculateColorForGL( int colorID, Float red, Float green, Float blue )
    {
        int color;

        color = ContextCompat.getColor( sAppContext, colorID );

        red     = Float.valueOf( color & 0xFF0000 );
        green   = Float.valueOf( color & 0x00FF00 );
        blue    = Float.valueOf( color & 0x0000FF );

        red     /= 255f;
        green   /= 255f;
        blue    /= 255f;
    }

    public static boolean hasGLES20()
    {
        ActivityManager activityManager;
        ConfigurationInfo configurationInfo;

        activityManager = ( ActivityManager )SlimPlayerApplication.getInstance().getSystemService( Context.ACTIVITY_SERVICE );

        configurationInfo = activityManager.getDeviceConfigurationInfo();

        return configurationInfo.reqGlEsVersion >= 0x20000;

    }


}
