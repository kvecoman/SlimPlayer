package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Miroslav on 21.12.2016..
 *
 * Helper class to communicate with stats database
 *
 *
 *
 * @author Miroslav Mihaljević
 */

public class StatsDbHelper extends SQLiteOpenHelper {
    protected final String TAG = getClass().getSimpleName();

    public static final int DATABASE_VERSION = 9;
    public static final String DATABASE_NAME = "Stats.db";



    private static StatsDbHelper sInstance;

    public synchronized static StatsDbHelper getInstance()
    {

        if (sInstance == null)
        {
            Log.d("StatsDbHelper","New instance created");
            sInstance = new StatsDbHelper();
        }

        return sInstance;
    }

    public synchronized static void closeInstance()
    {
        Log.v("StatsDbHelper","closeInstance()");
        if (sInstance != null)
        {
            sInstance.close();
            sInstance = null;
        }
    }

    private StatsDbHelper()
    {
        super( SlimPlayerApplication.getInstance(), DATABASE_NAME, null, DATABASE_VERSION );

    }

    @Override
    public void onCreate( SQLiteDatabase db )
    {
        //Create all tables in database
        db.execSQL( StatsContract.SourceStats.SQL_CREATE );
        db.execSQL( StatsContract.SourceRecords.SQL_CREATE );
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion )
    {
        db.execSQL( StatsContract.SourceStats.SQL_DROP );
        db.execSQL( StatsContract.SourceRecords.SQL_DROP );
        onCreate( db );
    }



    public void updateStatsAsync( final String source, @Nullable final String parameter, @Nullable final String displayName )
    {
        new AsyncTask<Void,Void,Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {

                updateStats( source, parameter, displayName );

                return null;
            }
        }.execute();
    }

    public synchronized void updateStats( final String source, @Nullable final String parameter, @Nullable final String displayName )
    {
        Log.v(TAG,"updateStats()");

        //Get database
        SQLiteDatabase statsDb;
        ContentValues sourceStatsValues;
        ContentValues sourceRecordValues;

        statsDb = getWritableDatabase();

        if ( statsDb == null || !statsDb.isOpen() )
            return;

        //Try to insert row with this source and parameter in source_stats (if it exists it wont be done)
        sourceStatsValues = new ContentValues();
        sourceStatsValues.put( StatsContract.SourceStats.COLUMN_NAME_SOURCE, source );
        sourceStatsValues.put( StatsContract.SourceStats.COLUMN_NAME_PARAMETER, parameter );
        sourceStatsValues.put( StatsContract.SourceStats.COLUMN_NAME_DISPLAY_NAME, displayName == null ? Utils.getDisplayName(source,parameter) : displayName );
        statsDb.insertWithOnConflict( StatsContract.SourceStats.TABLE_NAME, null, sourceStatsValues, SQLiteDatabase.CONFLICT_IGNORE) ;

        //Insert row in source_records
        sourceRecordValues = new ContentValues();
        sourceRecordValues.put( StatsContract.SourceRecords.COLUMN_NAME_SOURCE, source);
        sourceRecordValues.put( StatsContract.SourceRecords.COLUMN_NAME_PARAMETER, parameter);
        statsDb.insert( StatsContract.SourceRecords.TABLE_NAME, null, sourceRecordValues );

        //Delete old obsolete values from source records
        statsDb.execSQL( "DELETE FROM " + StatsContract.SourceRecords.TABLE_NAME +
                        " WHERE " + StatsContract.SourceRecords._ID + " NOT IN (" +
                                "SELECT " + StatsContract.SourceRecords._ID + " FROM " +
                                    "(SELECT " + StatsContract.SourceRecords._ID + " FROM " + StatsContract.SourceRecords.TABLE_NAME +
                                    " ORDER BY " + StatsContract.SourceRecords._ID +
                                    " DESC LIMIT " + Const.STATS_RECORDS_SCANNED +
                        ") )");

        //Update recent frequency of this source
        statsDb.execSQL(    "UPDATE " + StatsContract.SourceStats.TABLE_NAME +
                            " SET " + StatsContract.SourceStats.COLUMN_NAME_RECENT_FREQUENCY +
                                "=(SELECT COUNT() FROM " +
                                    "(SELECT * FROM " + StatsContract.SourceRecords.TABLE_NAME +
                                    " ORDER BY " + StatsContract.SourceRecords._ID + " DESC LIMIT " + Const.STATS_RECORDS_SCANNED + ")" +
                                " GROUP BY " + StatsContract.SourceRecords.COLUMN_NAME_SOURCE + ", " + StatsContract.SourceRecords.COLUMN_NAME_PARAMETER +
                                " HAVING " + StatsContract.SourceRecords.COLUMN_NAME_SOURCE + "='" + source + "' AND "
                                + StatsContract.SourceRecords.COLUMN_NAME_PARAMETER + "='" + parameter + "') " +
                            "WHERE " + StatsContract.SourceStats.COLUMN_NAME_SOURCE + "='" + source + "' AND " + StatsContract.SourceStats.COLUMN_NAME_PARAMETER + "='" + parameter + "'" );

        statsDb.close();
    }

    public void updateLastPositionAsync( final String source, final String parameter, final int position )
    {
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params)
            {
                updateLastPosition( source, parameter, position );

                return null;
            }
        }.execute();
    }

    public synchronized void updateLastPosition( final String source, final String parameter, final int position )
    {
        Log.v(TAG,"updateLastPosition()");

        //Get database
        SQLiteDatabase statsDb;

        if (source == null)
            return;

        statsDb = getWritableDatabase();

        statsDb.execSQL("UPDATE " + StatsContract.SourceStats.TABLE_NAME +
                " SET " + StatsContract.SourceStats.COLUMN_NAME_LAST_POSITION + "=" + position +
                " WHERE " + StatsContract.SourceStats.COLUMN_NAME_SOURCE + "='" + source + "' AND " + StatsContract.SourceStats.COLUMN_NAME_PARAMETER + "='" + parameter + "'");

        statsDb.close();
    }
}
