package mihaljevic.miroslav.foundry.slimplayer;

import android.provider.BaseColumns;

/**
 * Created by Miroslav on 21.12.2016..
 */

public final class StatsContract {

    private StatsContract(){}

    //Table that will keep data about how much each source has been played (invoked?) recently
    public static class SourceStats implements BaseColumns
    {
        public static final String TABLE_NAME = "source_stats";

        public static final String COLUMN_NAME_SOURCE = "source";
        public static final String COLUMN_NAME_PARAMETER = "parameter";
        public static final String COLUMN_NAME_LAST_POSITION = "last_position";
        public static final String COLUMN_NAME_RECENT_FREQUENCY = "recent_frequency";
        public static final String COLUMN_NAME_LAST_PLAY = "last_play";

        //TODO - varchar instead of text?
        public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME +
                                                " (" + _ID + " INTEGER PRIMARY KEY," +
                                                COLUMN_NAME_SOURCE + " TEXT," +
                                                COLUMN_NAME_PARAMETER + " TEXT," +
                                                COLUMN_NAME_LAST_POSITION + " INTEGER," +
                                                COLUMN_NAME_RECENT_FREQUENCY + " INTEGER," +
                                                COLUMN_NAME_LAST_PLAY + " TIMESTAMP)";

        public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class SourceRecords implements BaseColumns
    {
        public static final String TABLE_NAME = "source_records";

        public static final String COLUMN_NAME_SOURCE_STATS_ID = "source_stats_id";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME +
                                                " (" + _ID + " INTEGER PRIMARY KEY," +
                                                COLUMN_NAME_SOURCE_STATS_ID + " INTEGER," +
                                                COLUMN_NAME_TIME + " TIMESTAMP)";

        public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;


    }


}
