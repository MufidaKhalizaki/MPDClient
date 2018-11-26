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

import org.oucho.mpdclient.mpd.MPDCommand;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectionInfo implements Parcelable {

    @SuppressWarnings("unused")
    public static final Creator<ConnectionInfo> CREATOR = new Creator<ConnectionInfo>() {

        @Override
        public ConnectionInfo createFromParcel(final Parcel source) {
            final String pServer = source.readString();
            final int pPort = source.readInt();
            final String pPassword = source.readString();
            final String pStreamServer = source.readString();
            final int pStreamPort = source.readInt();
            final String pStreamSuffix = source.readString();
            final boolean[] pBoolArray = source.createBooleanArray();

            return new ConnectionInfo(pServer, pPort, pPassword, pStreamServer, pStreamPort, pStreamSuffix, pBoolArray[0], pBoolArray[1]);
        }

        @Override
        public ConnectionInfo[] newArray(final int size) {
            return new ConnectionInfo[size];
        }
    };


    public final String password;

    public final int port;

    public final String server;

    public final boolean serverInfoChanged;

    private final int streamPort;

    private final String streamServer;

    private final String streamSuffix;

    public final boolean streamingServerInfoChanged;

    /** Only call this to initialize. */
    public ConnectionInfo() {
        super();

        server = null;
        port = MPDCommand.DEFAULT_MPD_PORT;
        password = null;

        streamServer = null;
        streamPort = MPDCommand.DEFAULT_MPD_PORT;
        streamSuffix = null;

        serverInfoChanged = false;
        streamingServerInfoChanged = false;
    }

    /** The private constructor, constructed by the Build inner class. */
    private ConnectionInfo(final String pServer, final int pPort, final String pPassword,
            final String pStreamServer, final int pStreamPort, final String pStreamSuffix,
            final boolean pServerInfoChanged, final boolean pStreamingInfoChanged) {
        super();

        server = pServer;
        port = pPort;
        password = pPassword;

        streamServer = pStreamServer;
        streamPort = pStreamPort;
        streamSuffix = pStreamSuffix;

        serverInfoChanged = pServerInfoChanged;
        streamingServerInfoChanged = pStreamingInfoChanged;
    }


    @Override
    public final int describeContents() {
        return 0;
    }

    @Override
    public final String toString() {
        return "password: " + password +
                " port: " + port +
                " server: " + server +
                " serverInfoChanged: " + serverInfoChanged +
                " streamServerInfoChanged: " + streamingServerInfoChanged +
                " streamServer: " + streamServer +
                " streamPort: " + streamPort +
                " streamSuffix: " + streamSuffix;
    }


    @Override
    public final void writeToParcel(final Parcel dest, final int flags) {
        final boolean[] boolArray = {serverInfoChanged, streamingServerInfoChanged};

        dest.writeString(server);
        dest.writeInt(port);
        dest.writeString(password);
        dest.writeString(streamServer);
        dest.writeInt(streamPort);
        dest.writeString(streamSuffix);
        dest.writeBooleanArray(boolArray);
    }

    public static class Builder {

        private final String mPassword;

        private final int mPort;

        private boolean mPreviousRunFirst = false;

        private String mServer = null;

        private boolean mServerInfoChanged;

        private int mStreamPort;

        private String mStreamServer = null;

        private String mStreamSuffix;

        private boolean mStreamingServerInfoChanged;

        public Builder(final String server, final int port, final String password) {
            super();

            mServer = server;
            mPort = port;
            mPassword = password;
        }


        public final ConnectionInfo build() {
            if (!mPreviousRunFirst) {
                throw new IllegalStateException("setPreviousConnectionInfo() must be run prior to" +
                        " build()");
            }

            return new ConnectionInfo(mServer, mPort, mPassword,
                    mStreamServer, mStreamPort, mStreamSuffix, mServerInfoChanged, mStreamingServerInfoChanged);
        }


        private boolean hasServerChanged(final ConnectionInfo connectionInfo) {
            final boolean result;

            result = connectionInfo == null
                    || connectionInfo.server == null
                    || !connectionInfo.server.equals(mServer)
                    || connectionInfo.port != mPort
                    || connectionInfo.password == null
                    || !connectionInfo.password.equals(mPassword);

            return result;
        }


        private boolean hasStreamingServerChanged(final ConnectionInfo connectionInfo) {
            final boolean result;

            result = connectionInfo == null
                    || connectionInfo.streamServer == null
                    || !connectionInfo.streamServer.equals(mStreamServer)
                    || connectionInfo.streamPort != mStreamPort
                    || connectionInfo.streamSuffix == null
                    || !connectionInfo.streamSuffix.equals(mStreamSuffix);

            return result;
        }


        public final void setPreviousConnectionInfo(final ConnectionInfo connectionInfo) {
            if (mStreamServer == null) {
                throw new IllegalStateException("setPersistentNotification() && setStreamServer()" + "must be" + " run prior to setPersistentConnectionInfo()");
            }
            mPreviousRunFirst = true;

            if (connectionInfo == null) {
                mServerInfoChanged = true;
                mStreamingServerInfoChanged = true;
            } else {

                mServerInfoChanged = hasServerChanged(connectionInfo);
                mStreamingServerInfoChanged = hasStreamingServerChanged(connectionInfo);
            }
        }

        public final void setStreamingServer(final String server, final int port,
                final String suffix) {
            if (server == null || server.isEmpty()) {
                mStreamServer = mServer;
            } else {
                mStreamServer = server;
            }
            mStreamPort = port;
            mStreamSuffix = suffix;
        }
    }
}