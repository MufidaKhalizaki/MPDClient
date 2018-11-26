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

package org.oucho.mpdclient.cover.provider;

import org.oucho.mpdclient.cover.AbstractWebCover;
import org.oucho.mpdclient.helpers.AlbumInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class ItunesCover extends AbstractWebCover {

    private static final String TAG = "ItunesCover";

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) {
        final String response;
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        String coverUrl;
        JSONObject jsonObject;

        try {
            response = callCover("https://itunes.apple.com/search?term=" + albumInfo.getAlbum() + ' ' + albumInfo.getArtist() + "&limit=5&media=music&entity=album");
            jsonRootObject = new JSONObject(response);
            jsonArray = jsonRootObject.getJSONArray("results");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                coverUrl = jsonObject.getString("artworkUrl100");
                if (coverUrl != null) {
                    // Based on some tests even if the cover art size returned
                    // is 100x100
                    // Bigger versions also exists.
                    return new String[]{
                            coverUrl.replace("100x100", "600x600")
                    };
                }
            }

        } catch (final Exception e) {
            Log.e(TAG, "Failed to get cover URL from " + getName(), e);

        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "ITUNES";
    }
}
