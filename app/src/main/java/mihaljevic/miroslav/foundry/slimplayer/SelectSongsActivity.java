package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static mihaljevic.miroslav.foundry.slimplayer.PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY;
import static mihaljevic.miroslav.foundry.slimplayer.PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST;
import static mihaljevic.miroslav.foundry.slimplayer.PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2;

/**
 *
 *
 */

public abstract class SelectSongsActivity extends BackHandledFragmentActivity {

    //Key used to identify whether the user has finished selecting and it is time to exit all selection activities
    public static final String SELECTING_FINISHED_KEY = "selecting_finished";

    //Are we selecting songs for playlists
    protected boolean       mSelectSongsForResult;
    protected Set<String>   mSelectedSongIdsList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Check if we have started this activity only to select songs
        if ( TextUtils.equals(getIntent().getAction(),PlaylistSongsRecyclerFragment.ACTION_SELECT_SONGS))
        {
            mSelectSongsForResult   = true;
            mSelectedSongIdsList    = new HashSet<>();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {

        if ( mSelectSongsForResult )
        {
            //If we are selecting songs then add special options for it
            getMenuInflater().inflate( R.menu.select_songs_menu, menu );
        }

        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {

        int     id;
        Intent  intent;
        int     requestCode;

        id              = item.getItemId();
        intent          = new Intent();
        requestCode     = getIntent().getIntExtra( SlimActivity.REQUEST_CODE_KEY, SELECT_SONGS_REQUEST_2 );

        switch ( id )
        {
            //If the user user is finished with selecting songs
            case R.id.accept_selected_songs:


                intent.putStringArrayListExtra  ( SELECTED_SONGS_KEY, new ArrayList<>( mSelectedSongIdsList ) );
                intent.putExtra                 ( SelectSongsActivity.SELECTING_FINISHED_KEY, true);

                setResult( requestCode, intent );
                finish();

                break;
            case R.id.cancel_selected_songs:

                //We send signal that we want to finish whole proccess of selecting songs
                intent.putExtra ( SelectSongsActivity.SELECTING_FINISHED_KEY, true );
                setResult       ( requestCode, intent);
                finish();
                break;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onBackPressed()
    {


        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult( requestCode, resultCode, data );

        Intent          intent;
        int             currentRequestCode;
        List<String>    songIDs;


        //If we canceled playlist selecting just finish the whole thing
        if (requestCode == PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2
                && data != null
                && data.getBooleanExtra(SelectSongsActivity.SELECTING_FINISHED_KEY, false)
                && !data.hasExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY))
            finish();

        //REQUEST CODE 2 case - we are still selecting but we just take IDs that were selected in forResult activity
        if ( data != null && requestCode == PlaylistSongsRecyclerFragment.SELECT_SONGS_REQUEST_2 && data.hasExtra( PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY ) )
        {

            //Add all selected IDs to existing song ID collection
            songIDs = data.getStringArrayListExtra(PlaylistSongsRecyclerFragment.SELECTED_SONGS_KEY);

            for (String songID : songIDs)
            {
                mSelectedSongIdsList.add( songID );
            }

            //Check if we need to close this activity right now (this is if user confirmed his selection)
            if (data.hasExtra(SelectSongsActivity.SELECTING_FINISHED_KEY))
            {
                currentRequestCode = getIntent().getIntExtra(SlimActivity.REQUEST_CODE_KEY, SELECT_SONGS_REQUEST);

                intent = new Intent();
                intent.putStringArrayListExtra  ( SELECTED_SONGS_KEY, new ArrayList<>( mSelectedSongIdsList ) );
                intent.putExtra                 ( SelectSongsActivity.SELECTING_FINISHED_KEY,true );

                setResult( currentRequestCode, intent );
                finish();
            }
        }

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
