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

package org.oucho.mpdclient.fragments.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.cover.helper.AlbumCoverDownloadListener;
import org.oucho.mpdclient.cover.helper.CoverAsyncHelper;
import org.oucho.mpdclient.cover.helper.CoverDownloadListener;
import org.oucho.mpdclient.helpers.AlbumInfo;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.widgets.CustomSwipeAdapter;
import org.oucho.mpdclient.widgets.DragRecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PlaylistContentAdapter extends BaseAdapter<PlaylistContentAdapter.PlaylistContentViewHolder>
        implements CustomSwipeAdapter, MPDConfig {

    private static final String TAG = "PlayListContentAdapter";
    private final Context mContext;
    private List<? extends Item> mPlaylistContent = Collections.emptyList();

    private String mPlaylistName;

    private final DragRecyclerView mQueueView;

    private int mSelectedItemPosition = -1;


    public PlaylistContentAdapter(DragRecyclerView drag) {
        mContext = MPDApplication.getInstance();
        mQueueView = drag;
    }

    public void setData(List<? extends Item> data, String name) {

        mPlaylistContent = new ArrayList<>(data);
        mPlaylistName = name;

        notifyDataSetChanged();
    }


    @Override
    public PlaylistContentAdapter.PlaylistContentViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_playlist_content_item, parent, false);

        return new PlaylistContentAdapter.PlaylistContentViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(PlaylistContentAdapter.PlaylistContentViewHolder viewHolder, int position) {
        Item item = mPlaylistContent.get(position);

        Music music = (Music) item;

        viewHolder.vTitle.setText(music.getTitle());
        viewHolder.vArtist.setText(music.getArtist());

        loadAlbumCovers(viewHolder, music);
    }


    @Override
    public int getItemCount() {
        return mPlaylistContent.size();
    }

    public void moveItem(int oldPosition, int newPosition) {

        if (oldPosition < 0 || oldPosition >= mPlaylistContent.size()
                || newPosition < 0 || newPosition >= mPlaylistContent.size()) {
            return;
        }

        Collections.swap(mPlaylistContent, oldPosition, newPosition);

        if (mSelectedItemPosition == oldPosition) {
            mSelectedItemPosition = newPosition;
        } else if (mSelectedItemPosition == newPosition) {
            mSelectedItemPosition = oldPosition;
        }

        notifyItemMoved(oldPosition, newPosition);
    }


    @Override
    public void onItemSwiped(int position) {

        mPlaylistContent.remove(position);


        try {
            MPDApplication.getInstance().oMPDAsyncHelper.oMPD.removeFromPlaylist(mPlaylistName, position);

        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to remove.", e);
        }

        notifyItemRemoved(position);

    }


    class PlaylistContentViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        final TextView vTitle;
        final TextView vArtist;
        private final ProgressBar mCoverArtProgress;

        final ImageButton vArtwork;

        final View itemView;

        PlaylistContentViewHolder(View itemView) {
            super(itemView);

            mCoverArtProgress = itemView.findViewById(R.id.albumCoverProgress);

            vArtwork = itemView.findViewById(R.id.cover);

            vArtwork.setOnTouchListener(this);

            vTitle = itemView.findViewById(R.id.title);
            vArtist = itemView.findViewById(R.id.artist);

            this.itemView = itemView;
            itemView.setOnClickListener(this);

        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            triggerOnItemClickListener(position, v);
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getAdapterPosition();

            triggerOnItemLongClickListener(position, v);

            return true;
        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            mQueueView.startDrag(itemView);

            return false;
        }


    }




    private void loadAlbumCovers(PlaylistContentAdapter.PlaylistContentViewHolder holder, Music music) {

        Resources res = mContext.getResources();

        int itemWidth = (int) res.getDimension(R.dimen.art_thumbnail_playlist_size);

        final CoverAsyncHelper coverHelper = getCoverHelper(itemWidth);
        final AlbumInfo albumInfo = new AlbumInfo(music);

        setCoverListener(holder, coverHelper);

        if (albumInfo.isValid()) {
            loadArtwork(coverHelper, albumInfo);
        }
    }

    private static CoverAsyncHelper getCoverHelper(int size) {
        final CoverAsyncHelper coverHelper = new CoverAsyncHelper();

        coverHelper.setCoverMaxSize(size);

        loadPlaceholder(coverHelper);

        return coverHelper;
    }

    private static void setCoverListener(PlaylistContentViewHolder holder, CoverAsyncHelper coverHelper) {

        // listen for new artwork to be loaded
        final CoverDownloadListener acd = new AlbumCoverDownloadListener(holder.vArtwork, holder.mCoverArtProgress, false);
        final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) holder.vArtwork.getTag(R.id.AlbumCoverDownloadListener);

        if (oldAcd != null) {
            oldAcd.detach();
        }

        holder.vArtwork.setTag(R.id.CoverAsyncHelper, coverHelper);
        coverHelper.addCoverDownloadListener(acd);
    }

    private static void loadArtwork(final CoverAsyncHelper coverHelper, final AlbumInfo albumInfo) {
        coverHelper.downloadCover(albumInfo);
    }

    private static void loadPlaceholder(final CoverAsyncHelper coverHelper) {
        coverHelper.obtainMessage(CoverAsyncHelper.EVENT_COVER_NOT_FOUND).sendToTarget();
    }

}