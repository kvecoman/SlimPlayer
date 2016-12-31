package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import java.util.Set;

/**
 * Static class that contains cursor data for different screens
 *
 * @author Miroslav Mihaljević
 */


public final class ScreenBundles {

    //Keys that are used when transferring data about different screens
    public static final String CURSOR_SOURCE_KEY = "cursor_screen";

    public static final String CURSOR_PARAMETER_KEY = "cursor_parameter"; //ID of playlist, or artist, something like that
    public static final String CURSOR_URI_KEY = "cursor_uri";
    public static final String CURSOR_PROJECTION_KEY = "cursor_projection";
    public static final String CURSOR_SELECTION_KEY = "cursor_selection";
    public static final String CURSOR_SELECTION_ARGS_KEY = "cursor_selection_args";
    public static final String CURSOR_SORT_ORDER_KEY = "cursor_sort_order";

    public static final String DISPLAY_FIELD_KEY = "display_field";

    //Private constructor to make it non-instantiable
    private ScreenBundles(){}

    //Central function to obtain bundle for songs by playlist, or songs by artist screen
    public static Bundle getBundleForSubScreen (String currentScreen, Cursor cursor, Context context)
    {
        Bundle bundle = null;
        String parameter;
        

        if(currentScreen.equals(context.getString(R.string.pref_key_playlists_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
            bundle = ScreenBundles.getSongsByPlaylistBundle(context, parameter);
        }
        else if (currentScreen.equals(context.getString(R.string.pref_key_albums_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
            bundle = ScreenBundles.getSongsByAlbumBundle(context,parameter);
        }
        else if (currentScreen.equals(context.getString(R.string.pref_key_artists_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            bundle = ScreenBundles.getSongsByArtistsBundle(context,parameter);
        }
        else if (currentScreen.equals(context.getString(R.string.pref_key_genres_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres._ID));
            bundle = ScreenBundles.getSongsByGenreBundle(context,parameter);
        }


        return bundle;
    }

    //Also central function for retrieving bundle, but when we already know parameter
    public static Bundle getBundleForSubScreen (Context context, String currentScreen, String parameter )
    {
        Bundle bundle = null;

        if (currentScreen.equals(context.getString(R.string.pref_key_all_screen)))
        {
            bundle = ScreenBundles.getAllSongsBundle(context);
        }
        else if(currentScreen.equals(context.getString(R.string.pref_key_playlists_screen)))
        {
            bundle = ScreenBundles.getSongsByPlaylistBundle(context, parameter);
        }
        else if (currentScreen.equals(context.getString(R.string.pref_key_albums_screen)))
        {
            bundle = ScreenBundles.getSongsByAlbumBundle(context,parameter);
        }
        else if (currentScreen.equals(context.getString(R.string.pref_key_artists_screen)))
        {
            bundle = ScreenBundles.getSongsByArtistsBundle(context,parameter);
        }
        else if (currentScreen.equals(context.getString(R.string.pref_key_genres_screen)))
        {
            bundle = ScreenBundles.getSongsByGenreBundle(context,parameter);
        }

        return bundle;
    }

    //Returns bundle for cursor init for All songs screen
    public static Bundle getAllSongsBundle(Context context)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_all_screen);

        String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" + addDirectoryCheckSQL(context) + ")";
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        /*if (BuildConfig.DEBUG)
        {
            sortOrder = MediaStore.Audio.Media.DURATION + " ASC";
        }*/


        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }

    //Returns bundle for cursor init for Playlists screen
    public static Bundle getPlaylistsBundle(Context context)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_playlists_screen);

        String uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Playlists.NAME + " ASC";

        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Playlists.NAME);

        return bundle;
    }

