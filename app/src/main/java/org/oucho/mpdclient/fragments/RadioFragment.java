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


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.fragments.adapter.BaseAdapter;
import org.oucho.mpdclient.fragments.adapter.RadioListAdapter;
import org.oucho.mpdclient.fragments.loader.RadioLoader;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Stream;
import org.oucho.mpdclient.tools.StreamFetcher;
import org.oucho.mpdclient.tools.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class RadioFragment extends Fragment implements MPDConfig {

    private static final String TAG = "RadioFragment";

    private final ArrayList<Stream> mStreams = new ArrayList<>();

    private RadioListAdapter mAdapter;

    private boolean isRadioInstalled;

    private SwipeRefreshLayout mSwipeRefreshLayout;



    private final LoaderManager.LoaderCallbacks<ArrayList<Stream>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<ArrayList<Stream>>() {

        @Override
        public Loader<ArrayList<Stream>> onCreateLoader(int id, Bundle args) {
            return new RadioLoader(getContext());
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<Stream>> loader, ArrayList<Stream> radioList) {
            mAdapter.setData(radioList);
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<Stream>> loader) {
            Log.i(TAG, "onLoaderReset");
        }
    };


    public static RadioFragment newInstance() {
        return new RadioFragment();
    }


    /* *********************************************************************************************
     * Création de l'activité
     * ********************************************************************************************/

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        isRadioInstalled = checkApp();

        load();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_liste_radio, container, false);

        mSwipeRefreshLayout = rootView.findViewById(R.id.pullToRefresh);
        mSwipeRefreshLayout.setOnRefreshListener(onSwipe);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.recycler_view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new RadioListAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        load();

        // Active la touche back
        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {

            if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK ){

                if (!MPDApplication.getFragmentPlayer() && !MPDApplication.getFragmentQueue()) {
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

    private void load() {

        getLoaderManager().restartLoader(0, null, mLoaderCallbacks);
    }


    private final SwipeRefreshLayout.OnRefreshListener onSwipe = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {

            mSwipeRefreshLayout.setRefreshing(false);
            load();

        }
    };


    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.radio, menu);
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                addRadio();
                return true;
            default:
                return false;
        }
    }


    private void addRadio() {
        editRadio(null);
    }


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            Item item = mAdapter.getItem(position);

            switch (view.getId()) {
                case R.id.fond:

                    play(item);

                    break;
                case R.id.buttonMenu:
                    showMenu(position, view);
                    break;
                default:
                    break;
            }
        }
    };


    private void play(Item item) {
        try {
            final Stream stream = (Stream) item;
            MPDApplication.getInstance().oMPDAsyncHelper.oMPD.addStream(StreamFetcher.instance().get(stream.getUrl(), stream.getName()));
            int mIrAdded = R.string.streamAdded;
            Utils.notifyUser(mIrAdded, item);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add stream.", e);
        }
    }

    private void showMenu(final int position, View view) {

        final PopupMenu popup = new PopupMenu(getActivity(), view);

        MenuInflater inflater = popup.getMenuInflater();

        if (isRadioInstalled) {
            inflater.inflate(R.menu.radio_item_radio2, popup.getMenu());
        } else {
            inflater.inflate(R.menu.radio_item, popup.getMenu());
        }

        popup.setOnMenuItemClickListener(item -> {
            Item name = mAdapter.getItem(position);
            switch (item.getItemId()) {
                case R.id.menu_edit:
                    editRadio(name);
                    break;
                case R.id.menu_delete:
                    deleteRadio(name);
                    break;

                case R.id.menu_add_radio2:

                    Stream stream = (Stream) name;
                    String nom = stream.getName();
                    String url = stream.getUrl();

                    addRadio2(nom, url);

                    break;

                default:
                    break;
            }
            return false;
        });

        popup.show();
    }


    private void addRadio2(String name, String url) {
        Intent radio = new Intent();
        radio.setAction("org.oucho.radio2.ADD_RADIO");
        radio.putExtra("name", name);
        radio.putExtra("url", url);
        getContext().sendBroadcast(radio);
    }


    private void editRadio(Item item) {

        boolean add = false;

        String nomRadio = null;
        String streamUrl = null;
        int pos = -1;


        if (item == null) {
            add = true;
        } else  {

            Stream stream = (Stream) item;

            nomRadio = stream.getName();
            streamUrl = stream.getUrl();
            pos = stream.getPos();

        }

        final boolean ajout = add;
        final int position = pos;


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        int title = nomRadio == null ? R.string.addStream : R.string.editStream;

        builder.setTitle(getResources().getString(title));

        @SuppressLint("InflateParams") View editView = getActivity().getLayoutInflater().inflate(R.layout.layout_editwebradio, null);

        builder.setView(editView);

        final EditText editTextUrl = editView.findViewById(R.id.editTextUrl);
        final EditText editTextName = editView.findViewById(R.id.editTextName);


        if(!ajout) {
            editTextUrl.setText(streamUrl);
            editTextName.setText(nomRadio);
        }


        builder.setPositiveButton(R.string.ok, (dialog, id) -> {

            String url = editTextUrl.getText().toString().trim();
            String name = editTextName.getText().toString().trim();


            if("".equals(url) || "http://".equals(url)) {
                Toast.makeText(getContext(), R.string.streamErrorInvalidURL, Toast.LENGTH_SHORT).show();
                return;
            }

            if("".equals(name))
                name = url;


            try {

                if (ajout) {
                    MPDApplication.getInstance().oMPDAsyncHelper.oMPD.saveStream(url, name);
                } else {

                    MPDApplication.getInstance().oMPDAsyncHelper.oMPD.removeSavedStream(position);
                    MPDApplication.getInstance().oMPDAsyncHelper.oMPD.saveStream(url, name);

                }

            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to save stream.", e);
            }

            load();
        });

        builder.setNegativeButton(R.string.cancel, (dialog, id) -> load());


        AlertDialog dialog = builder.create();
        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }


    private void deleteRadio(Item item) {

        final Stream stream = (Stream) item;

        final int position = stream.getPos();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage(getResources().getString(R.string.deleteStreamPrompt) + " " + item.getName() + " ?");

        builder.setPositiveButton(R.string.deleteStream, (dialog, which) -> {

            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.removeSavedStream(position);
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to removed a saved stream.", e);
            }

            Collections.sort(mStreams);

            load();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }


    private boolean checkApp() {
        PackageManager packageManager = getContext().getPackageManager();

        try {
            packageManager.getPackageInfo("org.oucho.radio2", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
