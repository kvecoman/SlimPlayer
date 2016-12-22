package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
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
    //...as data source in this adapter, thenconnect adapter to recycler view in home fragment - ko plitak potok
public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.ViewHolder> {

    private Context mContext;

    public HomeAdapter(Context context)
    {
        mContext = context;
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
        //TODO - here goes text from data set
        holder.mTextView.setText();
    }

    @Override
    public int getItemCount()
    {
        //TODO - return count of data set items
        return
    }




    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public TextView mTextView;

        public ViewHolder(View v)
        {
            super(v);
            mTextView = (TextView)v.findViewById(R.id.card_text_view);
        }
    }

}
