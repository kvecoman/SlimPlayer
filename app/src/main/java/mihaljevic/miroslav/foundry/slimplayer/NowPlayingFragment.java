package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

//TODO - continue here - make seek bar work when playing song, and swithching songs with swipe left/right
/**
 *
 * Fragment that displays info about current song that is played by media service
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingFragment extends Fragment implements MediaPlayerService.MediaPlayerListener, SeekBar.OnSeekBarChangeListener {

    private Song mSong;
    private List<Song> mSongList;
    private int mPosition;
    private int mCount;

    private View mParentView;
    private SeekBar mSeekBar;

    private MediaPlayerService mPlayerService;
    private boolean mServiceBound;

    private MediaPlayer mPlayer;

    private Handler mSeekBarHandler;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            NowPlayingFragment.this.mPlayerService = playerBinder.getService();
            NowPlayingFragment.this.mServiceBound = true;

            //When we are connected request current play info
            mPlayerService.setMediaPlayerListener(NowPlayingFragment.this);
            mPlayerService.requestCurrentPlayInfo();

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
        // Inflate the layout for this fragment
        mParentView = inflater.inflate(R.layout.fragment_now_playing, container, false);
        return mParentView;
    }

    //Here is usually place for most of the init
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //TODO - maybe try block for this, looks dangerous
        //Here we init MediaPlayerService
        Intent playerServiceIntent = new Intent(getContext(), MediaPlayerService.class);
        getContext().bindService(playerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        mSeekBar = (SeekBar) mParentView.findViewById(R.id.seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);

        Log.d("slim","NowPlayingFragment - onActivityCreated()");
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mServiceBound)
        {
            //TODO - check the flow of all callback (log them all)
            mPlayerService.setMediaPlayerListener(this);
            mPlayerService.requestCurrentPlayInfo();
        }

        Log.d("slim","NowPlayingFragment - onStart()");
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
        mSongList = songList;
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
        ((TextView)mParentView.findViewById(R.id.song_title)).setText(mSong.getTitle());
        ((TextView)mParentView.findViewById(R.id.song_artist)).setText(mSong.getArtist());

        Log.d("slim","NowPlayingFragment - onSongChanged()");
    }

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
