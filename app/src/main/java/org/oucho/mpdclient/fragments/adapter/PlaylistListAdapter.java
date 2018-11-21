package org.oucho.mpdclient.fragments.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.widgets.fastscroll.FastScroller;

import java.util.Collections;
import java.util.List;


public class PlaylistListAdapter extends BaseAdapter<PlaylistListAdapter.PlaylistListViewHolder>
        implements FastScroller.SectionIndexer, MPDConfig {

    private List<? extends Item> mPlaylistList = Collections.emptyList();


    public PlaylistListAdapter() {
    }

    public void setData(List<? extends Item> data) {
        mPlaylistList = data;

        notifyDataSetChanged();
    }


    @Override
    public PlaylistListAdapter.PlaylistListViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_playlist_item, parent, false);

        return new PlaylistListAdapter.PlaylistListViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(PlaylistListAdapter.PlaylistListViewHolder viewHolder, int position) {
        Item item = mPlaylistList.get(position);

        viewHolder.vName.setText(item.getName());
    }


    @Override
    public int getItemCount() {
        return mPlaylistList.size();
    }

    public Item getItem(int position) {
        return mPlaylistList.get(position);
    }

    class PlaylistListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        final TextView vName;

        PlaylistListViewHolder(View itemView) {
            super(itemView);

            vName = itemView.findViewById(R.id.name);
            itemView.findViewById(R.id.menu_playlist).setOnClickListener(this);
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
    }

    @Override
    public String getSectionText(int position) {

        return null;
    }

}