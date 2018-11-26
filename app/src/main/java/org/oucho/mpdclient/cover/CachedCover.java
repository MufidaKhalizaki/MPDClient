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

package org.oucho.mpdclient.cover;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.helpers.AlbumInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.oucho.mpdclient.cover.helper.CoverManager.getCoverFileName;

public class CachedCover implements CoverRetriever, MPDConfig {

    private static final String FOLDER_SUFFIX = "/covers/";

    private static final String TAG = "CachedCover";

    public void clear() {
        delete(null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void delete(final AlbumInfo albumInfo) {
        final File[] files = getAllCachedCoverFiles();

        if (files != null) {

            for (final File file : files) {

                if (albumInfo != null && getCoverFileName(albumInfo).equals(file.getName())) {
                    Log.d(TAG, "Deleting cover : " + file.getName());
                }

                if (albumInfo == null || getCoverFileName(albumInfo).equals(file.getName())) {
                    file.delete();
                }
            }
        }
    }

    private String getAbsoluteCoverFolderPath() {
        final File cacheDir = MPDApplication.getInstance().getCacheDir();
        if (cacheDir == null) {
            return null;
        }
        return cacheDir.getAbsolutePath() + FOLDER_SUFFIX;
    }

    private String getAbsolutePathForSong(final AlbumInfo albumInfo) {
        final File cacheDir = MPDApplication.getInstance().getCacheDir();

        if (cacheDir == null) {
            return null;
        }

        return getAbsoluteCoverFolderPath() + getCoverFileName(albumInfo);
    }

    private File[] getAllCachedCoverFiles() {
        final String cacheFolderPath = getAbsoluteCoverFolderPath();
        File[] result = null;

        if (cacheFolderPath != null) {
            final File cacheFolder = new File(cacheFolderPath);
            if (cacheFolder.exists()) {
                result = cacheFolder.listFiles();
            }
        }

        return result;
    }

    public long getCacheUsage() {
        long size = 0L;
        final File[] files = getAllCachedCoverFiles();

        if (files != null) {
            for (final File file : files) {
                size += file.length();
            }
        }

        return size;
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo)  {
        final String storageState = Environment.getExternalStorageState();
        // If there is no external storage available, don't bother
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState) || Environment.MEDIA_MOUNTED.equals(storageState)) {
            final String url = getAbsolutePathForSong(albumInfo);

            assert url != null;
            if (new File(url).exists()) {
                return new String[]{url};
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "SD Card Cache";
    }

    @Override
    public boolean isCoverLocal() {
        return true;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public void save(final AlbumInfo albumInfo, final Bitmap cover) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // External storage is not there or read only, don't do anything
            Log.e(TAG, "No writable external storage, not saving cover to cache");
            return;
        }
        FileOutputStream out = null;
        try {
            new File(getAbsoluteCoverFolderPath()).mkdirs();
            out = new FileOutputStream(getAbsolutePathForSong(albumInfo));

            Bitmap img = ImageFactory.resizedBitmap(cover);
            img.compress(Bitmap.CompressFormat.JPEG, 95, out);


        } catch (final Exception e) {
            Log.e(TAG, "Cache cover write failure.", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover stream.", e);
                }
            }
        }
    }

}
