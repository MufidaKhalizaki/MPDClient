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

package org.oucho.mpdclient.mpd.item;

import org.oucho.mpdclient.mpd.Tools;

import java.util.Arrays;
import java.util.Locale;


abstract class AbstractArtist extends Item {

    private final String mName;

    private final String mSort;

    AbstractArtist(final String name) {
        super();
        mName = name;
        if (null != name && name.toLowerCase(Locale.getDefault()).startsWith("the ")) {
            mSort = name.substring(4);
        } else {
            mSort = null;
        }
    }

    AbstractArtist(final String name, final String sort) {
        super();

        mName = name;
        mSort = sort;
    }


    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            final AbstractArtist artist = (AbstractArtist) o;

            assert artist != null;
            if (Tools.isNotEqual(mName, artist.mName) || Tools.isNotEqual(mSort, artist.mSort)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mName, mSort});
    }

    @Override
    String sortText() {
        final String result;

        if (mSort == null && mName == null) {
            result = "";
        } else if (mSort == null) {
            result = super.sortText();
        } else {
            result = mSort;
        }

        return result;
    }
}
