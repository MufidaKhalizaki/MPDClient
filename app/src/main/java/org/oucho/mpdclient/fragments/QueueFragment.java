/*
 * MPDclient - Music Player Daemon (MPD) remote for android
 * Copyright (C) 2017  Old-Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oucho.mpdclient.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.MainActivity;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.fragments.adapter.BaseAdapter;
import org.oucho.mpdclient.fragments.adapter.QueueAdapter;
import org.oucho.mpdclient.helpers.QueueControl;
import org.oucho.mpdclient.mpd.MPDPlaylist;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.event.StatusChangeListener;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.widgets.CustomLayoutManager;
import org.oucho.mpdclient.widgets.CustomSwipe;
import org.oucho.mpdclient.widgets.DragRecyclerView;

import java.io.IOException;
import java.util.List;


public class QueueFragment extends Fragment implements StatusChangeListener, MPDConfig {


    private static final String TAG = "QueueFragment";

    private QueueAdapter mAdapter;

    private DragRecyclerView mRecyclerView;

    private void load() {

        MPDPlaylist playlist = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylist();

        List<Music> mList = playlist.getMusicList();
        mAdapter.setData(mList);

        for (int i = 0; i < mList.size(); i++) {

            Item item = mList.get(i);
            String song = ((Music) item).getTitle() ;

            Log.d(TAG, "load(): " +  MainActivity.getCurrentSong() + ", " + song);

            if ( MainActivity.getCurrentSong().equals(song)) {

                try {
                    mRecyclerView.smoothScrollToPosition(i);
                } catch (NullPointerException | IllegalStateException ignore) {
                    Log.w(TAG, "error smoothScrollToPosition" );
                }
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        MPDApplication.setFragmentQueue(true);
        final View view = inflater.inflate(R.layout.fragment_queue, container, false);

        mRecyclerView = view.findViewById(R.id.queue_view);
        mRecyclerView.setLayoutManager(new CustomLayoutManager(MPDApplication.getInstance()));
        mRecyclerView.setOnItemMovedListener(mOnItemtouchListener);

        mAdapter = new QueueAdapter(mRecyclerView);

        ItemTouchHelper.Callback callback = new CustomSwipe(mAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);

        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapter);

        load();

        return view;
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Item item = mAdapter.getItem(position);
            Music music = (Music) item;

            switch (view.getId()) {
                case R.id.item_view:
                    QueueControl.run(QueueControl.SKIP_TO_ID, music.getSongId());

                    new Handler().postDelayed(() -> mAdapter.notifyDataSetChanged(), 300);

                    break;
                default:
                    break;
            }
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
                        MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylist().moveByPosition(oldPosition, newP);
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to move a track on the queue.", e);
                    }
                }};

            mThread.setPriority(Thread.MIN_PRIORITY);
            mThread.start();
        }

    };


    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent blur = new Intent();
        blur.setAction(INTENT_QUEUE_BLUR);
        blur.putExtra("blur", true);

        getContext().sendBroadcast(blur);
    }

    public static QueueFragment newInstance() {
        QueueFragment fragment = new QueueFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.mpd_playlistmenu, menu);
        menu.removeItem(R.id.PLM_EditPL);
    }


    @Override
    public void onDetach() {
        super.onDetach();

        MPDApplication.setFragmentQueue(false);

        Intent blur = new Intent();
        blur.setAction(INTENT_QUEUE_BLUR);
        blur.putExtra("blur", false);
        getContext().sendBroadcast(blur);

    }

    @Override
    public void onPause() {
        MPDApplication.getInstance().oMPDAsyncHelper.removeStatusChangeListener(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        MPDApplication.getInstance().oMPDAsyncHelper.addStatusChangeListener(this);
        new Thread(this::load).start();
    }


    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        load();
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {

        new Handler().postDelayed(() -> mAdapter.notifyDataSetChanged(), 300);
    }

    @Override
    public void randomChanged(final boolean random) {}
    @Override
    public void repeatChanged(final boolean repeating) {}
    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {}
    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {}
    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {}
    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {}
    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {}

}
