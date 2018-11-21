package org.oucho.mpdclient.mpd.event;

import org.oucho.mpdclient.mpd.MPDStatus;


public interface StatusChangeListener {

    void connectionStateChanged(boolean connected, boolean connectionLost);

    void libraryStateChanged(boolean updating, boolean dbChanged);

    void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion);

    void randomChanged(boolean random);

    void repeatChanged(boolean repeating);

    void stateChanged(MPDStatus mpdStatus, int oldState);

    void stickerChanged(MPDStatus mpdStatus);

    void trackChanged(MPDStatus mpdStatus, int oldTrack);

    void volumeChanged(MPDStatus mpdStatus, int oldVolume);
}
