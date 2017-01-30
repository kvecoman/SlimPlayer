package mihaljevic.miroslav.foundry.slimplayer;

/**
 * Created by Miroslav on 16.1.2017..
 */

public final class Const {
    //Keys that are used when transferring data about different screens
    public static final String SOURCE_KEY = "source"; //Which screen, like all songs, genres etc
    public static final String PARAMETER_KEY = "parameter"; //ID of playlist, or artist, something like that
    public static final String URI_KEY = "uri";
    public static final String PROJECTION_KEY = "projection";
    public static final String SELECTION_KEY = "selection";
    public static final String SELECTION_ARGS_KEY = "selection_args";
    public static final String SORT_ORDER_KEY = "sort_order";
    public static final String DISPLAY_FIELD_KEY = "display_field";

    public static final String METADATA_KEY = "metadata";

    public static final String POSITION_KEY = "position";

    private Const(){}

    public static final String HOME_SCREEN = "home_screen";
    public static final String ALL_SCREEN = "all_screen";
    public static final String PLAYLISTS_SCREEN = "playlists_screen";
    public static final String ALBUMS_SCREEN = "albums_screen";
    public static final String ARTISTS_SCREEN = "artists_screen";
    public static final String GENRES_SCREEN = "genres_screen";

    public static final String [] SCREENS = new String[] {HOME_SCREEN, ALL_SCREEN, PLAYLISTS_SCREEN, ALBUMS_SCREEN, ARTISTS_SCREEN, GENRES_SCREEN};

    public static final String FILE_URI_KEY = "file_uri";
}
