package org.oucho.mpdclient.mpd;

public final class Log {

    private Log() {
        super();
    }

    static void debug(final String message) {
        android.util.Log.d(MPDStatus.TAG, message);
    }

    public static void error(final String tag, final String message) {
        android.util.Log.e(tag, message);
    }

    public static void error(final String tag, final String message, final Throwable tr) {
        android.util.Log.e(tag, message, tr);
    }

    static void warning(final String tag, final String message) {
        android.util.Log.w(tag, message);
    }

}
