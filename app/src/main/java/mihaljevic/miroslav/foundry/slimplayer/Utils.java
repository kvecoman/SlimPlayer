package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Miroslav on 23.11.2016..
 *
 * Static class, contains utility functions used in other components
 *
 * @author Miroslav Mihaljević
 */
public final class Utils {

    protected static final String TAG = Utils.class.getSimpleName();

    private Utils(){}

    //Render custom font
    /*public static Bitmap renderFont(Context context, String text, int color, float fontSizeSP, String path)
    {
        int fontSizePX = convertDipToPix(context, fontSizeSP);
        int pad = (fontSizePX / 9);
        Paint paint = new Paint();
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), path);
        paint.setAntiAlias(true);
        paint.setTypeface(typeface);
        paint.setColor(color);
        paint.setTextSize(fontSizePX);

        int textWidth = (int) (paint.measureText(text) + pad*2);
        int height = (int) (fontSizePX / 0.75);
        Bitmap bitmap = Bitmap.createBitmap(textWidth, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float xOriginal = pad;
        canvas.drawText(text, xOriginal, fontSizePX, paint);
        return bitmap;

    }

    public static int convertDipToPix(Context context,float dip)
    {
        int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
        return value;
    }*/


    //Helper function to set and calculate height of list view (assuming all rows are same)
    public static int calculateListHeight(ListView lv)
    {
        int height = 0;
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

    public static boolean checkIfPlaylistExist(Context context, String playlistName)
    {
        //Check if playlist with that name already exists
        Cursor playlistsCursor = context.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                                        new String [] {MediaStore.Audio.Playlists._ID,MediaStore.Audio.Playlists.NAME},null,null,null);
        for (int i = 0;i < playlistsCursor.getCount();i++)
        {
            playlistsCursor.moveToPosition(i);
            if (playlistName.equals(playlistsCursor.getString(playlistsCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME))))
            {
                return true;
            }
        }

