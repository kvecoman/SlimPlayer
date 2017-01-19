package mihaljevic.miroslav.foundry.slimplayer;


import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MediaBrowserTestFragment extends Fragment {

    protected Context mContext;
    protected RecyclerView mRecyclerView;
    protected MediaAdapter mAdapter;

    private MediaBrowserCompat mMediaBrowser;

    private MediaControllerCompat mMediaController;





    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback(){
        @Override
        public void onConnected() {
            MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();


            try
            {
                mMediaController = new MediaControllerCompat(getActivity(),token);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            //MediaControllerCompat.setMediaController(getActivity(),mMediaController);

            mMediaBrowser.subscribe(Const.ALL_SCREEN,mSubscriptionCallbacks);

        }

        @Override
        public void onConnectionSuspended() {
            //The service has crashed
        }

        @Override
        public void onConnectionFailed() {
            //Service refused connection
        }
    };


    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallbacks = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children) {

            mAdapter.swapMediaItemsList(children);

        }

        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {

        }

        @Override
        public void onError(@NonNull String parentId) {

        }

        @Override
        public void onError(@NonNull String parentId, @NonNull Bundle options) {

        }
    };


    public MediaBrowserTestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_slim_recycler, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContext = getContext();

        mAdapter = new MediaAdapter(mContext, null, R.layout.recycler_item, null, null);

        mRecyclerView = (RecyclerView) getView().findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mRecyclerView.getContext(), ((LinearLayoutManager) mRecyclerView.getLayoutManager()).getOrientation()));
        mRecyclerView.setAdapter(mAdapter);



        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(),MediaPlayerService.class),
                mConnectionCallbacks, null);
    }

    @Override
    public void onStart() {
        super.onStart();

        mMediaBrowser.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (MediaControllerCompat.getMediaController(getActivity()) != null)
        {
            //MediaControllerCompat.getMediaController(getActivity()).unregisterCallback();
        }

        mMediaBrowser.disconnect();
    }

    private void buildTransportControls()
    {

    }
}
