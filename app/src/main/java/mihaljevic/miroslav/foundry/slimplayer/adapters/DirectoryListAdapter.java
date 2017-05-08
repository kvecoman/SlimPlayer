package mihaljevic.miroslav.foundry.slimplayer.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.TreeMap;

import mihaljevic.miroslav.foundry.slimplayer.Utils;

/**
 * Created by Miroslav on 28.11.2016..
 *
 * This adapter is used in DirectorySelectDialogPreferenceFrag
 *
 * It is extension of ArrayAdapter that takes List of HashMaps
 *
 * @author Miroslav Mihalljević
 */
public class DirectoryListAdapter extends BaseAdapter
{

    private Context mContext;

    private TreeMap<String, File> mMap;

    public DirectoryListAdapter( Context context, TreeMap<String, File> objects )
    {
        super();

        mContext = context;
        mMap = objects;
    }

    @Override
    @NonNull public View getView( int position, View convertView, @NonNull ViewGroup parent )
    {

        View            view;
        LayoutInflater  inflater;
        String          name;

        view = convertView;
        name = (String)( mMap.keySet().toArray()[position]);

        //If there is no view provided then just inflate android default
        if ( view == null )
        {
            inflater = LayoutInflater.from( mContext );
            view = inflater.inflate( android.R.layout.simple_list_item_1, null );
        }


        if ( view instanceof TextView )
            ((TextView) view).setText( name );


        return view;
    }

    public void swap(TreeMap<String, File> data)
    {
        mMap = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        if ( mMap == null )
            return 0;

        return mMap.size();
    }

    @Override
    public Object getItem( int position )
    {
        if ( mMap == null )
            return null;

        return Utils.getByIndex( mMap, position );
    }

    @Override
    public long getItemId( int position )
    {
        if ( mMap == null)
            return -1;

        return Utils.getByIndex( mMap, position ).hashCode();
    }
}
