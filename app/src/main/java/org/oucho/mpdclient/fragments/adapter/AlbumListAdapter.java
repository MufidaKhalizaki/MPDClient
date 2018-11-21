package org.oucho.mpdclient.fragments.adapter;


import android.content.Context;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.cover.helper.AlbumCoverDownloadListener;
import org.oucho.mpdclient.helpers.AlbumInfo;
import org.oucho.mpdclient.cover.helper.CoverAsyncHelper;
import org.oucho.mpdclient.cover.helper.CoverDownloadListener;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.widgets.fastscroll.FastScroller;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;

public class AlbumListAdapter extends BaseAdapter<AlbumListAdapter.AlbumViewHolder> implements FastScroller.SectionIndexer, MPDConfig {

    @SuppressWarnings("unused")
    private static final String TAG = "Album List Adapter";
    private final Context mContext;
    private List<? extends Item> mAlbumList = Collections.emptyList();


    public AlbumListAdapter(Context context) {
        mContext = context;
    }

    public void setData(List<? extends Item> data) {
        mAlbumList = data;
        notifyDataSetChanged();
    }


    @Override
    public AlbumViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_liste_album_item, parent, false);

        return new AlbumViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(AlbumViewHolder viewHolder, int position) {

        Item item = mAlbumList.get(position);

        final Album album = (Album) item;
        final Artist artist = album.getArtist();


        viewHolder.vPlayerStatus.setVisibility(View.INVISIBLE);
        viewHolder.vPlayerStatusFond.setVisibility(View.INVISIBLE);


        viewHolder.vArtist.setText(artist.toString());
        viewHolder.vName.setText(String.valueOf(album.getName()));

        viewHolder.vYear.setVisibility(View.INVISIBLE);
        viewHolder.vBackgroundYear.setVisibility(View.INVISIBLE);


        String getTri = mSettings.getString("tri", "a-z");

        if ("artist".equals(getTri)) {

            viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
            viewHolder.vName.setTextSize(14);

            viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            viewHolder.vArtist.setTextSize(15);

            viewHolder.vYear.setVisibility(View.INVISIBLE);
            viewHolder.vBackgroundYear.setVisibility(View.INVISIBLE);

        } else if ("year".equals(getTri)) {

            viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            viewHolder.vName.setTextSize(15);

            viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
            viewHolder.vArtist.setTextSize(14);

            viewHolder.vYear.setText(String.valueOf(album.getYear()));

            viewHolder.vYear.setVisibility(View.VISIBLE);
            viewHolder.vBackgroundYear.setVisibility(View.VISIBLE);

        } else {

            viewHolder.vName.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            viewHolder.vName.setTextSize(15);

            viewHolder.vArtist.setTextColor(ContextCompat.getColor(mContext, R.color.grey_600));
            viewHolder.vArtist.setTextSize(14);

            viewHolder.vYear.setVisibility(View.INVISIBLE);
            viewHolder.vBackgroundYear.setVisibility(View.INVISIBLE);
        }

        loadAlbumCovers(viewHolder, album);

    }


    @Override
    public int getItemCount() {
        return mAlbumList.size();
    }

    public Item getItem(int position) {
        return mAlbumList.get(position);
    }

    class AlbumViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        final ImageView vArtwork;
        final TextView vName;
        final TextView vYear;
        final ImageView vBackgroundYear;

        private final ImageView vPlayerStatus;
        private final ImageView vPlayerStatusFond;

        private final ProgressBar mCoverArtProgress;

        private final LinearLayout vAlbumInfo;

        private final TextView vArtist;

        AlbumViewHolder(View itemView) {
            super(itemView);

            vArtwork = itemView.findViewById(R.id.album_artwork);
            vName = itemView.findViewById(R.id.album_name);
            vYear = itemView.findViewById(R.id.year);
            vBackgroundYear = itemView.findViewById(R.id.background_year);
            vPlayerStatus = itemView.findViewById(R.id.album_play);
            vPlayerStatusFond = itemView.findViewById(R.id.album_play_fond);

            mCoverArtProgress = itemView.findViewById(R.id.albumCoverProgress);

            vAlbumInfo = itemView.findViewById(R.id.album_info);

            vArtwork.setOnClickListener(this);

            vArtist = itemView.findViewById(R.id.artist_name);
            itemView.findViewById(R.id.album_info).setOnClickListener(this);

            vArtwork.setOnLongClickListener(this);
            vAlbumInfo.setOnLongClickListener(this);

            ImageButton menuButton = itemView.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(this);

            vArtwork.setOnLongClickListener(this);
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
        Item item = mAlbumList.get(position);

        final Album album = (Album) item;
        final Artist artist = album.getArtist();
        final long year = (int) album.getYear();

        String getTri = mSettings.getString("tri", "a-z");

        if ("artist".equals(getTri)) {

            String toto = String.valueOf(artist).replaceFirst("The ", "");

            return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));

        } else if ("year".equals(getTri)) {

            String toto = String.valueOf(year);

            return String.valueOf(toto);

        } else {

            String toto = String.valueOf(album.getName())
                    .replaceFirst("The ", "")
                    .replaceFirst("A ", "");

            String strip = "";
            try {
                return stripAccents(String.valueOf(toto.toUpperCase().charAt(0)));
            } catch (StringIndexOutOfBoundsException ignore) {

                return strip;
            }
        }
    }

    private static String stripAccents(String s) {
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    private void loadAlbumCovers(AlbumViewHolder holder, Album album) {

        Resources res = mContext.getResources();

        int itemWidth = (int) res.getDimension(R.dimen.fragmen_album_list_grid_item_width);

        final CoverAsyncHelper coverHelper = getCoverHelper(itemWidth);
        final AlbumInfo albumInfo = new AlbumInfo(album);

        setCoverListener(holder, coverHelper);

        // Can't get artwork for missing album name
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

    private static void setCoverListener(AlbumViewHolder holder, CoverAsyncHelper coverHelper) {

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