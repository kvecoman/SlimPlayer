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
 * <p>
 * Created by Miroslav on 02.02.17..
 */

public class EmbeddedArtGlide
{
    final String mPath;

    public EmbeddedArtGlide( String path )
    {
        mPath = path;
    }
}




