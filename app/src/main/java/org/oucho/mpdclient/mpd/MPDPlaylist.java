package org.oucho.mpdclient.mpd;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.connection.MPDConnection;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Music;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


public class MPDPlaylist implements MPDConfig {

    private static final String MPD_CMD_PLAYLIST_ADD = "add";

    private static final String MPD_CMD_PLAYLIST_CHANGES = "plchanges";

    private static final String MPD_CMD_PLAYLIST_CLEAR = "clear";

    private static final String MPD_CMD_PLAYLIST_DELETE = "rm";

    private static final String MPD_CMD_PLAYLIST_LIST = "playlistid";

    private static final String MPD_CMD_PLAYLIST_LOAD = "load";

    private static final String MPD_CMD_PLAYLIST_MOVE = "move";

    private static final String MPD_CMD_PLAYLIST_MOVE_ID = "moveid";

    private static final String MPD_CMD_PLAYLIST_REMOVE = "delete";

    private static final String MPD_CMD_PLAYLIST_REMOVE_ID = "deleteid";

    private static final String TAG = "MPDPlaylist";

    private final MPDConnection mConnection;

    private final MusicList mList;

    private int mLastPlaylistVersion = -1;


    MPDPlaylist(final MPDConnection mpdConnection) {
        super();

        mList = new MusicList();
        mConnection = mpdConnection;
    }

    static CommandQueue addAllCommand(final Iterable<Music> collection) {
        final CommandQueue commandQueue = new CommandQueue();

        for (final Music music : collection) {
            commandQueue.add(MPD_CMD_PLAYLIST_ADD, music.getFullPath());
        }

        return commandQueue;
    }

    static MPDCommand addCommand(final String fullPath) {
        return new MPDCommand(MPD_CMD_PLAYLIST_ADD, fullPath);
    }

    static MPDCommand clearCommand() {
        return new MPDCommand(MPD_CMD_PLAYLIST_CLEAR);
    }

    static CommandQueue cropCommand(final MPD mpd) {
        final CommandQueue commandQueue = new CommandQueue();
        final int currentTrackID = mpd.getStatus().getSongId();
        /* Null range ends are broken in MPD-0.18 on 32-bit arch, see bug #4080. */
        final int playlistLength = mpd.getStatus().getPlaylistLength();

        if (currentTrackID < 0) {
            throw new IllegalStateException("Cannot crop when media server is inactive.");
        }

        if (playlistLength == 1) {
            throw new IllegalStateException("Cannot crop when media server playlist length is 1.");
        }

        commandQueue.add(MPD_CMD_PLAYLIST_MOVE_ID, Integer.toString(currentTrackID), "0");
        commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, "1:" + playlistLength);

        return commandQueue;
    }

    static MPDCommand loadCommand(final String file) {
        return new MPDCommand(MPD_CMD_PLAYLIST_LOAD, file);
    }

    static CommandQueue removeByIndexCommand(final int... songs) {
        Arrays.sort(songs);
        final CommandQueue commandQueue = new CommandQueue();

        for (int i = songs.length - 1; i >= 0; i--) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE, Integer.toString(songs[i]));
        }

        return commandQueue;
    }

    public void clear() throws IOException, MPDException {
        mConnection.sendCommand(clearCommand());
    }

    public Music getByIndex(final int index) {
        return mList.getByIndex(index);
    }

    private Collection<Music> getFullPlaylist() throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(MPD_CMD_PLAYLIST_LIST);
        return Music.getMusicFromList(response, false);
    }

    public List<Music> getMusicList() {
        return mList.getMusic();
    }

    public void move(final int songId, final int to) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_MOVE_ID, Integer.toString(songId),
                Integer.toString(to));
    }

    public void moveByPosition(final int from, final int to) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_MOVE, Integer.toString(from),
                Integer.toString(to));
    }

    void refresh(final MPDStatus mpdStatus) throws IOException, MPDException {
        /* Synchronize this block to make sure the playlist version stays coherent. */
        synchronized (mList) {
            final int newPlaylistVersion = mpdStatus.getPlaylistVersion();

            if (mLastPlaylistVersion == -1 || mList.size() == 0) {
                mList.replace(getFullPlaylist());
            } else if (mLastPlaylistVersion != newPlaylistVersion) {
                final List<String> response =
                        mConnection.sendCommand(MPD_CMD_PLAYLIST_CHANGES,
                                Integer.toString(mLastPlaylistVersion));
                final Collection<Music> changes = Music.getMusicFromList(response, false);

                try {
                    mList.manipulate(changes, mpdStatus.getPlaylistLength());
                } catch (final IllegalStateException e) {
                    Log.error(TAG, "Partial update failed, running full update.", e);
                    mList.replace(getFullPlaylist());
                }
            }

            mLastPlaylistVersion = newPlaylistVersion;
        }
    }

    public void removeAlbumById(final int songId) throws IOException, MPDException {
        // Better way to get artist of given songId?
        String artist = "";
        String album = "";
        boolean usingAlbumArtist = true;

        synchronized (mList) {
            for (final Music song : mList) {
                if (song.getSongId() == songId) {
                    artist = song.getAlbumArtist();
                    if (artist == null || artist.isEmpty()) {
                        usingAlbumArtist = false;
                        artist = song.getArtist();
                    }
                    album = song.getAlbum();
                    break;
                }
            }
        }

        if (artist != null && album != null) {
            final CommandQueue commandQueue = new CommandQueue();

            /* Don't allow the list to change before we've computed the CommandList. */
            synchronized (mList) {
                for (final Music track : mList) {
                    if (album.equals(track.getAlbum())) {
                        final boolean songIsAlbumArtist =
                                usingAlbumArtist && artist.equals(track.getAlbumArtist());
                        final boolean songIsArtist =
                                !usingAlbumArtist && artist.equals(track.getArtist());

                        if (songIsArtist || songIsAlbumArtist) {
                            final String songID = Integer.toString(track.getSongId());
                            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE_ID, songID);
                        }
                    }
                }
            }

            commandQueue.send(mConnection);
        }

    }


    public void removeById(final int... songIds) throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        for (final int id : songIds) {
            commandQueue.add(MPD_CMD_PLAYLIST_REMOVE_ID, Integer.toString(id));
        }
        commandQueue.send(mConnection);
    }

    public void removePlaylist(final String file) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_DELETE, file);
    }

    public int size() {
        return mList.size();
    }

    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        synchronized (mList) {
            for (final Music music : mList) {
                stringBuilder.append(music);
                stringBuilder.append(MPDCommand.MPD_CMD_NEWLINE);
            }
        }
        return stringBuilder.toString();
    }

}
