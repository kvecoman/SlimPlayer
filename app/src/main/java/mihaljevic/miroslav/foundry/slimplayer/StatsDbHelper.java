package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by Miroslav on 21.12.2016..
 *
 * Helper class to communicate with stats database
 *
 * @author Miroslav Mihaljević
 */

public class StatsDbHelper extends SQLiteOpenHelper {
    protected final String TAG = getClass().getSimpleName();

    public static final int DATABASE_VERSION = 9;
    public static final String DATABASE_NAME = "Stats.db";

    private Context mContext;

    private static StatsDbHelper sInstance;

    public static StatsDbHelper getInstance(Context context)
    {

        if (sInstance == null)
        {
            Log.d("StatsDbHelper","New instance created");
            sInstance = new StatsDbHelper(context);
        }

        return sInstance;
    }

    public static void closeInstance()
    {
        Log.v("StatsDbHelper","closeInstance()");
        if (sInstance != null)
        {
            sInstance.close();
            sInstance = null;
        }
    }

    private StatsDbHelper(Context context)
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


    //TODO - what happens if database is reconstructed and id's for sources are different (it will be reset after N list changes)
    public void updateStats(final String source,final String parameter)
    {
        Log.v(TAG,"updateStats()");

        final int N = 10; //Number of last records we scan //TODO - make this number generic

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                //Get database
                SQLiteDatabase statsDb = getWritableDatabase();

                //Try to insert row with this source and parameter in source_stats (if it exists it wont be done)
                ContentValues sourceStatsValues = new ContentValues();
                sourceStatsValues.put(StatsContract.SourceStats.COLUMN_NAME_SOURCE,source);
                sourceStatsValues.put(StatsContract.SourceStats.COLUMN_NAME_PARAMETER,parameter);
                sourceStatsValues.put(StatsContract.SourceStats.COLUMN_NAME_DISPLAY_NAME, Utils.getDisplayName(source,parameter));
                statsDb.insertWithOnConflict(StatsContract.SourceStats.TABLE_NAME,null,sourceStatsValues,SQLiteDatabase.CONFLICT_IGNORE);

                //Insert row in source_records
                ContentValues sourceRecordValues = new ContentValues();
                sourceRecordValues.put(StatsContract.SourceRecords.COLUMN_NAME_SOURCE, source);
                sourceRecordValues.put(StatsContract.SourceRecords.COLUMN_NAME_PARAMETER, parameter);
                statsDb.insert(StatsContract.SourceRecords.TABLE_NAME,null,sourceRecordValues);

                //Delete old obsolete values from source records
                statsDb.execSQL("DELETE FROM " + StatsContract.SourceRecords.TABLE_NAME + " WHERE " +
                        StatsContract.SourceRecords._ID + " NOT IN (" + "SELECT " + StatsContract.SourceRecords._ID + " FROM " +
                        "(SELECT " + StatsContract.SourceRecords._ID + " FROM " + StatsContract.SourceRecords.TABLE_NAME + " ORDER BY " + StatsContract.SourceRecords._ID + " DESC LIMIT " + N + ") )");

                //Update recent frequency of this source
                statsDb.execSQL("UPDATE " + StatsContract.SourceStats.TABLE_NAME + " SET " + StatsContract.SourceStats.COLUMN_NAME_RECENT_FREQUENCY +
                        "=(SELECT COUNT() FROM " +
                        "(SELECT * FROM " + StatsContract.SourceRecords.TABLE_NAME +
                        " ORDER BY " + StatsContract.SourceRecords._ID + " DESC LIMIT " + N + ")" +
                        " GROUP BY " + StatsContract.SourceRecords.COLUMN_NAME_SOURCE + ", " + StatsContract.SourceRecords.COLUMN_NAME_PARAMETER +
                        " HAVING " + StatsContract.SourceRecords.COLUMN_NAME_SOURCE + "='" + source + "' AND "
                        + StatsContract.SourceRecords.COLUMN_NAME_PARAMETER + "='" + parameter + "') " +
                        "WHERE " + StatsContract.SourceStats.COLUMN_NAME_SOURCE + "='" + source + "' AND " + StatsContract.SourceStats.COLUMN_NAME_PARAMETER + "='" + parameter + "'");

                statsDb.close();

                return null;
            }
        }.execute();



    }

    public void updateLastPosition(final String source, final String parameter, final int position)
    {
        Log.v(TAG,"updateLastPosition()");

        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                //Get database
                SQLiteDatabase statsDb = getWritableDatabase();

                statsDb.execSQL("UPDATE " + StatsContract.SourceStats.TABLE_NAME + " SET " + StatsContract.SourceStats.COLUMN_NAME_LAST_POSITION + "=" + position +
                        " WHERE " + StatsContract.SourceStats.COLUMN_NAME_SOURCE + "='" + source + "' AND " + StatsContract.SourceStats.COLUMN_NAME_PARAMETER + "='" + parameter + "'");

                statsDb.close();

                return null;
            }
        }.execute();


    }
}
