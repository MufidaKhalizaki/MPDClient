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
