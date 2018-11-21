package org.oucho.mpdclient.helpers;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;

import org.oucho.mpdclient.mpd.MPD;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.exception.MPDException;

import android.support.annotation.IdRes;
import android.util.Log;

import java.io.IOException;


public final class MPDControl implements MPDConfig {

    private static final long INVALID_LONG = -5L;

    private static final String TAG = "MPDControl";

    private static final String FULLY_QUALIFIED_NAME = "org.oucho.mpdclient.helpers" + '.' + TAG + '.';


    private static final String ACTION_CONSUME = FULLY_QUALIFIED_NAME + "CONSUME";

    private static final String ACTION_MUTE = FULLY_QUALIFIED_NAME + "MUTE";

    private static final String ACTION_NEXT = FULLY_QUALIFIED_NAME + "NEXT";

    public static final String ACTION_PAUSE = FULLY_QUALIFIED_NAME + "PAUSE";

    public static final String ACTION_PLAY = FULLY_QUALIFIED_NAME + "PLAY";

    private static final String ACTION_PREVIOUS = FULLY_QUALIFIED_NAME + "PREVIOUS";

    public static final String ACTION_SEEK = FULLY_QUALIFIED_NAME + "SEEK";

    public static final String ACTION_VOLUME_SET = FULLY_QUALIFIED_NAME + "SET_VOLUME";

    private static final String ACTION_SINGLE = FULLY_QUALIFIED_NAME + "SINGLE";

    private static final String ACTION_STOP = FULLY_QUALIFIED_NAME + "STOP";

    private static final String ACTION_TOGGLE_PLAYBACK = FULLY_QUALIFIED_NAME + "PLAY_PAUSE";

    private static final String ACTION_TOGGLE_RANDOM = FULLY_QUALIFIED_NAME + "RANDOM";

    private static final String ACTION_TOGGLE_REPEAT = FULLY_QUALIFIED_NAME + "REPEAT";

    public static final String ACTION_VOLUME_STEP_DOWN = FULLY_QUALIFIED_NAME + "VOLUME_STEP_DOWN";

    public static final String ACTION_VOLUME_STEP_UP = FULLY_QUALIFIED_NAME + "VOLUME_STEP_UP";

    private static final int VOLUME_STEP = 5;

    private MPDControl() {
        super();
    }

    public static void run(final String userCommand) {

        run(MPDApplication.getInstance().oMPDAsyncHelper.oMPD, userCommand, INVALID_LONG);
    }

    public static void run(final String userCommand, final int i) {
        run(MPDApplication.getInstance().oMPDAsyncHelper.oMPD, userCommand, (long) i);
    }


    public static void run(@IdRes final int resId) {
        switch (resId) {
            case R.id.next:
            case R.id.next0:
                run(ACTION_NEXT);
                break;
            case R.id.prev:
            case R.id.prev0:
                run(ACTION_PREVIOUS);
                break;
            case R.id.playpause:
            case R.id.playpause0:
                run(ACTION_TOGGLE_PLAYBACK);
                break;
            case R.id.barB_repeat:
                run(ACTION_TOGGLE_REPEAT);
                break;
            case R.id.barB_shuffle:
                run(ACTION_TOGGLE_RANDOM);
                break;

            // TODO pour la radio
            case R.id.barB_stop:
                run(ACTION_STOP);
                break;
            default:
                break;
        }
    }


    private static void run(final MPD mpd, final String userCommand, final long l) {
        new Thread(new Runnable() {

            private void blockForConnection() {
                int loopIterator = 50; /* Give the connection 5 seconds, tops. */
                final long blockTimeout = 100L;

                while (!mpd.isConnected() || !mpd.getStatus().isValid()) {
                    synchronized (this) {
                        /* Send a notice once a second or so. */
                        if (loopIterator % 10 == 0) {
                            Log.w(TAG, "Blocking for connection...");
                        }

                        try {
                            wait(blockTimeout);
                        } catch (final InterruptedException ignored) {
                        }

                        if (loopIterator == 0) {
                            break;
                        }
                        loopIterator--;
                    }
                }
            }

            @Override
            public void run() {
                MPDApplication.getInstance().addConnectionLock(this);
                blockForConnection();

                /*
                  The main switch for running the command.
                 */
                try {
                    switch (userCommand) {
                        case ACTION_CONSUME:
                            mpd.setConsume(!mpd.getStatus().isConsume());
                            break;
                        case ACTION_MUTE:
                            mpd.setVolume(0);
                            break;
                        case ACTION_NEXT:
                            mpd.next();
                            break;
                        case ACTION_PAUSE:
                            if (!mpd.getStatus().isState(MPDStatus.STATE_PAUSED)) {
                                mpd.pause();
                            }
                            break;
                        case ACTION_TOGGLE_PLAYBACK:
                            if (mpd.getStatus().isState(MPDStatus.STATE_PLAYING)) {
                                mpd.pause();
                            } else {
                                mpd.play();
                            }
                            break;
                        case ACTION_PLAY:
                            mpd.play();
                            break;
                        case ACTION_PREVIOUS:
                            mpd.previous();
                            break;

                        case ACTION_SEEK:
                            long li = l;
                            if (li == INVALID_LONG) {
                                li = 0L;
                            }
                            mpd.seek(li);
                            break;
                        case ACTION_STOP:
                            mpd.stop();
                            break;
                        case ACTION_SINGLE:
                            mpd.setSingle(!mpd.getStatus().isSingle());
                            break;
                        case ACTION_TOGGLE_RANDOM:
                            mpd.setRandom(!mpd.getStatus().isRandom());
                            break;
                        case ACTION_TOGGLE_REPEAT:
                            mpd.setRepeat(!mpd.getStatus().isRepeat());
                            break;
                        case ACTION_VOLUME_SET:
                            if (l != INVALID_LONG) {
                                mpd.setVolume((int) l);
                            }
                            break;
                        case ACTION_VOLUME_STEP_DOWN:
                            mpd.adjustVolume(-VOLUME_STEP);
                            break;
                        case ACTION_VOLUME_STEP_UP:
                            mpd.adjustVolume(VOLUME_STEP);
                            break;
                        default:
                            break;
                    }
                } catch (final IOException | MPDException e) {
                    Log.w(TAG, "Failed to send a simple MPD command.", e);
                } finally {
                    MPDApplication.getInstance().removeConnectionLock(this);
                }
            }
        }
        ).start();
    }
}
