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

package org.oucho.mpdclient.helpers;

import org.oucho.mpdclient.ConnectionInfo;
import org.oucho.mpdclient.tools.SettingsHelper;
import org.oucho.mpdclient.tools.WeakLinkedList;

import org.oucho.mpdclient.mpd.MPD;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.event.StatusChangeListener;
import org.oucho.mpdclient.mpd.event.TrackPositionListener;

import android.os.Handler;
import android.os.Message;

import java.util.Collection;


public class MPDAsyncHelper implements Handler.Callback {

    private static final int LOCAL_UID = 600;

    static final int EVENT_CONNECT_FAILED = LOCAL_UID + 2;

    static final int EVENT_CONNECT_SUCCEEDED = LOCAL_UID + 3;

    static final int EVENT_CONNECTION_STATE = LOCAL_UID + 4;

    static final int EVENT_PLAYLIST = LOCAL_UID + 5;

    static final int EVENT_RANDOM = LOCAL_UID + 6;

    static final int EVENT_REPEAT = LOCAL_UID + 7;

    private static final int EVENT_SET_USE_CACHE = LOCAL_UID + 8;

    static final int EVENT_STATE = LOCAL_UID + 9;

    static final int EVENT_TRACK = LOCAL_UID + 10;

    static final int EVENT_TRACK_POSITION = LOCAL_UID + 11;

    static final int EVENT_UPDATE_STATE = LOCAL_UID + 12;

    static final int EVENT_VOLUME = LOCAL_UID + 13;

    static final int EVENT_STICKER_CHANGED = LOCAL_UID + 14;

    private static int sJobID = 0;

    public final MPD oMPD;

    private final Collection<AsyncExecListener> mAsyncExecListeners;

    private final Collection<ConnectionInfoListener> mConnectionInfoListeners;

    private final Collection<ConnectionListener> mConnectionListeners;

    private final Collection<StatusChangeListener> mStatusChangeListeners;

    private final Collection<TrackPositionListener> mTrackPositionListeners;

    private final Handler mWorkerHandler;

    private final MPDAsyncWorker oMPDAsyncWorker;

    private ConnectionInfo mConnectionInfo = new ConnectionInfo();


    public MPDAsyncHelper() {
        super();
        oMPD = new CachedMPD();

        oMPDAsyncWorker = new MPDAsyncWorker(new Handler(this), oMPD);
        mWorkerHandler = oMPDAsyncWorker.startThread();
        new SettingsHelper(this).updateConnectionSettings();

        mAsyncExecListeners = new WeakLinkedList<>("AsyncExecListener");
        mConnectionListeners = new WeakLinkedList<>("ConnectionListener");
        mConnectionInfoListeners = new WeakLinkedList<>("ConnectionInfoListener");
        mStatusChangeListeners = new WeakLinkedList<>("StatusChangeListener");
        mTrackPositionListeners = new WeakLinkedList<>("TrackPositionListener");
    }

    public void addAsyncExecListener(final AsyncExecListener listener) {
        if (!mAsyncExecListeners.contains(listener)) {
            mAsyncExecListeners.add(listener);
        }
    }

    public void addConnectionListener(final ConnectionListener listener) {
        if (!mConnectionListeners.contains(listener)) {
            mConnectionListeners.add(listener);
        }
    }

    public void addStatusChangeListener(final StatusChangeListener listener) {
        if (!mStatusChangeListeners.contains(listener)) {
            mStatusChangeListeners.add(listener);
        }
    }

    public void addTrackPositionListener(final TrackPositionListener listener) {
        if (!mTrackPositionListeners.contains(listener)) {
            mTrackPositionListeners.add(listener);
        }
    }

