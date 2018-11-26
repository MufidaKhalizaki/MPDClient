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


public class Album extends AbstractAlbum implements Parcelable {


    @SuppressWarnings("unused")
    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(final Parcel source) {
            return new Album(source);
        }

        @Override
        public Album[] newArray(final int size) {
            return new Album[size];
        }
    };

    public Album(final Album otherAlbum, final Artist artist) {
        super(otherAlbum, artist);
    }

    public Album(final String name) {
        super(name, null, false, 0L, 0L, 0L, null);

    }

    public Album(final String name, final Artist artist, final boolean hasAlbumArtist) {
        super(name, artist, hasAlbumArtist, 0L, 0L, 0L, null);

    }

    private Album(final Parcel in) {
        super(in.readString(), /* name */
                new Artist(in.readString()), /* artist */
                in.readInt() > 0, /* hasAlbumArtist */
                in.readLong(), /* songCount */
                in.readLong(), /* duration */
                in.readLong(), /* year */
                in.readString()); /* path */

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        final String artistName;

        final Artist artist = getArtist();
        final int hasAlbumArtist;

        if (artist == null) {
            artistName = "";
        } else {
            artistName = artist.getName();
        }

        if (hasAlbumArtist()) {
            hasAlbumArtist = 1;
        } else {
            hasAlbumArtist = 0;
        }

        dest.writeString(getName());
        dest.writeString(artistName);
        dest.writeInt(hasAlbumArtist);
        dest.writeLong(getSongCount());
        dest.writeLong(getDuration());
        dest.writeLong(getYear());
        dest.writeString(getPath());
    }
}
