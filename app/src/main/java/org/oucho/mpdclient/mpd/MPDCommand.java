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

package org.oucho.mpdclient.mpd;

import org.oucho.mpdclient.MPDConfig;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MPDCommand implements MPDCommandList, MPDConfig {

    public static final int DEFAULT_MPD_PORT = 6600;

    public static final int MAX_VOLUME = 100;

    public static final int MIN_VOLUME = 0;

    private static final List<String> NON_RETRYABLE_COMMANDS = Arrays.asList(MPD_CMD_NEXT, MPD_CMD_PREV, MPD_CMD_PLAYLIST_ADD, MPD_CMD_PLAYLIST_MOVE, MPD_CMD_PLAYLIST_DEL);

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private static final Pattern QUOTATION_DELIMITER = Pattern.compile("\"");

    private final String[] mArgs;

    private final String mCommand;

    private final int[] mNonfatalErrors;


    public MPDCommand(final String command, final String... args) {
        super();
        mCommand = command;
        mArgs = args.clone();
        mNonfatalErrors = EMPTY_INT_ARRAY;
    }


    static String booleanValue(final boolean valueToTranslate) {
        final String result;

        if (valueToTranslate) {
            result = "1";
        } else {
            result = "0";
        }

        return result;
    }

    public static boolean isRetryable(final String command) {
        return !NON_RETRYABLE_COMMANDS.contains(command);
    }

    public String getCommand() {
        return mCommand;
    }


    public boolean isErrorNonfatal(final int errorCodeToCheck) {
        boolean result = false;

        for (final int errorCode : mNonfatalErrors) {
            if (errorCode == errorCodeToCheck) {
                result = true;
                break;
            }
        }

        return result;
    }

    @Override
    public String toString() {
        final String outString;

        if (mArgs.length == 0) {
            outString = mCommand + MPD_CMD_NEWLINE;
        } else {
            final int argsLength = Arrays.toString(mArgs).length();
            final int approximateLength = argsLength + mCommand.length() + 10;
            final StringBuilder outBuf = new StringBuilder(approximateLength);

            outBuf.append(mCommand);
            for (final String arg : mArgs) {
                if (arg != null) {
                    outBuf.append(" \"");
                    outBuf.append(QUOTATION_DELIMITER.matcher(arg).replaceAll("\\\\\""));
                    outBuf.append('"');
                }
            }
            outBuf.append(MPD_CMD_NEWLINE);
            outString = outBuf.toString();
        }

        return outString;
    }
}
