package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Miroslav on 28.9.2016..
 *
 * Custom preference for displaying selected directorys from where
 * music is taken and selecting new ones also
 *
 * @author Miroslav Mihaljević
 */

public class DirectorySelectPreference extends DialogPreference implements Button.OnClickListener  {

    private MyListView mListView;
    private Set<String> mDirectoriesSet;
    private Button mActionButton;
    private ArrayAdapter<String> mAdapter;
    private Context mContext;
    private int mSelectedItem = -1;


    public DirectorySelectPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DirectorySelectPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setLayoutResource(R.layout.preference_folder_select);
        setWidgetLayoutResource(R.layout.preference_folder_select_widget);

    }

    public DirectorySelectPreference(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DirectorySelectPreference(Context context) {
        super(context);
    }

    //Here we init almost everything view related
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {

        mListView = (MyListView)holder.findViewById(R.id.directory_set_list);
        mActionButton = (Button)holder.findViewById(R.id.action_button);

        mActionButton.setOnClickListener(this);

        //Set up data for list view
        mDirectoriesSet = getSharedPreferences().getStringSet(getKey(),new HashSet<String>());
        mAdapter = new ArrayAdapter<String>(mContext,android.R.layout.simple_list_item_activated_1);
        mAdapter.addAll(mDirectoriesSet);
        mListView.setAdapter(mAdapter);

        //Set list height based on number of rows we have
        updateListHeight();

        //Set that items on list view are selectable
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mListView.setItemsCanFocus(false);

        //Set preference title and summary
        ((TextView)holder.findViewById(android.R.id.title)).setText(getTitle());
        ((TextView)holder.findViewById(android.R.id.summary)).setText(getSummary());


        //Handler for when LayoutChildren is called, and it is called on almost every kind of ListView update
        //...and thats why we use to catch moment when everything from list is unselected
        mListView.setOnLayoutChildrenListener(new MyListView.OnLayoutChildrenListener() {
            @Override
            public void onLayoutChildren() {
                //This is pretty bad, but even if we always deselect list, OnItemClick goes after this
                //... so it will be selected again

                if (mListView.mIsItemClicked == false)
                {
                    deselectList();
                }


                //TODO - think of some other way to get notified when everything is deselected?

            }
        });

        //Handler for when item is clicked
        mListView.setOnItemClickListener(new ListView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (mSelectedItem == position)
                {
                    //If user clicked on already selected item, we should deselect it
                    mListView.setItemChecked(position, false);
                    deselectList();
                }
                else
                {
                    //Select what user has clicked
                    mActionButton.setText(mContext.getString(R.string.directory_pref_button_delete));
                    mSelectedItem = position;
                }

                mListView.mIsItemClicked = false;
            }
        });
    }

    //Handler for when add directory(action button) is clicked
    @Override
    public void onClick(View v) {
        //If nothing is selcted then show dialog
        if (mSelectedItem < 0)
            DirectorySelectPreference.this.getPreferenceManager().showDialog(DirectorySelectPreference.this);
        else
        {
            //If something is selected, then delete it and update ListView, set and preference
            String str = mAdapter.getItem(mSelectedItem);
            mDirectoriesSet.remove(str);
            mAdapter.remove(str);
            mAdapter.notifyDataSetChanged();
            mListView.clearChoices();
            updateDirectoriesPref();
            updateListHeight();
            deselectList();

        }
    }

    //Instead of directly accesing SharedPreferences, here we should use persist methods, but I dont know :\
    //This method is called from dialog when select button is clicked
    public void addDirectoryPath(String path)
    {
        mDirectoriesSet.add(path);

        updateDirectoriesPref();

        //Update adapter for our list view and update list height
        mAdapter.add(path);
        mAdapter.notifyDataSetChanged();
        updateListHeight();
    }

    //Updates ListView and shared preference with latest directories set
    public void updateDirectoriesPref()
    {
        getSharedPreferences().edit().putStringSet(getKey(),mDirectoriesSet).commit();
    }



    //Functions that updates height of member list view based on number of items
    public void updateListHeight()
    {
        ViewGroup.LayoutParams params = mListView.getLayoutParams();
        params.height = Utils.calculateListHeight(mListView);
        mListView.setLayoutParams(params);
        mListView.requestLayout();
    }

    //Function that deselects item from list and sets apropriate action button text
    public void deselectList()
    {
        mSelectedItem = -1;
        mActionButton.setText(mContext.getString(R.string.directory_pref_button_select));
    }
}
