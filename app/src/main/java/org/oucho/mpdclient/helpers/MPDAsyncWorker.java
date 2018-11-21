package org.oucho.mpdclient.helpers;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.format.DateUtils;
import android.util.Log;

import org.oucho.mpdclient.ConnectionInfo;
import org.oucho.mpdclient.mpd.MPD;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.MPDStatusMonitor;
import org.oucho.mpdclient.mpd.event.StatusChangeListener;
import org.oucho.mpdclient.mpd.event.TrackPositionListener;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.tools.Utils;

import java.io.IOException;


class MPDAsyncWorker implements Handler.Callback, StatusChangeListener, TrackPositionListener {

    private static final int LOCAL_UID = 500;

    static final int EVENT_CONNECT = LOCAL_UID + 1;

    static final int EVENT_CONNECTION_CONFIG = LOCAL_UID + 2;

    static final int EVENT_DISCONNECT = LOCAL_UID + 3;

    static final int EVENT_EXEC_ASYNC = LOCAL_UID + 4;

    static final int EVENT_EXEC_ASYNC_FINISHED = LOCAL_UID + 5;

    static final int EVENT_START_STATUS_MONITOR = LOCAL_UID + 6;

    static final int EVENT_STOP_STATUS_MONITOR = LOCAL_UID + 7;

    private static final String TAG = "MPDAsyncWorker";

    private final Handler mHelperHandler;

    private final MPD mMPD;

    private ConnectionInfo mConInfo = new ConnectionInfo();

    private String[] mIdleSubsystems;

    private MPDStatusMonitor mStatusMonitor;

    MPDAsyncWorker(final Handler helperHandler, final MPD mpd) {
        super();

        mHelperHandler = helperHandler;
        mMPD = mpd;
    }

    private void connect() {
        try {
            mMPD.connect(mConInfo.server, mConInfo.port, mConInfo.password);
            mHelperHandler.sendEmptyMessage(MPDAsyncHelper.EVENT_CONNECT_SUCCEEDED);
        } catch (final IOException | MPDException e) {
            Log.e(TAG, "Error while connecting to the server.", e);
            mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_CONNECT_FAILED,
                    Utils.toObjectArray(e.getMessage())).sendToTarget();
        }
    }

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_CONNECTION_STATE,
                Utils.toObjectArray(connected, connectionLost)).sendToTarget();
    }

    private void disconnect() {

        mMPD.disconnect();
        Log.d(TAG, "Disconnected.");
    }

    @Override
    public final boolean handleMessage(final Message msg) {
        boolean result = true;

        switch (msg.what) {
            case EVENT_CONNECT:
                connect();
                break;
            case EVENT_START_STATUS_MONITOR:
                mIdleSubsystems = (String[]) msg.obj;
                startStatusMonitor();
                break;
            case EVENT_STOP_STATUS_MONITOR:
                stopStatusMonitor();
                break;
            case EVENT_DISCONNECT:
                disconnect();
                break;
            case EVENT_EXEC_ASYNC:
                final Runnable run = (Runnable) msg.obj;
                run.run();
                mHelperHandler.obtainMessage(EVENT_EXEC_ASYNC_FINISHED, msg.arg1, 0).sendToTarget();
                break;
            default:
                result = false;
                break;
        }

        return result;
    }


    boolean isStatusMonitorAlive() {
        final boolean isMonitorAlive;

        isMonitorAlive = mStatusMonitor != null && mStatusMonitor.isAlive() && !mStatusMonitor.isGivingUp();

        return isMonitorAlive;
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_UPDATE_STATE,
                Utils.toObjectArray(updating, dbChanged)).sendToTarget();
    }


    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_PLAYLIST,
                Utils.toObjectArray(mpdStatus, oldPlaylistVersion)).sendToTarget();
    }

    @Override
    public void randomChanged(final boolean random) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_RANDOM, Utils.toObjectArray(random))
                .sendToTarget();
    }

    @Override
    public void repeatChanged(final boolean repeating) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_REPEAT, Utils.toObjectArray(repeating))
                .sendToTarget();
    }


    final void setConnectionSettings(final ConnectionInfo connectionInfo) {
        if (mConInfo == null) {
            if (connectionInfo != null) {
                mConInfo = connectionInfo;
            }
        } else if (connectionInfo.serverInfoChanged || connectionInfo.streamingServerInfoChanged) {
            mHelperHandler.obtainMessage(EVENT_CONNECTION_CONFIG, mConInfo).sendToTarget();
            mConInfo = connectionInfo;
        }
    }

    private void startStatusMonitor() {
        mStatusMonitor =
                new MPDStatusMonitor(mMPD, DateUtils.SECOND_IN_MILLIS / 2L, mIdleSubsystems);
        mStatusMonitor.addStatusChangeListener(this);
        mStatusMonitor.addTrackPositionListener(this);
        mStatusMonitor.start();
    }


    final Handler startThread() {
        final HandlerThread handlerThread = new HandlerThread("MPDAsyncWorker");

        handlerThread.start();

        return new Handler(handlerThread.getLooper(), this);
    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_STATE, Utils.toObjectArray(mpdStatus, oldState))
                .sendToTarget();
    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_STICKER_CHANGED, Utils.toObjectArray(mpdStatus))
                .sendToTarget();
    }

    private void stopStatusMonitor() {
        if (mStatusMonitor != null) {
            mStatusMonitor.giveup();
        }
    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_TRACK, Utils.toObjectArray(mpdStatus, oldTrack))
                .sendToTarget();
    }

    @Override
    public void trackPositionChanged(final MPDStatus status) {
        mHelperHandler
                .obtainMessage(MPDAsyncHelper.EVENT_TRACK_POSITION, Utils.toObjectArray(status))
                .sendToTarget();
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
        mHelperHandler.obtainMessage(MPDAsyncHelper.EVENT_VOLUME,
                Utils.toObjectArray(mpdStatus, oldVolume)).sendToTarget();
    }
}
