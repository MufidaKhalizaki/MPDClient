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

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.util.Log;

import org.oucho.mpdclient.mpd.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.oucho.mpdclient.mpd.Tools.KEY;
import static org.oucho.mpdclient.mpd.Tools.VALUE;


public abstract class AbstractMusic extends Item implements FilesystemTreeEntry {

    public static final Comparator<AbstractMusic> COMPARE_WITHOUT_TRACK_NUMBER =
            new Comparator<AbstractMusic>() {
                /**
                 * Compares the two specified objects to determine their relative ordering. The ordering
                 * implied by the return value of this method for all possible pairs of
                 * {@code (lhs, rhs)} should form an <i>equivalence relation</i>.
                 * This means that
                 * <ul>
                 * <li>{@code compare(a, a)} returns zero for all {@code a}</li>
                 * <li>the sign of {@code compare(a, b)} must be the opposite of the sign of {@code
                 * compare(b, a)} for all pairs of (a,b)</li>
                 * <li>From {@code compare(a, b) > 0} and {@code compare(b, c) > 0} it must
                 * follow {@code compare(a, c) > 0} for all possible combinations of {@code
                 * (a, b, c)}</li>
                 * </ul>
                 *
                 * @param lhs an {@code Object}.
                 * @param rhs a second {@code Object} to compare with {@code lhs}.
                 * @return an integer < 0 if {@code lhs} is less than {@code rhs}, 0 if they are
                 * equal, and > 0 if {@code lhs} is greater than {@code rhs}.
                 * @throws ClassCastException if objects are not of the correct type.
                 */
                @Override
                public int compare(final AbstractMusic lhs, final AbstractMusic rhs) {
                    int compare = 0;

                    if (lhs != null) {
                        compare = lhs.compareTo(rhs, false);
                    }

                    return compare;
                }
            };


    private static final Pattern DATE_DELIMITER = Pattern.compile("\\D+");

    private static final int MUSIC_ATTRIBUTES = 30;

    private static final String TAG = "Music";

    private static final int UNDEFINED_INT = -1;

    final String mAlbum;

    final String mAlbumArtist;

    final String mArtist;

    final String mComposer;

    final long mDate;

    final int mDisc;

    final String mFullPath;

    final String mGenre;

    final String mName;

    final int mSongId;

    final int mSongPos;

    final long mTime;

    final String mTitle;

    final int mTotalTracks;

    final int mTrack;

    AbstractMusic(final String album, final String artist, final String albumArtist,
            final String composer, final String fullPath, final int disc, final long date,
            final String genre, final long time, final String title, final int totalTracks,
            final int track, final int songId, final int songPos, final String name) {
        super();

        mAlbum = album;
        mArtist = artist;
        mAlbumArtist = albumArtist;
        mComposer = composer;
        mFullPath = fullPath;
        mDisc = disc;
        mDate = date;
        mGenre = genre;
        mTime = time;
        mTitle = title;
        mTotalTracks = totalTracks;
        mTrack = track;
        mSongId = songId;
        mSongPos = songPos;
        mName = name;
    }

