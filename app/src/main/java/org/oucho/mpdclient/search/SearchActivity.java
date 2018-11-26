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

package org.oucho.mpdclient.search;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.MainActivity;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.helpers.MPDAsyncHelper.AsyncExecListener;
import org.oucho.mpdclient.helpers.MPDControl;
import org.oucho.mpdclient.library.SimpleLibraryActivity;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.search.bordel.SearchResultDataBinder;
import org.oucho.mpdclient.search.bordel.SeparatedListAdapter;
import org.oucho.mpdclient.tools.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements MPDConfig, OnMenuItemClickListener, AsyncExecListener, OnItemClickListener {

    private static final int ADD = 0;
    private static final int ADD_PLAY = 2;
    private static final int ADD_REPLACE = 1;
    private static final int ADD_REPLACE_PLAY = 3;
    private static final int GOTO_ALBUM = 4;

    private static final String TAG = "SearchActivity";

    private final ArrayList<Music> mSongResults;
    private final ArrayList<Album> mAlbumResults;
    private final ArrayList<Artist> mArtistResults;


    private int mJobID = -1;

    private View mLoadingView;
    private View mNoResultSongsView;
    private View mNoResultAlbumsView;
    private View mNoResultArtistsView;

    private ViewPager mPager;

    @StringRes
    private int mAddString;

    @StringRes
    private int mAddedString;

    private ListView mListAlbums = null;
    private View mListAlbumsFrame = null;

    private ListView mListArtists = null;
    private View mListArtistsFrame = null;

    private ListView mListSongs = null;
    private View mListSongsFrame = null;

    private String mSearchKeywords = null;

    private String mTabArtists = "";
    private String mTabAlbums = "";
    private String mTabSongs = "";


    private boolean firstRun = true;

    public SearchActivity() {
        super();
        mAddString = R.string.addSong;
        mAddedString = R.string.songAdded;
        mArtistResults = new ArrayList<>();
        mAlbumResults = new ArrayList<>();
        mSongResults = new ArrayList<>();
    }

    private SectionsPagerAdapter mSectionsPagerAdapter;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(mUIFlag);


        setContentView(R.layout.search_results);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();

        mSectionsPagerAdapter = new SectionsPagerAdapter();


        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mPager);


        assert actionBar != null;
        SearchView searchView = new SearchView(actionBar.getThemedContext());

        searchView.setIconifiedByDefault(false);
        actionBar.setCustomView(searchView);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);


        searchView.setOnQueryTextListener(searchQueryListener);


        mListArtistsFrame = findViewById(R.id.list_artists_frame);
        mNoResultArtistsView = mListArtistsFrame.findViewById(R.id.no_artist_result);
        mListArtists = mListArtistsFrame.findViewById(android.R.id.list);
        mListArtists.setOnItemClickListener(this);

        mListAlbumsFrame = findViewById(R.id.list_albums_frame);
        mNoResultAlbumsView = mListAlbumsFrame.findViewById(R.id.no_album_result);
        mListAlbums = mListAlbumsFrame.findViewById(android.R.id.list);
        mListAlbums.setOnItemClickListener(this);

        mListSongsFrame = findViewById(R.id.list_songs_frame);
        mNoResultSongsView = mListSongsFrame.findViewById(R.id.no_song_result);
        mListSongs = mListSongsFrame.findViewById(android.R.id.list);
        mListSongs.setOnItemClickListener(this);

        mLoadingView = findViewById(R.id.loadingLayout);
        mLoadingView.setVisibility(View.VISIBLE);

        registerForContextMenu(mListArtists);
        registerForContextMenu(mListAlbums);
        registerForContextMenu(mListSongs);


        mSearchKeywords = "";
        updateList();
    }


    public boolean dispatchKeyEvent(KeyEvent event){
        int keyCode = event.getKeyCode();
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch (keyCode) {

                case KeyEvent.KEYCODE_VOLUME_UP:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_UP);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_DOWN);
                    return true;

                default:
                    return super.dispatchKeyEvent(event);
            }
        }
        return super.dispatchKeyEvent(event);
    }



    private void add(final Artist artist, final Album album, final boolean replace, final boolean play) {
        String note = null;

        try {
            if (artist == null) {
                final Artist albumArtist = album.getArtist();

                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.add(album, replace, play);
                if (albumArtist != null) {
                    note = albumArtist.getName() + " - " + album.getName();
                }
            } else if (album == null) {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.add(artist, replace, play);
                note = artist.getName();
            }
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }

        if (note != null) {
            Utils.notifyUser(mAddedString, note);
        }
    }

    private void add(final Music music, final boolean replace, final boolean play) {
        try {
            MPDApplication.getInstance().oMPDAsyncHelper.oMPD.add(music, replace, play);
            Utils.notifyUser(R.string.songAdded, music.getTitle(), music.getName());
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Failed to add.", e);
        }
    }

    private void add(final Object object, final boolean replace, final boolean play) {
        setContextForObject(object);
        if (object instanceof Music) {
            add((Music) object, replace, play);
        } else if (object instanceof Artist) {
            add((Artist) object, null, replace, play);
        } else if (object instanceof Album) {
            add(null, (Album) object, replace, play);
        }
    }

    @Override
    public void asyncExecSucceeded(final int jobID) {
        if (mJobID == jobID) {
            updateFromItems();
        }
    }

    private void asyncUpdate() {
        final String finalSearch = mSearchKeywords.toLowerCase();

        List<Music> arrayMusic = null;

        try {
            arrayMusic = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.search(finalSearch);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "MPD search failure.", e);

        }

        if (arrayMusic == null) {
            return;
        }

        mArtistResults.clear();
        mAlbumResults.clear();
        mSongResults.clear();

        String tmpValue;
        boolean valueFound;
        for (final Music music : arrayMusic) {
            if (music.getTitle() != null && music.getTitle().toLowerCase().contains(finalSearch)) {
                mSongResults.add(music);
            }
            valueFound = false;
            Artist artist = music.getAlbumArtistAsArtist();
            if (artist == null || artist.isUnknown()) {
                artist = music.getArtistAsArtist();
            }
            if (artist != null) {
                final String name = artist.getName();
                if (name != null) {
                    tmpValue = name.toLowerCase();
                    if (tmpValue.contains(finalSearch)) {
                        for (final Artist artistItem : mArtistResults) {
                            final String artistItemName = artistItem.getName();
                            if (artistItemName != null &&
                                    artistItemName.equalsIgnoreCase(tmpValue)) {
                                valueFound = true;
                            }
                        }
                        if (!valueFound) {
                            mArtistResults.add(artist);
                        }
                    }
                }
            }

            valueFound = false;
            final Album album = music.getAlbumAsAlbum();
            if (album != null) {
                final String albumName = album.getName();
                if (albumName != null) {
                    tmpValue = albumName.toLowerCase();
                    if (tmpValue.contains(finalSearch)) {
                        for (final Album albumItem : mAlbumResults) {
                            final String albumItemName = albumItem.getName();
                            if (albumItemName.equalsIgnoreCase(tmpValue)) {
                                valueFound = true;
                            }
                        }
                        if (!valueFound) {
                            mAlbumResults.add(album);
                        }
                    }
                }
            }
        }

        Collections.sort(mArtistResults);
        Collections.sort(mAlbumResults);
        Collections.sort(mSongResults, Music.COMPARE_WITHOUT_TRACK_NUMBER);


        if (firstRun) {

            mTabArtists = " (0)";
            mTabAlbums = " (0)";
            mTabSongs = " (0)";

            firstRun = false;

        } else {

            runOnUiThread(() -> {


                mTabArtists = " (" + mArtistResults.size() + ')';
                if (mTabArtists.equals(" (0)")) {
                    mTabArtists = "";
                }

                mTabAlbums = " (" + mAlbumResults.size() + ')';
                if (mTabAlbums.equals(" (0)")) {
                    mTabAlbums = "";
                }

                mTabSongs = " (" + mSongResults.size() + ')';
                if (mTabSongs.equals(" (0)")) {
                    mTabSongs = "";
                }

                mSectionsPagerAdapter.notifyDataSetChanged();


            });
        }
    }


    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        switch (mPager.getCurrentItem()) {
            case 0:
                final Artist artist = mArtistResults.get((int) info.id);
                menu.setHeaderTitle(artist.mainText());
                setContextForObject(artist);
                break;
            case 1:
                final Album album = mAlbumResults.get((int) info.id);
                menu.setHeaderTitle(album.mainText());
                setContextForObject(album);
                break;
            case 2:
                final Music music = mSongResults.get((int) info.id);
                final MenuItem gotoAlbumItem = menu.add(Menu.NONE, GOTO_ALBUM, 0, R.string.goToAlbum);
                gotoAlbumItem.setOnMenuItemClickListener(this);
                menu.setHeaderTitle(music.mainText());
                setContextForObject(music);
                break;
            default:
                break;
        }

        final MenuItem addItem = menu.add(Menu.NONE, ADD, 0, getString(mAddString));
        final MenuItem addAndReplaceItem = menu.add(Menu.NONE, ADD_REPLACE, 0, R.string.addAndReplace);
        final MenuItem addReplacePlayItem = menu.add(Menu.NONE, ADD_REPLACE_PLAY, 0, R.string.addAndReplacePlay);
        final MenuItem addAndPlayItem = menu.add(Menu.NONE, ADD_PLAY, 0, R.string.addAndPlay);

        addItem.setOnMenuItemClickListener(this);
        addAndReplaceItem.setOnMenuItemClickListener(this);
        addReplacePlayItem.setOnMenuItemClickListener(this);
        addAndPlayItem.setOnMenuItemClickListener(this);
    }

    @Override
    public void onDestroy() {
        MPDApplication.getInstance().oMPDAsyncHelper.removeAsyncExecListener(this);
        super.onDestroy();
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position,
            final long id) {
        final Object selectedItem = parent.getAdapter().getItem(position);
        if (selectedItem instanceof Music) {

            add((Music) selectedItem, false, false);

        } else if (selectedItem instanceof Artist) {
            final Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("artist", (Parcelable) selectedItem);
            startActivityForResult(intent, -1);
        } else if (selectedItem instanceof Album) {
            final Intent intent = new Intent(this, SimpleLibraryActivity.class);
            intent.putExtra("album", (Parcelable) selectedItem);
            startActivityForResult(intent, -1);
        }
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final List<? extends Item> targetArray;
        switch (mPager.getCurrentItem()) {
            case 1:
                targetArray = mAlbumResults;
                break;
            case 2:
                targetArray = mSongResults;
                break;
            case 0:
            default:
                targetArray = mArtistResults;
                break;
        }
        final Object selectedItem = targetArray.get((int) info.id);
        if (item.getItemId() == GOTO_ALBUM) {
            if (selectedItem instanceof Music) {
                final Music music = (Music) selectedItem;
                final Intent intent = new Intent(this, SimpleLibraryActivity.class);
                final Parcelable artist = new Artist(music.getAlbumArtistOrArtist());
                intent.putExtra("artist", artist);
                intent.putExtra("album", music.getAlbumAsAlbum());
                startActivityForResult(intent, -1);
            }
        } else {
            MPDApplication.getInstance().oMPDAsyncHelper.execAsync(() -> {
                boolean replace = false;
                boolean play = false;
                switch (item.getItemId()) {
                    case ADD_REPLACE_PLAY:
                        replace = true;
                        play = true;
                        break;
                    case ADD_REPLACE:
                        replace = true;
                        break;
                    case ADD_PLAY:
                        play = true;
                        break;
                    default:
                        break;
                }
                add(selectedItem, replace, play);
            });
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean handled = true;

        switch (item.getItemId()) {
            case android.R.id.home:
                final Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            default:
                handled = false;
                break;
        }

        return handled;
    }

    @Override
    public void onStart() {
        super.onStart();
        MPDApplication.getInstance().setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        MPDApplication.getInstance().unsetActivity(this);
    }


    private void setContextForObject(final Object object) {
        if (object instanceof Music) {
            mAddString = R.string.addSong;
            mAddedString = R.string.songAdded;
        } else if (object instanceof Artist) {
            mAddString = R.string.addArtist;
            mAddedString = R.string.artistAdded;
        } else if (object instanceof Album) {
            mAddString = R.string.addAlbum;
            mAddedString = R.string.albumAdded;
        }
    }


    private void update(final ListView listView, final List<? extends Item> resultList,
            final View noResultsView) {
        final ListAdapter separatedListAdapter = new SeparatedListAdapter(this,
                new SearchResultDataBinder(), resultList);

        listView.setAdapter(separatedListAdapter);

        try {
            listView.setEmptyView(noResultsView);
            mLoadingView.setVisibility(View.GONE);
        } catch (final RuntimeException e) {
            Log.e(TAG, "Failed to update items.", e);
        }
    }


    private void updateFromItems() {
        update(mListArtists, mArtistResults, mNoResultArtistsView);
        update(mListAlbums, mAlbumResults, mNoResultAlbumsView);
        update(mListSongs, mSongResults, mNoResultSongsView);
    }

    private void updateList() {
        MPDApplication.getInstance().oMPDAsyncHelper.addAsyncExecListener(this);
        mJobID = MPDApplication.getInstance().oMPDAsyncHelper.execAsync(this::asyncUpdate);
    }



    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {

        @Override
        public boolean onQueryTextSubmit(String query) {
            //  Auto-generated method stub
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            mSearchKeywords = newText;
            updateList();

            mLoadingView.setVisibility(View.VISIBLE);

            return true;
        }

    };


    private class SectionsPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {

            final View v;
            switch (position) {
                case 1:
                    v = mListAlbumsFrame;
                    break;
                case 2:
                    v = mListSongsFrame;
                    break;
                case 0:
                default:
                    v = mListArtistsFrame;

                    break;
            }
            if (v.getParent() == null) {
                mPager.addView(v);
            }
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            String album = getResources().getString(R.string.albums) + mTabAlbums;
            String artist = getResources().getString(R.string.artists) + mTabArtists;
            String song = getResources().getString(R.string.songs) + mTabSongs;

            switch (position) {
                case 0:
                    return artist;
                case 1:
                    return album;
                case 2:
                    return song;
            }
            return null;
        }
    }

}
