package org.oucho.mpdclient.helpers;

import org.oucho.mpdclient.mpd.MPD;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class CachedMPD extends MPD {

    private final AlbumCache mCache;

    private boolean mIsEnabled = true;

    CachedMPD() {
        super();
        mCache = new AlbumCache(this);
        mIsEnabled = false;
    }

    private static String getArtistName(final Artist artist) {
        final String artistName;

        if (artist == null) {
            artistName = "";
        } else {
            artistName = artist.getName();
        }

        return artistName;
    }

    @Override
    public void addAlbumPaths(final List<Album> albums) throws IOException, MPDException {
        if (!isCached()) {
            super.addAlbumPaths(albums);
            return;
        }
        for (final Album album : albums) {
            final Artist artist = album.getArtist();
            final String artistName = getArtistName(artist);

            final AlbumCache.AlbumDetails details = mCache.getAlbumDetails(artistName, album.getName(), album.hasAlbumArtist());
            if (details != null) {
                album.setPath(details.mPath);
            }
        }
    }

    @Override
    protected void getAlbumDetails(final List<Album> albums, final boolean findYear/* ignored */)
            throws IOException, MPDException {
        if (isCached()) {
            for (final Album album : albums) {

                final Artist artist = album.getArtist();
                final String artistName = getArtistName(artist);
                final AlbumCache.AlbumDetails details;

                details = mCache.getAlbumDetails(artistName, album.getName(), album.hasAlbumArtist());

                if (null != details) {
                    album.setSongCount(details.mNumTracks);
                    album.setDuration(details.mTotalTime);
                    album.setYear(details.mDate);
                    album.setPath(details.mPath);
                }
            }
        } else {
            super.getAlbumDetails(albums, findYear);
        }
    }

    @Override
    public List<Album> getAllAlbums(final boolean trackCountNeeded)
            throws IOException, MPDException {
        final List<Album> allAlbums;

        if (isCached()) {
            final Set<List<String>> albumListSet = mCache.getUniqueAlbumSet();
            final Set<Album> albums = new HashSet<>(albumListSet.size());

            for (final List<String> ai : albumListSet) {
                final Album album;
                final String thirdList = ai.get(2);

                if (thirdList != null && thirdList.isEmpty()) { // no album artist
                    album = new Album(ai.get(0), new Artist(ai.get(1)), false);
                } else {
                    album = new Album(ai.get(0), new Artist(ai.get(2)), true);
                }

                albums.add(album);
            }

            if (albums.isEmpty()) {
                allAlbums = Collections.emptyList();
            } else {
                allAlbums = new ArrayList<>(albums);
                Collections.sort(allAlbums);
                getAlbumDetails(allAlbums, true);
            }
        } else {
            allAlbums = super.getAllAlbums(trackCountNeeded);
        }

        return allAlbums;
    }

    private boolean isCached() {
        return mIsEnabled && mCache.refresh();
    }

    @Override
    public List<String[]> listAlbumArtists(final List<Album> albums)
            throws IOException, MPDException {
        final List<String[]> albumArtists;

        if (isCached()) {
            albumArtists = new ArrayList<>(albums.size());
            for (final Album album : albums) {

                final Artist artist = album.getArtist();
                final Set<String> albumArtist;
                final String artistName = getArtistName(artist);

                albumArtist = mCache.getAlbumArtists(album.getName(), artistName);
                albumArtists.add(albumArtist.toArray(new String[albumArtist.size()]));
            }
        } else {
            albumArtists = super.listAlbumArtists(albums);
        }

        return albumArtists;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> listAlbums(final String artist, final boolean useAlbumArtist)
            throws IOException, MPDException {
        final List<String> albums;

        if (isCached()) {
            albums = new ArrayList(mCache.getAlbums(artist, useAlbumArtist));
        } else {
            albums = super.listAlbums(artist, useAlbumArtist);
        }

        return albums;
    }

    void setUseCache(final boolean useCache) {
        mIsEnabled = useCache;
    }

}
