package org.oucho.mpdclient.mpd.event;

import org.oucho.mpdclient.mpd.MPDStatus;


public interface TrackPositionListener {

    void trackPositionChanged(MPDStatus status);
}
