package org.oucho.mpdclient.helpers;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.MPD;
import org.oucho.mpdclient.mpd.MPDPlaylist;
import org.oucho.mpdclient.mpd.exception.MPDException;

import android.util.Log;

import java.io.IOException;


public final class QueueControl implements MPDConfig {

    private static final int CLEAR = 0;

    private static final int MOVE = 1;

    private static final int MOVE_TO_LAST = 2;

    private static final int MOVE_TO_NEXT = 3;

    private static final int REMOVE_ALBUM_BY_ID = 4;

    public static final int REMOVE_BY_ID = 5;

    public static final int SKIP_TO_ID = 7;

    private static final int INVALID_INT = -1;

    private static final MPD MPD = MPDApplication.getInstance().oMPDAsyncHelper.oMPD;

    private static final MPDPlaylist PLAYLIST = MPD.getPlaylist();

    private static final String TAG = "QueueControl";

    private QueueControl() {
        super();
    }

    public static void run(final int command, final int i) {
        run(command, i, INVALID_INT);
    }

    private static void run(final int command, final int arg1, final int arg2) {
        new Thread(() -> {
            int workingCommand = command;
            int j = arg2;

            try {
                switch (command) {
                    case MOVE_TO_LAST:
                        j = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus().getPlaylistLength() - 1;
                        workingCommand = MOVE;
                        break;
                    case MOVE_TO_NEXT:
                        j = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus().getSongPos();

                        if (arg1 >= j) {
                            j += 1;
                        }

                        workingCommand = MOVE;
                        break;
                    default:
                        break;
                }

                switch (workingCommand) {
                    case CLEAR:
                        PLAYLIST.clear();
                        break;
                    case MOVE:
                        PLAYLIST.move(arg1, j);
                        break;
                    case REMOVE_ALBUM_BY_ID:
                        PLAYLIST.removeAlbumById(arg1);
                        break;
                    case REMOVE_BY_ID:
                        PLAYLIST.removeById(arg1);
                        break;
                    case SKIP_TO_ID:
                        MPD.skipToId(arg1);
                        break;
                    default:
                        break;
                }
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to run simple playlist command. Argument 1: " + arg1 + " Argument 2: " + arg2, e);
            }
        }).start();
    }

}
