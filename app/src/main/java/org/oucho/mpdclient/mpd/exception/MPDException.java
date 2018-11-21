package org.oucho.mpdclient.mpd.exception;

import org.oucho.mpdclient.mpd.Log;

public class MPDException extends Exception {


    public static final int ACK_ERROR_PASSWORD = 3;

    public static final int ACK_ERROR_PERMISSION = 4;

    private static final int ACK_ERROR_UNKNOWN = 5;

    private static final String TAG = "MPDException";

    private static final long serialVersionUID = -5837769913914420046L;

    public final int mErrorCode;

    public MPDException(final String detailMessage) {
        super(detailMessage);

        mErrorCode = getAckErrorCode(detailMessage);
    }


    public static int getAckErrorCode(final String message) {
        final String parsed = parseString(message);
        int errorCode = ACK_ERROR_UNKNOWN;

        if (parsed != null) {
            try {
                errorCode = Integer.parseInt(parsed);
            } catch (final NumberFormatException e) {
                Log.error(TAG, "Failed to parse ACK error code.", e);
            }
        }

        return errorCode;
    }


    private static String parseString(final String message) {
        String result = null;

        if (message != null) {
            final int startIndex;
            final int endIndex;

            startIndex = message.indexOf('[') + 1;

            endIndex = message.indexOf('@', startIndex);

            if (startIndex != -1 && endIndex != -1) {
                result = message.substring(startIndex, endIndex);
            }
        }

        return result;
    }

}
