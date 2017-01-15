package mihaljevic.miroslav.foundry.slimplayer;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class TestActivity extends SlimActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_slim_recycler);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                    new String[]{MediaStore.Audio.Media.TITLE,
                                                            MediaStore.Audio.Media.ALBUM,
                                                            MediaStore.Audio.Media.ALBUM_ID,
                                                            MediaStore.Audio.Media.ARTIST,
                                                            MediaStore.Audio.Media.ARTIST_ID},
                                                    null,
                                                    null,
                                                    MediaStore.Audio.Media.TITLE + " ASC");

        CursorRecyclerAdapter adapter = new CursorRecyclerAdapter(this,
                                        cursor,R.layout.test_recycler_item,
                                        new String[]{MediaStore.Audio.Media.TITLE,
                                                MediaStore.Audio.Media.ALBUM,
                                                MediaStore.Audio.Media.ALBUM_ID,
                                                MediaStore.Audio.Media.ARTIST,
                                                MediaStore.Audio.Media.ARTIST_ID},
                                        null,null);

        recyclerView.setAdapter(adapter);

        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
    }
}
