package org.oucho.mpdclient.mpd;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.connection.MPDConnection;
import org.oucho.mpdclient.mpd.event.StatusChangeListener;
import org.oucho.mpdclient.mpd.event.TrackPositionListener;
import org.oucho.mpdclient.mpd.exception.MPDException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MPDStatusMonitor extends Thread implements MPDConfig {

    public static final String IDLE_DATABASE = "database";

    public static final String IDLE_MIXER = "mixer";

    public static final String IDLE_OPTIONS = "options";

    public static final String IDLE_OUTPUT = "output";

    public static final String IDLE_PLAYER = "player";

    public static final String IDLE_PLAYLIST = "playlist";

    public static final String IDLE_UPDATE = "update";

    private static final String TAG = "MPDStatusMonitor";

    private final long mDelay;

    private final MPD mMPD;

    private final Queue<StatusChangeListener> mStatusChangeListeners;

    private final String[] mSupportedSubsystems;

    private final Queue<TrackPositionListener> mTrackPositionListeners;

    private volatile boolean mGiveup;

    public MPDStatusMonitor(final MPD mpd, final long delay, final String[] supportedSubsystems) {
        super("MPDStatusMonitor");

        mMPD = mpd;
        mDelay = delay;
        mGiveup = false;
        mStatusChangeListeners = new LinkedList<>();
        mTrackPositionListeners = new LinkedList<>();
        mSupportedSubsystems = supportedSubsystems.clone();
    }


    public void addStatusChangeListener(final StatusChangeListener listener) {
        mStatusChangeListeners.add(listener);
    }

    public void addTrackPositionListener(final TrackPositionListener listener) {
        mTrackPositionListeners.add(listener);
    }

    /**
     * Gracefully terminate tread.
     */
    public void giveup() {
        mGiveup = true;
    }

    public boolean isGivingUp() {
        return mGiveup;
    }

    /**
     * Main thread method
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        // initialize value cache
        int oldSong = -1;
        int oldSongId = -1;
        int oldPlaylistVersion = -1;
        long oldElapsedTime = -1L;
        int oldState = MPDStatus.STATE_UNKNOWN;
        int oldVolume = -1;
        boolean oldUpdating = false;
        boolean oldRepeat = false;
        boolean oldRandom = false;
        boolean oldConnectionState = false;
        boolean connectionLost = false;

        /* Objects to keep cached in {@link MPD} */
        final MPDStatus status = mMPD.getStatus();
        final MPDPlaylist playlist = mMPD.getPlaylist();

        while (!mGiveup) {
            Boolean connectionState = mMPD.isConnected();
            boolean connectionStateChanged = false;

            if (connectionLost || oldConnectionState != connectionState) {
                for (final StatusChangeListener listener : mStatusChangeListeners) {
                    listener.connectionStateChanged(connectionState, connectionLost);
                }

                if (mMPD.isConnected()) {
                    try {
                        mMPD.updateStatistics();
                        mMPD.updateStatus();
                        playlist.refresh(status);
                    } catch (final IOException | MPDException e) {
                        Log.error(TAG, "Failed to force a status update.", e);
                    }
                }

                connectionLost = false;
                oldConnectionState = connectionState;
                connectionStateChanged = true;
            }

            if (connectionState.equals(Boolean.TRUE)) {
                // playlist
                try {
                    boolean dbChanged = false;
                    boolean statusChanged = false;
                    boolean stickerChanged = false;

                    if (connectionStateChanged) {
                        dbChanged = statusChanged = true;
                    } else {
                        final List<String> changes = waitForChanges();

                        mMPD.updateStatus();

                        for (final String change : changes) {
                            switch (change.substring("changed: ".length())) {
                                case "database":
                                    mMPD.updateStatistics();
                                    dbChanged = true;
                                    statusChanged = true;
                                    break;
                                case "playlist":
                                    statusChanged = true;
                                    break;
                                case "sticker":
                                    stickerChanged = true;
                                    break;
                                default:
                                    statusChanged = true;
                                    break;
                            }

                            if (dbChanged && statusChanged) {
                                break;
                            }
                        }
                    }

                    if (statusChanged) {
                        // playlist
                        if (connectionStateChanged
                                || (oldPlaylistVersion != status.getPlaylistVersion() && status
                                .getPlaylistVersion() != -1)) {
                            playlist.refresh(status);
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.playlistChanged(status, oldPlaylistVersion);
                            }
                            oldPlaylistVersion = status.getPlaylistVersion();
                        }

                        if (connectionStateChanged || oldSongId != status.getSongId()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.trackChanged(status, oldSong);
                            }
                            oldSong = status.getSongPos();
                            oldSongId = status.getSongId();
                        }

                        // time
                        if (connectionStateChanged || oldElapsedTime != status.getElapsedTime()) {
                            for (final TrackPositionListener listener : mTrackPositionListeners) {
                                listener.trackPositionChanged(status);
                            }
                            oldElapsedTime = status.getElapsedTime();
                        }

                        // state
                        if (connectionStateChanged || !status.isState(oldState)) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.stateChanged(status, oldState);
                            }
                            oldState = status.getState();
                        }

                        // volume
                        if (connectionStateChanged || oldVolume != status.getVolume()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.volumeChanged(status, oldVolume);
                            }
                            oldVolume = status.getVolume();
                        }

                        // repeat
                        if (connectionStateChanged || oldRepeat != status.isRepeat()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.repeatChanged(status.isRepeat());
                            }
                            oldRepeat = status.isRepeat();
                        }

                        // volume
                        if (connectionStateChanged || oldRandom != status.isRandom()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.randomChanged(status.isRandom());
                            }
                            oldRandom = status.isRandom();
                        }

                        // update database
                        if (connectionStateChanged || oldUpdating != status.isUpdating()) {
                            for (final StatusChangeListener listener : mStatusChangeListeners) {
                                listener.libraryStateChanged(status.isUpdating(), dbChanged);
                            }
                            oldUpdating = status.isUpdating();
                        }
                    }

                    if (stickerChanged) {
                        for (final StatusChangeListener listener : mStatusChangeListeners) {
                            listener.stickerChanged(status);
                        }
                    }
                } catch (final IOException e) {
                    // connection lost
                    connectionLost = true;
                    if (mMPD.isConnected()) {
                        Log.error(TAG, "Exception caught while looping.", e);
                    }
                } catch (final MPDException e) {
                    Log.error(TAG, "Exception caught while looping.", e);
                }
            }

            try {
                synchronized (this) {
                    if (!mMPD.isConnected()) {
                        wait(mDelay);
                    }
                }
            } catch (final InterruptedException e) {
                Log.error(TAG, "Interruption caught during disconnection and wait.", e);
            }
        }

    }


    private List<String> waitForChanges() throws IOException, MPDException {
        final MPDConnection mpdIdleConnection = mMPD.getIdleConnection();
        final MPDCommand idleCommand = new MPDCommand(MPDCommand.MPD_CMD_IDLE,
                mSupportedSubsystems);

        while (mpdIdleConnection != null && mpdIdleConnection.isConnected()) {
            final List<String> data = mpdIdleConnection.sendCommand(idleCommand);

            if (data == null || data.isEmpty()) {
                continue;
            }

            return data;
        }
        throw new IOException("IDLE connection lost");
    }
}
