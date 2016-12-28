package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Adapter for recycler view in home screen
 *
 * @author Miroslav Mihaljević
 */


//TODO - continue here - create 2 custom databases, one for lists(sources), other for songs and record stats in them, then use that
    //...as data source in this adapter, then connect adapter to recycler view in home fragment - ko plitak potok
public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {

    private Context mContext;
    private Cursor mCursor;


    public HomeAdapter(Context context, Cursor cursor)
    {
        mContext = context;

        mCursor = cursor;

    }

    @Override
    public HomeAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(mContext).inflate(R.layout.home_card,parent,false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {
        if (mCursor == null || mCursor.isClosed())
            return;

        String text;

        mCursor.moveToPosition(position);
        text = mCursor.getString(mCursor.getColumnIndex(StatsContract.SourceStats.COLUMN_NAME_DISPLAY_NAME));
        holder.mTextView.setText(text);

        mCursor.moveToPosition(position);

        holder.mSource = mCursor.getString(mCursor.getColumnIndex(StatsContract.SourceStats.COLUMN_NAME_SOURCE));
        holder.mParameter = mCursor.getString(mCursor.getColumnIndex(StatsContract.SourceStats.COLUMN_NAME_PARAMETER));
        holder.mPlayPosition = mCursor.getInt(mCursor.getColumnIndex(StatsContract.SourceStats.COLUMN_NAME_LAST_POSITION));
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

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
    }

    public void closeCursor()
    {
        if (mCursor != null && !mCursor.isClosed())
        {
            mCursor.close();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        public TextView mTextView;

        public String mSource;
        public String mParameter;
        public int mPlayPosition;

        public ViewHolder(View v)
        {
            super(v);
            v.setOnClickListener(this);
            mTextView = (TextView)v.findViewById(R.id.card_text_view);
        }



        @Override
        public void onClick(View v) {

            //Get bundle
            Bundle bundle = ScreenBundles.getBundleForSubScreen(v.getContext(),mSource,mParameter);
            bundle.putInt(SongListFragment.PLAY_POSITION_KEY,mPlayPosition);

            Intent intent = new Intent(v.getContext(),SongListActivity.class);

            //Insert last remembered position
            intent.putExtra(SongListActivity.FRAGMENT_BUNDLE_KEY, bundle);

            //Start activity
            v.getContext().startActivity(intent);
        }
    }

}
