package org.oucho.mpdclient.helpers;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.tools.Utils;

import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Music;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class AlbumCache implements MPDConfig {

    private static final boolean GZIP = false;

    private static final String TAG = "AlbumCache";

    private Map<String, AlbumDetails> mAlbumDetails;

    private Set<List<String>> mAlbumSet;

    private boolean mEnabled = true;

    private File mFilesDir;

    private Date mLastUpdate = null;

    private final CachedMPD cachedMPD;

    private int mPort;

    private String mServer;

    private Set<List<String>> mUniqueAlbumSet;

    AlbumCache(CachedMPD mpd) {
        super();
        Log.d(TAG, "Starting ...");

        cachedMPD = mpd;
    }

    private static String albumCode(String artist, String album,
            final boolean isAlbumArtist) {
        return (artist != null ? artist : "") + "//" +
                (isAlbumArtist ? "AA" : "A") +
                "//" + (album != null ? album : "");
    }

    private String cacheInfo() {
        return "AlbumCache: " +
                mAlbumSet.size() + " album/artist combinations, " +
                mUniqueAlbumSet.size() + " unique album/artist combinations, " + "Date: " + mLastUpdate;
    }

    Set<String> getAlbumArtists(String album, String artist) {
        final Set<String> aartists = new HashSet<>();
        for (final List<String> ai : mAlbumSet) {
            if (ai.get(0).equals(album) &&
                    ai.get(1).equals(artist)) {
                aartists.add(ai.get(2));
            }
        }
        return aartists;
    }

    AlbumDetails getAlbumDetails(String artist, String album, boolean isAlbumArtist) {
        return mAlbumDetails.get(albumCode(artist, album, isAlbumArtist));
    }

    Set<String> getAlbums(String artist, boolean albumArtist) {
        final Set<String> albums = new HashSet<>();
        for (final List<String> ai : mAlbumSet) {
            if (albumArtist && ai.get(2).equals(artist) ||
                    !albumArtist && ai.get(1).equals(artist)) {
                albums.add(ai.get(0));
            }
        }
        return albums;
    }

    private String getFilename() {
        return mServer + '_' + mPort;
    }

    Set<List<String>> getUniqueAlbumSet() {
        return mUniqueAlbumSet;
    }

    private synchronized boolean isUpToDate() {
        final Date mpdlast = cachedMPD.getStatistics().getDbUpdate();
        return (null != mLastUpdate && null != mpdlast && mLastUpdate.after(mpdlast));
    }

    @SuppressWarnings("unchecked")
    private synchronized boolean load() {
        final File file = new File(mFilesDir, getFilename() + "");

        if (!file.exists()) {
            return false;
        }

        final ObjectInputStream restore;
        boolean loadedOk = false;
        try {
            if (GZIP) {
                restore = new ObjectInputStream(new GZIPInputStream
                        (new FileInputStream(file)));
            } else {
                restore = new ObjectInputStream(new FileInputStream(file));
            }

            mLastUpdate = (Date) restore.readObject();
            mAlbumDetails = (Map<String, AlbumDetails>) restore.readObject();
            mAlbumSet = (Set<List<String>>) restore.readObject();
            restore.close();
            makeUniqueAlbumSet();
            loadedOk = true;

        } catch (final FileNotFoundException ignored) {
        } catch (final Exception e) {
            Log.e(TAG, "Exception.", e);
        }
        if (loadedOk) {
            Log.d(TAG, cacheInfo());
        } else {
            Log.d(TAG, "Error on load");
        }
        return loadedOk;
    }

    private void makeUniqueAlbumSet() {
        mUniqueAlbumSet = new HashSet<>(mAlbumSet.size());
        for (final List<String> ai : mAlbumSet) {
            final String album = ai.get(2);

            if (album != null && album.isEmpty()) {
                mUniqueAlbumSet.add(Arrays.asList(ai.get(0), ai.get(1), ""));
            } else {
                mUniqueAlbumSet.add(Arrays.asList(ai.get(0), "", ai.get(2)));
            }
        }
    }


    synchronized boolean refresh() {
        return refresh(false);
    }


    private synchronized boolean refresh(boolean force) {

        if (!mEnabled) {
            return false;
        }

        if (!updateConnection()) {
            return false;
        }

        if (!force && isUpToDate()) {
            return true;
        }

        mLastUpdate = Calendar.getInstance().getTime();

        Utils.notifyUser(R.string.updatingLocalAlbumCacheNote);

        final Date oldUpdate = mLastUpdate;
        mAlbumDetails = new HashMap<>();
        mAlbumSet = new HashSet<>();

        final List<Music> allmusic;
        try {
            allmusic = cachedMPD.listAllInfo();
        } catch (final IOException | MPDException e) {
            mEnabled = false;
            mLastUpdate = null;
            updateConnection();
            Log.d(TAG, "disabled AlbumCache", e);
            Utils.notifyUser("Error with the 'listallinfo' command. Probably you have to adjust your server's 'max_output_buffer_size'");
            return false;
        }

        try {
            for (final Music music : allmusic) {
                final String albumArtist = music.getAlbumArtist();
                final String artist = music.getArtist();
                String album = music.getAlbum();
                if (album == null) {
                    album = "";
                }
                final List<String> albumInfo = Arrays.asList(album, artist == null ? "" : artist, albumArtist == null ? "" : albumArtist);
                mAlbumSet.add(albumInfo);

                final boolean isAlbumArtist = albumArtist != null && !albumArtist.isEmpty();
                final String thisAlbum =
                        albumCode(isAlbumArtist ? albumArtist : artist, album, isAlbumArtist);
                final AlbumDetails details;

                if (mAlbumDetails.containsKey(thisAlbum)) {
                    details = mAlbumDetails.get(thisAlbum);
                } else {
                    details = new AlbumDetails();
                    mAlbumDetails.put(thisAlbum, details);
                }

                if (details.mPath == null) {
                    details.mPath = music.getPath();
                }

                details.mNumTracks += 1;
                details.mTotalTime += music.getTime();

                if (details.mDate == 0) {
                    details.mDate = music.getDate();
                }
            }

            makeUniqueAlbumSet();

            if (!save()) {
                mLastUpdate = oldUpdate;
                return false;
            }
        } catch (final Exception e) {
            Utils.notifyUser("Error updating Album Cache");
            mLastUpdate = oldUpdate;
            Log.e(TAG, "Error updating Album Cache.", e);
            return false;
        }
        return true;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private synchronized boolean save() {
        final File file = new File(mFilesDir, getFilename() + "");

        final File backupfile = new File(file.getAbsolutePath() + ".bak");
        if (file.exists()) {
            if (backupfile.exists()) {
                backupfile.delete();
            }
            file.renameTo(backupfile);
        }
        final ObjectOutputStream save;
        boolean error = false;
        try {
            if (GZIP) {
                save = new ObjectOutputStream(new GZIPOutputStream
                        (new FileOutputStream(file)));
            } else {
                save = new ObjectOutputStream(new BufferedOutputStream
                        (new FileOutputStream(file)));
            }

            save.writeObject(mLastUpdate);
            save.writeObject(mAlbumDetails);
            save.writeObject(mAlbumSet);
            save.close();

        } catch (final Exception e) {
            error = true;
            Log.e(TAG, "Failed to save.", e);
        }
        if (error) {
            file.delete();
            backupfile.renameTo(file);
        }
        return !error;
    }

    private synchronized boolean updateConnection() {
        // get server/port from mpd
        if (!mEnabled) {

            return false;
        }
        if (cachedMPD == null) {

            return false;
        }

        if (!cachedMPD.isConnected()) {

            return false;
        }
        if (mServer == null) {
            mServer = cachedMPD.getHostAddress().getHostName();
            mPort = cachedMPD.getHostPort();
            mFilesDir = MPDApplication.getInstance().getCacheDir();

            if (!load()) {
                refresh(true);
            }
        }
        return true;
    }

    static class AlbumDetails implements Serializable {

        private static final long serialVersionUID = 2465675380232237273L;

        long mDate = 0;

        long mNumTracks = 0;

        String mPath = null;

        long mTotalTime = 0;
    }

}
