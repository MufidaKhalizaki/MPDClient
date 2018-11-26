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
import java.util.Comparator;


public abstract class AbstractAlbum extends Item {

    private String mPath;
    private final String mName;
    private final Artist mArtist;

    private long mYear;
    private long mDuration;
    private long mSongCount;

    private boolean mHasAlbumArtist;


    public static final Comparator<AbstractAlbum> SORT_BY_ARTIST = new Comparator<AbstractAlbum>() {

        @Override
        public int compare(final AbstractAlbum lhn, final AbstractAlbum rhn) {

            final String leftArtist = formattedName(lhn.mArtist);
            final String rightArtist = formattedName(rhn.mArtist);

            return leftArtist.compareToIgnoreCase(rightArtist);
        }


        private String formattedName(final Artist artist) {

            final StringBuilder stringBuilder = new StringBuilder();

            String toto = String.valueOf(artist).replaceFirst("The ", "");

            stringBuilder.append(toto);

            return stringBuilder.toString();
        }
    };

    public static final Comparator<AbstractAlbum> SORT_BY_YEAR_DSC = new Comparator<AbstractAlbum>() {

        @Override
        public int compare(final AbstractAlbum lhs, final AbstractAlbum rhs) {
            int compare = 0;
            final int leftYear = formattedYear(lhs.mYear);
            final int rightYear = formattedYear(rhs.mYear);

            if (leftYear < rightYear) {
                compare = 1;
            } else if (leftYear > rightYear) {
                compare = -1;
            }

            if (compare == 0) {
                compare = lhs.compareTo(rhs);
            }

            return compare;
        }


        private int formattedYear(final long date) {
            final int result;

            if (date > 0L) {

                final StringBuilder stringBuilder = new StringBuilder(8);
                stringBuilder.append(date);

                final int yearLength = stringBuilder.length();

                if (yearLength > 8) {
                    stringBuilder.setLength(8);
                } else if (yearLength < 8) {
                    while (stringBuilder.length() < 8) {
                        stringBuilder.append('0');
                    }
                }
                result = Integer.parseInt(stringBuilder.toString());
            } else {
                result = 0;
            }

            return result;
        }
    };

    AbstractAlbum(AbstractAlbum album, Artist artist) {

        this(album.mName, artist, true, album.mSongCount, album.mDuration, album.mYear,
                album.mPath);
    }


    AbstractAlbum(String name, Artist artist, boolean hasAlbumArtist, long songCount, long duration, long year, String path) {
        super();
        mName = name;
        mSongCount = songCount;
        mDuration = duration;
        mYear = year;
        mArtist = artist;
        mHasAlbumArtist = hasAlbumArtist;
        mPath = path;
    }

    @Override
    public boolean doesNameExist(final Item o) {
        final boolean result;

        if (o instanceof AbstractAlbum) {
            final AbstractAlbum a = (AbstractAlbum) o;
            result = mName.equals(a.mName) && mArtist.doesNameExist(a.mArtist);
        } else {
            result = false;
        }

        return result;
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
            final AbstractAlbum album = (AbstractAlbum) o;

            assert album != null;
            if (Tools.isNotEqual(mName, album.mName) || Tools.isNotEqual(mArtist, album.mArtist)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual;
    }

    public Artist getArtist() {
        return mArtist;
    }

    long getDuration() {
        return mDuration;
    }

    @Override
    public String getName() {
        return mName;
    }

    public String getPath() {
        return mPath;
    }

    long getSongCount() {
        return mSongCount;
    }

    public long getYear() {
        return mYear;
    }

    public boolean hasAlbumArtist() {
        return mHasAlbumArtist;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mName, mArtist});
    }

    public void setDuration(final long duration) {
        mDuration = duration;
    }

    public void setHasAlbumArtist() {
        mHasAlbumArtist = false;
    }

    public void setPath(final String p) {
        mPath = p;
    }

    public void setSongCount(final long sc) {
        mSongCount = sc;
    }

    public void setYear(final long y) {
        mYear = y;
    }

}
