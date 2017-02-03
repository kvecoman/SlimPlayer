package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.media.MediaMetadataRetriever;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.module.GlideModule;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Glide related class to load embedded album art from mp3 files
 *
 * Created by miroslav on 02.02.17..
 */

public class EmbeddedArtModule implements GlideModule
{
    @Override
    public void applyOptions( Context context, GlideBuilder builder){}

    @Override
    public void registerComponents( Context context, Glide glide)
    {
        glide.register( EmbeddedArtGlide.class, InputStream.class, new EmbeddedArtLoader.Factory() );
    }
}

class EmbeddedArtLoader implements StreamModelLoader<EmbeddedArtGlide>
{
    @Override
    public DataFetcher<InputStream> getResourceFetcher( EmbeddedArtGlide model, int width, int height )
    {
        return new EmbeddedArtFetcher(model);
    }

    static class Factory implements ModelLoaderFactory<EmbeddedArtGlide, InputStream>
    {
        @Override
        public ModelLoader<EmbeddedArtGlide, InputStream> build( Context context, GenericLoaderFactory factories )
        {
            return new EmbeddedArtLoader();
        }

        @Override
        public void teardown() {}
    }
}

class EmbeddedArtFetcher implements DataFetcher<InputStream>
{
    private final EmbeddedArtGlide model;

    public EmbeddedArtFetcher(EmbeddedArtGlide model)
    {
        this.model = model;
    }

    @Override
    public String getId()
    {
        return model.mPath;
    }

    @Override
    public InputStream loadData( Priority priority ) throws Exception
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try
        {
            retriever.setDataSource( model.mPath );
            byte[] picture = retriever.getEmbeddedPicture();
            if (picture != null)
            {
                return new ByteArrayInputStream( picture );
            }
            else
            {
                return null;
            }
        }
        finally
        {
            retriever.release();
        }
    }

    @Override
    public void cleanup()
    {

    }

    @Override
    public void cancel()
    {

    }
}