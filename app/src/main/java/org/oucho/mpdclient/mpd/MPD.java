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

import org.oucho.mpdclient.mpd.connection.MPDConnection;
import org.oucho.mpdclient.mpd.connection.MPDConnectionMonoSocket;
import org.oucho.mpdclient.mpd.connection.MPDConnectionMultiSocket;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;
import org.oucho.mpdclient.mpd.item.FilesystemTreeEntry;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.mpd.item.PlaylistFile;
import org.oucho.mpdclient.mpd.item.Stream;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.oucho.mpdclient.mpd.Tools.KEY;
import static org.oucho.mpdclient.mpd.Tools.VALUE;


public class MPD implements MPDCommandList {

    private static final String STREAMS_PLAYLIST = "[Radio Streams]";

    private static final String TAG = "MPD";

    private final MPDPlaylist mPlaylist;

    private final MPDConnection mConnection;

    private final MPDConnection mIdleConnection;

    private final MPDStatistics mStatistics;

    private final MPDStatus mStatus;

    protected MPD() {
        super();
        mConnection = new MPDConnectionMultiSocket();
        mIdleConnection = new MPDConnectionMonoSocket();
        mStatistics = new MPDStatistics();

        mPlaylist = new MPDPlaylist(mConnection);
        mStatus = new MPDStatus();
    }


    private static String[] getAlbumArtistPair(final Album album) {

        final Artist artist = album.getArtist();
        final String[] artistPair;

        if (artist == null) {
            artistPair = new String[]{null, null};
        } else {
            if (album.hasAlbumArtist()) {
                artistPair = new String[]{MPD_TAG_ALBUM_ARTIST, artist.getName()};
            } else {
                artistPair = new String[]{MPD_TAG_ARTIST, artist.getName()};
            }
        }

        return artistPair;
    }

    private static MPDCommand getAlbumDetailsCommand(final Album album) {
        final String[] artistPair = getAlbumArtistPair(album);

        return new MPDCommand(MPD_CMD_COUNT, MPD_TAG_ALBUM, album.getName(),
                artistPair[0], artistPair[1]);
    }

    private static MPDCommand getSongsCommand(final Album album) {
        final String[] artistPair = getAlbumArtistPair(album);

        return new MPDCommand(MPD_CMD_FIND, MPD_TAG_ALBUM, album.getName(),
                artistPair[0], artistPair[1]);
    }

    private static MPDCommand listAlbumsCommand(final String artist, final boolean useAlbumArtist) {
        String albumArtist = null;

        if (useAlbumArtist) {
            albumArtist = MPD_TAG_ALBUM_ARTIST;
        }

        return new MPDCommand(MPD_CMD_LIST_TAG, MPD_TAG_ALBUM,
                albumArtist, artist);
    }


    private static MPDCommand listAllAlbumsGroupedCommand(final boolean useAlbumArtist) {
        final String artistTag;

        if (useAlbumArtist) {
            artistTag = MPD_TAG_ALBUM_ARTIST;
        } else {
            artistTag = MPD_TAG_ARTIST;
        }

        return new MPDCommand(MPD_CMD_LIST_TAG, MPD_TAG_ALBUM,
                MPD_CMD_GROUP, artistTag);
    }

    private static MPDCommand nextCommand() {
        return new MPDCommand(MPD_CMD_NEXT);
    }

    private static MPDCommand skipToPositionCommand(final int position) {
        return new MPDCommand(MPD_CMD_PLAY, Integer.toString(position));
    }


    public void add(final Album album, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue;

        if (isCommandAvailable(MPD_CMD_FIND_ADD)) {
            final String[] artistPair = getAlbumArtistPair(album);

            commandQueue = new CommandQueue();

            commandQueue.add(MPD_CMD_FIND_ADD, MPD_TAG_ALBUM, album.getName(), artistPair[0], artistPair[1]);
        } else {
            final List<Music> songs = getSongs(album);
            commandQueue = MPDPlaylist.addAllCommand(songs);
        }

        add(commandQueue, replace, play);
    }

