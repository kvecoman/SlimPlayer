package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

/**
 * Created by Miroslav on 6.10.2016..
 *
 * This class's purpose is to allow that we listen when everything in list is deselected (when it happens by
 * clicking outside of ListView)
 * This is done by hijacking(overriding) layoutChildren function of ListView and making it call our listener
 * and then checking if everything is really de-selected.
 * We do this because layoutChildren is called when click outside of ListView occurs.
 *
 * We use this class for settings screen, to de-select music directories
 *
 * @author Miroslav Mihaljević
 */
public class HackedListView extends ListView
{

    //This variable is really bad idea UPDATE: really, it is a monstrosity
    //We use to not deselect everything when OnLayoutChildren is called in DirectorySelectPreference
    private boolean mIsItemClicked = false;

    private OnLayoutChildrenListener mOnLayoutChildrenListener;

    public HackedListView( Context context )
    {
        super( context );
    }

    public HackedListView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }

    public HackedListView( Context context, AttributeSet attrs, int defStyleAttr )
    {
        super( context, attrs, defStyleAttr );
    }

    public boolean isItemClicked()
    {
        return mIsItemClicked;
    }

    public void setIsItemClicked( boolean mIsItemClicked )
    {
        this.mIsItemClicked = mIsItemClicked;
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
        if ( mOnLayoutChildrenListener != null )
        {
            mOnLayoutChildrenListener.onLayoutChildren( mIsItemClicked );
        }
    }

    //We are overriding this so we know when layoutChildren is called
    // because of click (important for deselection in DirectorySelectPreference)
    @Override
    public boolean onTouchEvent( MotionEvent ev )
    {
        boolean result;

        mIsItemClicked  = true;
        result          = super.onTouchEvent( ev );

        return result;
    }

    public interface OnLayoutChildrenListener
    {
        void onLayoutChildren( boolean isItemClicked );
    }

    public void setOnLayoutChildrenListener( OnLayoutChildrenListener listener )
    {
        mOnLayoutChildrenListener = listener;
    }
}
