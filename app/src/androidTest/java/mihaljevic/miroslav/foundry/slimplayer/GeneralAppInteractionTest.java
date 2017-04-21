package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static mihaljevic.miroslav.foundry.slimplayer.TestData.*;
import static org.hamcrest.CoreMatchers.allOf;

/**
 * Created by miroslav on 09.03.17..
 */

@RunWith( AndroidJUnit4.class )
public class GeneralAppInteractionTest
{
    /*@Rule
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule< MainActivity >( MainActivity.class );*/

    @Rule
    public IntentsTestRule<MainActivity> mActivityRule =
            new IntentsTestRule< MainActivity >( MainActivity.class );

    @Test
    public void testAreAllScreensLoaded()
    {
        ViewPager   pager;
        int         pageCount;



        pager       = ( ViewPager ) mActivityRule.getActivity().findViewById( R.id.pager );
        pageCount   = pager.getAdapter().getCount();

        assertEquals( Const.SCREENS.length, pageCount );
    }

    @Test
    public void testIsTargetScreenLoaded()
    {
        int swipes;

        swipes = -1;

        for (int i = 0;i < Const.SCREENS.length;i++)
        {
            if ( Const.SCREENS[i].equals( TEST_SOURCE ) )
                swipes = i;
        }

        if ( swipes == -1 )
            fail( "Could not find " + TEST_SOURCE + " in SCREENS array" );

        for ( int i = 0;i < swipes;i++ )
            onView( withId( R.id.pager ) ).perform( swipeLeft() );

        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.scrollTo(  withText( EXPECTED_DISPLAY_NAME   ) ) );

        onView( allOf( isDescendantOfA( withId( R.id.recycler ) ), withText( EXPECTED_DISPLAY_NAME ) ) )
                .check( matches( isDisplayed() ) );