    @SuppressWarnings("ConstantConditions")
    private static Music build(final Collection<String> response) {
        String album = null;
        String artist = null;
        String albumArtist = null;
        String composer = null;
        String fullPath = null;
        int disc = UNDEFINED_INT;
        long date = -1L;
        String genre = null;
        long time = -1L;
        String title = null;
        int totalTracks = UNDEFINED_INT;
        int track = UNDEFINED_INT;
        int songId = UNDEFINED_INT;
        int songPos = UNDEFINED_INT;
        String name = null;

        for (final String[] pair : Tools.splitResponse(response)) {

            switch (pair[KEY]) {
                case "file":
                    fullPath = pair[VALUE];
                    if (!fullPath.isEmpty() && fullPath.contains("://")) {
                        final int pos = fullPath.indexOf('#');
                        if (pos > 1) {
                            name = fullPath.substring(pos + 1, fullPath.length());
                            fullPath = fullPath.substring(0, pos);
                        }
                    }
                    break;
                case "Album":
                    album = pair[VALUE];
                    break;
                case "AlbumArtist":
                    albumArtist = pair[VALUE];
                    break;
                case "Artist":
                    artist = pair[VALUE];
                    break;
                case "Composer":
                    composer = pair[VALUE];
                    break;
                case "Date":
                    try {
                        final Matcher matcher = DATE_DELIMITER.matcher(pair[VALUE]);
                        date = Long.parseLong(matcher.replaceAll(""));
                    } catch (final NumberFormatException e) {
                        Log.w(TAG, "Not a valid date. " + e);
                    }
                    break;
                case "Disc":
                    final int discIndex = pair[VALUE].indexOf('/');

                    try {
                        if (discIndex == -1) {
                            disc = Integer.parseInt(pair[VALUE]);
                        } else {
                            disc = Integer.parseInt(pair[VALUE].substring(0, discIndex));
                        }
                    } catch (final NumberFormatException e) {
                        Log.w(TAG, "Not a valid disc number. " + e);
                    }
                    break;
                case "Genre":
                    genre = pair[VALUE];
                    break;
                case "Id":
                    try {
                        songId = Integer.parseInt(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        Log.e(TAG, "Not a valid song ID. " + e);
                    }
                    break;
                case "Name":
                    /*
                     * name may already be assigned to the stream name in file conditional
                     */
                    if (name == null) {
                        name = pair[VALUE];
                    }
                    break;
                case "Pos":
                    try {
                        songPos = Integer.parseInt(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        Log.e(TAG, "Not a valid song position. " + e);
                    }
                    break;
                case "Time":
                    try {
                        time = Long.parseLong(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        Log.e(TAG, "Not a valid time number. " + e);
                    }
                    break;
                case "Title":
                    title = pair[VALUE];
                    break;
                case "Track":
                    final int trackIndex = pair[VALUE].indexOf('/');

                    try {
                        if (trackIndex == -1) {
                            track = Integer.parseInt(pair[VALUE]);
                        } else {
                            track = Integer.parseInt(pair[VALUE].substring(0, trackIndex));
                            totalTracks = Integer.parseInt(pair[VALUE].substring(trackIndex + 1));
                        }
                    } catch (final NumberFormatException e) {
                        Log.w(TAG, "Not a valid track number. " + e);
                    }
                    break;
                default:
                    /*
                     * Ignore everything else, there are a lot of
                     * uninteresting blocks the server might send.
                     */
                    break;
            }
        }

        return new Music(album, artist, albumArtist, composer, fullPath, disc, date, genre,
                time, title, totalTracks, track, songId, songPos, name);
    }


    private static int compareIntegers(final int lhs, final int rhs) {
        int result = 0;

        if (lhs != rhs) {
            if (lhs == UNDEFINED_INT) {
                result = -1;
            } else if (rhs == UNDEFINED_INT) {
                result = 1;
            }
        }

        if (result == 0) {
            result = Integer.compare(lhs, rhs);
        }

        return result;
    }

    private static int compareString(final String lhs, final String rhs) {
        final int result;

        if (lhs == null && rhs == null) {
            result = 0;
        } else if (lhs == null) {
            result = -1; // lhs < rhs
        } else if (rhs == null) {
            result = 1;  // lhs > rhs
        } else {
            result = lhs.compareToIgnoreCase(rhs);
        }

        return result;
    }

    public static List<Music> getMusicFromList(final Collection<String> response,
            final boolean sort) {
        final Collection<String> lineCache = new ArrayList<>(MUSIC_ATTRIBUTES);
        final int size = response.size();
        final List<Music> result;

        if (size > MUSIC_ATTRIBUTES) {
            result = new ArrayList<>(size / MUSIC_ATTRIBUTES);
        } else {
            result = new ArrayList<>(0);
        }

        for (final String line : response) {
            if (line.startsWith("file: ")) {
                if (!lineCache.isEmpty()) {
                    result.add(build(lineCache));
                    lineCache.clear();
                }
            }
            lineCache.add(line);
        }

        if (!lineCache.isEmpty()) {
            result.add(build(lineCache));
        }

        if (sort) {
            Collections.sort(result);
        }

        return result;
    }

    private static boolean isEmpty(final String s) {
        return null == s || s.isEmpty();
    }


    @SuppressLint("DefaultLocale")
    public static String timeToString(final long totalSeconds) {
        final String result;
        final long secondsInHour = 3600L;
        final long secondsInMinute = 60L;
        final long hours;
        final long minutes;
        long seconds;

        if (totalSeconds < 0L) {
            seconds = 0L;
        } else {
            seconds = totalSeconds;
        }

        hours = seconds / secondsInHour;
        seconds -= secondsInHour * hours;

        minutes = seconds / secondsInMinute;
        seconds -= minutes * secondsInMinute;

        if (hours == 0) {
            result = String.format("%02d:%02d", minutes, seconds);
        } else {
            result = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }

        return result;
    }


    private int compareTo(final Item another, final boolean withTrackNumber) {
        int compareResult;

        if (another instanceof AbstractMusic) {
            final AbstractMusic om = (AbstractMusic) another;

            compareResult = compareIntegers(mSongId, om.mSongId);

            if (withTrackNumber) {
                if (compareResult == 0) {
                    /* Order by the disc number. */
                    compareResult = compareIntegers(mDisc, om.mDisc);
                }

                if (compareResult == 0) {
                    /* Order by track number. */
                    compareResult = compareIntegers(mTrack, om.mTrack);
                }
            }

            if (compareResult == 0) {
                /* Order by song title (getTitle() fallback on file names). */
                compareResult = compareString(getTitle(), om.getTitle());
            }

            if (compareResult == 0) {
                /* Order by name (this is helpful for streams). */
                compareResult = compareString(mName, om.mName);
            }

            if (compareResult == 0) {
                /* As a last resort, order by the full path. */
                compareResult = compareString(mFullPath, om.mFullPath);
            }
        } else {
            compareResult = super.compareTo(another);
        }

        return compareResult;
    }


    @Override
    public int compareTo(@NonNull final Item another) {
        return compareTo(another, true);
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
            final AbstractMusic music = (AbstractMusic) o;

            assert music != null;
            final Object[][] equalsObjects = {
                    {mAlbum, music.mAlbum},
                    {mAlbumArtist, music.mAlbumArtist},
                    {mArtist, music.mArtist},
                    {mComposer, music.mComposer},
                    {mGenre, music.mGenre},
                    {mName, music.mName},
                    {mTitle, music.mTitle}
            };

            final int[][] equalsInt = {
                    {mDisc, music.mDisc},
                    {mSongId, music.mSongId},
                    {mSongPos, music.mSongPos},
                    {mTotalTracks, music.mTotalTracks},
                    {mTrack, music.mTrack}
            };

            if (mDate != music.mDate || mTime != music.mTime || Tools.isNotEqual(equalsInt)) {
                isEqual = Boolean.FALSE;
            }

            if (!mFullPath.equals(music.mFullPath) || Tools.isNotEqual(equalsObjects)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getAlbumArtist() {
        return mAlbumArtist;
    }

    public Artist getAlbumArtistAsArtist() {
        return new Artist(mAlbumArtist);
    }

    public String getAlbumArtistOrArtist() {
        final String result;

        if (mAlbumArtist != null && !mAlbumArtist.isEmpty()) {
            result = mAlbumArtist;
        } else if (mArtist != null && !mArtist.isEmpty()) {
            result = mArtist;
        } else {
            result = getArtistAsArtist().mainText();
        }

        return result;
    }

    public Album getAlbumAsAlbum() {
        final boolean isAlbumArtist = !isEmpty(mAlbumArtist);
        final Artist artist;

        if (isAlbumArtist) {
            artist = new Artist(mAlbumArtist);
        } else {
            artist = new Artist(mArtist);
        }

        return new Album(mAlbum, artist, isAlbumArtist);
    }

    public String getArtist() {
        return mArtist;
    }

    public Artist getArtistAsArtist() {
        return new Artist(mArtist);
    }

    public long getDate() {
        return mDate;
    }

    public String getFilename() {
        String result = null;

        if (mFullPath != null) {
            final int pos = mFullPath.lastIndexOf('/');
            if (pos == -1 || pos == mFullPath.length() - 1) {
                result = mFullPath;
            } else {
                result = mFullPath.substring(pos + 1);
            }
        }

        return result;
    }

    @Override
    public String getFullPath() {
        return mFullPath;
    }

    @Override
    public String getName() {
        final String name;

        if (isEmpty(mName)) {
            name = getFilename();
        } else {
            name = mName;
        }

        return name;
    }

    public String getParent() {
        String parent = null;

        if (mFullPath != null) {
            final int pos = mFullPath.lastIndexOf('/');

            if (pos != -1) {
                parent = mFullPath.substring(0, pos);
            }
        }

        return parent;
    }

    public String getPath() {
        final String result;
        if (null != mFullPath && mFullPath.length() > getFilename().length()) {
            result = mFullPath.substring(0, mFullPath.length() - getFilename().length() - 1);
        } else {
            result = "";
        }
        return result;
    }

    public int getPos() {
        return mSongPos;
    }

    public int getSongId() {
        return mSongId;
    }

    public long getTime() {
        return mTime;
    }

    public String getTitle() {
        if (isEmpty(mTitle)) {
            return getFilename();
        } else {
            return mTitle;
        }
    }

    public boolean hasTitle() {
        return null != mTitle && !mTitle.isEmpty();
    }

    @Override
    public int hashCode() {
        final Object[] objects = {mAlbum, mArtist, mAlbumArtist, mGenre, mName, mTitle};

        int result = 31 * mFullPath.hashCode();
        result = 31 * result + mDisc;
        result = 31 * result + (int) (mDate ^ (mDate >>> 32));
        result = 31 * result + mSongId;
        result = 31 * result + mSongPos;
        result = 31 * result + (int) (mTime ^ (mTime >>> 32));
        result = 31 * result + mTotalTracks;
        result = 31 * result + mTrack;

        return result + Arrays.hashCode(objects);
    }

    public boolean isStream() {
        return null != mFullPath && mFullPath.contains("://");
    }

    @Override
    public String mainText() {
        return getTitle();
    }
}
