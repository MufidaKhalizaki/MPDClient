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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class Stream extends Item {

    private final String mName;

    private final String mUrl;

    private int mPos;

    public Stream(final String name, final String url, final int pos) {
        super();

        mName = name;
        mUrl = url;
        mPos = pos;
    }

    public static String addStreamName(final String url, final String name) {
        final StringBuilder streamName;

        if (name == null) {
            streamName = new StringBuilder(url.length() + 3);
        } else {
            streamName = new StringBuilder(url.length() + name.length() + 3);
        }
        streamName.append(url);

        if (name != null && !name.isEmpty()) {
            String path = null;

            try {
                path = new URL(url).getPath();
            } catch (final MalformedURLException ignored) { }

            if (path == null || path.isEmpty()) {
                streamName.append('/');
            }
            streamName.append('#');
            streamName.append(name);
        }

        return streamName.toString();
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
            final Stream stream = (Stream) o;

            assert stream != null;
            if (Tools.isNotEqual(mName, stream.mName)) {
                isEqual = Boolean.FALSE;
            }

            if (Tools.isNotEqual(mUrl, stream.mUrl)) {
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

    public int getPos() {
        return mPos;
    }

    public String getUrl() {
        return mUrl;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mName, mUrl});
    }

    public void setPos(final int pos) {
        mPos = pos;
    }
}
