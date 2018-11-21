package org.oucho.mpdclient.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.MainActivity;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.cover.helper.AlbumCoverDownloadListener;
import org.oucho.mpdclient.cover.helper.CoverAsyncHelper;
import org.oucho.mpdclient.cover.helper.CoverDownloadListener;
import org.oucho.mpdclient.cover.helper.CoverManager;
import org.oucho.mpdclient.dialog.PlaylistPickerDialog;
import org.oucho.mpdclient.fragments.adapter.AlbumSongAdapter;
import org.oucho.mpdclient.fragments.adapter.BaseAdapter;
import org.oucho.mpdclient.fragments.loader.AlbumSongLoader;
import org.oucho.mpdclient.helpers.AlbumInfo;
import org.oucho.mpdclient.helpers.MPDControl;
import org.oucho.mpdclient.helpers.QueueControl;
import org.oucho.mpdclient.mpd.MPDPlaylist;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.widgets.CustomLayoutManager;
import org.oucho.mpdclient.widgets.fastscroll.FastScroller;

import java.io.IOException;
import java.util.List;

public class AlbumSongsFragment extends Fragment implements MPDConfig {

    private static final String TAG = "AlbumSongsFragment";

    private static final int POPUP_COVER_SELECTIVE_CLEAN = 11;

    private static final int POPUP_PLAYLIST = 12;

    private ImageView mCoverArt;
    private ProgressBar mCoverArtProgress;

    private String currentAlbum = "";

    private static final String ARG_ID = "id";

    private Album mAlbum;
    private AlbumSongAdapter mAdapter;

    private RecyclerView mRecyclerView;

    private List<? extends Item> mItems;

    private boolean first = true;

    private static Item itemSong;

    private final Handler mHandler = new Handler();


    private final LoaderManager.LoaderCallbacks<List<? extends Item>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<? extends Item>>() {

        @Override
        public Loader<List<? extends Item>> onCreateLoader(int id, Bundle args) {
            return new AlbumSongLoader(getContext(), mAlbum);
        }

        @Override
        public void onLoadFinished(Loader<List<? extends Item>> loader, List<? extends Item> songList) {
            mAdapter.setData(songList);

            for (int i = 0; i < songList.size(); i++) {

                Item item = songList.get(i);
                String song = ((Music) item).getTitle() ;

                if ( MainActivity.getCurrentSong().equals(song)) {
                    try {
                        mRecyclerView.smoothScrollToPosition(i);
                    } catch (NullPointerException ignore) {
                        Log.w(TAG, "error smoothScrollToPosition" );
                    }
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<List<? extends Item>> loader) {
            //  Auto-generated method stub
        }
    };


    public static AlbumSongsFragment newInstance(Album album) {

        Log.d(TAG, "AlbumSongsFragment, path = " + album.getPath());

        AlbumSongsFragment fragment = new AlbumSongsFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_ID, album);

        fragment.setArguments(args);

        return fragment;
    }



    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            Parcelable id = args.getParcelable(ARG_ID);
            mAlbum = (Album) id;
        }

        try {
            mItems = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getSongs(mAlbum);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to async update.", e);
        }

