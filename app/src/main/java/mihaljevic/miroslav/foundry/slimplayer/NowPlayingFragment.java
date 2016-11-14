package mihaljevic.miroslav.foundry.slimplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

//TODO - continue here - make seek bar work when playing song, and swithching songs with swipe left/right
/**
 *
 * Fragment that displays info about current song that is played by media service
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingFragment extends Fragment implements MediaPlayerService.MediaPlayerListener {

    private Song mSong;
    private List<Song> mSongList;
    private int mPosition;
    private int mCount;

    private View mParentView;

    private MediaPlayerService mPlayerService;
    private boolean mServiceBound = true;

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
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            NowPlayingFragment.this.mServiceBound = false;
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




    }

    @Override
    public void onSongChanged(List<Song> songList, int position) {
        mSongList = songList;
        mPosition = position;

        mCount = songList.size();
        mSong = mSongList.get(mPosition);

        //Update text views with new info
        ((TextView)mParentView.findViewById(R.id.song_title)).setText(mSong.getTitle());
        ((TextView)mParentView.findViewById(R.id.song_artist)).setText(mSong.getArtist());
    }
}
