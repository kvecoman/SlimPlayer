package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Miroslav on 21.12.2016..
 */

//TODO - continue here - database helper is done, now actually write statistics in database and use it for home screen
public class StatsDbHelper extends SQLiteOpenHelper {
    protected final String TAG = getClass().getSimpleName();

    public static final int DATABASE_VERSION = 8;
    public static final String DATABASE_NAME = "Stats.db";

    private Context mContext;

    public StatsDbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mContext = context;
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

    //TODO - move, optimize
    //TODO - this to async
    //TODO - what happens if database is reconstructed and id's for sources are different
    public void updateStats(String source, String parameter)
    {
        Log.d(TAG,"updateStats()");

        //Get database
        SQLiteDatabase statsDb = getWritableDatabase();

        //Try to insert row with this source and parameter in source_stats (if it exists it wont be done)
        ContentValues sourceStatsValues = new ContentValues();
        sourceStatsValues.put(StatsContract.SourceStats.COLUMN_NAME_SOURCE,source);
        sourceStatsValues.put(StatsContract.SourceStats.COLUMN_NAME_PARAMETER,parameter);
        sourceStatsValues.put(StatsContract.SourceStats.COLUMN_NAME_DISPLAY_NAME, Utils.getDisplayName(mContext,source,parameter));
        long source_stats_id = statsDb.insertWithOnConflict(StatsContract.SourceStats.TABLE_NAME,null,sourceStatsValues,SQLiteDatabase.CONFLICT_IGNORE);

        //Insert row in source_records
        ContentValues sourceRecordValues = new ContentValues();
        sourceRecordValues.put(StatsContract.SourceRecords.COLUMN_NAME_SOURCE, source);
        sourceRecordValues.put(StatsContract.SourceRecords.COLUMN_NAME_PARAMETER, parameter);
        long source_record_id = statsDb.insert(StatsContract.SourceRecords.TABLE_NAME,null,sourceRecordValues);

        //Update recent frequency of this source
        int N = 10; //Number of last records we scan //TODO - make this number generic
        statsDb.execSQL("UPDATE " + StatsContract.SourceStats.TABLE_NAME + " SET " + StatsContract.SourceStats.COLUMN_NAME_RECENT_FREQUENCY +
                "=(SELECT COUNT() FROM " +
                "(SELECT * FROM " + StatsContract.SourceRecords.TABLE_NAME +
                " ORDER BY " + StatsContract.SourceRecords._ID + " DESC LIMIT " + N + ")" +
                " GROUP BY " + StatsContract.SourceRecords.COLUMN_NAME_SOURCE + ", " + StatsContract.SourceRecords.COLUMN_NAME_PARAMETER +
                " HAVING " + StatsContract.SourceRecords.COLUMN_NAME_SOURCE + "='" + source + "' AND "
                + StatsContract.SourceRecords.COLUMN_NAME_PARAMETER + "='" + parameter + "') " +
                "WHERE " + StatsContract.SourceStats.COLUMN_NAME_SOURCE + "='" + source + "' AND " + StatsContract.SourceStats.COLUMN_NAME_PARAMETER + "='" + parameter + "'");

        statsDb.close();

    }

    public void updateLastPosition(String source, String parameter, int position)
    {
        Log.d(TAG,"updateLastPosition()");

        //Get database
        SQLiteDatabase statsDb = getWritableDatabase();

        statsDb.execSQL("UPDATE " + StatsContract.SourceStats.TABLE_NAME + " SET " + StatsContract.SourceStats.COLUMN_NAME_LAST_POSITION + "=" + position +
                " WHERE " + StatsContract.SourceStats.COLUMN_NAME_SOURCE + "='" + source + "' AND " + StatsContract.SourceStats.COLUMN_NAME_PARAMETER + "='" + parameter + "'");

        statsDb.close();
    }
}