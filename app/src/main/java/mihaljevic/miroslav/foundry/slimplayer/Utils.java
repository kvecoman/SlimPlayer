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
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

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

    //Method that detects empty genres and stores that list into preferences
    /*public static void detectEmptyGenres(Context context)
    {
        ContentResolver resolver = context.getContentResolver();
        Set<String> idSet = new HashSet<>();
        Cursor genresCursor = resolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Genres._ID},null,null,null);
        int id;
        Cursor cursor;
        genresCursor.moveToFirst();
        do {
            id = genresCursor.getInt(0);
            cursor = resolver.query(MediaStore.Audio.Genres.getContentUriForAudioId("external", id),
                    new String[]{MediaStore.Audio.Genres.Members._ID,MediaStore.Audio.Genres.Members.DATA},ScreenBundles.addDirectoryCheckSQL(context),null,null);
            if (cursor.getCount() == 0)
            {
                idSet.add(String.valueOf(id));

            }
            genresCursor.moveToNext();
        }while (!genresCursor.isLast());

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putStringSet(context.getString(R.string.pref_key_empty_genres),idSet).commit();
    }*/
}
