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

    protected BackHandledFragmentActivity backHandler;
    public abstract boolean onBackPressed();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (!(getActivity() instanceof BackHandlerInterface))
        {
            throw new ClassCastException("Hosting activity must implement BackHandlerInterface");
        }
        else
        {
            backHandler = (BackHandledFragmentActivity) getActivity();
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //This is part of my dirty solution to make BackHandled fragments work with view pager
        if (backHandler.isSelectedFragmentNull())
            backHandler.setBackHandledFragment(this);
    }

    public interface BackHandlerInterface
    {
        void setBackHandledFragment(BackHandledListFragment backHandledFragment);
    }
}
