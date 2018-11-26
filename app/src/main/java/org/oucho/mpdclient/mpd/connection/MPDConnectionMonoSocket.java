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

package org.oucho.mpdclient.mpd.connection;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


public class MPDConnectionMonoSocket extends MPDConnection {

    private InputStreamReader mInputStream;

    private OutputStreamWriter mOutputStream;

    private Socket mSocket;

    public MPDConnectionMonoSocket() {
        super(0, 1);
    }

    @Override
    public InputStreamReader getInputStream() {
        return mInputStream;
    }

    @Override
    public OutputStreamWriter getOutputStream() {
        return mOutputStream;
    }

    @Override
    protected Socket getSocket() {
        return mSocket;
    }

    @Override
    public void setInputStream(final InputStreamReader inputStream) {
        mInputStream = inputStream;
    }

    @Override
    public void setOutputStream(final OutputStreamWriter outputStream) {
        mOutputStream = outputStream;
    }

    @Override
    protected void setSocket(final Socket socket) {
        mSocket = socket;
    }
}
