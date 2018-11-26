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

package org.oucho.mpdclient.search.bordel;

import org.oucho.mpdclient.R;

import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;

import android.view.View;
import android.widget.TextView;

public class SearchResultDataBinder implements SeparatedListDataBinder {


    private static String join(final String... parts) {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            final String part = parts[i];
            if (part != null && !part.isEmpty()) {
                result.append(part);
                if (i < parts.length - 1) {
                    result.append(" - ");
                }
            }
        }
        return result.toString();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onDataBind(final View targetView,
                           final Object item) {
        final TextView text1 = targetView.findViewById(R.id.line1);
        final TextView text2 = targetView.findViewById(R.id.line2);
        String formattedResult1 = null;
        String formattedResult2 = null;

        if (item instanceof Music) {
            final Music music;
            music = (Music) item;
            formattedResult1 = music.getTitle();
            formattedResult2 = join(music.getAlbum(), music.getArtist());
        } else if (item instanceof Artist) {
            formattedResult1 = ((Item) item).mainText();
        } else if (item instanceof Album) {
            final Album album = (Album) item;

            final Artist artist = album.getArtist();

            formattedResult1 = album.mainText();

            if (artist != null) {
                formattedResult2 = artist.mainText();
            }
        }

        if (formattedResult2 == null) {
            text2.setVisibility(View.GONE);
        } else {
            text2.setVisibility(View.VISIBLE);
        }

        text1.setText(formattedResult1);
        text2.setText(formattedResult2);
    }

}
