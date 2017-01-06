package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 */

public abstract class SelectSongsActivity extends BackHandledFragmentActivity {

    //Are we selecting songs for playlists
    protected boolean mSelectSongsForResult;
    protected Set<String> mSelectedSongIdsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Check if we have started this activity only to select songs
        if ( Utils.equalsIncludingNull(getIntent().getAction(),PlaylistSongsRecyclerFragment.ACTION_SELECT_SONGS))
        {
            mSelectSongsForResult = true;
            mSelectedSongIdsList = new HashSet<>();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (mSelectSongsForResult)
        {
            //If we are selecting songs then add special options for it
            getMenuInflater().inflate(R.menu.select_songs_menu,menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id)
        {
            //If the user user is finished with selecting songs
            case R.id.accept_selected_songs:
                if (mSelectedSongIdsList.size() > 0)
                {
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY,new ArrayList<>(mSelectedSongIdsList));
                    setResult(getIntent().getIntExtra(SlimActivity.REQUEST_CODE_KEY,PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2),intent);
                    finish();
                }
                else
                {
                    finish();
                }
                break;
            case R.id.cancel_selected_songs:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        /*if (mSelectSongsForResult)
        {
            //If user pressed back but has songs selected then we will set that as result
            if (mSelectedSongIdsList.size() > 0) {
                Intent intent = new Intent();
                intent.putStringArrayListExtra(PlaylistSongsFragment.SELECTED_SONGS_KEY,(ArrayList) mSelectedSongIdsList);
                setResult(getIntent().getIntExtra(SlimActivity.REQUEST_CODE_KEY, PlaylistSongsFragment.SELECT_SONGS_REQUEST_2), intent);
                finish();
            } else {
                finish();
            }
        }*/
    }

    public boolean isSelectSongsForResult()
    {
        return mSelectSongsForResult;
    }

    public Set<String> getSelectedSongsList()
    {
        return mSelectedSongIdsList;
    }
}
