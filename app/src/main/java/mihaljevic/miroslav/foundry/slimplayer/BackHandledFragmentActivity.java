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

    protected BackHandledListFragment mBackHandledListFragment;

    @Override
    public void setBackHandledFragment(BackHandledListFragment backHandledFragment) {
        mBackHandledListFragment = backHandledFragment;
    }

    @Override
    public void onBackPressed() {

        //TODO - this "if" might generate null-pointer exception
        if (mBackHandledListFragment == null || !mBackHandledListFragment.onBackPressed())
        {
            //Back press was not consumed, so we let system to do what it wants
            super.onBackPressed();
        }

    }

    //We check if the fragment has been set already
    public boolean isSelectedFragmentNull()
    {
        return mBackHandledListFragment == null;
    }
}
