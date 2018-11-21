package org.oucho.mpdclient.mpd.connection;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.Log;
import org.oucho.mpdclient.mpd.MPDCommand;
import org.oucho.mpdclient.mpd.MPDStatusMonitor;
import org.oucho.mpdclient.mpd.Tools;
import org.oucho.mpdclient.mpd.exception.MPDException;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public abstract class MPDConnection implements MPDConfig {

    private static final String MPD_RESPONSE_OK = "OK";

    private static final int CONNECTION_TIMEOUT = 10000;

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final int MAX_REQUEST_RETRY = 3;

    private static final String MPD_RESPONSE_ERR = "ACK";

    private static final String POOL_THREAD_NAME_PREFIX = "pool";

    private final Collection<String> mAvailableCommands = new HashSet<>();

    private final ThreadPoolExecutor mExecutor;

    private final Object mLock = new Object();

    private final int mReadWriteTimeout;

    private final String mTag;

    private boolean mCancelled = false;

    private boolean mIsConnected = false;

    private String mPassword = null;

    private InetSocketAddress mSocketAddress;

    MPDConnection(final int readWriteTimeout, final int maxConnections) {
        super();

        mReadWriteTimeout = readWriteTimeout;
        mExecutor = new ThreadPoolExecutor(1, maxConnections, (long) mReadWriteTimeout, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        mExecutor.prestartCoreThread();
        if (maxConnections > 1) {
            mTag = "MPDConnectionMultiSocket";
            mExecutor.allowCoreThreadTimeOut(true);
        } else {
            mTag = "MPDConnectionMonoSocket";
        }
    }


    public final void connect(final InetAddress host, final int port, final String password)
            throws IOException, MPDException {
        innerDisconnect();

        mCancelled = false;
        mPassword = password;
        mSocketAddress = new InetSocketAddress(host, port);

        final MPDCommand mpdCommand = new MPDCommand("commands");
        final CommandResult commandResult = processCommand(mpdCommand);

        synchronized (mAvailableCommands) {
            final Collection<String> response = Tools.
                    parseResponse(commandResult.getResult(), "command");
            mAvailableCommands.clear();
            mAvailableCommands.addAll(response);
        }

        if (!commandResult.isHeaderValid()) {
            throw new IOException("Failed initial connection.");
        }

        mIsConnected = true;
    }

    public void disconnect() throws IOException {
        mCancelled = true;
        innerDisconnect();
    }

    public InetAddress getHostAddress() {
        if (mSocketAddress == null) {
            throw new IllegalStateException("Connection endpoint not yet established.");
        }
        return mSocketAddress.getAddress();
    }

    public int getHostPort() {
        if (mSocketAddress == null) {
            throw new IllegalStateException("Connection endpoint not yet established.");
        }
        return mSocketAddress.getPort();
    }

    protected abstract InputStreamReader getInputStream();

    protected abstract OutputStreamWriter getOutputStream();

    protected abstract Socket getSocket();

    private void innerDisconnect() throws IOException {
        mIsConnected = false;
        synchronized (mLock) {
            if (getSocket() != null) {
                getSocket().close();
                setSocket(null);
            }
        }
    }

    public boolean isCommandAvailable(final String command) {
        return mAvailableCommands.contains(command);
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    private CommandResult processCommand(final MPDCommand command)
            throws IOException, MPDException {
        final CommandResult result;

        if (Thread.currentThread().getName().startsWith(POOL_THREAD_NAME_PREFIX)) {
            result = new CommandProcessor(command).call();
        } else {
            try {
                result = mExecutor.submit(new CommandProcessor(command)).get();
            } catch (final ExecutionException | InterruptedException e) {
                throw new IOException(e);
            }
        }

        if (result.getResult() == null) {
            if (result.isIOExceptionLast() == null) {

                throw new IOException(
                        "No result, no exception. This is a bug. Please report." + '\n' +
                                "Cancelled: " + mCancelled + '\n' +
                                "Command: " + command + '\n' +
                                "Connected: " + mIsConnected + '\n' +
                                "Connection result: " + result.getConnectionResult() + '\n');
            } else if (result.isIOExceptionLast().equals(Boolean.TRUE)) {
                throw result.getIOException();
            } else if (result.isIOExceptionLast().equals(Boolean.FALSE)) {
                throw result.getMPDException();
            }
        }

        return result;
    }

    public List<String> sendCommand(final MPDCommand command) throws IOException, MPDException {
        return processCommand(command).getResult();
    }

    public List<String> sendCommand(final String command, final String... args)
            throws IOException, MPDException {
        return sendCommand(new MPDCommand(command, args));
    }

    protected abstract void setInputStream(InputStreamReader inputStream);

    protected abstract void setOutputStream(OutputStreamWriter outputStream);

    protected abstract void setSocket(Socket socket);

    /** This class communicates with the server by sending the command and processing the result. */
    private class CommandProcessor implements Callable<CommandResult> {

        private final MPDCommand mCommand;

        CommandProcessor(final MPDCommand mpdCommand) {
            super();

            mCommand = mpdCommand;
        }


        @Override
        public final CommandResult call() {
            int retryCount = 0;
            final CommandResult result = new CommandResult();
            boolean isCommandSent = false;
            final String baseCommand = mCommand.getCommand();

            while (result.getResult() == null && retryCount < MAX_REQUEST_RETRY && !mCancelled) {
                try {
                    if (getSocket() == null || !getSocket().isConnected() ||
                            getSocket().isClosed()) {
                        result.setConnectionResult(innerConnect());
                    }

                    write();
                    isCommandSent = true;
                    result.setResult(read());
                } catch (final EOFException ex0) {
                    handleFailure(result, ex0);

                    if (MPDCommand.MPD_CMD_IDLE.equals(baseCommand)) {
                        result.setResult(Collections.singletonList(
                                "changed: " + MPDStatusMonitor.IDLE_PLAYLIST));
                    }
                } catch (final IOException e) {
                    handleFailure(result, e);
                } catch (final MPDException ex1) {
                    // Avoid getting in an infinite loop if an error occurred in the password cmd
                    if (ex1.mErrorCode == MPDException.ACK_ERROR_PASSWORD ||
                            ex1.mErrorCode == MPDException.ACK_ERROR_PERMISSION) {
                        result.setException(ex1);
                    } else {
                        handleFailure(result, ex1);
                    }
                }

                if (!MPDCommand.isRetryable(baseCommand) && isCommandSent) {
                    break;
                }

                retryCount++;
            }

            if (!mCancelled) {
                if (result.getResult() == null) {
                    logError(result, baseCommand, retryCount);
                } else {
                    mIsConnected = true;
                }
            }
            return result;
        }

        private void handleFailure(final CommandResult result, final IOException e) {
            if (isFailureHandled(result)) {
                result.setException(e);
            }
        }

        private void handleFailure(final CommandResult result, final MPDException e) {
            if (isFailureHandled(result)) {
                result.setException(e);
            }
        }


        private String innerConnect() throws IOException, MPDException {
            final String line;

            // Always release existing socket if any before creating a new one
            if (getSocket() != null) {
                try {
                    innerDisconnect();
                } catch (final IOException ignored) {
                }
            }

            setSocket(new Socket());
            getSocket().setSoTimeout(mReadWriteTimeout);
            getSocket().connect(mSocketAddress, CONNECTION_TIMEOUT);
            setInputStream(new InputStreamReader(getSocket().getInputStream(), "UTF-8"));
            final BufferedReader in = new BufferedReader(getInputStream(), DEFAULT_BUFFER_SIZE);
            setOutputStream(new OutputStreamWriter(getSocket().getOutputStream(), "UTF-8"));
            line = in.readLine();

            if (line == null) {
                throw new IOException("No response from server.");
            }

            /* Protocol says OK will begin the session, otherwise assume IO error. */
            if (!line.startsWith(MPD_RESPONSE_OK)) {
                throw new IOException("Bogus response from server.");
            }

            if (mPassword != null) {
                sendCommand(MPDCommand.MPD_CMD_PASSWORD, mPassword);
            }

            return line;
        }


        private boolean isFailureHandled(final CommandResult result) {
            boolean failureHandled = false;
            mIsConnected = false;

            try {
                Thread.sleep(500L);
            } catch (final InterruptedException ignored) {
            }

            try {
                innerConnect();
                failureHandled = true;
            } catch (final MPDException me) {
                result.setException(me);
            } catch (final IOException ie) {
                result.setException(ie);
            }

            return failureHandled;
        }


        private boolean isNonfatalACK(final String message) {
            final boolean isNonfatalACK;
            final int errorCode = MPDException.getAckErrorCode(message);

            isNonfatalACK = mCommand.isErrorNonfatal(errorCode);

            return isNonfatalACK;
        }

        private void logError(final CommandResult result, final String baseCommand,
                final int retryCount) {
            final StringBuilder stringBuilder = new StringBuilder(50);

            stringBuilder.append("Command ");
            stringBuilder.append(baseCommand);
            stringBuilder.append(" failed after ");
            stringBuilder.append(retryCount + 1);

            if (retryCount == 0) {
                stringBuilder.append(" attempt.");
            } else {
                stringBuilder.append(" attempts.");
            }

            if (result.isIOExceptionLast() == null) {
                Log.error(mTag, stringBuilder.toString());
            } else if (result.isIOExceptionLast().equals(Boolean.TRUE)) {
                Log.error(mTag, stringBuilder.toString(), result.getIOException());
            } else if (result.isIOExceptionLast().equals(Boolean.FALSE)) {
                Log.error(mTag, stringBuilder.toString(), result.getMPDException());
            }
        }


        private List<String> read() throws MPDException, IOException {
            final List<String> result = new ArrayList<>();
            final BufferedReader in = new BufferedReader(getInputStream(), DEFAULT_BUFFER_SIZE);

            boolean serverDataRead = false;
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                serverDataRead = true;

                if (line.startsWith(MPD_RESPONSE_OK)) {
                    break;
                }

                if (line.startsWith(MPD_RESPONSE_ERR)) {
                    if (isNonfatalACK(line)) {
                        break;
                    }

                    throw new MPDException(line);
                }
                result.add(line);
            }

            if (!serverDataRead) {
                // Close socket if there is no response...
                // Something is wrong (e.g. MPD shutdown..)
                throw new EOFException("Connection lost");
            }
            return result;
        }


        private void write() throws IOException {
            final String cmdString = mCommand.toString();

            getOutputStream().write(cmdString);
            getOutputStream().flush();
        }
    }
}
