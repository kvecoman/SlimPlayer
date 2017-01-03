package mihaljevic.miroslav.foundry.slimplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;



/**
 * Created by Miroslav on 18.12.2016..
 *
 * A list of songs whose source is coming from files
 */

public class FileSongs implements Songs {


    private List<String> files;
    private MediaMetadataRetriever mRetriever;

    FileSongs()
    {
        files = new ArrayList<>();
        mRetriever = new MediaMetadataRetriever();
    }

    public void addFile(String path)
    {
        //Check if path is valid and then add it
        File file = new File(path);
        if (file.exists())
            files.add(path);
    }

    public void removeFile(String path)
    {
        files.remove(path);
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public long getId(int position) {
        return position;
    }

    @Override
    public String getTitle(int position) {
        if (files == null || position < 0 && position >= files.size() )
            return null;

        mRetriever.setDataSource(files.get(position));
        return mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
    }

    @Override
    public String getArtist(int position) {

        if (files == null || position < 0 && position >= files.size() )
            return null;

        mRetriever.setDataSource(files.get(position));
        return mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
    }

    @Override
    public String getAlbum(int position) {

        if (files == null || position < 0 && position >= files.size() )
            return null;

        mRetriever.setDataSource(files.get(position));
        return mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
    }

    @Override
    public long getDuration(int position) {
        if (files == null || position < 0 && position >= files.size() )
            return 0;

        mRetriever.setDataSource(files.get(position));
        return Long.parseLong(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
    }

    @Override
    public String getData(int position) {

        if (files == null || position < 0 && position >= files.size() )
            return null;

        return files.get(position);
    }

    @Override
    public Bitmap getArt(int position) {
        Bitmap bitmap = null;

        mRetriever.setDataSource(getData(position));

        InputStream inputStream = null;
        byte[] bytes = mRetriever.getEmbeddedPicture();
        if (bytes != null)
            inputStream = new ByteArrayInputStream(bytes);

        bitmap = BitmapFactory.decodeStream(inputStream);

        return bitmap;
    }
}
