package org.oucho.mpdclient.mpd;

import android.support.annotation.NonNull;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.connection.MPDConnection;
import org.oucho.mpdclient.mpd.exception.MPDException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


class CommandQueue implements Iterable<MPDCommand>, MPDConfig {

    private static final String MPD_CMD_BULK_SEP = "list_OK";

    private static final String MPD_CMD_END_BULK = "command_list_end";

    private static final String MPD_CMD_START_BULK = "command_list_begin";

    private static final String MPD_CMD_START_BULK_OK = "command_list_ok_begin";

    private final List<MPDCommand> mCommandQueue;

    private int mCommandQueueStringLength;

    CommandQueue() {
        super();

        mCommandQueue = new ArrayList<>();
        mCommandQueueStringLength = getStartLength();
    }

    CommandQueue(final int size) {
        super();

        mCommandQueue = new ArrayList<>(size);
        mCommandQueueStringLength = getStartLength();
    }

    private static int getStartLength() {
        return MPD_CMD_START_BULK_OK.length() + MPD_CMD_END_BULK.length() + 5;
    }


    private static List<String[]> separatedQueueResults(final Iterable<String> lines) {
        final List<String[]> result = new ArrayList<>();
        final ArrayList<String> lineCache = new ArrayList<>();

        for (final String line : lines) {
            if (line.equals(MPD_CMD_BULK_SEP)) { // new part
                if (!lineCache.isEmpty()) {
                    result.add(lineCache.toArray(new String[lineCache.size()]));
                    lineCache.clear();
                }
            } else {
                lineCache.add(line);
            }
        }
        if (!lineCache.isEmpty()) {
            result.add(lineCache.toArray(new String[lineCache.size()]));
        }
        return result;
    }


    public void add(final CommandQueue commandQueue) {
        mCommandQueue.addAll(commandQueue.mCommandQueue);
        mCommandQueueStringLength += commandQueue.mCommandQueueStringLength;
    }


    public void add(final int position, final CommandQueue commandQueue) {
        mCommandQueue.addAll(position, commandQueue.mCommandQueue);
        mCommandQueueStringLength += commandQueue.mCommandQueueStringLength;
    }


    public void add(final int position, final MPDCommand command) {
        mCommandQueue.add(position, command);
        mCommandQueueStringLength += command.toString().length();
    }


    public void add(final MPDCommand command) {
        mCommandQueue.add(command);
        mCommandQueueStringLength += command.toString().length();
    }


    public void add(final String command, final String... args) {
        add(new MPDCommand(command, args));
    }

    public boolean isEmpty() {
        return mCommandQueue.isEmpty();
    }


    @NonNull
    @Override
    public Iterator<MPDCommand> iterator() {
        return mCommandQueue.iterator();
    }


    void send(final MPDConnection mpdConnection) throws IOException, MPDException {
        send(mpdConnection, false);
    }


    private List<String> send(final MPDConnection mpdConnection, final boolean separated)
            throws IOException, MPDException {
        final MPDCommand mpdCommand;

        if (mCommandQueue.isEmpty()) {
            throw new IllegalStateException("Cannot send an empty command queue.");
        }

        if (mCommandQueue.size() == 1) {
            /* OK, it's not really a command queue. Send it anyhow. */
            mpdCommand = mCommandQueue.get(0);
        } else {
            mpdCommand = new MPDCommand(toString(separated));
        }

        return mpdConnection.sendCommand(mpdCommand);
    }


    List<String[]> sendSeparated(final MPDConnection mpdConnection)
            throws IOException, MPDException {
        return separatedQueueResults(send(mpdConnection, true));
    }


    @Override
    public String toString() {
        return toString(false);
    }


    private String toString(final boolean separated) {
        final StringBuilder commandString = new StringBuilder(mCommandQueueStringLength);

        if (separated) {
            commandString.append(MPD_CMD_START_BULK_OK);
        } else {
            commandString.append(MPD_CMD_START_BULK);
        }
        commandString.append(MPDCommand.MPD_CMD_NEWLINE);

        for (final MPDCommand command : mCommandQueue) {
            commandString.append(command);
        }
        commandString.append(MPD_CMD_END_BULK);

        return commandString.toString();
    }
}
