package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.os.Build;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class DirectorySelectPreference extends DialogPreference implements Button.OnClickListener, ListView.OnItemClickListener, HackedListView.OnLayoutChildrenListener {

    private HackedListView          mListView;
    private Set<String>             mDirectoriesSet;
    private Button                  mActionButton;
    private ArrayAdapter<String>    mAdapter;
    private Context                 mContext;
    private int                     mSelectedItem = -1;

    public DirectorySelectPreference(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public DirectorySelectPreference( Context context ) {
        super(context);
    }

    public DirectorySelectPreference( Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes )
    {
        super( context, attrs, defStyleAttr, defStyleRes );
    }

    public DirectorySelectPreference( Context context, AttributeSet attrs, int defStyleAttr )
    {
        super( context, attrs, defStyleAttr );

        mContext = context;

        setLayoutResource       (R.layout.preference_folder_select);
        setWidgetLayoutResource (R.layout.preference_folder_select_widget);

    }





    //Here we init almost everything view related
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder)
    {

        //Set up data for list view
        mDirectoriesSet = new HashSet<>(getSharedPreferences().getStringSet(getKey(),new HashSet<String>()));

        mAdapter = new ArrayAdapter<>(mContext,android.R.layout.simple_list_item_activated_1);
        mAdapter.addAll(mDirectoriesSet);

        mListView = ( HackedListView )holder.findViewById(R.id.directory_set_list);

        mListView.setChoiceMode             ( ListView.CHOICE_MODE_SINGLE );
        mListView.setItemsCanFocus          ( false );
        mListView.setAdapter                ( mAdapter );
        mListView.setOnItemClickListener    ( this );
        mListView.setOnLayoutChildrenListener( this );

        mActionButton = (Button)holder.findViewById( R.id.action_button );
        mActionButton.setOnClickListener( this );


        //Set list height based on number of rows we have
        updateListHeight();


        //Set preference title and summary
        ( ( TextView ) holder.findViewById( android.R.id.title ) ).setText( getTitle() );
        ( ( TextView ) holder.findViewById( android.R.id.summary ) ).setText( getSummary() );

    }


    //Handler for when LayoutChildren is called, and it is called on almost every kind of ListView update
    //...and that's why we use to catch moment when everything from list is unselected
    @Override
    public void onLayoutChildren( boolean isItemClicked )
    {
        //This is pretty bad, but even if we always deselect list, OnItemClick goes after this
        //... so it will be selected again
        if ( !isItemClicked )
        {
            deselectList();
        }

    }

    //Handler for when add directory(action button) is clicked
    @Override
    public void onClick( View v )
    {
        String directory;

        //If nothing is selected then show dialog
        if ( mSelectedItem < 0 )
        {
            //If we don't have permission then just return
            if ( Build.VERSION.SDK_INT >= 16 && !Utils.checkPermission( android.Manifest.permission.READ_EXTERNAL_STORAGE ) )
                return;

            //Show dialog for selecting preferences
            getPreferenceManager().showDialog( this );
        }
        else
        {
            //If something is selected, then delete it and update ListView, set and preference
            directory = mAdapter.getItem( mSelectedItem );

            mDirectoriesSet.remove  ( directory );
            mAdapter.remove         ( directory );

            mListView.clearChoices();
            deselectList();

            mAdapter.notifyDataSetChanged();
            updateDirectoriesPref();
            updateListHeight();


        }
    }

    //Click handler for list view item click
    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        if ( mSelectedItem == position )
        {
            //If user clicked on already selected item, we should deselect it
            mListView.setItemChecked( position, false );
            deselectList();
        }
        else
        {
            //Select what user has clicked
            mActionButton.setText( mContext.getString( R.string.directory_pref_button_delete ) );
            mSelectedItem = position;
        }

        mListView.setIsItemClicked( false );
    }

    //Instead of directly accessing SharedPreferences, here we should use persist methods, but I don't know :\
    //This method is called from dialog when select button is clicked
    public void addDirectoryPath( String path )
    {
        mDirectoriesSet.add( path );

        updateDirectoriesPref();

        //Update adapter for our list view and update list height
        mAdapter.clear();
        mAdapter.addAll( mDirectoriesSet );
        mAdapter.notifyDataSetChanged();
        updateListHeight();
    }

    //Updates ListView and shared preference with latest directories set
    private void updateDirectoriesPref()
    {
        getSharedPreferences().edit()
                                .putStringSet( getKey(), mDirectoriesSet )
                                .apply();

        //We have to do this right here, because with this preference the onSharedPreferenceChanged isn't called
        ( ( SlimPlayerApplication ) getContext().getApplicationContext() ).notifyPreferencesChanged();
    }



    //Functions that updates height of member list view based on number of items
    private void updateListHeight()
    {
        ViewGroup.LayoutParams params;

        params          = mListView.getLayoutParams();
        params.height   = Utils.calculateListHeight(mListView);

        mListView.setLayoutParams(params);
        mListView.requestLayout();
    }

    //Function that deselects item from list and sets appropriate action button text
    private void deselectList()
    {
        mSelectedItem = -1;
        mActionButton.setText( mContext.getString( R.string.directory_pref_button_select ) );
    }
}
