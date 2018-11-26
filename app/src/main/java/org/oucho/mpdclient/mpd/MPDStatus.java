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


public class MPDStatus {


    public static final int STATE_PAUSED = 2;

    public static final int STATE_PLAYING = 0;

    public static final int STATE_STOPPED = 1;

    static final int STATE_UNKNOWN = 3;

    private static final String MPD_STATE_PAUSED = "pause";

    private static final String MPD_STATE_PLAYING = "play";

    private static final String MPD_STATE_STOPPED = "stop";

    private static final String MPD_STATE_UNKNOWN = "unknown";

    public static final String TAG = "MPDStatus";

    private long mBitRate;

    private int mBitsPerSample;

    private int mChannels;

    private boolean mConsume;

    private int mCrossFade;

    private long mElapsedTime;

    private float mElapsedTimeHighResolution;

    private String mError = null;

    private float mMixRampDB;

    private float mMixRampDelay;

    private boolean mMixRampDisabled;

    private int mNextSong;

    private int mNextSongId;

    private int mPlaylistLength;

    private int mPlaylistVersion;

    private boolean mRandom;

    private boolean mRepeat;

    private int mSampleRate;

    private boolean mSingle;

    private int mSong;

    private int mSongId;

    private int mState;

    private long mTotalTime;

    private long mUpdateTime;

    private boolean mUpdating;

    private int mVolume;

    MPDStatus() {
        super();
        resetValues();

        /* These are in every status update. */
        mConsume = false;
        mMixRampDB = 0.0f;
        mPlaylistLength = 0;
        mPlaylistVersion = 0;
        mRandom = false;
        mRepeat = false;
        mSingle = false;
        mState = STATE_UNKNOWN;
        mVolume = 0;
    }


    public final long getBitrate() {
        return mBitRate;
    }

    public final int getBitsPerSample() {
        return mBitsPerSample;
    }

    public final long getElapsedTime() {
        final long result;

        if (isState(STATE_PLAYING)) {
            /* We can't expect to always update right before this is called. */
            final long sinceUpdated = (new Date().getTime() - mUpdateTime) / 1000;

            result = sinceUpdated + mElapsedTime;
        } else {
            result = mElapsedTime;
        }

        return result;
    }

    public final int getPlaylistLength() {
        return mPlaylistLength;
    }

    final int getPlaylistVersion() {
        return mPlaylistVersion;
    }

    public final int getSampleRate() {
        return mSampleRate;
    }

    public final int getSongId() {
        return mSongId;
    }

    public final int getSongPos() {
        return mSong;
    }

    public final int getState() {
        return mState;
    }

    public final long getTotalTime() {
        return mTotalTime;
    }

    public final int getVolume() {
        return mVolume;
    }

    public final boolean isConsume() {
        return mConsume;
    }

    public final boolean isRandom() {
        return mRandom;
    }

