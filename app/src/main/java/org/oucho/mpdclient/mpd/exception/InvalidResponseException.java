package org.oucho.mpdclient.mpd.exception;


public class InvalidResponseException extends RuntimeException {

    private static final long serialVersionUID = 2105442123614116620L;

    public InvalidResponseException(final String message) {
        super(message);
    }

}
