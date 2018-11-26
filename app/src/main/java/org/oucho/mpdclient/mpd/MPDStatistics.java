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

package org.oucho.mpdclient.mpd;

import java.util.Collection;
import java.util.Date;

import static org.oucho.mpdclient.mpd.Tools.KEY;
import static org.oucho.mpdclient.mpd.Tools.VALUE;

public class MPDStatistics {

    private static final long MILLI_TO_SEC = 1000L;

    private static final String TAG = "MPDStatistics";

    private long mAlbums = -1L;

    private long mArtists = -1L;

    private long mDBPlaytime = -1L;

    private Date mDbUpdate = null;

    private long mPlayTime = -1L;

    private long mSongs = -1L;

    private long mUpTime = -1L;

    MPDStatistics() {
        super();
    }


    public Date getDbUpdate() {
        Date dbUpdate = null;

        if (mDbUpdate != null) {
            dbUpdate = (Date) mDbUpdate.clone();
        }

        return dbUpdate;
    }


    public String toString() {
        return "artists: " + mArtists +
                ", albums: " + mAlbums +
                ", last db update: " + mDbUpdate +
                ", database playtime: " + mDBPlaytime +
                ", playtime: " + mPlayTime +
                ", songs: " + mSongs +
                ", up time: " + mUpTime;
    }


    public final void update(final Collection<String> response) {
        for (final String[] pair : Tools.splitResponse(response)) {

            switch (pair[KEY]) {
                case "albums":
                    mAlbums = Long.parseLong(pair[VALUE]);
                    break;
                case "artists":
                    mArtists = Long.parseLong(pair[VALUE]);
                    break;
                case "db_playtime":
                    mDBPlaytime = Long.parseLong(pair[VALUE]);
                    break;
                case "db_update":
                    mDbUpdate = new Date(Long.parseLong(pair[VALUE]) * MILLI_TO_SEC);
                    break;
                case "playtime":
                    mPlayTime = Long.parseLong(pair[VALUE]);
                    break;
                case "songs":
                    mSongs = Long.parseLong(pair[VALUE]);
                    break;
                case "uptime":
                    mUpTime = Long.parseLong(pair[VALUE]);
                    break;
                default:
                    Log.warning(TAG,
                            "Undocumented statistic: Key: " + pair[KEY] + " Value: " + pair[VALUE]);
                    break;
            }
        }
    }
}
