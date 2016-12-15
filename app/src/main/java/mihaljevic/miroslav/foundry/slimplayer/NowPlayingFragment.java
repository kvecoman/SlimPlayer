package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

/**
 *
 * Fragment that displays info about current song that is played by media service
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, MediaPlayerService.SongResumeListener, MediaPlayerService.SongPlayListener, View.OnClickListener{

    public static final String SONG_POSITION_KEY = "song_position";

    private SlimPlayerApplication mApplication;

    private Context mContext;

    /*private Song mSong;
    private List<Song> mSongList;
    private int mCount;*/
    private int mPosition;


    private View mContentView;
    private SeekBar mSeekBar;


    private MediaPlayer mPlayer;

    private Handler mSeekBarHandler;
    private boolean mSeekBarBound;
    //private boolean mOnCreateOptionsCalled;

    //Runnable that runs on UI thread and updates seek bar
    private Runnable mSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSeekBarBound && mApplication.getMediaPlayerService().isPlaying() && mPosition == mApplication.getMediaPlayerService().getPosition()) {

                if (mPlayer != null) {
                    int position = mPlayer.getCurrentPosition();
                    mSeekBar.setProgress(position);
                }
                mSeekBarHandler.postDelayed(this, 1000);
            }
            else if(mPosition != mApplication.getMediaPlayerService().getPosition())
            {
                mSeekBar.setProgress(0);
            }


        }
    };



    public NowPlayingFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        setHasOptionsMenu(true);

        // Inflate the layout for this fragment
        mContentView = inflater.inflate(R.layout.fragment_now_playing, container, false);
        return mContentView;
    }

    //Here is usually place for most of the init
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


        mApplication = ((SlimPlayerApplication) getContext().getApplicationContext());

        mContext = getContext();

        mSeekBar = (SeekBar) mContentView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBarHandler = new Handler();

        mPlayer = mApplication.getMediaPlayerService().getMediaPlayer();

        //Handle taps on screen
        mContentView.setOnClickListener(this);


        //Get song position that this fragment represents
        Bundle args = getArguments();
        if (args != null)
        {
            mPosition = args.getInt(SONG_POSITION_KEY);
        }

        loadSongInfo();

    }

    @Override
    public void onResume() {
        super.onResume();

        //Indicate to resume updating seek bar
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
        mApplication.getMediaPlayerService().unregisterResumeListener(this);
        mApplication.getMediaPlayerService().unregisterPlayListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //super.onCreateOptionsMenu(menu, inflater);

        //OnCreateOptionsMenu is called when fragment is really visible in pager, we use that phenomena
        mApplication.getMediaPlayerService().registerResumeListener(this);
        mApplication.getMediaPlayerService().registerPlayListener(this);
        bindSeekBarToPlayer();

    }

    //TODO - this code can go back to activity
    //Handle onscreen taps, change between play/pause
    @Override
    public void onClick(View v) {

        MediaPlayerService playerService = mApplication.getMediaPlayerService();

        if (playerService.isReadyToPlay())
        {
            if (playerService.isPlaying())
            {
                playerService.pause();
            }
            else
            {
                if (getContext() instanceof NowPlayingActivity)
                {
                    playerService.resumeOrPlay(((NowPlayingActivity) getContext()).getPager().getCurrentItem());
                }
            }
        }
    }

   /* @Override
    public void onClick(View v) {
        MediaPlayerService playerService = mApplication.getMediaPlayerService();

        if (playerService.isReadyToPlay())
        {
            if (playerService.isPlaying())
            {
                playerService.pause();
            }
            else
            {
                playerService.resume();
            }
        }
    }*/

    public void loadSongInfo()
    {

        CursorSongs songs = mApplication.getMediaPlayerService().getSongs();
        //mCount = songs.getCount();

        mSeekBar.setMax(((int) songs.getDuration(mPosition)));

        //Update text views with new info
        ((TextView) mContentView.findViewById(R.id.song_title)).setText(songs.getTitle(mPosition));
        ((TextView) mContentView.findViewById(R.id.song_artist)).setText(songs.getArtist(mPosition));

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


    //Called when song is resumed from paused state (when user taps to play)
    @Override
    public void onSongResume() {
        //Start again updating seek bar
        bindSeekBarToPlayer();
    }

    //This also can come from tap
    @Override
    public void onPlay(CursorSongs songs, int position) {
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
