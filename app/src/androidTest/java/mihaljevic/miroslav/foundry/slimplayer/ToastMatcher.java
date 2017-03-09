package mihaljevic.miroslav.foundry.slimplayer;

import android.os.IBinder;
import android.support.test.espresso.Root;
import android.view.WindowManager;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Created by miroslav on 09.03.17..
 */


public class ToastMatcher extends TypeSafeMatcher<Root>
{
    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    @Override
    public boolean matchesSafely(Root root)
    {
        int     type;
        IBinder windowToken;
        IBinder appToken;

        type = root.getWindowLayoutParams().get().type;

        if ( type == WindowManager.LayoutParams.TYPE_TOAST )
        {
            windowToken = root.getDecorView().getWindowToken();
            appToken    = root.getDecorView().getApplicationWindowToken();
            if ( windowToken == appToken )
            {
                // windowToken == appToken means this window isn't contained by any other windows.
                // if it was a window for an activity, it would have TYPE_BASE_APPLICATION.
                return true;
            }
        }

        return false;
    }
}
