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

import android.graphics.Bitmap;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.cover.CoverRetriever;
import org.oucho.mpdclient.helpers.AlbumInfo;

class CoverInfo extends AlbumInfo implements MPDConfig {

    static final int MAX_SIZE = 0;

    private int mCoverMaxSize = MAX_SIZE;

    private int mCachedCoverMaxSize = MAX_SIZE;

    private Bitmap[] mBitmap = new Bitmap[0];

    private byte[] mCoverBytes = new byte[0];

    private CoverRetriever mCoverRetriever;

    private CoverDownloadListener mListener;

    private boolean mPriority;

    private boolean mRequestGivenUp = false;

    private STATE mState = STATE.NEW;

    CoverInfo(final AlbumInfo albumInfo) {
        super(albumInfo.getArtist(),
                albumInfo.getAlbum(),
                albumInfo.getPath(),
                albumInfo.getFilename());
    }

    CoverInfo(final CoverInfo coverInfo) {
        super(coverInfo.mArtist, coverInfo.mAlbum, coverInfo.mPath, coverInfo.mFilename);

        mState = coverInfo.mState;
        mBitmap = coverInfo.mBitmap;
        mCoverBytes = coverInfo.mCoverBytes;
        mPriority = coverInfo.mPriority;
        mCoverMaxSize = coverInfo.mCoverMaxSize;
        mCachedCoverMaxSize = coverInfo.mCachedCoverMaxSize;
        mCoverRetriever = coverInfo.mCoverRetriever;
        mListener = coverInfo.mListener;
        mRequestGivenUp = coverInfo.mRequestGivenUp;
    }

    public Bitmap[] getBitmap() {

        return mBitmap;
    }

    int getCachedCoverMaxSize() {
        return mCachedCoverMaxSize;
    }

    byte[] getCoverBytes() {
        return mCoverBytes;
    }

    int getCoverMaxSize() {
        return mCoverMaxSize;
    }

    CoverRetriever getCoverRetriever() {
        return mCoverRetriever;
    }

    CoverDownloadListener getListener() {
        return mListener;
    }

    STATE getState() {
        return mState;
    }

    boolean isPriority() {
        return mPriority;
    }

    boolean isRequestGivenUp() {
        return mRequestGivenUp;
    }

    public void setBitmap(final Bitmap[] bitmap) {
        mBitmap = bitmap;
    }

    void setCachedCoverMaxSize(final int cachedCoverMaxSize) {
        mCachedCoverMaxSize = cachedCoverMaxSize;
    }

    void setCoverBytes(final byte[] coverBytes) {
        mCoverBytes = coverBytes;
    }

    void setCoverMaxSize(final int coverMaxSize) {
        mCoverMaxSize = coverMaxSize;
    }

    void setCoverRetriever(final CoverRetriever coverRetriever) {
        mCoverRetriever = coverRetriever;
    }

    void setListener(final CoverDownloadListener listener) {
        mListener = listener;
    }

    void setPriority(final boolean priority) {
        mPriority = priority;
    }

    void setRequestGivenUp() {
        mRequestGivenUp = true;
    }

    void setState(final STATE state) {
        mState = state;
    }

    @Override
    public String toString() {
        return mAlbum + " priority=" + mPriority + '}';
    }

    enum STATE {
        NEW, CACHE_COVER_FETCH, WEB_COVER_FETCH, CREATE_BITMAP, COVER_FOUND, COVER_NOT_FOUND
    }
}
