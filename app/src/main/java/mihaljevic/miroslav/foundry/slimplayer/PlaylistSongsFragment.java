package mihaljevic.miroslav.foundry.slimplayer;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment that displays songs from selected playlist and adds option to add songs to it.
 *
 * @author Miroslav Mihaljević
 */
public class PlaylistSongsFragment extends SongListFragment {


    public PlaylistSongsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return super.onCreateView(inflater, container,savedInstanceState);
    }

    /*@Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.playlist_songs_menu,menu);

    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mSelectMode)
        {
            if (item.getItemId() == R.id.playlist_add_to_this) {
                //If we have not selected anything, then we run MainActivity for result
                Toast.makeText(getContext(), "Start MainActivity for result", Toast.LENGTH_SHORT).show();
                //TODO - continue here - start MainActivity for result to get songs to add to playlist
            }
        }
        else
        {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
