package mihaljevic.miroslav.foundry.slimplayer;

import android.support.test.espresso.Root;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * Created by miroslav on 09.03.17..
 */

public class TestUtils
{
    private TestUtils(){}

    public static void isToastMessageDisplayed( String text )
    {
        onView( withText( text ) ).inRoot ( isToast() )
                                    .check  ( matches( isDisplayed() ) );
    }

    public static Matcher< Root > isToast()
    {
        return new ToastMatcher();
    }
}
