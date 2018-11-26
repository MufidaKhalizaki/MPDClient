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

package org.oucho.mpdclient.cover.helper;

import org.oucho.mpdclient.helpers.AlbumInfo;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;

import java.util.Collection;
import java.util.LinkedList;


public class CoverAsyncHelper extends Handler implements CoverDownloadListener {

    public static final int EVENT_COVER_NOT_FOUND = 2;

    private static final Message COVER_NOT_FOUND_MESSAGE;

    private static final int EVENT_COVER_DOWNLOADED = 1;

    private static final int EVENT_COVER_DOWNLOAD_STARTED = 3;

    private static final int MAX_SIZE = 0;

    private int mCoverMaxSize = MAX_SIZE;

    private int mCachedCoverMaxSize = MAX_SIZE;

    static {
        COVER_NOT_FOUND_MESSAGE = new Message();
        COVER_NOT_FOUND_MESSAGE.what = EVENT_COVER_NOT_FOUND;
    }

    private final Collection<CoverDownloadListener> mCoverDownloadListeners;

    public CoverAsyncHelper() {
        super();

        mCoverDownloadListeners = new LinkedList<>();
    }

    public static void setCoverRetrieversFromPreferences() {
        CoverManager.getInstance().setCoverRetrieversFromPreferences();
    }

    public void addCoverDownloadListener(final CoverDownloadListener listener) {
        mCoverDownloadListeners.add(listener);
    }

    public void downloadCover(final AlbumInfo albumInfo) {
        downloadCover(albumInfo, false);
    }

    public void downloadCover(final AlbumInfo albumInfo, final boolean priority) {
        final CoverInfo info = new CoverInfo(albumInfo);
        info.setCoverMaxSize(mCoverMaxSize);
        info.setCachedCoverMaxSize(mCachedCoverMaxSize);
        info.setPriority(priority);
        info.setListener(this);
        tagListenerCovers(albumInfo);

        if (albumInfo.isValid()) {
            CoverManager.getInstance().addCoverRequest(info);
        } else {
            COVER_NOT_FOUND_MESSAGE.obj = info;
            handleMessage(COVER_NOT_FOUND_MESSAGE);
        }

    }

    @Override
    public void handleMessage(final Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
            case EVENT_COVER_DOWNLOADED:
                final CoverInfo coverInfo = (CoverInfo) msg.obj;
                if (coverInfo.getCachedCoverMaxSize() < mCachedCoverMaxSize || coverInfo.getCoverMaxSize() < mCoverMaxSize) {
                    downloadCover(coverInfo);
                    break;
                }

                for (final CoverDownloadListener listener : mCoverDownloadListeners) {
                    listener.onCoverDownloaded(coverInfo);
                }

                break;
            case EVENT_COVER_NOT_FOUND:
                for (final CoverDownloadListener listener : mCoverDownloadListeners) {
                    listener.onCoverNotFound((CoverInfo) msg.obj);
                }
                break;
            case EVENT_COVER_DOWNLOAD_STARTED:
                for (final CoverDownloadListener listener : mCoverDownloadListeners) {
                    listener.onCoverDownloadStarted((CoverInfo) msg.obj);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onCoverDownloadStarted(final CoverInfo cover) {
        obtainMessage(EVENT_COVER_DOWNLOAD_STARTED, cover).sendToTarget();
    }

    @Override
    public void onCoverDownloaded(final CoverInfo cover) {
        obtainMessage(EVENT_COVER_DOWNLOADED, cover).sendToTarget();
    }

    @Override
    public void onCoverNotFound(final CoverInfo coverInfo) {
        obtainMessage(EVENT_COVER_NOT_FOUND, coverInfo).sendToTarget();
    }


    public void setCachedCoverMaxSize(final int size) {
        if (size < 0) {
            mCachedCoverMaxSize = MAX_SIZE;
        } else {
            mCachedCoverMaxSize = size;
        }
    }

    public void setCoverMaxSize(final int size) {
        if (size < 0) {
            mCoverMaxSize = MAX_SIZE;
        } else {
            mCoverMaxSize = size;
        }
    }

    public void setCoverMaxSizeFromScreen(final Activity activity) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        setCoverMaxSize(Math.min(metrics.widthPixels, metrics.heightPixels));
    }

    @Override
    public void tagAlbumCover(final AlbumInfo albumInfo) {
    }

    private void tagListenerCovers(final AlbumInfo albumInfo) {
        for (final CoverDownloadListener listener : mCoverDownloadListeners) {
            listener.tagAlbumCover(albumInfo);
        }
    }
}
