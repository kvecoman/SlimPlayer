package mihaljevic.miroslav.foundry.slimplayer;

import android.provider.BaseColumns;

/**
 * Created by Miroslav on 21.12.2016..
 *
 * Scheme for database that is used to store song playng statistics to present user with most listened lists
 *
 * @author Miroslav Mihaljević
 */

public final class StatsContract {

    private StatsContract(){}

    //Table that will keep data about how much each source has been played (invoked?) recently
    public static class SourceStats implements BaseColumns
    {
        public static final String TABLE_NAME = "source_stats";

        public static final String COLUMN_NAME_SOURCE           = "source";
        public static final String COLUMN_NAME_PARAMETER        = "parameter";
        public static final String COLUMN_NAME_DISPLAY_NAME     = "display_name";
        public static final String COLUMN_NAME_LAST_POSITION    = "last_position";
        public static final String COLUMN_NAME_RECENT_FREQUENCY = "recent_frequency";
        public static final String COLUMN_NAME_LAST_PLAY        = "last_play";


        public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME +
                                                " (" +
                                                COLUMN_NAME_SOURCE + " TEXT," +
                                                COLUMN_NAME_PARAMETER + " TEXT," +
                                                COLUMN_NAME_DISPLAY_NAME + " TEXT," +
                                                COLUMN_NAME_LAST_POSITION + " INTEGER DEFAULT 0," +
                                                COLUMN_NAME_RECENT_FREQUENCY + " INTEGER DEFAULT 0," +
                                                COLUMN_NAME_LAST_PLAY + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                                "PRIMARY KEY (" + COLUMN_NAME_SOURCE + ", " + COLUMN_NAME_PARAMETER + "))";

        public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static class SourceRecords implements BaseColumns
    {
        public static final String TABLE_NAME = "source_records";

        public static final String COLUMN_NAME_SOURCE       = "source";
        public static final String COLUMN_NAME_PARAMETER    = "parameter";
        public static final String COLUMN_NAME_TIME         = "time";

        public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME +
                                                " (" + _ID + " INTEGER PRIMARY KEY," +
                                                COLUMN_NAME_SOURCE + " TEXT," +
                                                COLUMN_NAME_PARAMETER + " TEXT," +
                                                COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                                " FOREIGN KEY(" + COLUMN_NAME_SOURCE + "," + COLUMN_NAME_PARAMETER + ")" +
                                                " REFERENCES " + SourceStats.TABLE_NAME + "(" + SourceStats.COLUMN_NAME_SOURCE + "," + SourceStats.COLUMN_NAME_PARAMETER + ")" +
                                                "  )";

        public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;


    }


}
