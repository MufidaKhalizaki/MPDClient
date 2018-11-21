package org.oucho.mpdclient.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.helpers.MPDControl;
import org.oucho.mpdclient.mpd.MPDStatus;

public class PhoneStateReceiver extends BroadcastReceiver implements MPDConfig {

    private static final String PAUSED_MARKER = "wasPausedInCall";

    private static boolean isLocalNetworkConnected() {

        final ConnectivityManager cm = (ConnectivityManager) MPDApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isLocalNetwork = false;

        if (cm != null) {
            final NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null) {
                final int networkType = networkInfo.getType();

                if (networkInfo.isConnected() && networkType == ConnectivityManager.TYPE_WIFI ||
                        networkType == ConnectivityManager.TYPE_ETHERNET) {
                    isLocalNetwork = true;
                }
            }
        }

        return isLocalNetwork;
    }

    private static void setPausedMarker(final boolean value) {
        mSettings.edit().putBoolean(PAUSED_MARKER, value).apply();
    }

    private static boolean shouldPauseForCall() {
        boolean result = false;
        final boolean isPlaying = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus().isState(MPDStatus.STATE_PLAYING);

        if (isPlaying) {
            result = mSettings.getBoolean("pauseOnPhoneStateChange", false);
        }

        return result;
    }

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final String telephonyState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if (isLocalNetworkConnected() || telephonyState != null) {

            if ((telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING) ||
                    telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)) &&
                    shouldPauseForCall()) {

                MPDControl.run(MPDControl.ACTION_PAUSE);
                setPausedMarker(true);
            } else if (telephonyState.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)) {
                final boolean playOnCallStop = mSettings.getBoolean("playOnPhoneStateChange", false);
                if (playOnCallStop && mSettings.getBoolean(PAUSED_MARKER, false)) {

                    MPDControl.run(MPDControl.ACTION_PLAY);
                }
                setPausedMarker(false);
            }
        }
    }
}