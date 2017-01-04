package mihaljevic.miroslav.foundry.slimplayer;

import android.graphics.Bitmap;

/**
 * Interface that represents list of songs to use for playback purposes
 *
 * @author Miroslav Mihaljević
 */

public interface Songs {

    int getCount();

    long getId(int position);
    String getTitle(int position);
    String getArtist(int position);
    String getAlbum(int position);
    long getDuration(int position);
    String getData(int position);

    Bitmap getArt(int position);


}
