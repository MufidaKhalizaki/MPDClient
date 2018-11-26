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

package org.oucho.mpdclient.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.fragments.AlbumListFragment;
import org.oucho.mpdclient.fragments.AlbumSongsFragment;
import org.oucho.mpdclient.fragments.adapter.BaseAdapter;
import org.oucho.mpdclient.fragments.adapter.PlaylistListAdapter;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;

import java.io.IOException;
import java.util.List;


public class PlaylistPickerDialog extends DialogFragment implements MPDConfig{

    private static final String TAG = "PlaylistPickerDialog";

    private PlaylistListAdapter mAdapter;

    private List<? extends Item> mItems = null;

    private final OnClickListener mOnClickListener = v -> {
        switch (v.getId()) {
            case R.id.new_playlist:
                CreatePlaylistDialog dialog = CreatePlaylistDialog.newInstance();
                dialog.setOnPlaylistCreatedListener(() -> {

                    Toast.makeText(getContext(), "Playlist créée", Toast.LENGTH_SHORT).show();

                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    assert imm != null;
                    imm.toggleSoftInput(InputMethodManager.RESULT_HIDDEN,0);
                    dismiss();

                });
                dialog.show(getChildFragmentManager(), "create_playlist");

                break;
            default: //do nothing
                break;
        }


    };

    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Item playlist = mAdapter.getItem(position);

            addToPlaylist(playlist.getName());

            dismiss();
        }
    };


    private void addToPlaylist(String playlistName) {

        if (MPDApplication.getPlaylistTypeAlbum()) {
            Item item = AlbumListFragment.getAlbumItem();

            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.addToPlaylist(playlistName, (Album) item);
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to add.", e);
            }

        } else {
            Item item = AlbumSongsFragment.getSongItem();

            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.addToPlaylist(playlistName, (Music) item);
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to add.", e);
            }
        }
    }


    public static PlaylistPickerDialog newInstance() {

        return new PlaylistPickerDialog();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            mItems = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylists();
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to update.", e);
        }

        mAdapter.setData(mItems);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        mAdapter = new PlaylistListAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        @SuppressLint("InflateParams")
        View rootView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_playlist_picker, null);

        Toolbar toolbar = rootView.findViewById(R.id.dialog_playlist_picker_toolbar);
        toolbar.setTitle(R.string.addToPlaylist);
        toolbar.setTitleTextColor(0xffffffff);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mRecyclerView.setAdapter(mAdapter);


        Button newPlaylistButton = rootView.findViewById(R.id.new_playlist);
        newPlaylistButton.setOnClickListener(mOnClickListener);

        builder.setView(rootView);
        return builder.create();
    }

}
