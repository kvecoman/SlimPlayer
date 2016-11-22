package mihaljevic.miroslav.foundry.slimplayer;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays only songs, and responds appropriately to click
 *
 * @author Miroslav Mihaljević
 */
public class SongListFragment extends SlimListFragment {

    //TODO - move this somewhere sane (idk)
    protected MediaPlayerService mPlayerService;
    protected boolean mServiceBound = false;

    protected boolean mSelectMode = false;

    protected List<Song> mSongList;

    //Here we set-up service connection that is used when service is started
    protected ServiceConnection mServiceConnection = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.MediaPlayerBinder playerBinder = (MediaPlayerService.MediaPlayerBinder)service;
            SongListFragment.this.mPlayerService = playerBinder.getService();
            SongListFragment.this.mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            SongListFragment.this.mServiceBound = false;
        }
    };


    public SongListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_slim_list, container, false);
    }

    //Most of the init is done here
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        //TODO - maybe try block for this, looks dangerous
        //Here we init MediaPlayerService
        Intent playerServiceIntent = new Intent(mContext, MediaPlayerService.class);
        mContext.bindService(playerServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);



        //Handler for long click
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (!mSelectMode)
                {
                    //If we are not in select mode, activate it
                    ((ListView)parent).setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                    ((ListView)parent).setItemChecked(position, true);
                    mSelectMode = true;
                    getActivity().invalidateOptionsMenu();
                }
                return true;
            }
        });
    }

    //Here we load all songs in songList when Cursor loader is finished
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);

        Log.d("slim",mCurrentScreen + " - onLoadFinished()");

        mSongList = getSongListFromCursor(data);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        if (mSelectMode)
        {
            //We are selecting items
            //TODO - make this look sane
        }
        else
        {
            //Pass list of songs from which we play and play current position
            //TODO - maybe optimize this?
            mPlayerService.setSongList(mSongList);
            mPlayerService.play(position);


            //TODO - continue here -  move this - notification stuff
            RemoteViews notificationView = new RemoteViews(getContext().getPackageName(),R.layout.notification_player4);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());

            notificationView.setTextViewText(R.id.notification_title,mSongList.get(position).getTitle());

            int fontSize = 20;
            notificationView.setImageViewBitmap(R.id.notification_close,renderFont(getContext(), getString(R.string.icon_close), Color.LTGRAY,fontSize));
            notificationView.setImageViewBitmap(R.id.notification_previous,renderFont(getContext(), getString(R.string.icon_previous), Color.LTGRAY,fontSize));
            notificationView.setImageViewBitmap(R.id.notification_play,renderFont(getContext(), getString(R.string.icon_play), Color.LTGRAY,fontSize));
            notificationView.setImageViewBitmap(R.id.notification_next,renderFont(getContext(), getString(R.string.icon_next), Color.LTGRAY,fontSize));

           /* builder.setOngoing(true);
            builder.setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Content title").setContentText("Content text");
            builder.setContentIntent(PendingIntent.getActivity(getContext(),0,new Intent(getContext(),NowPlayingActivity.class),0));
            builder.setTicker("Playing").setAutoCancel(true).setContent(notificationView);*/

            builder.setSmallIcon(R.mipmap.ic_launcher).setContent(notificationView).setContentIntent(PendingIntent.getActivity(getContext(),0,new Intent(getContext(),NowPlayingActivity.class),0));

            Notification notification = builder.build();
            NotificationManager notificationManager = (NotificationManager)getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(5,notification);

            //Use bundle to send some starting info to NowPlayingActivity
            Intent intent = new Intent(mContext,NowPlayingActivity.class);

            intent.putExtra(NowPlayingActivity.SONG_COUNT_KEY,mSongList.size());
            intent.putExtra(NowPlayingActivity.SONG_POSITION_KEY,position);

            startActivity(intent);
        }

    }

    //TODO - this function is for notification player, move somewhere else
    public Bitmap renderFont(Context context, String text, int color, float fontSizeSP)
    {
        int fontSizePX = convertDipToPix(context, fontSizeSP);
        int pad = (fontSizePX / 9);
        Paint paint = new Paint();
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/icons.ttf");
        paint.setAntiAlias(true);
        paint.setTypeface(typeface);
        paint.setColor(color);
        paint.setTextSize(fontSizePX);

        int textWidth = (int) (paint.measureText(text) + pad*2);
        int height = (int) (fontSizePX / 0.75);
        Bitmap bitmap = Bitmap.createBitmap(textWidth, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float xOriginal = pad;
        canvas.drawText(text, xOriginal, fontSizePX, paint);
        return bitmap;

    }

    //TODO - used in renderFont(), also move
    public int convertDipToPix(Context context,float dip)
    {
        int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
        return value;
    }

    @Override
    public boolean onBackPressed() {

        //Here we store if the back button event is consumed
        boolean backConsumed = false;

        if (mSelectMode)
        {
            //Deselect everything
            mSelectMode = false;
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            getListView().clearChoices();
            getListView().requestLayout();
            backConsumed = true;
        }

        return backConsumed;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        //TODO - maybe delete this override, isnt used for anything
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        MenuItem playlistAddMenuItem = menu.findItem(R.id.playlist_add);

        if (mSelectMode)
        {
            //If we are selecting items then we want option to add them to playlist
            playlistAddMenuItem.setVisible(true);
        }
        else
        {
            //Hide option to add to playlist
            playlistAddMenuItem.setVisible(false);
        }

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {



        if (mSelectMode)
        {
            if (item.getItemId() == R.id.playlist_add)
            {

                //Get all checked positions
                SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();

                Log.d("slim", getListView().getCount() + " of list items ----- " + checkedPositions.size() + " of items in boolean array");

                Cursor cursor = mCursorAdapter.getCursor();
                List<String> idList = new ArrayList<>(checkedPositions.size());

                //Transfer IDs from selected songs to ID list
                for (int i = 0; i < checkedPositions.size();i++)
                {
                    int position = checkedPositions.keyAt(i);
                    Log.d("slim","Position of checked song: " + position);

                    cursor.moveToPosition(position);
                    idList.add(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));

                }
                //Here we call add to playlists activity and pass ID list
                Intent intent = new Intent(mContext,AddToPlaylistActivity.class);
                intent.putExtra(AddToPlaylistActivity.ID_LIST_KEY,(ArrayList)idList);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {

        //Unbind service
        if (mServiceBound)
            mContext.unbindService(mServiceConnection);

        super.onDestroy();
    }

    //Here we create an ArrayList of all songs from cursor
    public List<Song> getSongListFromCursor(Cursor cursor)
    {
        List<Song> songList = new ArrayList<>();
        Song song;

        //If there are nothing in cursor just remove empty list
        if (cursor.getCount() == 0)
            return songList;

        //Transfer all data from cursor to ArrayList of songs
        cursor.moveToFirst();
        do
        {
            song = new Song(
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));

            songList.add(song);

        }
        while (cursor.moveToNext());

        return songList;
    }
}
