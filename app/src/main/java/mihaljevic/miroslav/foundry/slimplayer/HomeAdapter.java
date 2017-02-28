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

import java.util.ArrayList;

/**
 * Adapter for recycler view in home screen
 *
 * @author Miroslav Mihaljević
 */


public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {

    private Context                 mContext;
    private View.OnClickListener    mOnClickListener;
    private ArrayList<StatRow>      mData;


    public HomeAdapter(Context context, ArrayList<StatRow> data, @Nullable View.OnClickListener listener)
    {
        mContext            = context;
        mData               = data;
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
        if ( mData == null )
            return;

        String itemTitle;


        itemTitle = mData.get( position ).displayName;
        holder.mTextView.setText( itemTitle );

    }

    @Override
    public int getItemCount()
    {
        if ( mData != null )
            return mData.size();

        return 0;
    }


    public ArrayList<StatRow> getData() { return mData; }

    public void setData( ArrayList<StatRow> data ) { mData = data; }



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

    public static class StatRow
    {
        public String   source;
        public String   parameter;
        public String   displayName;
        public int      lastPosition;

        public StatRow()
        {
        }
    }

}
