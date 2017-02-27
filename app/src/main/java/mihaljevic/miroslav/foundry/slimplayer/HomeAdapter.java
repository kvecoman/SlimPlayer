package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Adapter for recycler view in home screen
 *
 * @author Miroslav Mihaljević
 */


public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {

    private Context                 mContext;
    private Cursor                  mCursor;
    private View.OnClickListener    mOnClickListener;


    public HomeAdapter(Context context, Cursor cursor,@Nullable View.OnClickListener listener)
    {
        mContext            = context;
        mCursor             = cursor;
        mOnClickListener    = listener;

    }

    @Override
    public HomeAdapter.ViewHolder onCreateViewHolder( ViewGroup parent, int viewType )
    {
        View            view;
        LayoutInflater  inflater;

        inflater    = LayoutInflater.from( mContext );
        view        = inflater.inflate( R.layout.home_card, parent, false );

        return new ViewHolder( view, mOnClickListener );
    }

    @Override
    public void onBindViewHolder( ViewHolder holder, int position )
    {
        if ( mCursor == null || mCursor.isClosed() )
            return;

        String itemTitle;

        mCursor.moveToPosition( position );
        itemTitle = mCursor.getString( mCursor.getColumnIndex( StatsContract.SourceStats.COLUMN_NAME_DISPLAY_NAME ) );
        holder.mTextView.setText( itemTitle );

    }

    @Override
    public int getItemCount()
    {
        if ( mCursor != null && !mCursor.isClosed() )
            return mCursor.getCount();

        return 0;
    }


    public Cursor getCursor()
    {
        return mCursor;
    }

    public void setCursor( Cursor mCursor )
    {
        this.mCursor = mCursor;
    }

    public void closeCursor()
    {
        if ( mCursor != null && !mCursor.isClosed() )
        {
            mCursor.close();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView mTextView;


        public ViewHolder( View v, View.OnClickListener listener )
        {
            super( v );
            v.setOnClickListener( listener );
            mTextView = ( TextView ) v.findViewById( R.id.card_text_view );
        }


    }

}
