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

import android.os.Parcel;
import android.os.Parcelable;


public class Artist extends AbstractArtist implements Parcelable {

    @SuppressWarnings("unused")
    public static final Creator<Artist> CREATOR = new Creator<Artist>() {

                @Override
                public Artist createFromParcel(final Parcel source) {
                    return new Artist(source);
                }

                @Override
                public Artist[] newArray(final int size) {
                    return new Artist[size];
                }
            };

    public Artist(final String name) {
        super(name);
    }

    private Artist(final Parcel in) {
        super(in.readString(), /* name */
                in.readString()); /* sort */
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(getName());
        dest.writeString(sortText());
    }
}
