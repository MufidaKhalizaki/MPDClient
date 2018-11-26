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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.MainActivity;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.widgets.CustomSwipeAdapter;
import org.oucho.mpdclient.helpers.QueueControl;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.widgets.DragRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class QueueAdapter extends BaseAdapter<QueueAdapter.QueueViewHolder> implements CustomSwipeAdapter, MPDConfig {

    private static final String TAG = "Queue Adapter";

    private List<Music> mItems;

    private final DragRecyclerView mQueueView;

    private int mSelectedItemPosition = -1;

    public QueueAdapter(DragRecyclerView drag) {
        mQueueView = drag;
    }

    public void setData(List<Music> data) {
        mItems = new ArrayList<>(data);

        try {
            notifyDataSetChanged();

        } catch (IllegalStateException ignore) {
            Log.w(TAG, "error notifyDataSetChanged.");
        }
    }


    @Override
    public QueueAdapter.QueueViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_queue_item, parent, false);

        return new QueueAdapter.QueueViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(QueueAdapter.QueueViewHolder viewHolder, int position) {

        Item item = mItems.get(position);

        Music music = (Music) item;

        if (position == mSelectedItemPosition) {
            viewHolder.itemView.setSelected(true);

        } else {
            viewHolder.itemView.setSelected(false);
        }

        if (MainActivity.getCurrentSong().equals(music.getTitle())) {
            viewHolder.mTitle.setTextColor(ContextCompat.getColor(viewHolder.mTitle.getContext(), R.color.colorAccent));
        } else {
            viewHolder.mTitle.setTextColor(ContextCompat.getColor(viewHolder.mTitle.getContext(), R.color.grey_600));
        }

        viewHolder.mArtist.setText(music.getArtist());
        viewHolder.mTitle.setText(music.getTitle());
        viewHolder.mMenuButton.setTag(music.getSongId());

    }


    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public Item getItem(int position) {

        // Rustine drag
        if (position == -1)
            position = 0;

        return mItems.get(position);
    }


    public void moveItem(int oldPosition, int newPosition) {

        if (oldPosition < 0 || oldPosition >= mItems.size()
                || newPosition < 0 || newPosition >= mItems.size()) {
            return;
        }

        Collections.swap(mItems, oldPosition, newPosition);

        if (mSelectedItemPosition == oldPosition) {
            mSelectedItemPosition = newPosition;
        } else if (mSelectedItemPosition == newPosition) {
            mSelectedItemPosition = oldPosition;
        }

        notifyItemMoved(oldPosition, newPosition);
    }

    @Override
    public void onItemSwiped(int position) {

        Item item = mItems.get(position);

        Music music = (Music) item;

        mItems.remove(position);

        QueueControl.run(QueueControl.REMOVE_BY_ID, music.getSongId());

        notifyItemRemoved(position);

    }


    class QueueViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener {

        final TextView mArtist;

        final View mMenuButton;

        final ImageButton vReorderButton;

        final TextView mTitle;
        final View itemView;


        QueueViewHolder(View itemView) {
            super(itemView);

            mArtist = itemView.findViewById(R.id.artist);
            mTitle = itemView.findViewById(R.id.titre);

            vReorderButton = itemView.findViewById(R.id.reorder_button);
            vReorderButton.setOnTouchListener(this);

            mMenuButton = itemView.findViewById(R.id.menu);
            mMenuButton.setOnClickListener(this);

            this.itemView = itemView;

            itemView.setOnClickListener(this);

        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            mQueueView.startDrag(itemView);
            return false;
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