    public void add(final Artist artist, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue;

        if (isCommandAvailable(MPD_CMD_FIND_ADD)) {
            commandQueue = new CommandQueue();

            commandQueue
                    .add(MPD_CMD_FIND_ADD, MPD_TAG_ARTIST, artist.getName());
        } else {
            final List<Music> songs = getSongs(artist);
            commandQueue = MPDPlaylist.addAllCommand(songs);
        }

        add(commandQueue, replace, play);
    }

    public void add(final FilesystemTreeEntry music, final boolean replace, final boolean play)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        if (music instanceof PlaylistFile) {
            commandQueue.add(MPDPlaylist.loadCommand(music.getFullPath()));
        } else {
            commandQueue.add(MPDPlaylist.addCommand(music.getFullPath()));
        }

        add(commandQueue, replace, play);
    }

    private void add(final CommandQueue commandQueue, final boolean replace,
                     final boolean playAfterAdd) throws IOException, MPDException {
        int playPos = 0;
        final boolean isPlaying = mStatus.isState(MPDStatus.STATE_PLAYING);
        final boolean isConsume = mStatus.isConsume();
        final boolean isRandom = mStatus.isRandom();
        final int playlistLength = mStatus.getPlaylistLength();

        /* Replace */
        if (replace) {
            if (isPlaying) {
                if (playlistLength > 1) {
                    try {
                        commandQueue.add(0, MPDPlaylist.cropCommand(this));
                    } catch (final IllegalStateException ignored) {
                        /* Shouldn't occur, we already checked for playing. */
                    }
                }
            } else {
                commandQueue.add(0, MPDPlaylist.clearCommand());
            }
        } else if (playAfterAdd && !isRandom) {
            /* Since we didn't clear the playlist queue, we need to play the (current queue+1) */
            playPos = mPlaylist.size();
        }

        if (replace) {
            if (isPlaying) {
                commandQueue.add(nextCommand());
            } else if (playAfterAdd) {
                commandQueue.add(skipToPositionCommand(playPos));
            }
        } else if (playAfterAdd) {
            commandQueue.add(skipToPositionCommand(playPos));
        }

        /* Finally, clean up the last playing song. */
        if (replace && isPlaying && !isConsume) {
            commandQueue.add(MPDPlaylist.removeByIndexCommand(0));

        }

        /*
          It's rare, but possible to make it through the add()
          methods without adding to the command queue.
         */
        if (!commandQueue.isEmpty()) {
            commandQueue.send(mConnection);
        }
    }

    public void playPlaylist(final String playlist)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();

        commandQueue.add(MPDPlaylist.loadCommand(playlist));

        add(commandQueue, true, true);
    }

    protected void addAlbumPaths(List<Album> albums) throws IOException, MPDException {
        if (albums != null && !albums.isEmpty()) {
            for (final Album album : albums) {
                final List<Music> songs = getFirstTrack(album);
                if (!songs.isEmpty()) {
                    album.setPath(songs.get(0).getPath());
                }
            }
        }
    }

    /** TODO: This needs to be an add(Stream, ...) method. */
    public void addStream(final String stream)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue();
        commandQueue.add(MPDPlaylist.addCommand(stream));

        add(commandQueue, true, true);
    }

    public void addToPlaylist(final String playlistName, final Album album)
            throws IOException, MPDException {
        if (mIdleConnection.isCommandAvailable(MPD_CMD_SEARCH_ADD_PLAYLIST)) {
            final String[] artistPair = getAlbumArtistPair(album);

            mConnection.sendCommand(MPD_CMD_SEARCH_ADD_PLAYLIST, playlistName,
                    MPD_SEARCH_ALBUM, album.getName(), artistPair[0], artistPair[1]);
        } else {
            addToPlaylist(playlistName, new ArrayList<>(getSongs(album)));
        }
    }

    private void addToPlaylist(final String playlistName, final Collection<Music> musicCollection)
            throws IOException, MPDException {
        if (null != musicCollection && !musicCollection.isEmpty()) {
            final CommandQueue commandQueue = new CommandQueue();

            for (final Music music : musicCollection) {
                commandQueue.add(MPD_CMD_PLAYLIST_ADD, playlistName, music.getFullPath());
            }
            commandQueue.send(mConnection);
        }
    }

    public void addToPlaylist(final String playlistName, final Music music)
            throws IOException, MPDException {
        final Collection<Music> songs = new ArrayList<>(1);
        songs.add(music);
        addToPlaylist(playlistName, songs);
    }

    public void adjustVolume(final int modifier) throws IOException, MPDException {
        // calculate final volume (clip value with [0, 100])
        int vol = mStatus.getVolume() + modifier;
        vol = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, vol));

        mConnection.sendCommand(MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    private synchronized void connect(final InetAddress server, final int port, final String password) throws IOException, MPDException {
        if (!isConnected()) {
            mConnection.connect(server, port, password);
            mIdleConnection.connect(server, port, password);
        }
    }

    public final void connect(final String server, final int port, final String password)
            throws IOException, MPDException {
        final InetAddress address = InetAddress.getByName(server);
        connect(address, port, password);
    }

    public synchronized void disconnect() {
        if (mConnection.isConnected()) {
            try {
                mConnection.disconnect();
            } catch (final IOException e) {
                // exception
            }
        }
        if (mIdleConnection.isConnected()) {
            try {
                mIdleConnection.disconnect();
            } catch (final IOException e) {
                // exception
            }
        }
    }


    private List<Music> find(final String[] args) throws IOException, MPDException {
        return genericSearch(MPD_CMD_FIND, args, true);
    }

    private void fixAlbumArtists(final List<Album> albums) {
        if (albums != null && !albums.isEmpty()) {
            List<String[]> albumArtists = null;
            try {
                albumArtists = listAlbumArtists(albums);
            } catch (final IOException | MPDException e) {
                Log.error(TAG, "Failed to fix album artists.", e);
            }

            if (albumArtists != null && albumArtists.size() == albums.size()) {
                /* Split albums are rare, let it allocate as needed. */
                @SuppressWarnings("CollectionWithoutInitialCapacity")
                final Collection<Album> splitAlbums = new ArrayList<>();
                int i = 0;
                for (Album album : albums) {
                    final String[] aartists = albumArtists.get(i);
                    if (aartists.length > 0) {
                        Arrays.sort(aartists); // make sure "" is the first one
                        if (aartists[0] != null && !aartists[0]
                                .isEmpty()) { // one albumartist, fix this
                            // album
                            final Artist artist = new Artist(aartists[0]);
                            final Album newAlbum = new Album(album, artist);
                            albums.set(i, newAlbum);
                        } // do nothing if albumartist is ""
                        if (aartists.length > 1) { // it's more than one album, insert
                            for (int n = 1; n < aartists.length; n++) {
                                final Artist artist = new Artist(aartists[n]);
                                final Album newAlbum = new Album(album, artist);
                                splitAlbums.add(newAlbum);
                            }
                        }
                    }
                    i++;
                }
                albums.addAll(splitAlbums);
            }
        }
    }

    private List<Music> genericSearch(final String searchCommand, final String[] args,
                                      final boolean sort) throws IOException, MPDException {
        return Music.getMusicFromList(mConnection.sendCommand(searchCommand, args), sort);
    }

    private List<Music> genericSearch(final String strToFind) throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(MPDCommandList.MPD_CMD_SEARCH, "any", strToFind);
        return Music.getMusicFromList(response, true);
    }


    protected void getAlbumDetails(final List<Album> albums, final boolean findYear)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(albums.size());
        for (final Album album : albums) {
            commandQueue.add(getAlbumDetailsCommand(album));
        }
        final List<String[]> response = commandQueue.sendSeparated(mConnection);

        if (response.size() == albums.size()) {
            for (int i = 0; i < response.size(); i++) {
                final String[] list = response.get(i);
                final Album a = albums.get(i);
                for (final String[] pair : Tools.splitResponse(list)) {
                    if ("songs".equals(pair[KEY])) {
                        a.setSongCount(Long.parseLong(pair[VALUE]));
                    } else if ("playtime".equals(pair[KEY])) {
                        a.setDuration(Long.parseLong(pair[VALUE]));
                    }
                }

                if (findYear) {
                    final List<Music> songs = getFirstTrack(a);
                    if (null != songs && !songs.isEmpty()) {
                        a.setYear(songs.get(0).getDate());
                        a.setPath(songs.get(0).getPath());
                    }
                }
            }
        }
    }

    public List<Album> getAlbums(final Artist artist) throws IOException, MPDException {
        List<Album> albums = getAlbums(artist, false);

        if (artist != null && !artist.isUnknown()) {
            albums = Item.merged(getAlbums(artist, true), albums);
        } else {
            addAlbumYearPath(albums);
        }

        return albums;
    }

    private void addAlbumYearPath(List<Album> albums) throws IOException, MPDException {
        if (albums != null && !albums.isEmpty()) {
            for (final Album album : albums) {
                final List<Music> songs = getFirstTrack(album);
                if (!songs.isEmpty()) {
                    album.setYear(songs.get(0).getDate());
                    album.setPath(songs.get(0).getPath());
                }
            }
        }
    }

    private void addYear(List<Album> albums) throws IOException, MPDException {
        if (albums != null && !albums.isEmpty()) {
            for (final Album album : albums) {
                final List<Music> songs = getFirstTrack(album);
                if (!songs.isEmpty()) {
                    album.setYear(songs.get(0).getDate());
                }
            }
        }
    }

    private List<Album> getAlbums(final Artist artist,
                                  final boolean useAlbumArtist)
            throws IOException, MPDException {


        final List<Album> albums;


        if (artist == null) {

            albums = getAllAlbums(false);
        } else {

            final List<String> albumNames = listAlbums(artist.getName(), useAlbumArtist);
            albums = new ArrayList<>(albumNames.size());

            if (!albumNames.isEmpty()) {
                for (final String album : albumNames) {
                    albums.add(new Album(album, artist, useAlbumArtist));
                }

                if (!useAlbumArtist) {
                    fixAlbumArtists(albums);
                }

                addAlbumPaths(albums);

                addYear(albums);

                Collections.sort(albums);
            }
        }

        return albums;
    }

    protected List<Album> getAllAlbums(final boolean trackCountNeeded)
            throws IOException, MPDException {

        return listAllAlbumsGrouped();
    }


    private List<Music> getFirstTrack(final Album album) throws IOException, MPDException {
        final Artist artist = album.getArtist();
        final String[] args = new String[6];

        if (artist == null) {
            args[0] = "";
            args[1] = "";
        } else if (album.hasAlbumArtist()) {
            args[0] = MPD_TAG_ALBUM_ARTIST;
        } else {
            args[0] = MPD_TAG_ARTIST;
        }

        if (artist != null) {
            args[1] = artist.getName();
        }

        args[2] = MPD_TAG_ALBUM;
        args[3] = album.getName();
        args[4] = "track";
        args[5] = "1";
        List<Music> songs = find(args);
        if (null == songs || songs.isEmpty()) {
            args[5] = "01";
            songs = find(args);
        }
        if (null == songs || songs.isEmpty()) {
            args[5] = "1";
            songs = search(args);
        }
        if (null == songs || songs.isEmpty()) {
            final String[] args2 = Arrays.copyOf(args, 4); // find all tracks
            songs = find(args2);
        }
        return songs;
    }

    public InetAddress getHostAddress() {
        return mConnection.getHostAddress();
    }

    public int getHostPort() {
        return mConnection.getHostPort();
    }

    MPDConnection getIdleConnection() {
        return mIdleConnection;
    }

    public MPDPlaylist getPlaylist() {
        return mPlaylist;
    }

    public List<Music> getPlaylistSongs(final String playlistName)
            throws IOException, MPDException {
        final String[] args = new String[1];
        args[0] = playlistName;

        return genericSearch(MPD_CMD_PLAYLIST_INFO, args, false);
    }

    public List<Item> getPlaylists() throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(MPD_CMD_LISTPLAYLISTS);
        final List<Item> result = new ArrayList<>(response.size());
        for (final String[] pair : Tools.splitResponse(response)) {
            if ("playlist".equals(pair[KEY])) {
                if (null != pair[VALUE] && !STREAMS_PLAYLIST.equals(pair[VALUE])) {
                    result.add(new PlaylistFile(pair[VALUE]));
                }
            }
        }
        Collections.sort(result);

        return result;
    }

    public List<Music> getSavedStreams() throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(MPD_CMD_LISTPLAYLISTS);
        List<Music> savedStreams = null;

        for (final String[] pair : Tools.splitResponse(response)) {
            if ("playlist".equals(pair[KEY])) {
                if (STREAMS_PLAYLIST.equals(pair[VALUE])) {
                    final String[] args = {pair[VALUE]};

                    savedStreams = genericSearch(MPD_CMD_PLAYLIST_INFO, args, false);
                    break;
                }
            }
        }

        return savedStreams;
    }

    public List<Music> getSongs(final Album album) throws IOException, MPDException {
        final List<Music> songs = Music.getMusicFromList
                (mConnection.sendCommand(getSongsCommand(album)), true);
        if (album.hasAlbumArtist()) {
            // remove songs that don't have this album artist (mpd >=0.18 puts them in)
            final Artist artist = album.getArtist();
            String artistName = null;

            if (artist != null) {
                artistName = artist.getName();
            }

            for (int i = songs.size() - 1; i >= 0; i--) {
                final String albumArtist = songs.get(i).getAlbumArtist();
                if (albumArtist != null && !albumArtist.isEmpty()
                        && !albumArtist.equals(artistName)) {
                    songs.remove(i);
                }
            }
        }
        if (null != songs) {
            Collections.sort(songs);
        }
        return songs;
    }

    private List<Music> getSongs(final Artist artist) throws IOException, MPDException {
        final List<Album> albums = getAlbums(artist);
        final List<Music> songs = new ArrayList<>(albums.size());
        for (final Album album : albums) {
            songs.addAll(getSongs(album));
        }
        return songs;
    }

    public MPDStatistics getStatistics() {
        return mStatistics;
    }

    public MPDStatus getStatus() {
        return mStatus;
    }



    public boolean isCommandAvailable(final String command) {
        return mConnection.isCommandAvailable(command);
    }

    public boolean isConnected() {
        return mIdleConnection.isConnected();
    }

    protected List<String[]> listAlbumArtists(final List<Album> albums)
            throws IOException, MPDException {
        final CommandQueue commandQueue = new CommandQueue(albums.size());
        final List<String[]> response;
        List<String[]> albumArtists = null;

        for (final Album album : albums) {
            final Artist artist = album.getArtist();
            String artistCommand = null;
            String artistName = null;

            if (artist != null) {
                artistCommand = MPD_TAG_ARTIST;
                artistName = artist.getName();
            }

            commandQueue.add(MPD_CMD_LIST_TAG, MPD_TAG_ALBUM_ARTIST,
                    artistCommand, artistName,
                    MPD_TAG_ALBUM, album.getName());
        }

        response = commandQueue.sendSeparated(mConnection);
        if (response.size() == albums.size()) {
            for (int i = 0; i < response.size(); i++) {
                for (int j = 0; j < response.get(i).length; j++) {
                    response.get(i)[j] = response.get(i)[j].substring("AlbumArtist: ".length());
                }
            }
            albumArtists = response;
        } else {
            Log.warning(TAG, "Response and album size differ when listing album artists.");
        }

        return albumArtists;
    }

    protected List<String> listAlbums(final String artist, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(listAlbumsCommand(artist, useAlbumArtist));
        final List<String> result;

        if (response.isEmpty()) {
            result = Collections.emptyList();
        } else {
            result = Tools.parseResponse(response, "Album");
            Collections.sort(result);
        }

        return result;
    }

    private List<Album> listAllAlbumsGrouped() throws IOException, MPDException {
        final List<Album> artistAlbums = listAllAlbumsGrouped(false);
        final List<Album> albumArtistAlbums = listAllAlbumsGrouped(true);

        for (final Album artistAlbum : artistAlbums) {
            for (final Album albumArtistAlbum : albumArtistAlbums) {
                if (artistAlbum.getArtist() != null && artistAlbum.doesNameExist(albumArtistAlbum)) {
                    albumArtistAlbum.setHasAlbumArtist();
                    break;
                }
            }
        }

        return albumArtistAlbums;
    }

    private List<Album> listAllAlbumsGrouped(final boolean useAlbumArtist) throws IOException, MPDException {
        final String albumResponse = "Album";
        final String artistResponse;
        final List<String> response =
                mConnection.sendCommand(listAllAlbumsGroupedCommand(useAlbumArtist));
        final List<Album> result = new ArrayList<>(response.size() / 2);
        String currentAlbum = null;

        if (useAlbumArtist) {
            artistResponse = "AlbumArtist";
        } else {
            artistResponse = "Artist";
        }

        for (final String[] pair : Tools.splitResponse(response)) {

            if (artistResponse.equals(pair[KEY])) {
                if (currentAlbum != null) {
                    final Artist artist = new Artist(pair[VALUE]);
                    result.add(new Album(currentAlbum, artist, useAlbumArtist));

                    currentAlbum = null;
                }
            } else if (albumResponse.equals(pair[KEY])) {
                if (currentAlbum != null) {
                    /* There was no artist in this response, add the album alone */
                    result.add(new Album(currentAlbum));
                }

                if (!pair[VALUE].isEmpty()) {
                    currentAlbum = pair[VALUE];
                } else {
                    currentAlbum = null;
                }
            }
        }

        Collections.sort(result);

        return result;
    }

    public List<Music> listAllInfo() throws IOException, MPDException {
        final List<String> allInfo = mConnection.sendCommand(MPD_CMD_LISTALLINFO);
        return Music.getMusicFromList(allInfo, false);
    }


    public void movePlaylistSong(final String playlistName, final int from, final int to)
            throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_MOVE, playlistName,
                Integer.toString(from), Integer.toString(to));
    }

    public void next() throws IOException, MPDException {
        mConnection.sendCommand(nextCommand());
    }

    public void pause() throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PAUSE);
    }

    public void play() throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAY);
    }

    public void previous() throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PREV);
    }

    public void refreshDatabase() throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_REFRESH);
    }

    public void removeFromPlaylist(final String playlistName, final Integer pos)
            throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_DEL, playlistName,
                Integer.toString(pos));
    }

    public void removeSavedStream(final Integer pos) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_DEL, STREAMS_PLAYLIST,
                Integer.toString(pos));
    }

    public void saveStream(final String url, final String name) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAYLIST_ADD, STREAMS_PLAYLIST,
                Stream.addStreamName(url, name));
    }

    public List<Music> search(final String locatorString)
            throws IOException, MPDException {
        return genericSearch(locatorString);
    }

    private List<Music> search(final String[] args) throws IOException, MPDException {
        return genericSearch(MPD_CMD_SEARCH, args, true);
    }

    public void seek(final long position) throws IOException, MPDException {
        seekById(mStatus.getSongId(), position);
    }

    private void seekById(final int songId, final long position) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_SEEK_ID, Integer.toString(songId),
                Long.toString(position));
    }

    public void setConsume(final boolean consume) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_CONSUME, MPDCommand.booleanValue(consume));
    }

    public void setRandom(final boolean random) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_RANDOM, MPDCommand.booleanValue(random));
    }

    public void setRepeat(final boolean repeat) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_REPEAT, MPDCommand.booleanValue(repeat));
    }

    public void setSingle(final boolean single) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_SINGLE, MPDCommand.booleanValue(single));
    }

    public void setVolume(final int volume) throws IOException, MPDException {
        final int vol = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, volume));
        mConnection.sendCommand(MPD_CMD_SET_VOLUME, Integer.toString(vol));
    }

    public void skipToId(final int id) throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_PLAY_ID, Integer.toString(id));
    }

    public void stop() throws IOException, MPDException {
        mConnection.sendCommand(MPD_CMD_STOP);
    }

    public void updateStatistics() throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(MPD_CMD_STATISTICS);

        mStatistics.update(response);
    }

    void updateStatus() throws IOException, MPDException {
        final List<String> response = mConnection.sendCommand(MPD_CMD_STATUS);

        if (response == null) {
            Log.error(TAG, "No status response from the MPD server.");
        } else {
            mStatus.updateStatus(response);
        }
    }
}
