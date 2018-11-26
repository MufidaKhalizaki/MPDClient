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

public final class Log {

    private Log() {
        super();
    }

    static void debug(final String message) {
        android.util.Log.d(MPDStatus.TAG, message);
    }

    public static void error(final String tag, final String message) {
        android.util.Log.e(tag, message);
    }

    public static void error(final String tag, final String message, final Throwable tr) {
        android.util.Log.e(tag, message, tr);
    }

    static void warning(final String tag, final String message) {
        android.util.Log.w(tag, message);
    }

}
