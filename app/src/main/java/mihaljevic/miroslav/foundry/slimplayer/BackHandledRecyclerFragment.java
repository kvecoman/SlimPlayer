package mihaljevic.miroslav.foundry.slimplayer;


import android.support.v4.app.Fragment;

/**
 * Class that allows for back button to be handled inside of fragment.
 *
 * Whole concept came from Achin of Vinsol.com
 *
 * @author Miroslav Mihaljević
 */
public abstract class BackHandledRecyclerFragment extends Fragment {


    //Parent activity that implements BackHandlerInterface
    //protected BackHandledFragmentActivity backHandler;

    //Function that is called when hosting activity detects back button press
    public abstract boolean onBackPressed();


    //Interface that hosting activity needs to implement
    public interface BackHandlerInterface
    {
        void setBackHandledFragment(BackHandledRecyclerFragment backHandledFragment);
    }

}
