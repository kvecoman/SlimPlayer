package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

/**
 *Base activity that shares common options in options menu
 *
 *@author Miroslav Mihaljević
 *
 */

public abstract class SlimActivity extends AppCompatActivity {

    public static String REQUEST_CODE_KEY = "request_code";


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Inflate options menu
        getMenuInflater().inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //Activate corresponding action that was selected in menu
        switch (id)
        {
            case R.id.toggle_repeat:
                //Toggle repeat action - whether to repeat playlist or not at the end
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                boolean repeat = preferences.getBoolean(getString(R.string.pref_key_repeat),true);

                repeat = !repeat;

                if (repeat)
                    item.setIcon(R.drawable.ic_repeat_white_24dp);
                else
                    item.setIcon(R.drawable.ic_repeat_gray_24dp);

                preferences.edit().putBoolean(getString(R.string.pref_key_repeat),repeat).apply();
                SlimPlayerApplication.getInstance().getMediaPlayerService().refreshRepeat();
                break;
            case R.id.settings:
                //Start settings screen
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.exit:
                //Exit the app
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    //Lets make sure that we always send request code when starting activity for result
    /*@Override
    public void startActivityForResult(Intent intent, int requestCode) {

        intent.putExtra(REQUEST_CODE_KEY,requestCode);
        super.startActivityForResult(intent, requestCode);
    }*/
}
