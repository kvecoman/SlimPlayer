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
public class NowPlayingFragment extends Fragment implements MediaPlayerService.MediaPlayerListener, SeekBar.OnSeekBarChangeListener {

    public static final String SONG_POSITION_KEY = "song_position";

    private Song mSong;
    private List<Song> mSongList;
    private int mPosition;
    private int mCount;

    private LayoutInflater mInflater;
    private View mParentView;
    private View mContentView;
    private View mLeftView;
    private View mRightView;
    private SeekBar mSeekBar;

    private MediaPlayerService mPlayerService;
    private boolean mServiceBound;

    private MediaPlayer mPlayer;

    private Handler mSeekBarHandler;
    private boolean mSeekBarBound;
    private boolean mOnCreateOptionsCalled;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            NowPlayingFragment.this.mPlayerService = playerBinder.getService();
            NowPlayingFragment.this.mServiceBound = true;

            //When we are connected request current play info
            mPlayerService.setMediaPlayerListener(NowPlayingFragment.this);
            mPlayerService.setCurrentPlayInfoToListener();

            loadSongInfo();

            //This is a occurance that happens if this fragment is the first one after NowPlayingActivity loads
            //This is connected to flow of code in OnCreateOptionsMenu()
            if (mOnCreateOptionsCalled && !mSeekBarBound)
                bindSeekBarToPlayer();

            Log.d("slim","NowPlayingFragment - onServiceConnected()");

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            NowPlayingFragment.this.mServiceBound = false;
            Log.d("slim","NowPlayingFragment - onServiceDisconnected()");
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

        //TODO - maybe try block for this, looks dangerous
        //Here we init MediaPlayerService
        Intent playerServiceIntent = new Intent(getContext(), MediaPlayerService.class);
        getContext().bindService(playerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        mSeekBar = (SeekBar) mContentView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        //Get song position that this fragment represents
        Bundle args = getArguments();
        if (args != null)
        {
            mPosition = args.getInt(SONG_POSITION_KEY);
        }


        Log.d("slim","NowPlayingFragment - onActivityCreated()");
    }

    @Override
    public void onStart() {
        super.onStart();



        Log.d("slim","NowPlayingFragment - onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("slim","NowPlayingFragment - onResume()");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);


        if (mServiceBound)
        {
            //TODO - find better way?
            //Explanation - onCreateOptionsMenu is almost only function that is called ONLY when fragment is REALLY
            //... visible and not while he is created and cached, and we need that because only one fragment can be
            //connected to media player so the SeekBar would work and it has to be current selected fragment in view pager
            //Connect seek bar to media player
            bindSeekBarToPlayer();
        }


        mOnCreateOptionsCalled = true;

        Log.d("slim","NowPlayingFragment - onCreateOptionsMenu()");
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mServiceBound)
            mPlayerService.setMediaPlayerListener(null);



        Log.d("slim","NowPlayingFragment - onStop()");
    }

    @Override
    public void onDestroy() {

        //Unbind service
        if (mServiceBound)
            getContext().unbindService(mServiceConnection);

        super.onDestroy();

        Log.d("slim","NowPlayingFragment - onDestroy()");
    }

    @Override
    public void onSongChanged(List<Song> songList, int position) {
       /* mSongList = songList;
        mPosition = position;

        mCount = songList.size();
        mSong = mSongList.get(mPosition);

        mPlayer = mPlayerService.getMediaPlayer();

        mSeekBarHandler = new Handler();

        mSeekBar.setMax(((int) mSong.getDuration()));
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null)
                {
                    int position = mPlayer.getCurrentPosition();
                    mSeekBar.setProgress(position);
                }
                mSeekBarHandler.postDelayed(this, 1000);
            }
        });

        //Update text views with new info
        ((TextView) mContentView.findViewById(R.id.song_title)).setText(mSong.getTitle());
        ((TextView) mContentView.findViewById(R.id.song_artist)).setText(mSong.getArtist());


        Log.d("slim","NowPlayingFragment - onSongChanged()");*/
    }

    public void loadSongInfo()
    {
        //Set a lot of member variables and update views with it (from MediaPlayerService)
        if (mServiceBound)
        {
            mCount = mPlayerService.getCount();
            mSong = mPlayerService.getSong(mPosition);

            mSeekBar.setMax(((int) mSong.getDuration()));


            //Update text views with new info
            ((TextView) mContentView.findViewById(R.id.song_title)).setText(mSong.getTitle());
            ((TextView) mContentView.findViewById(R.id.song_artist)).setText(mSong.getArtist());


            Log.d("slim","NowPlayingFragment - loadSongInfo()");
        }
    }

    //TODO - CONTINUE HERE - swiping screens works, now find way to call this when fragment is made active(maybe onStart method)
    //This connects seek bar to current song that is played by media player service
    public void bindSeekBarToPlayer()
    {
        mPlayer = mPlayerService.getMediaPlayer();

        mSeekBarHandler = new Handler();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null)
                {
                    int position = mPlayer.getCurrentPosition();
                    mSeekBar.setProgress(position);
                }
                mSeekBarHandler.postDelayed(this, 1000);
            }
        });

        mSeekBarBound = true;
    }

    /*
    CODE FOR CUSTOM SWIPING WITHOUT VIEW PAGER
    public void prepareSwipeViews()
    {
        if (mPosition > 0)
        {
            updateContentView(mLeftView, mSongList.get(mPosition-1));
        }

        if (mPosition < (mCount - 1))
        {
            updateContentView(mRightView, mSongList.get(mPosition+1));
        }
    }

    public void updateContentView(View contentView, Song song)
    {
        ((TextView) contentView.findViewById(R.id.song_title)).setText(song.getTitle());
        ((TextView) contentView.findViewById(R.id.song_artist)).setText(song.getArtist());
    }
*/


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mPlayer != null && fromUser)
        {
            mPlayer.seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
