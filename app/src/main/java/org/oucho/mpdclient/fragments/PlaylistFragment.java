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


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.fragments.adapter.BaseAdapter;
import org.oucho.mpdclient.fragments.adapter.PlaylistListAdapter;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.tools.Utils;

import java.io.IOException;
import java.util.List;


public class PlaylistFragment extends Fragment implements MPDConfig {

    private static final String TAG = "Playlist List Fragment";

    private PlaylistListAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private List<? extends Item> mItems = null;

    public static PlaylistFragment newInstance() {
        return new PlaylistFragment();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void load() {

        try {
            mItems = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylists();
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }

        mAdapter.setData(mItems);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist_list, container, false);

        mSwipeRefreshLayout = rootView.findViewById(R.id.pullToRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(onSwipe);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new PlaylistListAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mAdapter.setData(mItems);

        mRecyclerView.setAdapter(mAdapter);

        load();

        return rootView;
    }



    @Override
    public void onResume() {
        super.onResume();

        // Active la touche back
        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){

                Log.w(TAG, "backspace " + (!MPDApplication.getFragmentPlayer() && !MPDApplication.getFragmentQueue()) );

                if (!MPDApplication.getFragmentPlayer() && !MPDApplication.getFragmentQueue() && !MPDApplication.getFragmentPlaylist()) {
                    Intent backToPrevious = new Intent();
                    backToPrevious.setAction(INTENT_BACK);
                    getContext().sendBroadcast(backToPrevious);
                   // LibraryFragment.backToPrevious();
                    return true;
                }

                return false;
            }
            return false;
        });
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {

            Item playlist = mAdapter.getItem(position);

            switch (view.getId()) {
                case R.id.item_view:

                    String playlistName = playlist.getName();

                    goPlaylist(playlistName);
                    break;

                case R.id.menu_playlist:
                    showMenu(position, view);
                    break;

                default:
                    break;
            }
        }
    };

    private void goPlaylist(String playlist) {
        Fragment fragment = PlaylistContentFragment.newInstance(playlist);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction().addToBackStack("stored_playlist");
        ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom);
        ft.replace(R.id.primary_frame, fragment);
        ft.commit();

    }

    private void showMenu(final int position, final View view) {

        PopupMenu popup = new PopupMenu(getActivity(), view);

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.playlist_item, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {

            final Item playlist = mAdapter.getItem(position);

            switch (item.getItemId()) {
                case R.id.menu_delete:

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.deletePlaylist);
                    builder.setMessage(getResources().getString(R.string.deletePlaylistPrompt, playlist.getName()));

                    builder.setPositiveButton(R.string.deletePlaylist, (dialog, which) -> {

                        try {
                            MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylist().removePlaylist(playlist.getName());
                            if (isAdded()) {
                                Utils.notifyUser(R.string.playlistDeleted, playlist.getName());
                            }

                            load();

                        } catch (final IOException | MPDException e) {
                            Log.e(TAG, "Error delete playlist: " + e);
                        }
                    });

                    builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
                        // This constructor is intentionally empty, pourquoi ? parce que !
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();

                    break;

                default:
                    break;
            }
            return false;
        });

        popup.show();
    }

    private final SwipeRefreshLayout.OnRefreshListener onSwipe = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            mSwipeRefreshLayout.setRefreshing(false);
            load();

        }
    };

}
