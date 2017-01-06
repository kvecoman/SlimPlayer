package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Miroslav on 28.11.2016..
 *
 * This adapter is used in DirectorySelectDialogPreferenceFrag
 *
 * It is extension of ArrayAdapter that takes List of HashMaps
 *
 * @author Miroslav Mihalljević
 */
public class DirectoryListAdapter extends ArrayAdapter<HashMap<String,Object>> {

    public static final String NAME_KEY = "name";
    public static final String DIR_KEY = "dir";

    private List<HashMap<String,Object>> mHashMapList;

    public DirectoryListAdapter(Context context, int resource, List<HashMap<String, Object>> objects) {
        super(context, resource, objects);

        mHashMapList = objects;
    }

    @Override
    @NonNull public View getView(int position, View convertView,@NonNull ViewGroup parent) {

        View view = convertView;

        //If there is no view provided then just inflate android default
        if (view == null)
        {
            LayoutInflater i = LayoutInflater.from(getContext());
            view = i.inflate(android.R.layout.simple_list_item_1,null);
        }

        HashMap<String,Object> item = getItem(position);
        if (view instanceof TextView)
        {
            ((TextView) view).setText((String)item.get(NAME_KEY));
        }

        return view;
    }
}