    public void connect() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_CONNECT);
    }

    public void disconnect() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_DISCONNECT);
    }

    public int execAsync(final Runnable run) {
        final int activeJobID = sJobID;
        incrementeJobID();
        mWorkerHandler.obtainMessage(MPDAsyncWorker.EVENT_EXEC_ASYNC, activeJobID, 0, run).sendToTarget();
        return activeJobID;
    }

    public ConnectionInfo getConnectionSettings() {
        return mConnectionInfo;
    }


    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        try {
            final Object[] args = (Object[]) msg.obj;
            switch (msg.what) {
                case EVENT_CONNECTION_STATE:

                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.connectionStateChanged((Boolean) args[0], (Boolean) args[1]);
                    }

                    if ((Boolean) args[0]) {
                        for (final ConnectionListener listener : mConnectionListeners) {
                            listener.connectionSucceeded();
                        }
                    }

                    if ((Boolean) args[1]) {
                        for (final ConnectionListener listener : mConnectionListeners) {
                            listener.connectionFailed("Connection Lost");
                        }
                    }

                    break;
                case MPDAsyncWorker.EVENT_CONNECTION_CONFIG:
                    mConnectionInfo = (ConnectionInfo) args[0];
                    for (final ConnectionInfoListener listener : mConnectionInfoListeners) {
                        listener.onConnectionConfigChange(mConnectionInfo);
                    }

                    break;
                case EVENT_PLAYLIST:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.playlistChanged((MPDStatus) args[0], (Integer) args[1]);
                    }

                    break;
                case EVENT_RANDOM:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.randomChanged((Boolean) args[0]);
                    }
                    break;
                case EVENT_REPEAT:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.repeatChanged((Boolean) args[0]);
                    }
                    break;
                case EVENT_SET_USE_CACHE:
                    ((CachedMPD) oMPD).setUseCache((Boolean) args[0]);
                    break;
                case EVENT_STATE:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.stateChanged((MPDStatus) args[0], (int) args[1]);
                    }
                    break;
                case EVENT_TRACK:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.trackChanged((MPDStatus) args[0], (int) args[1]);
                    }
                    break;
                case EVENT_UPDATE_STATE:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.libraryStateChanged((Boolean) args[0], (Boolean) args[1]);
                    }
                    break;
                case EVENT_VOLUME:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.volumeChanged((MPDStatus) args[0], (Integer) args[1]);
                    }
                    break;
                case EVENT_TRACK_POSITION:
                    for (final TrackPositionListener listener : mTrackPositionListeners) {
                        listener.trackPositionChanged((MPDStatus) args[0]);
                    }
                    break;
                case EVENT_STICKER_CHANGED:
                    for (final StatusChangeListener listener : mStatusChangeListeners) {
                        listener.stickerChanged((MPDStatus) args[0]);
                    }
                    break;
                case EVENT_CONNECT_FAILED:
                    for (final ConnectionListener listener : mConnectionListeners) {
                        listener.connectionFailed((String) args[0]);
                    }
                    break;
                case EVENT_CONNECT_SUCCEEDED:
                    for (final ConnectionListener listener : mConnectionListeners) {
                        listener.connectionSucceeded();
                    }
                    break;
                case MPDAsyncWorker.EVENT_EXEC_ASYNC_FINISHED:
                    for (final AsyncExecListener listener : mAsyncExecListeners) {
                        if (listener != null) {
                            listener.asyncExecSucceeded(msg.arg1);
                        }
                    }
                    break;
                default:
                    result = false;
                    break;
            }
        } catch (final ClassCastException ignore) {
        }

        return result;
    }

    public boolean isStatusMonitorAlive() {
        return oMPDAsyncWorker.isStatusMonitorAlive();
    }

    public void removeAsyncExecListener(final AsyncExecListener listener) {
        mAsyncExecListeners.remove(listener);
    }

    public void removeStatusChangeListener(final StatusChangeListener listener) {
        mStatusChangeListeners.remove(listener);
    }

    public void removeTrackPositionListener(final TrackPositionListener listener) {
        mTrackPositionListeners.remove(listener);
    }

    public void setConnectionSettings(final ConnectionInfo connectionInfo) {
        mConnectionInfo = connectionInfo;
        oMPDAsyncWorker.setConnectionSettings(connectionInfo);
    }

    public void startStatusMonitor(final String[] idleSubsystems) {
        Message.obtain(mWorkerHandler, MPDAsyncWorker.EVENT_START_STATUS_MONITOR, idleSubsystems)
                .sendToTarget();
    }

    public void stopStatusMonitor() {
        mWorkerHandler.sendEmptyMessage(MPDAsyncWorker.EVENT_STOP_STATUS_MONITOR);
    }

    public interface AsyncExecListener {

        void asyncExecSucceeded(int jobID);
    }

    interface ConnectionInfoListener {

        void onConnectionConfigChange(ConnectionInfo connectionInfo);
    }

    public interface ConnectionListener {

        void connectionFailed(String message);

        void connectionSucceeded();
    }

    private static void incrementeJobID() {
        sJobID++;
    }
}
