package org.oucho.mpdclient.helpers;

import org.oucho.mpdclient.mpd.Tools;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;
import org.oucho.mpdclient.mpd.item.Music;

import java.util.Arrays;

import static org.oucho.mpdclient.mpd.Tools.getHashFromString;

public class AlbumInfo {

    private static final String INVALID_ALBUM_KEY = "INVALID_ALBUM_KEY";

    protected final String mAlbum;

    protected final String mArtist;

    protected String mFilename;

    protected String mPath;

    public AlbumInfo(final Music music) {
        super();
        String artist = music.getAlbumArtist();
        if (artist == null) {
            artist = music.getArtist();
        }
        mArtist = artist;
        mAlbum = music.getAlbum();
        mPath = music.getPath();
        mFilename = music.getFilename();
    }

    public AlbumInfo(final Album album) {
        super();
        final Artist artistName = album.getArtist();
        if (artistName != null) {
            mArtist = artistName.getName();
        } else {
            mArtist = null;
        }

        mAlbum = album.getName();
        mPath = album.getPath();
    }

    public AlbumInfo(final String artist, final String album) {
        super();
        mArtist = artist;
        mAlbum = album;
    }

    public AlbumInfo(final String artist, final String album, final String path,
            final String filename) {
        super();
        mArtist = artist;
        mAlbum = album;
        mPath = path;
        mFilename = filename;
    }

    @Override
    public boolean equals(final Object o) {
        Boolean isEqual = null;

        if (this == o) {
            isEqual = Boolean.TRUE;
        } else if (o == null || getClass() != o.getClass()) {
            isEqual = Boolean.FALSE;
        }

        if (isEqual == null || isEqual.equals(Boolean.TRUE)) {
            final AlbumInfo albumInfo = (AlbumInfo) o;

            assert albumInfo != null;
            if (Tools.isNotEqual(mAlbum, albumInfo.mAlbum)) {
                isEqual = Boolean.FALSE;
            }

            if (Tools.isNotEqual(mArtist, albumInfo.mArtist)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getArtist() {
        return mArtist;
    }

    public String getFilename() {
        return mFilename;
    }

    public String getKey() {
        return isValid() ? getHashFromString(mArtist + mAlbum) : INVALID_ALBUM_KEY;
    }

    public String getPath() {
        return mPath;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mArtist, mAlbum});
    }

    public boolean isValid() {
        final boolean isArtistEmpty = mArtist == null || mArtist.isEmpty();
        final boolean isAlbumEmpty = mAlbum == null || mAlbum.isEmpty();
        return !isAlbumEmpty && !isArtistEmpty;
    }

    @Override
    public String toString() {
        return "AlbumInfo{" + "artist='" + mArtist + '\'' + ", album='" + mAlbum + '\'' + ", path='" + mPath + '\'' + ", filename='" + mFilename + '\'' + '}';
    }
}
