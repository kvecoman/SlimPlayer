package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Handler;
import android.widget.Toast;

/**
 * Activity used to host fragments that need to handle back button press
 *
 * @author Miroslav Mihaljević
 *
 *
 */

public abstract class BackHandledFragmentActivity extends SlimActivity implements BackHandledRecyclerFragment.BackHandlerInterface {

    //Fragment which needs to handle back button press
    protected BackHandledRecyclerFragment backHandledRecyclerFragment;

    //Used for confirmation whether the user really wants to leave the app
    protected boolean mBackPressedOnce = false;

    //Setter for BackHandled fragment
    @Override
    public void setBackHandledFragment(BackHandledRecyclerFragment backHandledFragment) {
        backHandledRecyclerFragment = backHandledFragment;
    }

    @Override
    public void onBackPressed() {

        //Allow fragment to handle back button press
        if (backHandledRecyclerFragment == null || !backHandledRecyclerFragment.onBackPressed())
        {
            //Back press was not consumed, so we let system to do what it wants

            //If this activity is first in stack tree, confirm that user really wants to exit
            if (isTaskRoot())
            {
                if (mBackPressedOnce)
                {
                    //If this is second time in 2 seconds that user pressed back then exit the app
                    super.onBackPressed();
                }
                else
                {
                    mBackPressedOnce = true;
                    Toast.makeText(this,getString(R.string.toast_exit_confirm),Toast.LENGTH_SHORT).show();
                }


                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        //Reset back press state after 2 seconds
                        mBackPressedOnce = false;
                    }
                }, 2000);
            }
            else
            {
                super.onBackPressed();
            }


        }
    }

}
