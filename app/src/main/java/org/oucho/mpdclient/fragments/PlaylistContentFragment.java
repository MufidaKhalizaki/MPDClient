package org.oucho.mpdclient.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.fragments.adapter.BaseAdapter;
import org.oucho.mpdclient.fragments.adapter.PlaylistContentAdapter;
import org.oucho.mpdclient.fragments.loader.PlaylistContentLoader;
import org.oucho.mpdclient.helpers.MPDControl;
import org.oucho.mpdclient.helpers.QueueControl;
import org.oucho.mpdclient.mpd.MPDPlaylist;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.widgets.CustomSwipe;
import org.oucho.mpdclient.widgets.DragRecyclerView;

import java.io.IOException;
import java.util.List;


public class PlaylistContentFragment extends Fragment implements MPDConfig {

    private static final String PARAM_PLAYLIST_NAME = "playlist_name";

    private static final String TAG = "Playlist Content";

    private String mPlaylistName;

    private PlaylistContentAdapter mAdapter;

    private boolean first = true;

    private String currentPlaylist = "";


    private final LoaderManager.LoaderCallbacks<List<? extends Item>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<? extends Item>>() {

        @Override
        public Loader<List<? extends Item>> onCreateLoader(int id, Bundle args) {
            return new PlaylistContentLoader(getContext(), mPlaylistName);
        }

        @Override
        public void onLoadFinished(Loader<List<? extends Item>> loader, List<? extends Item> list) {
            mAdapter.setData(list, mPlaylistName);
        }

        @Override
        public void onLoaderReset(Loader<List<? extends Item>> loader) {}
    };


    public static PlaylistContentFragment newInstance(String playlist) {
        PlaylistContentFragment fragment = new PlaylistContentFragment();

        Bundle args = new Bundle();

        args.putString(PARAM_PLAYLIST_NAME, playlist);

        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setHasOptionsMenu(true);
        if (args != null) {
            mPlaylistName = args.getString(PARAM_PLAYLIST_NAME);
        }

        load();

        MPDApplication.setFragmentPlaylist(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist_content, container, false);

        setTitre();

        DragRecyclerView mRecyclerView = rootView.findViewById(R.id.list_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setOnItemMovedListener(mOnItemtouchListener);

        mAdapter = new PlaylistContentAdapter(mRecyclerView);

        ItemTouchHelper.Callback callback = new CustomSwipe(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapter);


        return rootView;
    }


    private void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = (position, view) -> {

        switch (view.getId()) {
            case R.id.item_view:

                goPlaylist(position);

                break;
            case R.id.menu_button:
                break;
            default:
                break;
        }
    };


    private final DragRecyclerView.OnItemMovedListener mOnItemtouchListener = new DragRecyclerView.OnItemMovedListener() {

        @Override
        public void onItemMoved(final int oldPosition,int newPosition) {
            mAdapter.moveItem(oldPosition, newPosition);

            // Rustine drag
            if (newPosition == -1)
                newPosition = 0;

            final int newP = newPosition;

            Thread mThread = new Thread() {
                public void run() {

                    try {
                        MPDApplication.getInstance().oMPDAsyncHelper.oMPD.movePlaylistSong(mPlaylistName, oldPosition, newP);
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to move a track on the queue.", e);
                    }
                }};
            mThread.setPriority(Thread.MIN_PRIORITY);
            mThread.start();
        }

    };

    private void goPlaylist(final int position) {

        if (!currentPlaylist.equals(mPlaylistName)) {
            currentPlaylist = mPlaylistName;

            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.playPlaylist(mPlaylistName);
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to add.", e);
            }
        }


        if (first) {
            MPDControl.run(R.id.barB_stop);
            new Handler().postDelayed(() -> play(position), 500);
        } else {
            play(position);
        }

        first = false;

    }


    private void play(int position) {

        MPDPlaylist playlist = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylist();

        List<Music> musics = playlist.getMusicList();
        final Music tptp = musics.get(position);
        QueueControl.run(QueueControl.SKIP_TO_ID, tptp.getSongId());

    }

    @Override
    public void onPause() {
        super.onPause();
        MPDApplication.setFragmentPlaylist(false);

    }

    private void setTitre() {

        new Handler().postDelayed(() -> {

            Intent intent = new Intent();
            intent.setAction("org.oucho.mdpclient.setTitle");
            intent.putExtra("Titre", mPlaylistName);
            intent.putExtra("Second", "");
            intent.putExtra("elevation", true);

            getContext().sendBroadcast(intent);

        }, 300);

    }
}