        load();

    }


    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_song_album, container, false);

        mCoverArt = rootView.findViewById(R.id.album_artwork);
        mCoverArt.setOnLongClickListener(mOnLongClickListener);
        mCoverArtProgress = rootView.findViewById(R.id.albumCoverProgress);

        TextView titreAlbum = rootView.findViewById(R.id.line1);
        titreAlbum.setText(mAlbum.getName());

        LinearLayout presentation = rootView.findViewById(R.id.presentation);
        presentation.setOnClickListener(null);

        setTitre();

        TextView mHeaderArtist = rootView.findViewById(R.id.line2);
        mHeaderArtist.setText(mAlbum.getArtist().getName());

        TextView mHeaderYear = rootView.findViewById(R.id.line3);
        mHeaderYear.setText(String.valueOf(getDate()));

        TextView mHeaderNBtrack = rootView.findViewById(R.id.line4);

        String nb_Morceaux;

            if (mItems.size() < 2) {
                nb_Morceaux = String.valueOf(mItems.size()) + " " + getString(R.string.title);
            } else {
                nb_Morceaux = String.valueOf(mItems.size()) + " " + getString(R.string.titles);
            }

        mHeaderNBtrack.setText(nb_Morceaux);

        TextView durée = rootView.findViewById(R.id.duration);
        durée.setText(getTotalTimeForList());

        mRecyclerView = rootView.findViewById(R.id.recycler_view);

        FastScroller.setVoirBubulle(false);

        mRecyclerView.setLayoutManager(new CustomLayoutManager(getActivity()));
        mAdapter = new AlbumSongAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mAdapter.setOnItemLongClickListener(mOnItemLongClickListener);

        mRecyclerView.setAdapter(mAdapter);

        loadAlbumCovers(mAlbum);

        return rootView;
    }

    private void load() {
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }


    /* *********************************************************************************************
     * Click Items
     * ********************************************************************************************/
    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            switch (view.getId()) {
                case R.id.item_view:
                    addAlbum(mAlbum, position);
                    break;
                default:
                    break;
            }
        }
    };

    private final BaseAdapter.OnItemLongClickListener mOnItemLongClickListener = new BaseAdapter.OnItemLongClickListener() {
        @Override
        public void onItemLongClick(int position, View view) {

            switch (view.getId()) {
                case R.id.item_view:
                    Item itemPosition = mAdapter.getItem(position);
                    showPopup(itemPosition);

                    break;

                default:
                    break;
            }
        }
    };


    private void showPopup(final Item itemPosition) {

        PopupMenu mPopupMenu = new PopupMenu(getActivity(), mCoverArt);

        mPopupMenu.getMenu().add(POPUP_PLAYLIST, POPUP_PLAYLIST, 0, R.string.addToPlaylist);
        mPopupMenu.setOnMenuItemClickListener(item -> {
            final int groupId = item.getGroupId();
            boolean result = true;

            if (groupId == POPUP_PLAYLIST) {

                showPlaylistPicker(itemPosition);

            } else {
                result = false;
            }

            return result;
        });

        mPopupMenu.show();
    }


    private void showPlaylistPicker(final Item song) {

        MPDApplication.setPlaylistTypeAlbum(false);

        itemSong = song;

        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();

        picker.show(getChildFragmentManager(), "pick_playlist");

    }


    /* *********************************************************************************************
     * click general
     * ********************************************************************************************/
    private final View.OnLongClickListener mOnLongClickListener = v -> {

        showCoverPopup();

        return true;
    };


    private void showCoverPopup() {

        PopupMenu mCoverPopupMenu = new PopupMenu(getActivity(), mCoverArt);

        mCoverPopupMenu.getMenu().add(POPUP_COVER_SELECTIVE_CLEAN, POPUP_COVER_SELECTIVE_CLEAN, 0,
                R.string.resetCover);
        mCoverPopupMenu.setOnMenuItemClickListener(item -> {
            final int groupId = item.getGroupId();
            boolean result = true;

             if (groupId == POPUP_COVER_SELECTIVE_CLEAN) {
                final AlbumInfo albumInfo = new AlbumInfo(mAlbum);

                CoverManager.getInstance().clear(albumInfo);
                 loadAlbumCovers(mAlbum);

             } else {
                result = false;
            }

            return result;
        });

        mCoverPopupMenu.show();
    }



    private void addAlbum(final Item item, final int position) {

        if (!currentAlbum.equals(mAlbum.getName())) {
            currentAlbum = mAlbum.getName();

            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.add((Album) item, true, false);
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to add.", e);
            }
        }

        if (first) {
            MPDControl.run(R.id.barB_stop);
            mHandler.postDelayed(() -> play(position), 500);

        } else {
            play(position);
        }

        first = false;

    }


    private void play(final int position) {

        MPDPlaylist playlist;

            do {

                playlist = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylist();


                if (mItems.size() == playlist.size()) {
                    List<Music> musics = playlist.getMusicList();
                    final Music tptp = musics.get(position);
                    QueueControl.run(QueueControl.SKIP_TO_ID, tptp.getSongId());
                }


            } while (mItems.size() != playlist.size());

    }


    private void loadAlbumCovers(Album album) {

        Resources res = getContext().getResources();

        int itemWidth = (int) res.getDimension(R.dimen.fragmen_album_list_grid_item_width);

        final CoverAsyncHelper coverHelper = getCoverHelper(itemWidth);
        final AlbumInfo albumInfo = new AlbumInfo(album);

        setCoverListener(coverHelper);

        Log.d(TAG, "loadAlbumCovers path = " + albumInfo.getPath());

        if (albumInfo.isValid()) {
            loadArtwork(coverHelper, albumInfo);
        }
    }

    private CoverAsyncHelper getCoverHelper(int size) {
        final CoverAsyncHelper coverHelper = new CoverAsyncHelper();

        coverHelper.setCoverMaxSize(size);

        loadPlaceholder(coverHelper);

        return coverHelper;
    }

    private void setCoverListener(CoverAsyncHelper coverHelper) {

        final CoverDownloadListener acd = new AlbumCoverDownloadListener(mCoverArt, mCoverArtProgress, false);
        final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) mCoverArt.getTag(R.id.AlbumCoverDownloadListener);

        if (oldAcd != null) {
            oldAcd.detach();
        }

        mCoverArt.setTag(R.id.CoverAsyncHelper, coverHelper);
        coverHelper.addCoverDownloadListener(acd);
    }

    private void loadArtwork(final CoverAsyncHelper coverHelper, final AlbumInfo albumInfo) {
        coverHelper.downloadCover(albumInfo);
    }

    private void loadPlaceholder(final CoverAsyncHelper coverHelper) {
        coverHelper.obtainMessage(CoverAsyncHelper.EVENT_COVER_NOT_FOUND).sendToTarget();
    }


    @SuppressLint("DefaultLocale")
    private String getTotalTimeForList() {
        String result;
        String temps;
        Music song;

        final long minutes;

        int totalTime = 0;
        for (final Item item : mItems) {
            song = (Music) item;

            if (song.getTime() > 0)
                totalTime += song.getTime();
        }

        minutes = totalTime / 60L;
        result = String.format("%d", minutes);

        if (result.equals("0") || result.equals("1")) {
            temps = result + " " + getString(R.string.minute_singulier);
        } else {
            temps = result + " " + getString(R.string.minute_pluriel);
        }

        return temps;
    }


    private long getDate() {
        Music music;
        long date = 0;
        for (final Item item : mItems) {
            music = (Music) item;
            date = music.getDate();
        }
        return date;
    }


    @Override
    public void onResume() {
        super.onResume();

        MPDApplication.setFragmentAlbumSong(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction("org.oucho.mdpclient.onTrackInfoUpdate");
        filter.addAction("org.oucho.mdpclient.refreshTitleAlbumSongList");
        setTitre();

        getContext().registerReceiver(mServiceListener, filter);
    }

    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            Log.i(TAG, "onTrackInfoUpdate " + receiveIntent);

            assert receiveIntent != null;
            if (receiveIntent.equals("org.oucho.mdpclient.onTrackInfoUpdate")) {

                String titre = intent.getStringExtra("Titre");

                for (int i = 0; i < mItems.size(); i++) {

                    Item item = mItems.get(i);
                    String song = ((Music) item).getTitle() ;

                    if ( titre.equals(song)) {
                        mRecyclerView.smoothScrollToPosition(i);
                    }
                }

                mAdapter.notifyDataSetChanged();
            }

            if (receiveIntent.equals("org.oucho.mdpclient.refreshTitleAlbumSongList")) {

                setTitre();

            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();

        getContext().unregisterReceiver(mServiceListener);

        MPDApplication.setFragmentAlbumSong(false);

        String tri = mSettings.getString("tri", "");

        if (tri.equals("year")) {
            tri = getContext().getString(R.string.menu_sort_by_year);
        } else if (tri.equals("artist")) {
            tri = getContext().getString(R.string.menu_sort_by_artist);
        }

        Intent intent = new Intent();
        intent.setAction("org.oucho.mdpclient.setTitle");
        intent.putExtra("Titre", getContext().getString(R.string.albums));
        intent.putExtra("Second", tri);
        intent.putExtra("elevation", true);

        getContext().sendBroadcast(intent);

        Intent menu = new Intent();
        menu.setAction(INTENT_SET_MENU);
        menu.putExtra("menu", true);
        getContext().sendBroadcast(menu);
    }

    private void setTitre() {


        mHandler.postDelayed(() -> {

            Intent intent = new Intent();
            intent.setAction("org.oucho.mdpclient.setTitle");
            intent.putExtra("Titre", mAlbum.getName());
            intent.putExtra("Second", "");
            intent.putExtra("elevation", false);

            getContext().sendBroadcast(intent);

            Intent menu = new Intent();
            menu.setAction(INTENT_SET_MENU);
            menu.putExtra("menu", false);
            getContext().sendBroadcast(menu);

        }, 300);

    }

    public static Item getSongItem() {
        return itemSong;
    }

}
