package mihaljevic.miroslav.foundry.slimplayer;


import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import java.io.File;
import java.util.Set;

/**
 * Fragment that load preferences from xml file and display it to user
 *
 * @author Miroslav Mihaljević
 */
public class SettingsFragment extends PreferenceFragmentCompat {


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        setPreferencesFromResource(R.xml.settings, rootKey);

        //OnClick listener for Refresh preference
        findPreference(getResources().getString(R.string.pref_key_refresh)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                //Get selected directories and run rescan on them
                SharedPreferences preferences  = PreferenceManager.getDefaultSharedPreferences(getContext());
                Set<String> directoriesSet = preferences.getStringSet(getString(R.string.pref_key_directories_set),null);

                //Detect empty genres so they can be hidden in future
                //Utils.detectEmptyGenres(getContext());

                if (directoriesSet != null && !directoriesSet.isEmpty())
                {
                    //Send broadcasts to scan selected directories
                    for (String directory : directoriesSet)
                    {
                        getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(directory))));
                    }
                    Toast.makeText(getContext(),"Started scanning selected directories for new songs...",Toast.LENGTH_SHORT).show();
                }
                else
                {
                    //If there are none selected directories, then scan whole system
                    //TODO - scan removable SD (one day)
                    getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(Environment.getExternalStorageDirectory())));
                    Toast.makeText(getContext(),"Started scanning system for new songs...",Toast.LENGTH_SHORT).show();
                }

                return false;
            }
        });
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference)
    {
        DialogFragment dialogFragment = null;

        //If the selected preference is Directory Select Preference we then send it
        if (preference instanceof DirectorySelectPreference)
        {
            dialogFragment = new DirectorySelectDialogPreferenceFrag();
            Bundle bundle = new Bundle(1);
            bundle.putString("key",preference.getKey());
            dialogFragment.setArguments(bundle);

            dialogFragment.setTargetFragment(this,0);
            dialogFragment.show(this.getFragmentManager(),"android.support.v7.preference.PreferenceFragment.DIALOG");
        }
        else {
            super.onDisplayPreferenceDialog(preference);
        }
    }






}
