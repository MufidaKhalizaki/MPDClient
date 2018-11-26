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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.cover.helper.CoverManager;
import org.oucho.mpdclient.dialog.PlaylistPickerDialog;
import org.oucho.mpdclient.fragments.adapter.AlbumListAdapter;
import org.oucho.mpdclient.fragments.adapter.BaseAdapter;
import org.oucho.mpdclient.fragments.loader.AlbumListLoader;
import org.oucho.mpdclient.helpers.AlbumInfo;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.tools.Utils;
import org.oucho.mpdclient.widgets.CustomGridLayoutManager;
import org.oucho.mpdclient.widgets.fastscroll.FastScrollRecyclerView;
import org.oucho.mpdclient.widgets.fastscroll.FastScroller;

import java.io.IOException;
import java.util.List;


public class AlbumListFragment extends Fragment {

    private static final String TAG = "Album List Fragment";

    private static final int POPUP_COVER_SELECTIVE_CLEAN = 11;
    private static final String ARG_ID = "id";

    private static final String ARG_NAME = "name";

    private View mLoadingView;

    private AlbumListAdapter mAdapter;

    private Artist mArtist = null;

    private static Item itemAlbum;
    private static List<? extends Item> mAlbumsList = null;

    private String mArtistName = null;

    private Context mContext;

    private SharedPreferences mSettings;

    // TODO rustine
    private static boolean antiReload = false;

    private final LoaderManager.LoaderCallbacks<List<? extends Item>> mLoaderCallbacks = new LoaderCallbacks<List<? extends Item>>() {

        @Override
        public Loader<List<? extends Item>> onCreateLoader(int id, Bundle args) {
            AlbumListLoader loader;
            setAntiReload(false);

            if (mArtist != null) {
                loader = new AlbumListLoader(mContext, mArtist);
            } else {
                loader = new AlbumListLoader(mContext);
            }

            return loader;


        }

        @Override
        public void onLoadFinished(Loader<List<? extends Item>> loader, List<? extends Item> albumList) {

            // Log.d(TAG, "LOAD: " + albumList);

            if (!getAntiReload() && albumList != null) {
                mAdapter.setData(albumList);
                mAlbumsList = albumList;
                setAntiReload(true);
            }
            mLoadingView.setVisibility(View.GONE);
        }

        @Override
        public void onLoaderReset(Loader<List<? extends Item>> loader) {
            //  Auto-generated method stub
        }
    };


    public static AlbumListFragment newInstance() {
        return new AlbumListFragment();
    }

    public AlbumListFragment newInstance(Artist artist) {

        AlbumListFragment fragment = new AlbumListFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_ID, artist);
        args.putString(ARG_NAME, artist.getName());
        fragment.setArguments(args);

