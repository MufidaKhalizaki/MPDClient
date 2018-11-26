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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.cover.CoverBitmapDrawable;
import org.oucho.mpdclient.helpers.AlbumInfo;

public class AlbumCoverDownloadListener implements CoverDownloadListener {

    private static final String TAG = "CoverDownloadListener";

    private final boolean mBigCoverNotFound;

    private ImageView mCoverArt;

    private ProgressBar mCoverArtProgress;

    public AlbumCoverDownloadListener(final ImageView coverArt, final ProgressBar coverArtProgress, final boolean bigCoverNotFound) {
        super();

        mCoverArt = coverArt;
        mBigCoverNotFound = bigCoverNotFound;
        mCoverArt.setVisibility(View.VISIBLE);
        mCoverArtProgress = coverArtProgress;
        mCoverArtProgress.setIndeterminate(true);
        mCoverArtProgress.setVisibility(View.INVISIBLE);
        freeCoverDrawable();
    }

    @DrawableRes
    public static int getLargeNoCoverResource() {
        return getNoCoverResource(true);
    }

    @DrawableRes
    private static int getNoCoverResource() {
        return getNoCoverResource(false);
    }

    @DrawableRes
    private static int getNoCoverResource(final boolean isLarge) {
        final int newResource;

        if (isLarge) {
            newResource = R.drawable.no_cover_art_light_big;
        } else {
            newResource = R.drawable.no_cover_art_light;
        }
        return newResource;
    }

    public void detach() {
        mCoverArtProgress = null;
        mCoverArt = null;
    }

    private void freeCoverDrawable() {
        freeCoverDrawable(null);
    }

    private void freeCoverDrawable(final Drawable oldDrawable) {
        if (mCoverArt == null) {
            return;
        }

        final Drawable coverDrawable = oldDrawable == null ? mCoverArt.getDrawable() : oldDrawable;
        if (coverDrawable == null || !(coverDrawable instanceof CoverBitmapDrawable)) {
            return;
        }

        if (oldDrawable == null) {
            final int noCoverDrawable;
            if (mBigCoverNotFound) {
                noCoverDrawable = getLargeNoCoverResource();
            } else {
                noCoverDrawable = getNoCoverResource();
            }
            mCoverArt.setImageResource(noCoverDrawable);
        }

        coverDrawable.setCallback(null);
        final Bitmap coverBitmap = ((BitmapDrawable) coverDrawable).getBitmap();
        if (coverBitmap != null) {
            coverBitmap.recycle();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isMatchingCover(final CoverInfo coverInfo) {
        return coverInfo != null && mCoverArt != null && (mCoverArt.getTag() == null || mCoverArt.getTag().equals(coverInfo.getKey()));
    }

    @Override
    public void onCoverDownloadStarted(final CoverInfo cover) {
        if (!isMatchingCover(cover)) {
            return;
        }
        if (mCoverArtProgress != null) {
            mCoverArtProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCoverDownloaded(final CoverInfo cover) {

        Resources res = MPDApplication.getInstance().getResources();

        if (!isMatchingCover(cover)) {
            return;
        }
        if (cover.getBitmap() == null) {
            return;
        }
        try {
            if (mCoverArtProgress != null) {
                mCoverArtProgress.setVisibility(View.INVISIBLE);
            }

            freeCoverDrawable(mCoverArt.getDrawable());

            Drawable art = new CoverBitmapDrawable(res, cover.getBitmap()[0]);

            // affichage du covert en fondu
            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
            fadeIn.setDuration(100);
            mCoverArt.setAnimation(fadeIn);
            mCoverArt.setImageDrawable(art);

            cover.setBitmap(null);
        } catch (final Exception e) {
            Log.w(TAG, "Exception.", e);
        }
    }

    @Override
    public void onCoverNotFound(final CoverInfo coverInfo) {
        if (!isMatchingCover(coverInfo)) {
            return;
        }
        coverInfo.setBitmap(null);
        if (mCoverArtProgress != null) {
            mCoverArtProgress.setVisibility(View.INVISIBLE);
        }
        freeCoverDrawable();
    }

    @Override
    public void tagAlbumCover(final AlbumInfo albumInfo) {
        if (mCoverArt != null && albumInfo != null) {
            mCoverArt.setTag(albumInfo.getKey());
        }
    }
}
