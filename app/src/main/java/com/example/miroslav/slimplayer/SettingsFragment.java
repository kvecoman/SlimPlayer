package com.example.miroslav.slimplayer;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

                //TODO - actually do something here?

                //Here we update song list and delete empty genres
                //TODO - put heavy work in seperate thread
                Toast.makeText(getActivity(),"This button currently does nothing...",Toast.LENGTH_SHORT).show();
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