        return fragment;
    }


    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        mContext = getContext();
        mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (args != null) {
            Parcelable id = args.getParcelable(ARG_ID);
            mArtistName = args.getString(ARG_NAME);
            mArtist = (Artist) id;
        }

        setHasOptionsMenu(true);

        load();

        setTitre();
    }



    /* *********************************************************************************************
     * Création de la vue
     * ********************************************************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_liste_album, container, false);

        mLoadingView = rootView.findViewById(R.id.loadingLayout);

        mLoadingView.setVisibility(View.VISIBLE);

        FastScrollRecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);

        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        Resources res = mContext.getResources();

        assert wm != null;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        float screenWidth = size.x;
        float itemWidth = res.getDimension(R.dimen.fragmen_album_list_grid_item_width);

        mRecyclerView.setLayoutManager(new CustomGridLayoutManager(mContext, Math.round(screenWidth / itemWidth)));

        mAdapter = new AlbumListAdapter(mContext);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnLongClickListener);

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }


    private void load() {

        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);

    }


    /* *********************************************************************************************
     * Menu
     * ********************************************************************************************/

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (mArtist != null) {
            inflater.inflate(R.menu.albumlist_search_sort_by, menu);
        } else {
            inflater.inflate(R.menu.albumlist_sort_by, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:

                mSettings.edit().putString("tri", "a-z").apply();
                load();
                setTitre();
                mLoadingView.setVisibility(View.VISIBLE);

                break;
            case R.id.menu_sort_by_artist:

                mSettings.edit().putString("tri", "artist").apply();
                load();
                setTitre();
                mLoadingView.setVisibility(View.VISIBLE);

                break;
            case R.id.menu_sort_by_year:

                mSettings.edit().putString("tri", "year").apply();
                load();
                setTitre();
                mLoadingView.setVisibility(View.VISIBLE);

                break;

            default:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }



    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Item item = mAdapter.getItem(position);

            switch (view.getId()) {
                case R.id.album_artwork:
                case R.id.album_info:

                    goAlbum((Album) item);
                    break;
                case R.id.menu_button:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };


    private void goAlbum(Album album) {
        Fragment fragment = AlbumSongsFragment.newInstance(album);
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction().addToBackStack("AlbumSongsFragment");
        ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom);
        ft.replace(R.id.primary_frame, fragment);
        ft.commit();

    }

    private final BaseAdapter.OnItemLongClickListener mOnLongClickListener = new BaseAdapter.OnItemLongClickListener() {
        @Override
        public void onItemLongClick(int position, View view) {

            Item item = mAdapter.getItem(position);

            showPopup(view, item);
        }
    };


    private void showPopup(View view, final Item itemView) {

        android.widget.PopupMenu mCoverPopupMenu = new android.widget.PopupMenu(getActivity(), view);

        mCoverPopupMenu.getMenu().add(POPUP_COVER_SELECTIVE_CLEAN, POPUP_COVER_SELECTIVE_CLEAN, 0, R.string.resetCover);
        mCoverPopupMenu.setOnMenuItemClickListener(item -> {
            final int groupId = item.getGroupId();
            boolean result = true;

            if (groupId == POPUP_COVER_SELECTIVE_CLEAN) {

                Album album = (Album) itemView;

                final AlbumInfo albumInfo = new AlbumInfo(album);

                CoverManager.getInstance().clear(albumInfo);

                mAdapter.notifyDataSetChanged();

            } else {
                result = false;
            }

            return result;
        });

        mCoverPopupMenu.show();
    }


    private void showMenu(final int position, View view) {

        PopupMenu popup = new PopupMenu(getActivity(), view);

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.albumlist_item, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {

            Item itemPosition = mAdapter.getItem(position);

            switch (item.getItemId()) {
                case R.id.ADD_REPLACE_PLAY:
                    addAndReplace(itemPosition);
                    return true;

                case R.id.ADD_TO_PLAYLIST:
                    showPlaylistPicker(itemPosition);
                    return true;

                default:
                    break;
            }
            return false;
        });

        popup.show();
    }

    private void showPlaylistPicker(final Item playlist) {

        MPDApplication.setPlaylistTypeAlbum(true);

        itemAlbum = playlist;

        PlaylistPickerDialog picker = PlaylistPickerDialog.newInstance();

        picker.show(getChildFragmentManager(), "pick_playlist");

    }



    private void addAndReplace(final Item itemPosition) {

        MPDApplication.getInstance().oMPDAsyncHelper.execAsync(() -> {
            boolean play = false;
            final MPDStatus status = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus();
            /* Let the user know if we're not going to play the added music. */
            if (status.isRandom() && status.isState(MPDStatus.STATE_PLAYING)) {
                Utils.notifyUser(R.string.notPlayingInRandomMode);
            } else {
                play = true;
            }
            add(itemPosition, play);
        });
    }


    private void add(final Item item, final boolean play) {
        try {
            MPDApplication.getInstance().oMPDAsyncHelper.oMPD.add((Album) item, true, play);
            int mIrAdded = R.string.albumAdded;
            Utils.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }


    /* *********************************************************************************************
     * Titre
     * ********************************************************************************************/

    @Override
    public void onPause() {
        super.onPause();

        setAntiReload(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        FastScroller.setVoirBubulle(true);

        if (getAntiReload())
            mAdapter.setData(mAlbumsList);

    }

    private void setTitre() {

            final String tri = mSettings.getString("tri", "");

            Intent intent = new Intent();
            intent.setAction("org.oucho.mdpclient.setTitle");
            intent.putExtra("elevation", true);

            if (mArtistName != null) {
                intent.putExtra("Titre", "Album");
                intent.putExtra("Second", tri);
            } else {
                intent.putExtra("Titre", mArtistName);
                intent.putExtra("Second", "");
            }
            getContext().sendBroadcast(intent);

    }

    public static Item getAlbumItem() {
        return itemAlbum;
    }

    private static void setAntiReload(boolean value) {
        antiReload = value;
    }

    private static boolean getAntiReload() {
        return antiReload;
    }

}
