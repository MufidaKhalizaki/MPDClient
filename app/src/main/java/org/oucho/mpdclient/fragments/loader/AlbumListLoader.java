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

package org.oucho.mpdclient.fragments.loader;

import android.content.Context;
import android.util.Log;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;
import org.oucho.mpdclient.mpd.item.Item;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class AlbumListLoader extends BaseLoader<List<? extends Item>> implements MPDConfig {

    private Artist mArtist = null;
    private List<? extends Item> mItems = null;

    public AlbumListLoader(Context context) {
        super(context);
    }

    public AlbumListLoader(Context context, Artist artist) {
        super(context);
        mArtist = artist;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<? extends Item> loadInBackground() {

        final String tri = mSettings.getString("tri", "az");

        try {

            mItems = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getAlbums(mArtist);

           if (tri.equals("year")) {
               Collections.sort((List<? extends Album>) mItems, Album.SORT_BY_YEAR_DSC);
           } else if (tri.equals("artist")) {

               if (mArtist == null)
                Collections.sort((List<? extends Album>) mItems, Album.SORT_BY_ARTIST);
           }

        } catch (final IOException | MPDException ignore) {
            Log.w("AlbumListLoader", "Failed to update.");
        }

        return mItems;
    }

}