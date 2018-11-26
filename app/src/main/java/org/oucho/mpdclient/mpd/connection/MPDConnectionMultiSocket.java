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


public class MPDConnectionMultiSocket extends MPDConnection {

    private static final ThreadLocal<InputStreamReader> INPUT_STREAM = new ThreadLocal<>();

    private static final ThreadLocal<OutputStreamWriter> OUTPUT_STREAM = new ThreadLocal<>();

    private static final ThreadLocal<Socket> SOCKET = new ThreadLocal<>();

    public MPDConnectionMultiSocket() {
        // Timeout, connexion max
        super(5000, 2);
    }

    @Override
    public InputStreamReader getInputStream() {
        return INPUT_STREAM.get();
    }

    @Override
    public OutputStreamWriter getOutputStream() {
        return OUTPUT_STREAM.get();
    }

    @Override
    protected Socket getSocket() {
        return SOCKET.get();
    }

    @Override
    public void setInputStream(final InputStreamReader inputStream) {
        INPUT_STREAM.set(inputStream);
    }

    @Override
    public void setOutputStream(final OutputStreamWriter outputStream) {
        OUTPUT_STREAM.set(outputStream);
    }

    @Override
    protected void setSocket(final Socket socket) {
        SOCKET.set(socket);
    }
}
