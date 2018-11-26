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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.MainActivity;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;

import java.util.Collections;
import java.util.List;


public class AlbumSongAdapter extends BaseAdapter<AlbumSongAdapter.SongViewHolder> implements MPDConfig {

    private List<? extends Item> mSongList = Collections.emptyList();


    public AlbumSongAdapter() {
    }

    public void setData(List<? extends Item> data) {
        mSongList = data;
        notifyDataSetChanged();
    }


    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.fragment_song_album_item, parent, false);

        return new SongViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(SongViewHolder viewHolder, int position) {

        Item item = mSongList.get(position);

        Music music = (Music) item;
        String titre = music.getTitle();
        long secondes = music.getTime();

        @SuppressLint("DefaultLocale")
        String duration = String.valueOf( (secondes % 3600) / 60 ) + ":" + String.format("%02d", (secondes % 3600) % 60 );

        viewHolder.vTime.setText(duration);

        viewHolder.vTitle.setText(titre);

        viewHolder.vTrackNumber.setText(String.valueOf(position + 1));

        if (MainActivity.getCurrentSong().equals(titre)) {

            viewHolder.vTime.setTextColor(ContextCompat.getColor(viewHolder.vTime.getContext(), R.color.colorAccent));
            viewHolder.vTime.setTextSize(15);

            if (MainActivity.getPlayStatus()) {

                viewHolder.vTrackNumber.setVisibility(View.INVISIBLE);
                viewHolder.PlayView.setVisibility(View.VISIBLE);

            } else {

                viewHolder.vTrackNumber.setVisibility(View.INVISIBLE);
                viewHolder.PlayView.setImageResource(R.drawable.ic_pause_amber_700_24dp);
                viewHolder.PlayView.setVisibility(View.VISIBLE);
            }

        } else {

            viewHolder.vTime.setTextColor(ContextCompat.getColor(viewHolder.vTime.getContext(), R.color.grey_600));

            viewHolder.PlayView.setVisibility(View.INVISIBLE);
            viewHolder.vTrackNumber.setVisibility(View.VISIBLE);
        }

    }


    @Override
    public int getItemCount() {
        return mSongList.size();
    }

    public Item getItem(int position) {
        return mSongList.get(position);
    }


    class SongViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final TextView vTime;
        private final TextView vTitle;
        private final TextView vTrackNumber;

        final ImageView PlayView;

        SongViewHolder(View itemView) {
            super(itemView);

            vTime = itemView.findViewById(R.id.time);

            vTitle = itemView.findViewById(R.id.title);

            vTrackNumber = itemView.findViewById(R.id.track_number);

            PlayView = itemView.findViewById(R.id.play);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
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
    }
}