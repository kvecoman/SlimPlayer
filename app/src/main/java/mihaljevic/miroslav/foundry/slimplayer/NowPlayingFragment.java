package mihaljevic.miroslav.foundry.slimplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 *
 * Fragment that displays info about current song that is played by media service
 *
 * @author Miroslav Mihaljević
 */
public class NowPlayingFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, MediaPlayerService.SongResumeListener, MediaPlayerService.SongPlayListener,
                                                            SlimPlayerApplication.PlayerServiceListener, ViewTreeObserver.OnGlobalLayoutListener{

    private final String TAG = getClass().getSimpleName();

    //TODO - check if this class can be optimized - logs are set now
    public static final String SONG_POSITION_KEY = "song_position";

    private Context mContext;

    private int mPosition;


    private View mContentView;
    private SeekBar mSeekBar;

    private boolean mAlbumArtDisplayed = false;
    private boolean mOnGlobalLayoutCalled = false;
    private Bitmap mAlbumArt;

    private Handler mSeekBarHandler;
    private boolean mSeekBarBound;

    private MediaPlayerService mPlayerService;

    //Runnable that runs on UI thread and updates seek bar
    private Runnable mSeekBarRunnable = new Runnable() {
        @Override
        public void run() {



            if (mSeekBar == null ||
                    !mSeekBarBound ||
                    mPlayerService == null ||
                    mPlayerService.getMediaPlayer() == null)
                return;

            if (mPosition != mPlayerService.getPosition() || mPlayerService.isStopped()) {
                mSeekBar.setProgress(0);
                return;
            }

            //This is down here (and not in first IF) so we allow seek bar to be set to 0 if the player is stopped
            if (!mPlayerService.isPlaying())
                return;


            int position = mPlayerService.getMediaPlayer().getCurrentPosition();
            mSeekBar.setProgress(position);


            if (mSeekBarHandler != null)
                mSeekBarHandler.postDelayed(this, 1000);
        }
    };



    public NowPlayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG,"onCreate()");

        //Make sure that we get onCreateOptionsMenu() call
        setHasOptionsMenu(true);

        //Keep alive this fragment after configuration changes (so we can re-use data)
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG,"onCreateView()");

        // Inflate the layout for this fragment
        mContentView = inflater.inflate(R.layout.fragment_now_playing, container, false);
        return mContentView;
    }

    //Here is usually place for most of the init
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.v(TAG,"onActivityCreated()");

        mContext = getContext();

        mSeekBar = (SeekBar) mContentView.findViewById(R.id.seek_bar);
        mSeekBar.setProgress(0);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBarHandler = new Handler();

        //Start retrieving MediaPlayerService
        SlimPlayerApplication.getInstance().registerPlayerServiceListener(this);


        //Handle taps on screen
        if (mContext instanceof View.OnClickListener)
            mContentView.setOnClickListener((View.OnClickListener)mContext);


        //Get song position that this fragment represents
        Bundle args = getArguments();
        if (args != null)
        {
            mPosition = args.getInt(SONG_POSITION_KEY);
        }


        //Load album art if it is not loaded already (we do this here after we get MediaPlayerService with registering listener)
        if (mAlbumArt == null)
            loadArtAsync();

        //Little hack so we know that UI is already set up when we need to use it
        mContentView.getViewTreeObserver().addOnGlobalLayoutListener(this);

    }

    @Override
    public void onStart() {
        super.onStart();

        //Here we again register MediaPlayerService just in case (it won't be duplicated)
        SlimPlayerApplication.getInstance().registerPlayerServiceListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.v(TAG,"onResume()");

        //Indicate to resume updating seek bar
        bindSeekBarToPlayer();
    }

    //OnCreateOptionsMenu is called when fragment is really visible in pager, we use that phenomena
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG,"onCreateOptionsMenu()");
        //super.onCreateOptionsMenu(menu, inflater);

        if (mPlayerService == null)
            return;

        mPlayerService.registerResumeListener(this);
        mPlayerService.registerPlayListener(this);
        bindSeekBarToPlayer();


    }

    @Override
    public void onPlayerServiceBound(MediaPlayerService playerService) {
        mPlayerService = playerService;

        loadSongInfo();

    }

    @Override
    public void onGlobalLayout()
    {
        //We keep this listener for as long as we need until we get valid width and height values
        if (mContentView.getWidth() <= 0 || mContentView.getHeight() <= 0)
            return;


        mOnGlobalLayoutCalled = true;

        if (Build.VERSION.SDK_INT >= 16)
            mContentView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        else
            mContentView.getViewTreeObserver().removeGlobalOnLayoutListener(this);

        //This will crop and display album art
        if (!mAlbumArtDisplayed && mAlbumArt != null && mOnGlobalLayoutCalled)
            displayArtAsync();

    }



    @Override
    public void onStop() {
        super.onStop();
        Log.v(TAG,"onStop()");

        //Pause updating seek bar
        mSeekBarBound = false;

        SlimPlayerApplication.getInstance().unregisterPlayerServiceListener(this);

        if (mPlayerService != null)
        {
            mPlayerService.unregisterResumeListener(this);
            mPlayerService.unregisterPlayListener(this);
        }
    }

    //Here we do cleanup of things expecting hosting activity will be recreated, so we don't want to leak anything
    @Override
    public void onDetach() {
        super.onDetach();

        mContext = null;
        mContentView = null;
        mAlbumArtDisplayed = false;
        mOnGlobalLayoutCalled = false;
        mSeekBarHandler = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"onDestroy()");

        //End seek bar binding
        mSeekBarHandler = null;

    }


    public void loadSongInfo()
    {
        Log.v(TAG,"loadSongInfo()");

        if (mPlayerService == null)
            return;

        Songs songs = mPlayerService.getSongs();

        mSeekBar.setMax(((int) songs.getDuration(mPosition)));

        //Update text views with new info
        ((TextView) mContentView.findViewById(R.id.song_title)).setText(songs.getTitle(mPosition));
        ((TextView) mContentView.findViewById(R.id.song_artist)).setText(songs.getArtist(mPosition));

    }

    //Tries to load album art if it exist (with async task), returns true if task is going to be executed
    public void loadArtAsync()
    {
        Log.v(TAG,"loadArtAsync()");


        new AsyncTask<Void,Void,Void>()
        {

            @Override
            protected Void doInBackground(Void... params)
            {

                if (mPlayerService == null)
                    return null;

                //Load art
                mAlbumArt = mPlayerService.getSongs().getArt(mPosition);

                return null;

            }

            @Override
            protected void onPostExecute(Void aVoid) {

                //This will crop and display album art
                if (!mAlbumArtDisplayed && mAlbumArt != null && mOnGlobalLayoutCalled)
                    displayArtAsync();
            }
        }.execute();
    }

    private void displayArtAsync()
    {
        Log.v(TAG,"displayArtAsync()");

        if (mContentView.getWidth() <= 0 || mContentView.getHeight() <= 0)
            return;

        final float viewRatio = (float)mContentView.getWidth() / (float)mContentView.getHeight();

        new AsyncTask<Void,Void,Bitmap>(){
            @Override
            protected Bitmap doInBackground(Void... params) {
                //We crop album art so it fits screen(s)
                return Utils.cropBitmapToRatio(mAlbumArt,viewRatio);
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

                mAlbumArtDisplayed = true;
            }

        }.execute();
    }

    //This connects seek bar to current song that is played by media player service
    public void bindSeekBarToPlayer()
    {
        Log.v(TAG,"bindSeekBarToPlayer()");
        mSeekBarBound = true;
        if (mContext instanceof FragmentActivity)
        {
            ((FragmentActivity) mContext).runOnUiThread(mSeekBarRunnable);
        }
    }

    /*private MediaPlayerService getPlayerService()
    {
        return SlimPlayerApplication.getInstance().getMediaPlayerService();
    }*/


    //Called when song is resumed from paused state (when user taps to play)
    @Override
    public void onSongResume() {
        Log.v(TAG,"onSongResume()");
        //Start again updating seek bar
        bindSeekBarToPlayer();
    }

    //This also can come from tap
    @Override
    public void onPlay(Songs songs, int position) {
        Log.v(TAG,"onPlay()");
        bindSeekBarToPlayer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        //Only if touch is coming from user then seek song
        if (mPlayerService != null && mPlayerService.getMediaPlayer() != null && fromUser)
        {
            Log.d(TAG,"onProgressChanged() - user changed progress");
            mPlayerService.getMediaPlayer().seekTo(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
