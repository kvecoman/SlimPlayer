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
import android.support.v4.app.Fragment;
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
public class NowPlayingFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {

    public static final String SONG_POSITION_KEY = "song_position";

    private SlimPlayerApplication mApplication;

    private Song mSong;
    private List<Song> mSongList;
    private int mPosition;
    private int mCount;


    private View mContentView;
    private SeekBar mSeekBar;


    private MediaPlayer mPlayer;

    private Handler mSeekBarHandler;
    private boolean mSeekBarBound;
    private boolean mOnCreateOptionsCalled;



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

        mSeekBar = (SeekBar) mContentView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        //Check if hosting activity can handle clicks and set it as click listener for content view (play/pause taps)
        if (getContext() instanceof View.OnClickListener)
        {
            mContentView.setOnClickListener(((View.OnClickListener) getContext()));
        }

        //Get song position that this fragment represents
        Bundle args = getArguments();
        if (args != null)
        {
            mPosition = args.getInt(SONG_POSITION_KEY);
        }

        loadSongInfo();

    }

    @Override
    public void onStop() {
        super.onStop();

        //Indicate that we don't need to update seek bar anymore
        mSeekBarBound = false;
        mSeekBarHandler = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        bindSeekBarToPlayer();
        mOnCreateOptionsCalled = true;

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
        Log.d("slim","NowPlayingFragment - loadSongInfo()");

        mCount = mApplication.getMediaPlayerService().getCount();
        mSong = mApplication.getMediaPlayerService().getSong(mPosition);

        mSeekBar.setMax(((int) mSong.getDuration()));

        //Update text views with new info
        ((TextView) mContentView.findViewById(R.id.song_title)).setText(mSong.getTitle());
        ((TextView) mContentView.findViewById(R.id.song_artist)).setText(mSong.getArtist());

    }

    //This connects seek bar to current song that is played by media player service
    public void bindSeekBarToPlayer()
    {
        mPlayer = mApplication.getMediaPlayerService().getMediaPlayer();

        mSeekBarBound = true;

        mSeekBarHandler = new Handler();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null && mApplication.getMediaPlayerService().isPlaying())
                {
                    int position = mPlayer.getCurrentPosition();
                    mSeekBar.setProgress(position);
                }
                if (mSeekBarBound && mPosition == mApplication.getMediaPlayerService().getPosition())
                    mSeekBarHandler.postDelayed(this, 1000);
            }
        });


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
