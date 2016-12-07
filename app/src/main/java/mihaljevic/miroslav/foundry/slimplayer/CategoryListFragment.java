package mihaljevic.miroslav.foundry.slimplayer;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

/**
 * Fragment that displays categories/playlists and opens appropriate song lists
 *
 * @author Miroslav Mihaljević
 */

public class CategoryListFragment extends SlimListFragment {


    public CategoryListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        //Get cursor and move it to current position
        Cursor cursor = mCursorAdapter.getCursor();
        cursor.moveToPosition(position);

        Intent intent = new Intent(mContext,SongListActivity.class);

        //Choose bundle and send it to songList fragment
        intent.putExtra(    SongListActivity.FRAGMENT_BUNDLE_KEY,
                            ScreenBundles.getBundleForSubScreen( mCurrentScreen, cursor, mContext ));
        //Start next screen
        startActivity(intent);

    }


    @Override
    public boolean onBackPressed() {
        return false;
    }
}