    public final boolean isRepeat() {
        return mRepeat;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public final boolean isSingle() {
        return mSingle;
    }

    public final boolean isState(final int queryState) {
        return mState == queryState;
    }

    final boolean isUpdating() {
        return mUpdating;
    }

    public final boolean isValid() {
        return mState != STATE_UNKNOWN;
    }

    private void resetValues() {
        mBitRate = 0L;
        mBitsPerSample = 0;
        mChannels = 0;
        mCrossFade = 0;
        mElapsedTime = 0L;
        mElapsedTimeHighResolution = 0.0f;
        //noinspection AssignmentToNull
        mError = null;
        mMixRampDelay = 0.0f;
        mMixRampDisabled = false;
        mNextSong = -1;
        mNextSongId = 0;
        mSampleRate = 0;
        mSong = 0;
        mSongId = 0;
        mTotalTime = 0L;
        mUpdating = false;
        mVolume = 0;
    }

    public final String toString() {
        return "bitsPerSample: " + mBitsPerSample +
                ", bitrate: " + mBitRate +
                ", channels: " + mChannels +
                ", consume: " + mConsume +
                ", crossfade: " + mCrossFade +
                ", elapsedTime: " + mElapsedTime +
                ", elapsedTimeHighResolution: " + mElapsedTimeHighResolution +
                ", error: " + mError +
                ", nextSong: " + mNextSong +
                ", nextSongId: " + mNextSongId +
                ", mixRampDB: " + mMixRampDB +
                ", mixRampDelay: " + mMixRampDelay +
                ", mixRampDisabled: " + mMixRampDisabled +
                ", playlist: " + mPlaylistVersion +
                ", playlistLength: " + mPlaylistLength +
                ", random: " + mRandom +
                ", repeat: " + mRepeat +
                ", sampleRate: " + mSampleRate +
                ", single: " + mSingle +
                ", song: " + mSong +
                ", songid: " + mSongId +
                ", state: " + mState +
                ", totalTime: " + mTotalTime +
                ", updating: " + mUpdating +
                ", volume: " + mVolume;
    }

    @SuppressWarnings("ConstantConditions")
    final void updateStatus(final Collection<String> response) {
        resetValues();

        for (final String[] pair : Tools.splitResponse(response)) {

            switch (pair[KEY]) {
                case "audio":
                    final int delimiterIndex = pair[VALUE].indexOf(':');
                    final String tmp = pair[VALUE].substring(delimiterIndex + 1);
                    final int secondIndex = tmp.indexOf(':');

                    try {
                        mSampleRate = Integer.parseInt(pair[VALUE].substring(0, delimiterIndex));
                        mBitsPerSample = Integer.parseInt(tmp.substring(0, secondIndex));
                        mChannels = Integer.parseInt(tmp.substring(secondIndex + 1));
                    } catch (final NumberFormatException ignored) {
                        // Sometimes mpd sends "?" as a sampleRate or
                        // bitsPerSample, etc ... hotfix for a bugreport I had.
                    }
                    break;
                case "bitrate":
                    mBitRate = Long.parseLong(pair[VALUE]);
                    break;
                case "consume":
                    mConsume = "1".equals(pair[VALUE]);
                    break;
                case "elapsed":
                    mElapsedTimeHighResolution = Float.parseFloat(pair[VALUE]);
                    break;
                case "error":
                    mError = pair[VALUE];
                    break;
                case "mixrampdb":
                    try {
                        mMixRampDB = Float.parseFloat(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        if ("nan".equals(pair[VALUE])) {
                            mMixRampDisabled = true;
                        } else {
                            Log.error(TAG, "Unexpected value from mixrampdb.", e);
                        }
                    }
                    break;
                case "mixrampdelay":
                    try {
                        mMixRampDelay = Float.parseFloat(pair[VALUE]);
                    } catch (final NumberFormatException e) {
                        if ("nan".equals(pair[VALUE])) {
                            mMixRampDisabled = true;
                        } else {
                            Log.error(TAG, "Unexpected value from mixrampdelay", e);
                        }
                    }
                    break;
                case "nextsong":
                    mNextSong = Integer.parseInt(pair[VALUE]);
                    break;
                case "nextsongid":
                    mNextSongId = Integer.parseInt(pair[VALUE]);
                    break;
                case "playlist":
                    mPlaylistVersion = Integer.parseInt(pair[VALUE]);
                    break;
                case "playlistlength":
                    mPlaylistLength = Integer.parseInt(pair[VALUE]);
                    break;
                case "random":
                    mRandom = "1".equals(pair[VALUE]);
                    break;
                case "repeat":
                    mRepeat = "1".equals(pair[VALUE]);
                    break;
                case "single":
                    mSingle = "1".equals(pair[VALUE]);
                    break;
                case "song":
                    mSong = Integer.parseInt(pair[VALUE]);
                    break;
                case "songid":
                    mSongId = Integer.parseInt(pair[VALUE]);
                    break;
                case "state":
                    switch (pair[VALUE]) {
                        case MPD_STATE_PLAYING:
                            mState = STATE_PLAYING;
                            break;
                        case MPD_STATE_PAUSED:
                            mState = STATE_PAUSED;
                            break;
                        case MPD_STATE_STOPPED:
                            mState = STATE_STOPPED;
                            break;
                        case MPD_STATE_UNKNOWN:
                        default:
                            mState = STATE_UNKNOWN;
                            break;
                    }
                    break;
                case "time":
                    final int timeIndex = pair[VALUE].indexOf(':');

                    mElapsedTime = Long.parseLong(pair[VALUE].substring(0, timeIndex));
                    mTotalTime = Long.parseLong(pair[VALUE].substring(timeIndex + 1));
                    mUpdateTime = new Date().getTime();
                    break;
                case "volume":
                    mVolume = Integer.parseInt(pair[VALUE]);
                    break;
                case "xfade":
                    mCrossFade = Integer.parseInt(pair[VALUE]);
                    break;
                case "updating_db":
                    mUpdating = true;
                    break;
                default:
                    Log.debug(
                            "Status was sent an unknown response: key: " + pair[KEY] + " value: "
                                    + pair[VALUE]);
            }
        }
    }
}
