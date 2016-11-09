package com.example.miroslav.slimplayer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;


/**
 * Created by Miroslav on 8.10.2016..
 *
 * Implementation of cursor adapter used to load songs in ListView
 *
 * @author Miroslav Mihaljević
 */
public class SongCursorAdapter extends CursorAdapter {

    private String mDisplayField;

    public SongCursorAdapter(Context context, Cursor c, boolean autoRequery, String displayField) {
        super(context, c, autoRequery);
        mDisplayField = displayField;
    }

    public SongCursorAdapter(Context context, Cursor c, int flags, String displayField) {
        super(context, c, flags);
        mDisplayField = displayField;

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(android.R.layout.simple_list_item_activated_1,null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView textView = (TextView)view.findViewById(android.R.id.text1);
        String text = cursor.getString(cursor.getColumnIndex(mDisplayField));

        textView.setText(text);
    }
}
