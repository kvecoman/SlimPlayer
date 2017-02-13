package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Fragment used for displaying of different categories like all genres, all abums etc.
 *
 * @author Miroslav Mihaljević
 */
public class CategoryRecyclerFragment extends SlimRecyclerFragment {



    public CategoryRecyclerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onClick(View v) {


        Intent intent;
        String parameter;
        String displayName;
        int position;
        Context context;

        context = getContext();
        position = mRecyclerView.getChildLayoutPosition(v);
        parameter = mAdapter.getMediaItemsList().get(position).getMediaId();
        displayName = mAdapter.getMediaItemsList().get(position).getDescription().getTitle().toString();

        if (mSelectSongsForResult)
        {
            intent = new Intent(PlaylistSongsRecyclerFragment.ACTION_SELECT_SONGS, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,getContext(),SongListActivity.class);

            //Choose bundle and send it to songList fragment
            intent.putExtra( Const.SOURCE_KEY, mSource );
            intent.putExtra( Const.PARAMETER_KEY, parameter);
            intent.putExtra( Const.DISPLAY_NAME, displayName );
            intent.putExtra(SlimActivity.REQUEST_CODE_KEY,PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2);

            //We let the hosting activity to handle results of selecting songs
            if (context instanceof AppCompatActivity)
                ((AppCompatActivity)context).startActivityForResult(intent, PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2);
        }
        else
        {
            //If we are in normal mode just start activity
            intent = new Intent(context,SongListActivity.class);


            //Choose bundle and send it to songList fragment
            intent.putExtra( Const.SOURCE_KEY, mSource );
            intent.putExtra( Const.PARAMETER_KEY, parameter);
            intent.putExtra( Const.DISPLAY_NAME, displayName );

            //Start next screen
            startActivity(intent);
        }

    }

    //We don't use long click here, but must implement it
    @Override
    public boolean onLongClick(View v) {
        return false;
    }


}
