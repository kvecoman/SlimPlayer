package mihaljevic.miroslav.foundry.slimplayer.fragments;

import android.content.Context;
import android.os.Environment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.io.File;
import java.io.FileFilter;
import java.util.TreeMap;

import mihaljevic.miroslav.foundry.slimplayer.DirectorySelectPreference;
import mihaljevic.miroslav.foundry.slimplayer.R;
import mihaljevic.miroslav.foundry.slimplayer.Utils;
import mihaljevic.miroslav.foundry.slimplayer.adapters.DirectoryListAdapter;

/**
 * Created by Miroslav on 29.9.2016..
 *
 * Dialog that opens when "select directory" preference is selected
 *
 * @author Miroslav Mihaljević
 */


public class DirectorySelectDialogPreferenceFrag extends PreferenceDialogFragmentCompat implements DialogPreference.TargetFragment, AdapterView.OnItemClickListener {
    private final String TAG = getClass().getSimpleName();


    //Controls from layout
    private ListView mListView;

    //Current directory and listing of all directories in it
    private File                    mCurrentDir;
    private TreeMap<String, File>   mDirectoriesList2;

    //Adapter responsible for mListView
    private DirectoryListAdapter mAdapter;


    //Filter we use to get visible, readable directories only
    private FileFilter mDirectoryFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory() && !file.isHidden() && file.canRead();
        }
    };

    @Override
    protected View onCreateDialogView(Context context)
    {
        LayoutInflater inflater;

        inflater = LayoutInflater.from(context);


        return inflater.inflate( R.layout.directory_select_dialog, null );
    }

    //Most of the init is done in here, also the adapter is set up here
    @Override
    protected void onBindDialogView( View v )
    {
        //Bind views to member variables
        mListView = (ListView)v.findViewById(R.id.directory_select_list);
        mListView.setOnItemClickListener(this);

        Button selectButton = (Button)v.findViewById(R.id.directory_select_button);
        Button cancelButton = (Button)v.findViewById(R.id.directory_cancel_button);

        selectButton.setOnClickListener( new SelectButtonListener() );
        cancelButton.setOnClickListener( new CancelButtonListener() );

        //Set starting directory and load its contents
        mCurrentDir = Environment.getExternalStorageDirectory();
        updateDirectories();
    }

    //Click listener for list items
    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        mCurrentDir = ( File ) Utils.getByIndex( mDirectoriesList2, position );
        updateDirectories();
    }

    @Override
    public void onDialogClosed( boolean positiveResult )
    {
        //I have no idea how to use this properly, but adding directory in set should be done here
        //Problem is I don't know how to close custom dialog with positive result, only how to dismiss it
    }

    //I am not sure what this is for
    @Override
    public Preference findPreference( CharSequence charSequence )
    {
        return getPreference();
    }

    //Updates everything(views, list, adapter) based on current directory
    private void updateDirectories()
    {
        updateDirectoryListing();
        updateAdapter();
    }

    //Function that updates directory listing based on current selected directory
    private void updateDirectoryListing()
    {
        mDirectoriesList2 = new TreeMap<>( new Utils.alphabetSort() );


        //If there is parent directory, add it on list as "/.."
        if ( mCurrentDir.getParent() != null )
        {
            if ( mCurrentDir.getParentFile().canRead() && ( mCurrentDir.getParent().length() > 0 ) )
            {
                File backDir = mCurrentDir.getParentFile();
                mDirectoriesList2.put( "/..", backDir );
            }

        }

        if ( !mCurrentDir.canRead() )
        {
            Log.w(TAG, "Can't read current directory, READ_EXTERNAL_STORAGE permission???");
            return;
        }

        //Go through all directories and add them to hash map
        for ( File dir : mCurrentDir.listFiles( mDirectoryFilter ) )
        {
            mDirectoriesList2.put( dir.getName(), dir );
        }
    }

    //Updates adapter based on current directory listing and current directory (or creates a new one)
    private void updateAdapter()
    {

        if ( mAdapter == null )
        {
            //Create new adapter if it doesn't exist
            mAdapter = new DirectoryListAdapter( getContext(), mDirectoriesList2 );
            mListView.setAdapter( mAdapter );
        }
        else
        {
            //If it exist just reflect the changes in data
            mAdapter.swap( mDirectoriesList2 );
        }


    }


    private class SelectButtonListener implements Button.OnClickListener
    {
        @Override
        public void onClick( View v )
        {
            //NOTE - proper way to do all of this is using "persist*" methods of Preference,
            //       but it doesn't have persistStringSet, and we are using string set

            //Here we update the set of selected directories
            DirectorySelectPreference pref;

            pref = (DirectorySelectPreference)getPreference();

            //Add selected directory to preference
            pref.addDirectoryPath(mCurrentDir.getAbsolutePath());


            getDialog().dismiss();
        }
    }

    private class CancelButtonListener implements Button.OnClickListener
    {
        @Override
        public void onClick( View v )
        {
            getDialog().dismiss();
        }
    }
}
