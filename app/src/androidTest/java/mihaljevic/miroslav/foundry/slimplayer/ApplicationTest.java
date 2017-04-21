package mihaljevic.miroslav.foundry.slimplayer;

import android.app.Application;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ApplicationTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.fail;
import static mihaljevic.miroslav.foundry.slimplayer.TestData.EXPECTED_DISPLAY_NAME;
import static mihaljevic.miroslav.foundry.slimplayer.TestData.TEST_SOURCE;
import static org.hamcrest.CoreMatchers.allOf;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith( AndroidJUnit4.class )
public class ApplicationTest
{


    @Rule
    public IntentsTestRule<MainActivity> mActivityRule =
            new IntentsTestRule< MainActivity >( MainActivity.class );


    //NOTE - this test doesn't work
    @Test
    public void swipeAllSongs()
    {

        int swipes;

        swipes = -1;

        for ( int i = 0; i < Const.SCREENS.length; i++ )
        {
            if ( Const.SCREENS[i].equals( "all_screen" ) )
                swipes = i;
        }

        if ( swipes == -1 )
            fail( "Could not find all_screen in SCREENS array" );

        for ( int i = 0;i < swipes;i++ )
            onView( withId( R.id.pager ) ).perform( swipeLeft() );

        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( "+1 ft. Sam White" ) ) ) ).perform( RecyclerViewActions.actionOnItemAtPosition( 0, click() ) );

        //onView( allOf( withId( R.id.song_title ), isCompletelyDisplayed() ) ).check( matches( isDisplayed() ) );

        for ( int i = 0; i < 500; i++ )
        {
            onView( withId( R.id.pager ) ).perform( swipeLeft() );
        }
    }


}