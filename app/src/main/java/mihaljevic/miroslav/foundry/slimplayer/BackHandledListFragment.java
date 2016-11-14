package mihaljevic.miroslav.foundry.slimplayer;

import android.os.Bundle;
import android.support.v4.app.ListFragment;

/**
 * Class that allows for back button to be handled inside of fragment.
 *
 * Whole concept came from Achin of Vinsol.com
 *
 * @author Miroslav Mihaljević
 */
public abstract class BackHandledListFragment extends ListFragment {

    //Parent activity that implements BackHandlerInterface
    protected BackHandledFragmentActivity backHandler;

    //Function that is called when hosting activity detects back button press
    public abstract boolean onBackPressed();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Here we make sure host activity is implementing BackHandlerInterface
        if (!(getActivity() instanceof BackHandlerInterface))
        {
            throw new ClassCastException("Hosting activity must implement BackHandlerInterface");
        }
        else
        {
            //Get host activity
            backHandler = (BackHandledFragmentActivity) getActivity();
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //This is part of my dirty solution to make BackHandled fragments work with view pager
        //If BackHandledFragment has not been set anywhere else then we set it for first time here
        if (backHandler.isSelectedFragmentNull())
            backHandler.setBackHandledFragment(this);
    }

    //Interface that hosting activity needs to implement
    public interface BackHandlerInterface
    {
        void setBackHandledFragment(BackHandledListFragment backHandledFragment);
    }
}
