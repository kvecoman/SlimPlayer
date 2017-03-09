package mihaljevic.miroslav.foundry.slimplayer;

/**
 * Created by miroslav on 07.03.17..
 */

public class TestData
{
    //GALAXY S2 TEST DATA WITH 530 of 1000 greatest hits of all time in folder TestSongs

    public static String    TEST_MUSIC_DIR                  = "/storage/sdcard0/Samsung/TestSongs/";
    public static String    TEST_MUSIC                      = "0804. Michael Jackson - Billy Jean.mp3";
    public static String    TEST_MUSIC_PATH                 = TEST_MUSIC_DIR + TEST_MUSIC;
    public static int       EXPECTED_POSITION               = 54;
    public static String    EXPECTED_AUDIO_ID               = "85060";
    public static String    EXPECTED_TITLE                  = "Billy Jean";
    public static String    EXPECTED_ARTIST                 = "Michael Jackson";
    public static String    EXPECTED_ALBUM                  = "M1+ Top 1000";
    public static String    EXPECTED_GENRE                  = "Oldies";
    public static int       EXPECTED_COUNT                  = 530;
    public static int       EXPECTED_SEEK                   = 50000;

    public static String    TEST_SOURCE                     = Const.GENRES_SCREEN;
    public static String    TEST_PARAMETER                  = "16";
    public static String    EXPECTED_PARENT_STRING          = TEST_SOURCE + ":" + TEST_PARAMETER;
    public static String    EXPECTED_DISPLAY_NAME           = "Oldies";

    public static String    TEST_PLAYLIST                   = "test";
    public static long      TEST_PLAYLIST_ID                = 88566;
    public static int       EXPECTED_POSITION_IN_PLAYLIST   = 1;

    public static String    CREATED_TEST_PLAYLIST           = "created_test_playlist";

    public static String    TEST_STRING                     = "String for test purposes";
}
