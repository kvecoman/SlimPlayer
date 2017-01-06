package mihaljevic.miroslav.foundry.slimplayer;

/**
 * Created by Miroslav on 12.11.2016..
 *
 * Class that represent a song that can be played
 *
 * @author Miroslav Mihaljević
 */
@Deprecated
public class Song {

    private long mId;
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    private long mDuration;
    private String mData;

    public Song(long mId, String mTitle, String mArtist, String mAlbum, long mDuration, String mData) {
        //Just assign everything
        this.mId = mId;
        this.mTitle = mTitle;
        this.mArtist = mArtist;
        this.mAlbum = mAlbum;
        this.mDuration = mDuration;
        this.mData = mData;
    }

    public long getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getArtist() {
        return mArtist;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public long getDuration() {
        return mDuration;
    }

    public String getData() {
        return mData;
    }
}