    //Returns bundle for cursor init for Albums screen
    public static Bundle getAlbumsBundle(Context context)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_albums_screen);

        /*String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" + addDirectoryCheckSQL(context) + ")" + ") GROUP BY (" + MediaStore.Audio.Media.ALBUM;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";*/

        String uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
        };
        //TODO - there might be need to somehow check for directories of songs in particular album, also for artists
        String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";

        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Albums.ALBUM);

        return bundle;
    }




    //Returns bundle for cursor init for All songs screen
    public static Bundle getArtistsBundle(Context context)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_artists_screen);

        /*String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" + addDirectoryCheckSQL(context) + ")" + ") GROUP BY (" + MediaStore.Audio.Media.ARTIST;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";*/

        String uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
        };
        String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Artists.ARTIST + " ASC";

        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Artists.ARTIST);

        return bundle;
    }

    //Returns bundle for cursor init for Genres screen
    public static Bundle getGenresBundle(Context context)
    {

        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_genres_screen);

        String uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
        };
        String selection = "LENGTH(" +  MediaStore.Audio.Genres.NAME + ") > 0";
        //String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Genres.NAME + " ASC";

        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Genres.NAME);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs from playlist (included in parameter string)
    public static Bundle getSongsByPlaylistBundle(Context context, String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_playlists_screen);

        String uri = MediaStore.Audio.Playlists.Members.getContentUri("external",Long.valueOf(parameter)).toString();
        String [] projection = {
                MediaStore.Audio.Playlists.Members._ID,
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members.DISPLAY_NAME,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.ALBUM,
                MediaStore.Audio.Playlists.Members.DURATION,
                MediaStore.Audio.Playlists.Members.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                addDirectoryCheckSQL(context) + ")";
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Playlists.Members.TITLE + " ASC";


        bundle.putString(CURSOR_PARAMETER_KEY,parameter);
        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Playlists.Members.TITLE);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs by specified artist (included in parameter string)
    public static Bundle getSongsByAlbumBundle(Context context, String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_albums_screen);

        String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                addDirectoryCheckSQL(context) + ") AND " +
                MediaStore.Audio.Media.ALBUM_ID + "=?";
        String [] selectionArgs = {parameter};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";


        bundle.putString(CURSOR_PARAMETER_KEY,parameter);
        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs by specified artist (included in parameter string)
    public static Bundle getSongsByArtistsBundle(Context context, String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_artists_screen);

        String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                                                addDirectoryCheckSQL(context) + ") AND " +
                                                MediaStore.Audio.Media.ARTIST_ID + "=?";
        String [] selectionArgs = {parameter};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";


        bundle.putString(CURSOR_PARAMETER_KEY,parameter);
        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs by specified genre (included in parameter string)
    public static Bundle getSongsByGenreBundle(Context context, String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = context.getString(R.string.pref_key_genres_screen);

        String uri = MediaStore.Audio.Genres.Members.getContentUri("external",Long.parseLong(parameter)).toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                addDirectoryCheckSQL(context) + ")";
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";


        bundle.putString(CURSOR_PARAMETER_KEY,parameter);
        bundle.putString(CURSOR_SOURCE_KEY,cursorScreen);
        bundle.putString(CURSOR_URI_KEY,uri);
        bundle.putStringArray(CURSOR_PROJECTION_KEY,projection);
        bundle.putString(CURSOR_SELECTION_KEY,selection);
        bundle.putStringArray(CURSOR_SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString(CURSOR_SORT_ORDER_KEY,sortOrder);

        bundle.putString(DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }


    //Function that returns part of where clause which filters songs by directories selected
    //... in preferences
    public static String addDirectoryCheckSQL(Context context)
    {
        String result = "";
        String dataField = MediaStore.Audio.Media.DATA;

        Set<String> directories = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(context.getString(R.string.pref_key_directories_set),null);

        //If directories are null then just load everything
        if (directories == null || directories.isEmpty())
        {
            return "1=1";
        }

        //Loop every directory and create condition for it
        for (String dir : directories)
        {
            result += " " + dataField + " LIKE \"" + dir + "%\" OR";
        }
        //Remove the excess OR from the string
        result = result.substring(0,result.length() - 2);

        return result;
    }

    /*public static  String excludeEmptyGenresSQL(Context context)
    {
        String result = "";
        String genreField = MediaStore.Audio.Genres._ID;
        Set<String> idSet = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(context.getString(R.string.pref_key_empty_genres),null);

        if (idSet == null ||idSet.isEmpty())
        {
            return "1=1";
        }

        for (String id : idSet)
        {
            result += " " + genreField + " <> " + id + " AND";
        }
        //Remove the excess AND from the string
        result = result.substring(0,result.length() - 3);

        return result;
    }*/
}