        /*onView( allOf( withId( R.id.recycler ), isCompletelyDisplayed() ) )
                .check( matches(  ) )
                .perform( RecyclerViewActions.actionOnItem( withText( EXPECTED_DISPLAY_NAME ), click() ) );*/
    }

    //NOTE - THIS TEST NEEDS TO BE STARTED IN DEBUG MODE TO WORK
    @Test
    public void testTargetScreenSongListIsLoaded()
    {
        int swipes;

        swipes = -1;

        for (int i = 0;i < Const.SCREENS.length;i++)
        {
            if ( Const.SCREENS[i].equals( TEST_SOURCE ) )
                swipes = i;
        }

        if ( swipes == -1 )
            fail( "Could not find " + TEST_SOURCE + " in SCREENS array" );

        for ( int i = 0;i < swipes;i++ )
            onView( withId( R.id.pager ) ).perform( swipeLeft() );

        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.scrollTo(  withText( EXPECTED_DISPLAY_NAME   ) ) );

        //NOTE - THIS TEST NEEDS TO BE STARTED IN DEBUG MODE TO WORK

        //Intents.init();
        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.actionOnItem( withText( EXPECTED_DISPLAY_NAME ), click() ) );

        //Intents.release();

        //while (true){}

        onView( withId( R.id.recycler ) )
                .perform( RecyclerViewActions.scrollTo(  withText( EXPECTED_TITLE   ) ) );

        onView( allOf( isDescendantOfA( withId( R.id.recycler ) ), withText( EXPECTED_TITLE ) ) )
                .check( matches( isDisplayed() ) );
    }

    //NOTE - THIS TEST NEEDS TO BE STARTED IN DEBUG MODE TO WORK
    @Test
    public void testPlayTestSong()
    {
        int swipes;

        swipes = -1;

        for (int i = 0;i < Const.SCREENS.length;i++)
        {
            if ( Const.SCREENS[i].equals( TEST_SOURCE ) )
                swipes = i;
        }

        if ( swipes == -1 )
            fail( "Could not find " + TEST_SOURCE + " in SCREENS array" );

        for ( int i = 0;i < swipes;i++ )
            onView( withId( R.id.pager ) ).perform( swipeLeft() );

        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.scrollTo(  withText( EXPECTED_DISPLAY_NAME   ) ) );

        //NOTE - THIS TEST NEEDS TO BE STARTED IN DEBUG MODE TO WORK


        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.actionOnItem( withText( EXPECTED_DISPLAY_NAME ), click() ) );


        onView( withId( R.id.recycler ) )
                .perform( RecyclerViewActions.scrollTo(  withText( EXPECTED_TITLE   ) ) );

        onView( withId( R.id.recycler ) )
                .perform( RecyclerViewActions.actionOnItem( withText( EXPECTED_TITLE ), click() ) );

        onView( allOf( withId( R.id.song_title ), isCompletelyDisplayed() ) ).check( matches( withText( EXPECTED_TITLE ) ) );
        onView( allOf( withId( R.id.song_artist ), isCompletelyDisplayed() ) ).check( matches( withText( EXPECTED_ARTIST ) ) );
    }

    @Test
    public void testSwipeToTestSong_fromBeginning()
    {
        int swipes;

        swipes = -1;

        for (int i = 0;i < Const.SCREENS.length;i++)
        {
            if ( Const.SCREENS[i].equals( TEST_SOURCE ) )
                swipes = i;
        }

        if ( swipes == -1 )
            fail( "Could not find " + TEST_SOURCE + " in SCREENS array" );

        for ( int i = 0;i < swipes;i++ )
            onView( withId( R.id.pager ) ).perform( swipeLeft() );

        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.scrollTo(  withText( EXPECTED_DISPLAY_NAME   ) ) );

        //NOTE - THIS TEST NEEDS TO BE STARTED IN DEBUG MODE TO WORK


        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.actionOnItem( withText( EXPECTED_DISPLAY_NAME ), click() ) );


        onView( withId( R.id.recycler ) )
                .perform( RecyclerViewActions.actionOnItemAtPosition( 0, click() ) );

        for ( int i = 0; i < EXPECTED_POSITION; i++ )
        {
            onView( withId( R.id.pager ) ).perform( swipeLeft() );
        }

        onView( allOf( withId( R.id.song_title ), isCompletelyDisplayed() ) ).check( matches( withText( EXPECTED_TITLE ) ) );
        onView( allOf( withId( R.id.song_artist ), isCompletelyDisplayed() ) ).check( matches( withText( EXPECTED_ARTIST ) ) );
    }

    @Test
    public void testSwipeToTestSong_fromEnd()
    {
        int swipes;
        int songCount;
        List<MediaBrowserCompat.MediaItem> mediaItems;
        String lastSongTitle;

        swipes = -1;

        for (int i = 0;i < Const.SCREENS.length;i++)
        {
            if ( Const.SCREENS[i].equals( TEST_SOURCE ) )
                swipes = i;
        }

        if ( swipes == -1 )
            fail( "Could not find " + TEST_SOURCE + " in SCREENS array" );

        for ( int i = 0;i < swipes;i++ )
            onView( withId( R.id.pager ) ).perform( swipeLeft() );

        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.scrollTo(  withText( EXPECTED_DISPLAY_NAME   ) ) );

        //NOTE - THIS TEST NEEDS TO BE STARTED IN DEBUG MODE TO WORK


        onView( allOf( withId( R.id.recycler ), hasDescendant( withText( EXPECTED_DISPLAY_NAME ) ) ) )
                .perform( RecyclerViewActions.actionOnItem( withText( EXPECTED_DISPLAY_NAME ), click() ) );


        mediaItems      = MusicProvider.getInstance().loadMedia( TEST_SOURCE, TEST_PARAMETER );
        songCount       = mediaItems.size();
        lastSongTitle   = ( String ) mediaItems.get( songCount - 1 ).getDescription().getTitle();

        onView( withId( R.id.recycler ) ).perform( RecyclerViewActions.scrollTo( withText( lastSongTitle ) ) );

        onView( withId( R.id.recycler ) )
                .perform( RecyclerViewActions.actionOnItem( withText( lastSongTitle ), click() ) );

        for ( int i = songCount - 1; i > EXPECTED_POSITION; i-- )
        {
            onView( withId( R.id.pager ) ).perform( swipeRight() );
        }

        onView( allOf( withId( R.id.song_title ), isCompletelyDisplayed() ) ).check( matches( withText( EXPECTED_TITLE ) ) );
        onView( allOf( withId( R.id.song_artist ), isCompletelyDisplayed() ) ).check( matches( withText( EXPECTED_ARTIST ) ) );
    }
}
