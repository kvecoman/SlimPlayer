package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Application;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ApplicationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by miroslav on 06.03.17..
 */
@RunWith(AndroidJUnit4.class)
public class TestTest
{

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule< MainActivity >( MainActivity.class );

    @Test
    public void test()
    {

        for ( int i = 0; i < 5; i++ )
        {
            onView( withId( R.id.pager ) )
                    .perform( swipeLeft() );
        }
    }
}
