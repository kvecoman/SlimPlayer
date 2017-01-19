package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Miroslav on 17.1.2017..
 */

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

    private List<MediaBrowserCompat.MediaItem> mMediaItemsList;

    private Context mContext;

    private int mLayoutId;

    private View.OnClickListener mOnClickListener;


    private SparseBooleanArray mSelectedItems; //Array of selected items, init is done outside

    public MediaAdapter(Context context, List<MediaBrowserCompat.MediaItem> mediaItemList, int layoutId, @Nullable View.OnClickListener listener, @Nullable SparseBooleanArray selectedItemsArray)
    {
        mContext = context;
        mMediaItemsList = mediaItemList;
        mLayoutId = layoutId;
        mOnClickListener = listener;
        mSelectedItems = selectedItemsArray;
    }

    @Override
    public MediaAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate and return row view
        View v = LayoutInflater.from(mContext).inflate(mLayoutId,parent,false);
        return new MediaAdapter.ViewHolder(v, mOnClickListener);
    }

    @Override
    public void onBindViewHolder(MediaAdapter.ViewHolder holder, int position) {
        String text;
        View view;


        text = mMediaItemsList.get(position).getDescription().getTitle().toString();
        //TODO - multiple fields are removed, solve this
        view = holder.findViewById(mContext.getResources().getIdentifier("item_text" + 0,"id",mContext.getPackageName()));

        if (view != null && view instanceof TextView)
            ((TextView)view).setText(text);


        //Check if this item needs to be selected
        if (mSelectedItems != null)
        {
            if (mSelectedItems.get(position))
            {
                holder.mParentView.setSelected(true);
            }
            else
            {
                holder.mParentView.setSelected(false);
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mMediaItemsList == null)
            return 0;

        return mMediaItemsList.size();
    }

    public List<MediaBrowserCompat.MediaItem> getMediaItemsList()
    {
        return mMediaItemsList;
    }

    public void setMediaItemsList(List<MediaBrowserCompat.MediaItem> mediaItemsList) {
        this.mMediaItemsList = mediaItemsList;
    }

    public void swapMediaItemsList(List<MediaBrowserCompat.MediaItem> mediaItemsList)
    {
        this.mMediaItemsList = mediaItemsList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public View mParentView;

        public ViewHolder(View v)
        {
            super(v);
            mParentView = v;
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

        public View findViewById(int id)
        {
            return mParentView.findViewById(id);
        }
    }
}
