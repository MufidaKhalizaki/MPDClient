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

package org.oucho.mpdclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Looper;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

import com.squareup.leakcanary.LeakCanary;

import org.oucho.mpdclient.bugDroid.IMMLeaks;
import org.oucho.mpdclient.helpers.MPDAsyncHelper;
import org.oucho.mpdclient.helpers.MPDAsyncHelper.ConnectionListener;
import org.oucho.mpdclient.helpers.UpdateTrackInfo;
import org.oucho.mpdclient.mpd.MPDStatusMonitor;
import org.oucho.mpdclient.settings.ConnectionSettings;
import org.oucho.mpdclient.settings.SettingsActivity;
import org.oucho.mpdclient.tools.SettingsHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class MPDApplication extends Application implements MPDConfig {

    private static final long DISCONNECT_TIMER = 15000L;

    private static final int SETTINGS = 5;

    private static final String TAG = "MPDApplication";

    private static MPDApplication sInstance;

    private final Collection<Object> mConnectionLocks = Collections.synchronizedCollection(new LinkedList<>());

    public MPDAsyncHelper oMPDAsyncHelper = null;

    public UpdateTrackInfo updateTrackInfo = null;

    private AlertDialog mAlertDialog = null;

    private Activity mCurrentActivity = null;

    private Timer mDisconnectScheduler = null;

    private SettingsHelper mSettingsHelper = null;

    private boolean mSettingsShown = false;

    private static boolean addPlaylistTypeAlbum;

    private static boolean fragmentPlayer = false;
    private static boolean fragmentQueue = false;
    private static boolean fragmentAlbumSong = false;
    private static boolean fragmentPlaylist = false;


    @Override
    public final void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        setInstance(this);

        IMMLeaks.fixFocusedViewLeak(this);

        final StrictMode.VmPolicy vmPolicy = new StrictMode.VmPolicy.Builder().penaltyLog().build();
        final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        StrictMode.setVmPolicy(vmPolicy);

        // Init the default preferences (meaning we won't have different defaults between code/xml)
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        oMPDAsyncHelper = new MPDAsyncHelper();
        mSettingsHelper = new SettingsHelper(oMPDAsyncHelper);
        oMPDAsyncHelper.addConnectionListener(connectionListener);

        mDisconnectScheduler = new Timer();

    }



    public static synchronized MPDApplication getInstance() {
        return sInstance;
    }

    private static void setInstance(MPDApplication value) {
        sInstance = value;
    }


    public final void addConnectionLock(final Object lockOwner) {
        if (!mConnectionLocks.contains(lockOwner)) {
            mConnectionLocks.add(lockOwner);
            checkConnectionNeeded();
            cancelDisconnectScheduler();
        }
    }


    private AlertDialog.Builder buildConnectionFailedMessage(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);
        final OnClickListener oDialogClickListener = new DialogClickListener();

        builder.setTitle(R.string.connectionFailed);
        builder.setMessage(getResources().getString(R.string.connectionFailedMessage, message));
        builder.setCancelable(false);

        builder.setNegativeButton(R.string.quit, oDialogClickListener);
        builder.setNeutralButton(R.string.settings, oDialogClickListener);
        builder.setPositiveButton(R.string.retry, oDialogClickListener);

        return builder;
    }


    private AlertDialog.Builder buildConnectionFailedSettings(final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mCurrentActivity);

        builder.setCancelable(false);
        builder.setMessage(getResources().getString(R.string.connectionFailedMessageSetting, message));
        builder.setPositiveButton("OK", (dialog, which) -> {});
        return builder;
    }

    private void cancelDisconnectScheduler() {
        mDisconnectScheduler.cancel();
        mDisconnectScheduler.purge();
        mDisconnectScheduler = new Timer();
    }

    private void checkConnectionNeeded() {
        if (mConnectionLocks.isEmpty()) {
            disconnect();
        } else {
            if (!oMPDAsyncHelper.isStatusMonitorAlive()) {
                oMPDAsyncHelper.startStatusMonitor(new String[]{
                        MPDStatusMonitor.IDLE_DATABASE,
                        MPDStatusMonitor.IDLE_MIXER,
                        MPDStatusMonitor.IDLE_OPTIONS,
                        MPDStatusMonitor.IDLE_OUTPUT,
                        MPDStatusMonitor.IDLE_PLAYER,
                        MPDStatusMonitor.IDLE_PLAYLIST,
                        MPDStatusMonitor.IDLE_UPDATE
                });
            }
            if (!oMPDAsyncHelper.oMPD.isConnected() && (mCurrentActivity == null || !mCurrentActivity.getClass().equals(ConnectionSettings.class))) {
                connect();
            }
        }
    }

    public final void connect() {
        if (!mSettingsHelper.updateConnectionSettings()) {
            // Absolutely no settings defined! Open SettingsActivity!
            if (mCurrentActivity != null && !mSettingsShown) {
                mCurrentActivity.startActivityForResult(new Intent(mCurrentActivity, ConnectionSettings.class), SETTINGS);
                mSettingsShown = true;
            }
        }

        connectMPD();
    }

    private void connectMPD() {
        // dismiss possible dialog
        dismissAlertDialog();

        /* Returns null if the calling thread is not associated with a Looper.*/
        final Looper localLooper = Looper.myLooper();
        final boolean isUIThread = localLooper != null && localLooper.equals(Looper.getMainLooper());

        // show connecting to server dialog, only on the main thread.
        if (mCurrentActivity != null && isUIThread) {
            mAlertDialog = new ProgressDialog(getApplicationContext());
            mAlertDialog.setTitle(R.string.connecting);
            mAlertDialog.setMessage(getResources().getString(R.string.connectingToServer));
            mAlertDialog.setCancelable(false);
            mAlertDialog.setOnKeyListener((dialog, keyCode, event) -> {
                // Handle all keys!
                return true;
            });
            try {
                mAlertDialog.show();
            } catch (final BadTokenException ignored) {
                // Can't display it. Don't care.
            }
        }

        cancelDisconnectScheduler();

        // really connect
        oMPDAsyncHelper.connect();
    }

    ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connectionFailed(String message) {
            if (mAlertDialog == null || mAlertDialog instanceof ProgressDialog ||
                    !mAlertDialog.isShowing()) {

                // dismiss possible dialog
                dismissAlertDialog();

                oMPDAsyncHelper.disconnect();

                if (mCurrentActivity != null && !mConnectionLocks.isEmpty()) {
                    try {
                        // are we in the settings activity?
                        if (mCurrentActivity.getClass().equals(SettingsActivity.class)) {
                            mAlertDialog = buildConnectionFailedSettings(message).show();
                        } else {
                            mAlertDialog = buildConnectionFailedMessage(message).show();
                        }
                    } catch (final BadTokenException ignored) {
                    }
                }
            }
        }

        @Override
        public void connectionSucceeded() {

            dismissAlertDialog();
        }
    };


    private void disconnect() {
        cancelDisconnectScheduler();
        startDisconnectScheduler();
    }

    private void dismissAlertDialog() {
        if (mAlertDialog != null) {
            if (mAlertDialog.isShowing()) {
                try {
                    mAlertDialog.dismiss();
                } catch (final IllegalArgumentException ignored) {
                    // We don't care, it has already been destroyed
                }
            }
        }
    }

    public void stopAlertDialog() {
        mAlertDialog = null;
    }


    public final void removeConnectionLock(final Object lockOwner) {
        mConnectionLocks.remove(lockOwner);
        checkConnectionNeeded();
    }

    public final void setActivity(final Object activity) {
        if (activity instanceof Activity) {
            mCurrentActivity = (Activity) activity;
        }

        addConnectionLock(activity);
    }



    private void startDisconnectScheduler() {
        try {
            mDisconnectScheduler.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.w(TAG, "Disconnecting (" + DISCONNECT_TIMER + " ms timeout)");
                    oMPDAsyncHelper.stopStatusMonitor();
                    oMPDAsyncHelper.disconnect();
                }
            }, DISCONNECT_TIMER);
        } catch (final IllegalStateException e) {
            Log.d(TAG, "Disconnection timer interrupted.", e);
        }
    }


    public final void unsetActivity(final Object activity) {
        removeConnectionLock(activity);

        if (mCurrentActivity != null && mCurrentActivity.equals(activity)) {
            mCurrentActivity = null;
        }
    }

    private class DialogClickListener implements OnClickListener {

        @Override
        public final void onClick(final DialogInterface dialog, final int which) {
            switch (which) {
                case DialogInterface.BUTTON_NEUTRAL:
                    final Intent intent = new Intent(mCurrentActivity, ConnectionSettings.class);
                    // Show SettingsActivity
                    mCurrentActivity.startActivityForResult(intent, SETTINGS);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    mCurrentActivity.finish();
                    break;
                case DialogInterface.BUTTON_POSITIVE:
                    connectMPD();
                    break;
                default:
                    break;
            }
        }
    }


    public void stopDisconnectScheduler() {
        mDisconnectScheduler.cancel();
        mDisconnectScheduler.purge();
    }



    public static void setPlaylistTypeAlbum(boolean value) {
        addPlaylistTypeAlbum = value;
    }

    public static boolean getPlaylistTypeAlbum() {
        return addPlaylistTypeAlbum;
    }

    public static void setFragmentPlayer(boolean value) {
        fragmentPlayer = value;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean getFragmentPlayer() {
        return fragmentPlayer;
    }

    public static void setFragmentQueue(boolean value) {
        fragmentQueue = value;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean getFragmentQueue() {
        return fragmentQueue;
    }

    public static void setFragmentPlaylist(boolean value) {
        fragmentPlaylist = value;
    }

    public static boolean getFragmentPlaylist() {
        return fragmentPlaylist;
    }

    public static void setFragmentAlbumSong(boolean value) {
        fragmentAlbumSong = value;
    }

    public static boolean getFragmentAlbumSong() {
        return fragmentAlbumSong;
    }

}
