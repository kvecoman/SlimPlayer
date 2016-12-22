package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Miroslav on 21.12.2016..
 */

//TODO - continue here - database helper is done, now actually write statistics in database and use it for home screen
public class StatsDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Stats.db";

    public StatsDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        //Create all tables in database
        db.execSQL(StatsContract.SourceStats.SQL_CREATE);
        db.execSQL(StatsContract.SourceRecords.SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(StatsContract.SourceStats.SQL_DROP);
        db.execSQL(StatsContract.SourceRecords.SQL_DROP);
        onCreate(db);
    }
}
