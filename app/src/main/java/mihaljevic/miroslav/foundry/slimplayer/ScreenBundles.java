package mihaljevic.miroslav.foundry.slimplayer;

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

    public static SlimPlayerApplication sAppContext = SlimPlayerApplication.getInstance();

    //Private constructor to make it non-instantiable
    private ScreenBundles(){}

    public static Bundle getBundleForMainScreen(String screen)
    {
        Bundle bundle = null;

        switch (screen)
        {
            case Const.ALL_SCREEN:
                bundle = getAllSongsBundle();
                break;
            case Const.PLAYLISTS_SCREEN:
                bundle = getPlaylistsBundle();
                break;
            case Const.ALBUMS_SCREEN:
                bundle = getAlbumsBundle();
                break;
            case Const.ARTISTS_SCREEN:
                bundle = getArtistsBundle();
                break;
            case Const.GENRES_SCREEN:
                bundle = getGenresBundle();
                break;
        }

        return bundle;
    }

    //Central function to obtain bundle for songs by playlist, or songs by artist screen
    public static Bundle getBundleForSubScreen (String currentScreen, Cursor cursor )
    {
        Bundle bundle = null;
        String parameter;


        switch (currentScreen)
        {
            case Const.ALL_SCREEN:
                bundle = ScreenBundles.getAllSongsBundle();
                break;
            case Const.PLAYLISTS_SCREEN:
                parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
                bundle = ScreenBundles.getSongsByPlaylistBundle(parameter);
                break;
            case Const.ALBUMS_SCREEN:
                parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                bundle = ScreenBundles.getSongsByAlbumBundle(parameter);
                break;
            case Const.ARTISTS_SCREEN:
                parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
                bundle = ScreenBundles.getSongsByArtistsBundle(parameter);
                break;
            case Const.GENRES_SCREEN:
                parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres._ID));
                bundle = ScreenBundles.getSongsByGenreBundle(parameter);
                break;
        }

        /*if(currentScreen.equals(sAppContext.getString(R.string.pref_key_playlists_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
            bundle = ScreenBundles.getSongsByPlaylistBundle(sAppContext, parameter);
        }
        else if (currentScreen.equals(sAppContext.getString(R.string.pref_key_albums_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            bundle = ScreenBundles.getSongsByAlbumBundle(sAppContext,parameter);
        }
        else if (currentScreen.equals(sAppContext.getString(R.string.pref_key_artists_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));
            bundle = ScreenBundles.getSongsByArtistsBundle(sAppContext,parameter);
        }
        else if (currentScreen.equals(sAppContext.getString(R.string.pref_key_genres_screen)))
        {
            parameter = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Genres._ID));
            bundle = ScreenBundles.getSongsByGenreBundle(sAppContext,parameter);
        }*/


        return bundle;
    }

    //Also central function for retrieving bundle, but when we already know parameter
    public static Bundle getBundleForSubScreen (String currentScreen, String parameter )
    {
        Bundle bundle = null;

        switch (currentScreen)
        {
            case Const.ALL_SCREEN:
                bundle = ScreenBundles.getAllSongsBundle();
                break;
            case Const.PLAYLISTS_SCREEN:
                bundle = ScreenBundles.getSongsByPlaylistBundle(parameter);
                break;
            case Const.ALBUMS_SCREEN:
                bundle = ScreenBundles.getSongsByAlbumBundle(parameter);
                break;
            case Const.ARTISTS_SCREEN:
                bundle = ScreenBundles.getSongsByArtistsBundle(parameter);
                break;
            case Const.GENRES_SCREEN:
                bundle = ScreenBundles.getSongsByGenreBundle(parameter);
                break;
        }

        /*if (currentScreen.equals(sAppContext.getString(R.string.pref_key_all_screen)))
        {
            bundle = ScreenBundles.getAllSongsBundle(sAppContext);
        }
        else if(currentScreen.equals(sAppContext.getString(R.string.pref_key_playlists_screen)))
        {
            bundle = ScreenBundles.getSongsByPlaylistBundle(sAppContext, parameter);
        }
        else if (currentScreen.equals(sAppContext.getString(R.string.pref_key_albums_screen)))
        {
            bundle = ScreenBundles.getSongsByAlbumBundle(sAppContext,parameter);
        }
        else if (currentScreen.equals(sAppContext.getString(R.string.pref_key_artists_screen)))
        {
            bundle = ScreenBundles.getSongsByArtistsBundle(sAppContext,parameter);
        }
        else if (currentScreen.equals(sAppContext.getString(R.string.pref_key_genres_screen)))
        {
            bundle = ScreenBundles.getSongsByGenreBundle(sAppContext,parameter);
        }*/

        return bundle;
    }

    //Returns bundle for cursor init for All songs screen
    public static Bundle getAllSongsBundle()
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.ALL_SCREEN;

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
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" + addDirectoryCheckSQL() + ")";
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        /*if (BuildConfig.DEBUG)
        {
            sortOrder = MediaStore.Audio.Media.DURATION + " ASC";
        }*/


        bundle.putString( Const.PARAMETER_KEY,""); //Empty string just so we don't mess up database
        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }

    //Returns bundle for cursor init for Playlists screen
    public static Bundle getPlaylistsBundle()
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.PLAYLISTS_SCREEN;

        String uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME
        };
        String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Playlists.NAME + " ASC";

        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Playlists.NAME);

        return bundle;
    }

    //Returns bundle for cursor init for Albums screen
    public static Bundle getAlbumsBundle()
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.ALBUMS_SCREEN;

        String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" + addDirectoryCheckSQL() + ")" + ") GROUP BY (" + MediaStore.Audio.Media.ALBUM;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.ALBUM + " ASC";

        //NOTE - this code can't filter by directory
        /*String uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
        };
        String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Albums.ALBUM + " ASC";*/

        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Media.ALBUM);

        return bundle;
    }




    //Returns bundle for cursor init for All songs screen
    public static Bundle getArtistsBundle()
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.ARTISTS_SCREEN;

        String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" + addDirectoryCheckSQL() + ")" + ") GROUP BY (" + MediaStore.Audio.Media.ARTIST;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.ARTIST + " ASC";

        //NOTE - this code can't filter by directory
        /*String uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
        };
        String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Artists.ARTIST + " ASC";*/

        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Artists.ARTIST);

        return bundle;
    }

    //Returns bundle for cursor init for Genres screen
    public static Bundle getGenresBundle()
    {

        Bundle bundle = new Bundle();

        String cursorScreen = Const.GENRES_SCREEN;

        String uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
        };
        String selection = "LENGTH(" +  MediaStore.Audio.Genres.NAME + ") > 0";
        //String selection = null;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Genres.NAME + " ASC";

        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Genres.NAME);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs from playlist (included in parameter string)
    public static Bundle getSongsByPlaylistBundle(String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.PLAYLISTS_SCREEN;

        String uri = MediaStore.Audio.Playlists.Members.getContentUri("external",Long.valueOf(parameter)).toString();
        String [] projection = {
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members._ID,
                MediaStore.Audio.Playlists.Members.DISPLAY_NAME,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.ALBUM,
                MediaStore.Audio.Playlists.Members.DURATION,
                MediaStore.Audio.Playlists.Members.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                addDirectoryCheckSQL() + ")";
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Playlists.Members.TITLE + " ASC";


        bundle.putString( Const.PARAMETER_KEY,parameter);
        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Playlists.Members.TITLE);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs by specified artist (included in parameter string)
    public static Bundle getSongsByAlbumBundle( String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.ALBUMS_SCREEN;

        String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                addDirectoryCheckSQL() + ") AND " +
                MediaStore.Audio.Media.ALBUM_ID + "=?";
        String [] selectionArgs = {parameter};
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";


        bundle.putString( Const.PARAMETER_KEY,parameter);
        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs by specified artist (included in parameter string)
    public static Bundle getSongsByArtistsBundle( String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.ARTISTS_SCREEN;

        String uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        String [] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                                                addDirectoryCheckSQL() + ") AND " +
                                                MediaStore.Audio.Media.ARTIST_ID + "=" + parameter;
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";


        bundle.putString( Const.PARAMETER_KEY,parameter);
        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }

    //Get bundle that creates cursor that will get all songs by specified genre (included in parameter string)
    public static Bundle getSongsByGenreBundle( String parameter)
    {
        Bundle bundle = new Bundle();

        String cursorScreen = Const.GENRES_SCREEN;

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
                addDirectoryCheckSQL() + ")";
        String [] selectionArgs = null;
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";


        bundle.putString( Const.PARAMETER_KEY,parameter);
        bundle.putString( Const.SOURCE_KEY,cursorScreen);
        bundle.putString( Const.URI_KEY,uri);
        bundle.putStringArray( Const.PROJECTION_KEY,projection);
        bundle.putString( Const.SELECTION_KEY,selection);
        bundle.putStringArray( Const.SELECTION_ARGS_KEY,selectionArgs);
        bundle.putString( Const.SORT_ORDER_KEY,sortOrder);

        bundle.putString( Const.DISPLAY_FIELD_KEY,MediaStore.Audio.Media.TITLE);

        return bundle;
    }


    //Function that returns part of where clause which filters songs by directories selected
    //... in preferences
    public static String addDirectoryCheckSQL()
    {
        String result = "";
        String dataField = MediaStore.Audio.Media.DATA;

        Set<String> directories = PreferenceManager.getDefaultSharedPreferences(sAppContext).getStringSet(sAppContext.getString(R.string.pref_key_directories_set),null);

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

    /*public static  String excludeEmptyGenresSQL()
    {
        String result = "";
        String genreField = MediaStore.Audio.Genres._ID;
        Set<String> idSet = PreferenceManager.getDefaultSharedPreferences(sAppContext).getStringSet(sAppContext.getString(R.string.pref_key_empty_genres),null);

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
