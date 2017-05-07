package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Activity that is used to display SongListFragment after one of categories is selected from CategoryListFragment
 *
 * @author Miroslav Mihaljević
 */


public class SongListActivity extends SelectSongsActivity {
    protected final String TAG = getClass().getSimpleName();


    private String mSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_holder);

        Fragment    fragment;
        Bundle      args;
        String      displayName;
        ActionBar   actionBar;


        //Retrieve bundle intended to be sent with SlimListFragment
        args = getIntent().getExtras();


        //Check if everything is okay with bundle
        if (args == null || !getIntent().hasExtra( Const.SOURCE_KEY))
        {
            Log.i(TAG, "onCreate() - Could not load data list fragment, loading empty one instead");
            //If something is wrong with bundle just show empty fragment
            fragment    = new EmptyMessageFragment();
            args        = new Bundle();
            args.putString(EmptyMessageFragment.MESSAGE_KEY,getString(R.string.empty_songlist));
        }
        else
        {
            mSource = args.getString( Const.SOURCE_KEY );

            //Here we set display name to action bar
            displayName = args.getString( Const.DISPLAY_NAME );
            actionBar = getSupportActionBar();
            if ( actionBar != null )
            {
                actionBar.setTitle( displayName );
                actionBar.setDisplayHomeAsUpEnabled( true );
            }

            //If we are opening playlist then load PlaylistSongsFragment
            if ( TextUtils.equals( mSource, getString( R.string.pref_key_playlists_screen ) ) )
                fragment = new PlaylistSongsRecyclerFragment();
            else
                fragment = new SongRecyclerFragment();
        }





        fragment.setArguments( args );



        getSupportFragmentManager() .beginTransaction()
                                    .replace( R.id.fragment_container, fragment )
                                    .commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //If we are in normal mode and this is playlist songs screen then inflate playlist songs menu
        if ( !mSelectSongsForResult && TextUtils.equals(mSource,getString(R.string.pref_key_playlists_screen)) )
            getMenuInflater().inflate( R.menu.playlist_songs_menu, menu );



        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        int id;

        id = item.getItemId();

        switch ( id )
        {
            case android.R.id.home:
                goUp();
                break;
            default:
                return super.onOptionsItemSelected( item );
        }

        return true;
    }


    private void goUp()
    {
        Intent intent;

        intent = NavUtils.getParentActivityIntent( this );


        finish();
        startActivity( intent );
    }
}
