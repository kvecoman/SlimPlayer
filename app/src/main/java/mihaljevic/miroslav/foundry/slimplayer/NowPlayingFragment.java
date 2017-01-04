package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 *
 * Fragment that displays info about current song that is played by media service
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, MediaPlayerService.SongResumeListener, MediaPlayerService.SongPlayListener, View.OnClickListener{

    public static final String SONG_POSITION_KEY = "song_position";

    private Context mContext;

    private int mPosition;


    private View mContentView;
    private SeekBar mSeekBar;


    private MediaPlayer mPlayer;

    private Handler mSeekBarHandler;
    private boolean mSeekBarBound;

    //Runnable that runs on UI thread and updates seek bar
    private Runnable mSeekBarRunnable = new Runnable() {
        @Override
        public void run() {

            if (mSeekBar == null ||
                    !mSeekBarBound ||
                    !getPlayerService().isPlaying())
                return;

            if (mPosition != getPlayerService().getPosition()) {
                mSeekBar.setProgress(0);
                return;
            }

            if (mPlayer != null)
            {
                int position = mPlayer.getCurrentPosition();
                mSeekBar.setProgress(position);
            }

            if (mSeekBarHandler != null)
                mSeekBarHandler.postDelayed(this, 1000);
        }
    };



    public NowPlayingFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mContentView = inflater.inflate(R.layout.fragment_now_playing, container, false);
        return mContentView;
    }

    //Here is usually place for most of the init
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Make sure that we get onCreateOptionsMenu() call
        setHasOptionsMenu(true);

        mContext = getContext();

        mSeekBar = (SeekBar) mContentView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBarHandler = new Handler();

        mPlayer = getPlayerService().getMediaPlayer();

        //Handle taps on screen
        mContentView.setOnClickListener(this);


        //Get song position that this fragment represents
        Bundle args = getArguments();
        if (args != null)
        {
            mPosition = args.getInt(SONG_POSITION_KEY);
        }

        loadSongInfo();

        //Little hack so we know that UI is already set up when we need to use it
        mContentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if (Build.VERSION.SDK_INT >= 16)
                    mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                else
                    mContentView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                //Load album art if it exist
                loadArtAsync();

            }
        });



    }

    @Override
    public void onResume() {
        super.onResume();

        //Indicate to resume updating seek bar
        bindSeekBarToPlayer();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);

        //OnCreateOptionsMenu is called when fragment is really visible in pager, we use that phenomena
        getPlayerService().registerResumeListener(this);
        getPlayerService().registerPlayListener(this);
        bindSeekBarToPlayer();


    }

    @Override
    public void onStop() {
        super.onStop();

        //Pause updating seek bar
        mSeekBarBound = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //End seek bar binding
        //mSeekBarBound = false;
        mSeekBarHandler = null;
        getPlayerService().unregisterResumeListener(this);
        getPlayerService().unregisterPlayListener(this);
    }



    //TODO - this code can go back to activity
    //Handle onscreen taps, change between play/pause
    @Override
    public void onClick(View v) {

        if (getPlayerService().isReadyToPlay())
        {
            if (getPlayerService().isPlaying())
            {
                getPlayerService().pause();
            }
            else
            {
                if (getContext() instanceof NowPlayingActivity)
                {
                    getPlayerService().resumeOrPlay(((NowPlayingActivity) getContext()).getPager().getCurrentItem());
                }
            }
        }
    }

    public void loadSongInfo()
    {

        Songs songs = getPlayerService().getSongs();

        mSeekBar.setMax(((int) songs.getDuration(mPosition)));

        //Update text views with new info
        ((TextView) mContentView.findViewById(R.id.song_title)).setText(songs.getTitle(mPosition));
        ((TextView) mContentView.findViewById(R.id.song_artist)).setText(songs.getArtist(mPosition));



    }

    //Tries to load album art if it exist (with async task)
    public void loadArtAsync()
    {
        final float viewRatio = (float)mContentView.getWidth() / (float)mContentView.getHeight();

        new AsyncTask<Void,Void,Bitmap>(){
            @Override
            protected Bitmap doInBackground(Void... params) {
                //We crop album art so it fits screen(s)
                return Utils.cropBitmapToRatio(getPlayerService().getSongs().getArt(mPosition),viewRatio);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null)
                    return;

                //Update background with album art if it exist
                if(Build.VERSION.SDK_INT >= 16) {
                    mContentView.setBackground(new BitmapDrawable(getResources(),bitmap));
                }
                else
                {
                    mContentView.setBackgroundDrawable(new BitmapDrawable(getResources(),bitmap));
                }
            }
        }.execute();

    }

    //This connects seek bar to current song that is played by media player service
    public void bindSeekBarToPlayer()
    {
        mSeekBarBound = true;
        if (mContext instanceof FragmentActivity)
        {
            ((FragmentActivity) mContext).runOnUiThread(mSeekBarRunnable);
        }
    }

    private MediaPlayerService getPlayerService()
    {
        return SlimPlayerApplication.getInstance().getMediaPlayerService();
    }


    //Called when song is resumed from paused state (when user taps to play)
    @Override
    public void onSongResume() {
        //Start again updating seek bar
        bindSeekBarToPlayer();
    }

    //This also can come from tap
    @Override
    public void onPlay(Songs songs, int position) {
        bindSeekBarToPlayer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        //Only if touch is coming from user then seek song
        if (mPlayer != null && fromUser)
        {
            mPlayer.seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
