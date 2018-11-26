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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.cover.helper.AlbumCoverDownloadListener;
import org.oucho.mpdclient.cover.helper.CoverAsyncHelper;
import org.oucho.mpdclient.cover.helper.CoverManager;
import org.oucho.mpdclient.helpers.AlbumInfo;
import org.oucho.mpdclient.helpers.MPDControl;
import org.oucho.mpdclient.helpers.UpdateTrackInfo;
import org.oucho.mpdclient.library.SimpleLibraryActivity;
import org.oucho.mpdclient.mpd.MPDCommand;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.Tools;
import org.oucho.mpdclient.mpd.event.StatusChangeListener;
import org.oucho.mpdclient.mpd.event.TrackPositionListener;
import org.oucho.mpdclient.mpd.item.Music;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerFragment extends Fragment
        implements
        MPDConfig,
        StatusChangeListener,
        TrackPositionListener,
        OnMenuItemClickListener,
        OnSharedPreferenceChangeListener,
        UpdateTrackInfo.FullTrackInfoUpdate {


    private static final int POPUP_ALBUM = 2;
    private static final int POPUP_ALBUM_ARTIST = 1;
    private static final int POPUP_ARTIST = 0;
    private static final int POPUP_COVER_SELECTIVE_CLEAN = 7;
    private static final int POPUP_FOLDER = 3;
    private static final int POPUP_SHARE = 5;
    private static final int POPUP_STREAM = 4;

    private final Timer mVolTimer = new Timer();

    private FragmentActivity mActivity;
    private ImageView mCoverArt;
    private CoverAsyncHelper mCoverAsyncHelper = null;
    private Music mCurrentSong = null;
    private Handler mHandler;

    private Timer mPosTimer = null;
    private TimerTask mVolTimerTask = null;

    private SeekBar mTrackSeekBar = null;
    private SeekBar mVolumeSeekBar = null;

    private TextView mAlbumNameText;
    private TextView mAudioInfoText = null;
    private TextView mSongNameText;
    private TextView mYearNameText;
    private TextView mTrackTime = null;
    private TextView mTrackTotalTime = null;

    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final int viewLayout;
        final View view;

        mSettings.registerOnSharedPreferenceChangeListener(this);

        viewLayout = R.layout.fragment_player;

        view = inflater.inflate(viewLayout, container, false);

        mTrackTime = view.findViewById(R.id.trackTime);
        mTrackTotalTime = view.findViewById(R.id.trackTotalTime);

        mAlbumNameText = view.findViewById( R.id.albumName);

        mAudioInfoText = view.findViewById(R.id.audioInfo);
        mSongNameText = view.findViewById(R.id.songName);
        mSongNameText.setText(R.string.notConnected);
        mYearNameText = view.findViewById(R.id.yearName);

        mCoverArt = getCoverArt(view);
        mCoverAsyncHelper = getCoverAsyncHelper(view);
        mTrackSeekBar = getTrackSeekBar(view);
        mVolumeSeekBar = getVolumeSeekBar(view);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        forceStatusUpdate();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
        MPDApplication.setFragmentPlayer(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MPDApplication.setFragmentPlayer(false);

        mActivity = null;
        LibraryFragment.setTitre(LibraryFragment.getPosition());
    }

    @Override
    public void onDestroy() {
        mSettings.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }


    private static SeekBar getTrackSeekBar(final View view) {
        final SeekBar.OnSeekBarChangeListener seekBarTrackListener =
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {}

                    @Override
                    public void onStartTrackingTouch(final SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        MPDControl.run(MPDControl.ACTION_SEEK, seekBar.getProgress());
                    }
                };

        final SeekBar seekBarTrack = view.findViewById(R.id.progress_track);
        seekBarTrack.setOnSeekBarChangeListener(seekBarTrackListener);

        return seekBarTrack;
    }


    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        if (connected) {
            forceStatusUpdate();
        } else {
            mSongNameText.setText(R.string.notConnected);
        }
    }

    private void downloadCover(final AlbumInfo albumInfo) {
        mCoverAsyncHelper.downloadCover(albumInfo, true);
    }

    private void forceStatusUpdate() {
        final MPDStatus status = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus();

        if (status.isValid()) {
            volumeChanged(status, -1);
            updateStatus(status);
            updateTrackInfo(status, true);
        }
    }


    private ImageView getCoverArt(final View view) {
        final ImageView coverArt = view.findViewById(R.id.albumCover);
        final PopupMenu coverMenu = new PopupMenu(mActivity, coverArt);
        final Menu menu = coverMenu.getMenu();

        menu.add(Menu.NONE, POPUP_COVER_SELECTIVE_CLEAN, Menu.NONE, R.string.resetCover);
        coverMenu.setOnMenuItemClickListener(this);
        coverArt.setOnLongClickListener(v -> {
            final boolean isConsumed;

            if (mCurrentSong != null) {
                menu.setGroupVisible(Menu.NONE, new AlbumInfo(mCurrentSong).isValid());
                coverMenu.show();
                isConsumed = true;
            } else {
                isConsumed = false;
            }

            return isConsumed;
        });

        return coverArt;
    }


    private CoverAsyncHelper getCoverAsyncHelper(final View view) {
        final CoverAsyncHelper coverAsyncHelper = new CoverAsyncHelper();
        final ProgressBar coverArtProgress = view.findViewById(R.id.albumCoverProgress);

        coverAsyncHelper.setCoverMaxSizeFromScreen(mActivity);
        coverAsyncHelper.setCachedCoverMaxSize(mCoverArt.getWidth());

        AlbumCoverDownloadListener mCoverDownloadListener = new AlbumCoverDownloadListener(mCoverArt, coverArtProgress, true);
        coverAsyncHelper.addCoverDownloadListener(mCoverDownloadListener);

        return coverAsyncHelper;
    }


    private String getShareString() {
        final char[] separator = {' ', '-', ' '};
        final String fullPath = mCurrentSong.getFullPath();
        final String sharePrefix = getString(R.string.sharePrefix);
        final String trackArtist = mCurrentSong.getArtist();
        final String trackTitle = mCurrentSong.getTitle();
        final int initialLength = trackTitle.length() + sharePrefix.length() + 64;
        final StringBuilder shareString = new StringBuilder(initialLength);

        shareString.append(sharePrefix);
        shareString.append(' ');

        if (trackArtist != null) {
            shareString.append(trackArtist);
            shareString.append(separator);
        }
        shareString.append(trackTitle);

        if (mCurrentSong.isStream() && !fullPath.startsWith(trackTitle)) {
            shareString.append(separator);
            shareString.append(fullPath);
        }

        return shareString.toString();
    }




    private SeekBar getVolumeSeekBar(final View view) {
        final SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                mVolTimerTask = new TimerTask() {
                    private int mLastSentVol = -1;

                    private SeekBar mProgress;

                    @Override
                    public void run() {
                        final int progress = mProgress.getProgress();

                        if (mLastSentVol != progress) {
                            mLastSentVol = progress;
                            MPDControl.run(MPDControl.ACTION_VOLUME_SET, progress);
                        }
                    }

                    TimerTask setProgress(final SeekBar prg) {
                        mProgress = prg;
                        return this;
                    }
                }.setProgress(seekBar);

                mVolTimer.scheduleAtFixedRate(mVolTimerTask, (long) MPDCommand.MIN_VOLUME,
                        (long) MPDCommand.MAX_VOLUME);
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                mVolTimerTask.cancel();
                mVolTimerTask.run();
            }
        };

        final SeekBar volumeSeekBar = view.findViewById(R.id.progress_volume);
        volumeSeekBar.setOnSeekBarChangeListener(seekBarListener);

        return volumeSeekBar;
    }


    private boolean isSimpleLibraryItem(final int itemId) {
        Intent intent = null;

        switch (itemId) {
            case POPUP_ALBUM:
            case POPUP_ALBUM_ARTIST:
            case POPUP_ARTIST:
            case POPUP_FOLDER:
                if (mCurrentSong != null) {
                    intent = simpleLibraryMusicItem(itemId);
                }
                break;
            case POPUP_STREAM:
                intent = new Intent(mActivity, SimpleLibraryActivity.class);
                intent.putExtra("streams", true);
                break;
            default:
                break;
        }

        if (intent != null) {

            startActivityForResult(intent, 1);
        }

        return intent != null;
    }


    private Intent simpleLibraryMusicItem(final int itemId) {
        final Intent intent = new Intent(mActivity, SimpleLibraryActivity.class);

        switch (itemId) {
            case POPUP_ALBUM:
                intent.putExtra("album", mCurrentSong.getAlbumAsAlbum());
                break;
            case POPUP_ALBUM_ARTIST:
                intent.putExtra("artist", mCurrentSong.getAlbumArtistAsArtist());
                break;
            case POPUP_ARTIST:
                intent.putExtra("artist", mCurrentSong.getArtistAsArtist());
                break;
            case POPUP_FOLDER:
                final String path = mCurrentSong.getFullPath();
                final String parent = mCurrentSong.getParent();
                if (path == null || parent == null) {
                    break;
                }
                intent.putExtra("folder", parent);
                break;
            default:
                break;
        }

        return intent;
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {}


    @Override
    public final void onCoverUpdate(final AlbumInfo albumInfo) {
        final int noCoverResource = AlbumCoverDownloadListener.getLargeNoCoverResource();
        mCoverArt.setImageResource(noCoverResource);

        if (albumInfo != null) {
            downloadCover(albumInfo);
        }
    }


    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        final AlbumInfo albumInfo;
        boolean result = true;
        final int itemId = item.getItemId();

        switch (item.getItemId()) {
            case POPUP_COVER_SELECTIVE_CLEAN:
                albumInfo = new AlbumInfo(mCurrentSong);
                CoverManager.getInstance().clear(albumInfo);
                downloadCover(albumInfo);
                break;

            case POPUP_SHARE:
                final Intent intent = new Intent(Intent.ACTION_SEND, null);
                intent.putExtra(Intent.EXTRA_TEXT, getShareString());
                intent.setType("text/plain");
                startActivity(intent);
                break;
            default:
                result = isSimpleLibraryItem(itemId);
                break;
        }

        return result;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case CoverManager.PREFERENCE_LOCALSERVER:
                CoverAsyncHelper.setCoverRetrieversFromPreferences();
                break;
            default:
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (MPDApplication.getInstance().updateTrackInfo == null) {
            MPDApplication.getInstance().updateTrackInfo = new UpdateTrackInfo();
        }

        MPDApplication.getInstance().updateTrackInfo.addCallback(this);
        MPDApplication.getInstance().oMPDAsyncHelper.addStatusChangeListener(this);
        MPDApplication.getInstance().oMPDAsyncHelper.addTrackPositionListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        MPDApplication.getInstance().updateTrackInfo.removeCallback();
        MPDApplication.getInstance().oMPDAsyncHelper.removeStatusChangeListener(this);
        MPDApplication.getInstance().oMPDAsyncHelper.removeTrackPositionListener(this);
        stopPosTimer();
    }


    @Override
    public final void onTrackInfoUpdate(Music updatedSong, CharSequence album, CharSequence artist, CharSequence date, CharSequence title) {
        mCurrentSong = updatedSong;
        mAlbumNameText.setText(album);

        if (artist != null) {
            setTitre(artist.toString());
        } else if (album != null) {
            setTitre(album.toString());
        } else if (title != null) {
            setTitre(title.toString());
        } else {
            setTitre("Radio");
        }

        mSongNameText.setText(title);
        mYearNameText.setText(date);
        updateAudioNameText(MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus());
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {

        if (mCurrentSong != null && mCurrentSong.isStream() ||
                mpdStatus.isState(MPDStatus.STATE_STOPPED)) {
            updateTrackInfo(mpdStatus, false);
        }
    }

    @Override
    public void randomChanged(final boolean random) {}

    @Override
    public void repeatChanged(final boolean repeating) {}

    private void startPosTimer(final long start, final long total) {
        stopPosTimer();
        mPosTimer = new Timer();
        final TimerTask posTimerTask = new PosTimerTask(start, total);
        mPosTimer.scheduleAtFixedRate(posTimerTask, 0L, DateUtils.SECOND_IN_MILLIS);
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
        if (mActivity != null) {
            updateStatus(mpdStatus);
            updateAudioNameText(mpdStatus);
        }
    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {}

    private void stopPosTimer() {
        if (null != mPosTimer) {
            mPosTimer.cancel();
            mPosTimer = null;
        }
    }


    private void toggleTrackProgress(final MPDStatus status) {
        final long totalTime = status.getTotalTime();

        if (totalTime == 0) {
            mTrackTime.setVisibility(View.INVISIBLE);
            mTrackTotalTime.setVisibility(View.INVISIBLE);
            stopPosTimer();
            mTrackSeekBar.setProgress(0);
            mTrackSeekBar.setEnabled(false);
        } else {
            final long elapsedTime = status.getElapsedTime();

            if (status.isState(MPDStatus.STATE_PLAYING)) {
                startPosTimer(elapsedTime, totalTime);
            } else {
                stopPosTimer();
                updateTrackProgress(elapsedTime, totalTime);
            }

            mTrackSeekBar.setMax((int) totalTime);

            mTrackTime.setVisibility(View.VISIBLE);
            mTrackTotalTime.setVisibility(View.VISIBLE);
            mTrackSeekBar.setEnabled(true);
        }
    }


    private void toggleVolumeBar(final int volume) {
        if (volume < MPDCommand.MIN_VOLUME || volume > MPDCommand.MAX_VOLUME) {
            mVolumeSeekBar.setEnabled(false);
            mVolumeSeekBar.setVisibility(View.GONE);
        } else {
            mVolumeSeekBar.setEnabled(true);
            mVolumeSeekBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        updateTrackInfo(mpdStatus, false);
    }

    @Override
    public void trackPositionChanged(final MPDStatus status) {
        toggleTrackProgress(status);
    }

    private void updateAudioNameText(final MPDStatus status) {
        StringBuilder optionalTrackInfo = null;

        if (mCurrentSong != null && !status.isState(MPDStatus.STATE_STOPPED)) {

            final char[] separator = {' ', '-', ' '};
            final String fileExtension = Tools.getExtension(mCurrentSong.getFullPath());
            final long bitRate = status.getBitrate();
            final int bitsPerSample = status.getBitsPerSample();
            final int sampleRate = status.getSampleRate();
            optionalTrackInfo = new StringBuilder(40);


            if (fileExtension != null) {
                optionalTrackInfo.append(fileExtension.toUpperCase());
            }

            /* The server can give out buggy (and empty) information from time to time. */
            if (bitRate > 0L) {
                if (optionalTrackInfo.length() > 0) {
                    optionalTrackInfo.append(separator);
                }

                optionalTrackInfo.append(bitRate);
                optionalTrackInfo.append("kbps");
            }

            if (bitsPerSample > 0) {
                if (optionalTrackInfo.length() > 0) {
                    optionalTrackInfo.append(separator);
                }

                optionalTrackInfo.append(bitsPerSample);
                optionalTrackInfo.append("bits");
            }

            if (sampleRate > 1000) {
                if (optionalTrackInfo.length() > 0) {
                    optionalTrackInfo.append(separator);
                }

                optionalTrackInfo.append(Math.abs(sampleRate / 1000.0f));
                optionalTrackInfo.append("kHz");
            }

            if (optionalTrackInfo.length() > 0) {
                mAudioInfoText.setText(optionalTrackInfo);
                mAudioInfoText.setVisibility(View.VISIBLE);
            }
        }

        if (optionalTrackInfo == null || optionalTrackInfo.length() == 0) {
            mAudioInfoText.setVisibility(View.GONE);
        }
    }


    private void updateStatus(final MPDStatus status) {
        toggleTrackProgress(status);
    }

    private void updateTrackInfo(final MPDStatus status, final boolean forcedUpdate) {
        if (MPDApplication.getInstance().oMPDAsyncHelper.oMPD.isConnected() && isAdded()) {
            toggleTrackProgress(status);
            MPDApplication.getInstance().updateTrackInfo.refresh(status, forcedUpdate);
        }
    }


    private void updateTrackProgress(final long elapsed, final long totalTrackTime) {

        final long elapsedTime;

        if (elapsed > totalTrackTime) {
            elapsedTime = totalTrackTime;
        } else {
            elapsedTime = elapsed;
        }

        mTrackSeekBar.setProgress((int) elapsedTime);

        mHandler.post(() -> {
            mTrackTime.setText(Music.timeToString(elapsedTime));
            mTrackTotalTime.setText(Music.timeToString(totalTrackTime));
        });
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
        final int volume = mpdStatus.getVolume();

        toggleVolumeBar(volume);
        mVolumeSeekBar.setProgress(volume);
    }


    private class PosTimerTask extends TimerTask {

        private final long mTimerStartTime;

        private long mElapsedTime = 0L;

        private long mStartTrackTime = 0L;

        private long mTotalTrackTime = 0L;

        private PosTimerTask(final long start, final long total) {
            super();
            mStartTrackTime = start;
            mTotalTrackTime = total;
            mTimerStartTime = new Date().getTime();
        }

        @Override
        public void run() {
            final long elapsedSinceTimerStart = new Date().getTime() - mTimerStartTime;

            mElapsedTime = mStartTrackTime + elapsedSinceTimerStart / DateUtils.SECOND_IN_MILLIS;

            updateTrackProgress(mElapsedTime, mTotalTrackTime);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

            Intent intent = new Intent();
            intent.setAction("org.oucho.mdpclient.setTitle");
            intent.putExtra("Titre", " ");
            intent.putExtra("Second", "none");

            if (MPDApplication.getFragmentAlbumSong())
                intent.putExtra("elevation", false);
            else
                intent.putExtra("elevation", true);

        getContext().sendBroadcast(intent);

            if (MPDApplication.getFragmentAlbumSong()) {
                Intent refresh = new Intent();
                refresh.setAction("org.oucho.mdpclient.refreshTitleAlbumSongList");
                getContext().sendBroadcast(refresh);
            }

        if (!MPDApplication.getFragmentAlbumSong()) {
            Intent menu = new Intent();
            menu.setAction(INTENT_SET_MENU);
            menu.putExtra("menu", true);
            getContext().sendBroadcast(menu);
        }

    }

    private void setTitre(final String artist) {

        mHandler.postDelayed(() -> {

            Intent intent = new Intent();
            intent.setAction("org.oucho.mdpclient.setTitle");
            intent.putExtra("Titre", artist);
            intent.putExtra("Second", "");
            intent.putExtra("elevation", false);
            intent.putExtra("playbar", false);

            getContext().sendBroadcast(intent);

            Intent menu = new Intent();
            menu.setAction(INTENT_SET_MENU);
            menu.putExtra("menu", false);
            getContext().sendBroadcast(menu);

        }, 300);

    }
}
