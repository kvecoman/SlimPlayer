package mihaljevic.miroslav.foundry.slimplayer;

import android.support.v7.app.AppCompatActivity;

/**
 * Activity used to host fragments that need to handle back button press
 *
 * @author Miroslav Mihaljević
 *
 *
 */

public class BackHandledFragmentActivity extends AppCompatActivity implements BackHandledListFragment.BackHandlerInterface {

    //Fragment which needs to handle back button press
    protected BackHandledListFragment mBackHandledListFragment;

    //Setter for BackHandled fragment
    @Override
    public void setBackHandledFragment(BackHandledListFragment backHandledFragment) {
        mBackHandledListFragment = backHandledFragment;
    }

    @Override
    public void onBackPressed() {

        //Allow fragment to handle back button press
        if (mBackHandledListFragment == null || !mBackHandledListFragment.onBackPressed())
        {
            //Back press was not consumed, so we let system to do what it wants
            super.onBackPressed();
        }
    }

}
