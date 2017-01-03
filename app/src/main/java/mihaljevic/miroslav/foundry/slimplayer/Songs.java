package mihaljevic.miroslav.foundry.slimplayer;

import android.graphics.Bitmap;

/**
 * Created by Miroslav on 18.12.2016..
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
