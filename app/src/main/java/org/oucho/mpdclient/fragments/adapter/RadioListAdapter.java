package org.oucho.mpdclient.fragments.adapter;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Stream;

import java.util.ArrayList;

public class RadioListAdapter extends BaseAdapter<RadioListAdapter.RadioViewHolder> implements MPDConfig {

    private ArrayList<Stream> mRadioList = new ArrayList<>();


    public RadioListAdapter() {
    }

    public void setData(ArrayList<Stream> data) {
        mRadioList = data;
        notifyDataSetChanged();
    }


    @Override
    public RadioViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_radio_item, parent, false);

        return new RadioViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(RadioViewHolder viewHolder, int position) {

        Item item = mRadioList.get(position);

        final Stream radio = (Stream) item;

        String nomRadio = radio.getName();

        viewHolder.text.setText(radio.getName());

        if (radio.getName().equals(nomRadio)  ) {

            viewHolder.image.setColorFilter(ContextCompat.getColor(viewHolder.image.getContext(), R.color.colorAccent));

        }

    }


    @Override
    public int getItemCount() {

        return mRadioList.size();
    }

    public Item getItem(int position) {
        return mRadioList.get(position);
    }

    class RadioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final TextView text;
        private final ImageButton menu;

        private final ImageView image;

        private final RelativeLayout fond;

        RadioViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.textViewRadio);

            menu = itemView.findViewById(R.id.buttonMenu);
            menu.setOnClickListener(this);

            image = itemView.findViewById(R.id.imageViewRadio);

            fond  = itemView.findViewById(R.id.fond);
            fond.setOnClickListener(this);

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