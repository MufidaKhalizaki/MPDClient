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

import org.oucho.mpdclient.mpd.exception.MPDException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

class CommandResult {

    private Boolean isIOExceptionLast = null;

    private String mConnectionResult;

    private IOException mIOException;

    private MPDException mMPDException;

    private List<String> mResult;


    String getConnectionResult() {
        return mConnectionResult;
    }

    final IOException getIOException() {
        return mIOException;
    }

    final MPDException getMPDException() {
        return mMPDException;
    }


    final List<String> getResult() {
        //noinspection ReturnOfCollectionOrArrayField
        return mResult;
    }

    boolean isHeaderValid() {
        final boolean isHeaderValid;

        isHeaderValid = mConnectionResult != null;

        return isHeaderValid;
    }

    Boolean isIOExceptionLast() {
        return isIOExceptionLast;
    }

    final void setConnectionResult(final String result) {
        mConnectionResult = result;
    }

    final void setException(final IOException exception) {
        isIOExceptionLast = Boolean.TRUE;
        mIOException = exception;
    }

    final void setException(final MPDException exception) {
        isIOExceptionLast = Boolean.FALSE;
        mMPDException = exception;
    }

    final void setResult(final List<String> result) {
        if (result == null) {
            mResult = null;
        } else {
            mResult = Collections.unmodifiableList(result);
        }
    }
}
