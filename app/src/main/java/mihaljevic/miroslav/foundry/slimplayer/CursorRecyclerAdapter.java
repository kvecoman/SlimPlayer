package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Adapter for recycler view that uses cursor as its base source of data.
 * Used for lists od data coming from media store.
 *
 * @author Miroslav Mihaljević
 */

public class CursorRecyclerAdapter extends RecyclerView.Adapter<CursorRecyclerAdapter.ViewHolder> {

    private Context mContext;
    private Cursor mCursor;

    private String mDisplayField; //Name of field we use to display in row/item

    private View.OnClickListener mOnClickListener;


    private SparseBooleanArray mSelectedItems; //Array of selected items, init is done outside


    public CursorRecyclerAdapter(Context context, Cursor cursor, String displayField, View.OnClickListener listener, SparseBooleanArray selectedItemsArray)
    {
        mContext = context;
        mCursor = cursor;
        mDisplayField = displayField;
        mOnClickListener = listener;
        mSelectedItems = selectedItemsArray;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        //Inflate and return row view
        View v = LayoutInflater.from(mContext).inflate(R.layout.recycler_item,parent,false);
        return new ViewHolder(v, mOnClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        if (mCursor == null || mCursor.isClosed())
            return;

        String text;

        mCursor.moveToPosition(position);
        text = mCursor.getString(mCursor.getColumnIndex(mDisplayField));
        holder.mTextView.setText(text);

        //Check if this item needs to be selected
        if (mSelectedItems.get(position))
        {
            holder.mParentView.setSelected(true);
        }
        else
        {
            holder.mParentView.setSelected(false);
        }
    }

    @Override
    public int getItemCount()
    {
        if (mCursor != null && !mCursor.isClosed())
            return mCursor.getCount();

        return 0;
    }


    public Cursor getCursor() {
        return mCursor;
    }

    /*public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
    }*/

    public void swapCursor(Cursor cursor)
    {
        if (mCursor == cursor)
            return;

        this.mCursor = cursor;
        notifyDataSetChanged();
    }

    public void closeCursor()
    {
        if (mCursor != null && !mCursor.isClosed() && !SlimPlayerApplication.getInstance().getMediaPlayerService().isCursorUsed(mCursor))
        {
            mCursor.close();
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public View mParentView;
        public TextView mTextView;

        public ViewHolder(View v)
        {
            super(v);
            mParentView = v;
            mTextView = (TextView)v.findViewById(R.id.recycler_item_text);
        }

        public ViewHolder(View v, View.OnClickListener listener)
        {
            this(v);

            if (listener == null)
                return;


            v.setOnClickListener(listener);

            //Check if listener is also instance of long click listener
            if (listener instanceof View.OnLongClickListener)
                v.setOnLongClickListener(((View.OnLongClickListener) listener));
        }


    }
}
