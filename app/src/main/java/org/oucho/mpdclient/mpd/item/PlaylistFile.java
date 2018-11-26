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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlaylistFile extends Item implements FilesystemTreeEntry {

    private static final Pattern PLAYLIST_FILE_REGEXP = Pattern.compile("^.*/(.+)\\.(\\w+)$");

    private final String mFullPath;

    public PlaylistFile(final String path) {
        super();
        mFullPath = path;
    }

    @Override
    public String getFullPath() {
        return mFullPath;
    }

    @Override
    public String getName() {
        String result = "";

        if (mFullPath != null) {
            final Matcher matcher = PLAYLIST_FILE_REGEXP.matcher(mFullPath);
            if (matcher.matches()) {
                result = matcher.replaceAll("[$2] $1.$2");
            } else {
                result = mFullPath;
            }
        }
        return result;
    }
}