        return false;
    }

    //Creates playlist and returns id
    public static long createPlaylist(Context context, String playlistName)
    {
        //Create content values with new playlist info
        ContentValues inserts = new ContentValues();
        inserts.put(MediaStore.Audio.Playlists.NAME, playlistName);
        inserts.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        inserts.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

        //Insert new playlist values
        Uri uri = context.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,inserts);

        //Get ID of newly created playlist
        Cursor c = context.getContentResolver().query(uri, new String[]{MediaStore.Audio.Playlists._ID},null,null,null);
        c.moveToFirst();
        long pId = c.getLong(0);
        c.close();

        return pId;
    }

    //Returns number of inserted items
    public static int insertIntoPlaylist(Context context, List<String> IDs, long playlistId)
    {
        //long [] IDs = data.getLongArrayExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY);
        //long playlistId = ;
        Uri playlistUri = MediaStore.Audio.Playlists.Members.getContentUri("external",playlistId);
        List<ContentValues> valuesList = new ArrayList<>();

        Cursor playlistCursor = context.getContentResolver().query(playlistUri,new String[]{MediaStore.Audio.Playlists.Members._ID, MediaStore.Audio.Playlists.Members.AUDIO_ID},null,null,null);
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

        ContentResolver resolver = context.getContentResolver();
        ContentValues[] valuesArray = new ContentValues[valuesList.size()];
        valuesList.toArray(valuesArray);
        int result = resolver.bulkInsert(playlistUri, valuesArray);
        resolver.notifyChange(playlistUri,null);

        return result;

    }

    public static int deleteFromList(Context context, Cursor cursor, Uri uri, SparseBooleanArray checkedPositions)
    {
        int position = -1;
        long id;
        int deletedCount = 0;

        if (cursor == null || cursor.isClosed() || cursor.getColumnIndex("_id") == -1)
            return  0;

        for (int i = 0;i < checkedPositions.size();i++)
        {
            position = checkedPositions.keyAt(i);

            if (position >= 0 && position < cursor.getCount() && checkedPositions.get(position))
            {
                cursor.moveToPosition(position);
                id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
                deletedCount += context.getContentResolver().delete(  uri,
                        "_id" + "=?",
                        new String  [] {id + ""});
            }

        }

        return deletedCount;
    }

    public static Cursor querySongListCursor(Context context, Bundle args)
    {
        Log.d(TAG,"queryCursor()");

        if (args == null)
            return null;

        //Create query that will fetch songs that we need for this screen
        Uri uri = Uri.parse(args.getString(SlimListFragment.CURSOR_URI_KEY));
        String [] projection = args.getStringArray(SlimListFragment.CURSOR_PROJECTION_KEY);
        String selection = args.getString(SlimListFragment.CURSOR_SELECTION_KEY);
        String [] selectionArgs = args.getStringArray(SlimListFragment.CURSOR_SELECTION_ARGS_KEY);
        String sortOrder = args.getString(SlimListFragment.CURSOR_SORT_ORDER_KEY);

        return context.getContentResolver().query(uri,projection,selection,selectionArgs,sortOrder);
    }

    //Method that detects empty genres and stores that list into preferences
    public static int deleteEmptyGenres(Context context)
    {
        ContentResolver resolver = context.getContentResolver();
        int count = 0;
        Cursor genresCursor = resolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Genres._ID},null,null,null);

        if (genresCursor == null)
            return 0;

        int id;
        Cursor cursor;
        genresCursor.moveToFirst();
        do
        {
            id = genresCursor.getInt(0);
            cursor = resolver.query(MediaStore.Audio.Genres.Members.getContentUri("external", id),
                    new String[]{MediaStore.Audio.Genres.Members._ID},null,null,null);
            if (cursor.getCount() == 0)
            {
                //Here we delete if genre is empty
                resolver.delete(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,MediaStore.Audio.Genres._ID + "=" + id,null);
                count++;
            }
            cursor.close();
        }while(genresCursor.moveToNext());

        genresCursor.close();

        return count;

    }



    //Checks if two strings are equal even if they are null
    public static boolean equalsIncludingNull(String str1, String str2)
    {
        if (str1 == null && str2 == null)
            return true;

        if (str1 == null && str2 != null)
            return false;

        if (str1 != null && str2 == null)
            return false;

        return str1.equals(str2);
    }


    //Gets display name either for playlist or genre or artist etc.
    public static String getDisplayName(Context context, String source, String parameter)
    {

        String displayName = null;
        Cursor cursor;
        ContentResolver resolver = context.getContentResolver();

        if (source == null)
            return null;

        if (source.equals(context.getString(R.string.pref_key_all_screen)))
        {
            //TODO - move to resource
            displayName = "All songs";
        }
        else if(source.equals(context.getString(R.string.pref_key_playlists_screen)))
        {
            cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Playlists.NAME}, MediaStore.Audio.Playlists._ID + "=" + parameter,null,null);
            if (cursor.getCount() == 0)
                return null;

            cursor.moveToFirst();
            displayName = cursor.getString(0);
        }
        else if (source.equals(context.getString(R.string.pref_key_albums_screen)))
        {
            cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Albums.ALBUM}, MediaStore.Audio.Albums._ID + "=" + parameter,null,null);
            if (cursor.getCount() == 0)
                return null;

            cursor.moveToFirst();
            displayName = cursor.getString(0);
        }
        else if (source.equals(context.getString(R.string.pref_key_artists_screen)))
        {
            cursor = resolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Artists.ARTIST}, MediaStore.Audio.Artists._ID + "=" + parameter,null,null);
            if (cursor.getCount() == 0)
                return null;

            cursor.moveToFirst();
            displayName = cursor.getString(0);
        }
        else if (source.equals(context.getString(R.string.pref_key_genres_screen)))
        {
            cursor = resolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Genres.NAME}, MediaStore.Audio.Genres._ID + "=" + parameter,null,null);
            if (cursor.getCount() == 0)
                return null;

            cursor.moveToFirst();
            displayName = cursor.getString(0);
        }

        return displayName;
    }
}
