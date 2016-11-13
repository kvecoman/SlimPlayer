package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Bundle;
import android.view.Menu;

/**
 * Activity that is used to display SongListFragment after one of categories is selected from CategoryListFragment
 *
 * @author Miroslav Mihaljević
 */

public class SongListActivity extends BackHandledFragmentActivity {

    //Key for bundle that is intended to be sent with SlimListFragmet
    public static final String FRAGMENT_BUNDLE_KEY = "fragment_bundle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        //Retrive bundle intended to be sent with SlimListFragment
        Bundle fragmentBundle = getIntent().getBundleExtra(FRAGMENT_BUNDLE_KEY);

        if (fragmentBundle != null)
        {
            //If there is bundle for fragment then create that fragment and add it to container
            SongListFragment fragment = new SongListFragment();
            fragment.setArguments(fragmentBundle);

            getSupportFragmentManager().beginTransaction().replace(R.id.list_fragment_container,fragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }
}
