package mihaljevic.miroslav.foundry.slimplayer;

import android.support.v4.media.MediaMetadataCompat;

/**
 * Wrapper class for MediaMetadataCompat that adds some database specific members like Artist and Album ID
 *
 * @author Miroslav Mihaljević
 */


@Deprecated
public class SlimMetadata {

    public MediaMetadataCompat mMetadata;

    private String mAlbumId;

    private String mArtistId;

    public SlimMetadata(MediaMetadataCompat metadata, String albumId, String artistId) {
        this.mMetadata = metadata;
        this.mAlbumId = albumId;
        this.mArtistId = artistId;
    }

    public MediaMetadataCompat getMetadata() {
        return mMetadata;
    }

    public void setMetadata(MediaMetadataCompat metadata) {
        this.mMetadata = mMetadata;
    }

    public String getAlbumId() {
        return mAlbumId;
    }

    public void setAlbumId(String albumId) {
        this.mAlbumId = mAlbumId;
    }

    public String getArtistId() {
        return mArtistId;
    }

    public void setArtistId(String artistId) {
        this.mArtistId = mArtistId;
    }
}
