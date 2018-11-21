package org.oucho.mpdclient.tools;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.oucho.mpdclient.ConnectionInfo;
import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.helpers.MPDAsyncHelper;
import org.oucho.mpdclient.mpd.MPDCommand;

public class SettingsHelper implements MPDConfig {

    private static final int DEFAULT_STREAMING_PORT = 8000;

    private final MPDAsyncHelper mMPDAsyncHelper;


    private final WifiManager mWifiManager;

    public SettingsHelper(final MPDAsyncHelper mpdAsyncHelper) {
        super();

        mWifiManager = (WifiManager) MPDApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mMPDAsyncHelper = mpdAsyncHelper;
    }

    private static String getStringWithSSID(final String param, final String wifiSSID) {
        if (wifiSSID == null) {
            return param;
        } else {
            return wifiSSID + param;
        }
    }

    private String getCurrentSSID() {
        final WifiInfo info = mWifiManager.getConnectionInfo();
        final String ssid = info.getSSID();
        return ssid == null ? null : ssid.replace("\"", "");
    }

    private int getIntegerSetting(final String name, final int defaultValue) {
        try {
            return Integer
                    .parseInt(mSettings.getString(name, Integer.toString(defaultValue)).trim());
        } catch (final NumberFormatException ignored) {
            return MPDCommand.DEFAULT_MPD_PORT;
        }
    }

    private String getStringSetting(final String name) {
        final String value = mSettings.getString(name, "").trim();
        final String result;

        if (value.isEmpty()) {
            result = null;
        } else {
            result = value;
        }

        return result;
    }

    public final boolean updateConnectionSettings() {
        final String wifiSSID = getCurrentSSID();
        boolean result = true;

        if (getStringSetting(getStringWithSSID("hostname", wifiSSID)) != null) {
            // an empty SSID should be null
            if (wifiSSID != null && wifiSSID.isEmpty()) {
                updateConnectionSettings(null);
            } else {
                updateConnectionSettings(wifiSSID);
            }
        } else if (getStringSetting("hostname") != null) {
            updateConnectionSettings(null);
        } else {
            result = false;
        }

        return result;
    }

    private void updateConnectionSettings(final String wifiSSID) {
        final String server = getStringSetting(getStringWithSSID("hostname", wifiSSID));
        final int port = getIntegerSetting(getStringWithSSID("port", wifiSSID), MPDCommand.DEFAULT_MPD_PORT);
        final String password = getStringSetting(getStringWithSSID("password", wifiSSID));
        final ConnectionInfo.Builder connectionInfo = new ConnectionInfo.Builder(server, port, password);

        final String streamServer = getStringSetting(getStringWithSSID("hostnameStreaming", wifiSSID));
        final int streamPort = getIntegerSetting(getStringWithSSID("portStreaming", wifiSSID), DEFAULT_STREAMING_PORT);
        String streamSuffix = getStringSetting(getStringWithSSID("suffixStreaming", wifiSSID));
        if (streamSuffix == null) {
            streamSuffix = "";
        }
        connectionInfo.setStreamingServer(streamServer, streamPort, streamSuffix);

        connectionInfo.setPreviousConnectionInfo(mMPDAsyncHelper.getConnectionSettings());

        mMPDAsyncHelper.setConnectionSettings(connectionInfo.build());
    }
}
