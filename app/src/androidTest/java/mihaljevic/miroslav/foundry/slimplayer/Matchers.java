package mihaljevic.miroslav.foundry.slimplayer;

import android.support.v4.view.ViewPager;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Created by miroslav on 09.03.17..
 */

public class Matchers
{

    public static Matcher<ViewPager> withPageCount( final int count )
    {
        return new TypeSafeMatcher< ViewPager >()
        {
            @Override
            protected boolean matchesSafely( ViewPager pager )
            {
                return ( pager.getAdapter().getCount() == count );
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendText( "ViewPager should have " + count + " pages" );
            }
        };
    }

}
