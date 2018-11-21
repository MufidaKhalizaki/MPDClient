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
