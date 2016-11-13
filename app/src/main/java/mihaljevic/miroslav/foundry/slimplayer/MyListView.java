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
 * @author Miroslav Mihaljević
 */
public class MyListView extends ListView {

    //This variable is really bad idea UPDATE: really, it is monstrosity
    //We use to not deselect everything when OnLayoutChildren is called in DirectorySelectPreference
    public boolean mIsItemClicked = false;

    private OnLayoutChildrenListener mOnLayoutChildrenListener;

    public MyListView(Context context) {
        super(context);
    }

    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
        if (mOnLayoutChildrenListener != null)
        {
            mOnLayoutChildrenListener.onLayoutChildren();
        }
    }

    //We are overriding this so we know when layoutChildren is called
    // because of click (important for deselection in DirectorySelectPreference)
    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        boolean result;
        mIsItemClicked = true;
        result = super.onTouchEvent(ev);
        return result;
    }

    public interface OnLayoutChildrenListener
    {
        public void onLayoutChildren();
    }

    public void setOnLayoutChildrenListener(OnLayoutChildrenListener listener)
    {
        mOnLayoutChildrenListener = listener;
    }
}
