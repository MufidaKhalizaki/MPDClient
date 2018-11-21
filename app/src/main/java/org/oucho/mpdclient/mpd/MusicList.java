package org.oucho.mpdclient.mpd;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.item.Music;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


final class MusicList implements Iterable<Music>, MPDConfig {

    private final List<Music> mList;

    private final List<Integer> mSongID;

    MusicList() {
        super();

        mList = Collections.synchronizedList(new ArrayList<Music>());
        mSongID = Collections.synchronizedList(new ArrayList<Integer>());
    }

    private void add(final Music music) {
        synchronized (mList) {
            final int songPos = music.getPos();

            if (mList.size() == songPos) {

                mList.add(music);
                mSongID.add(music.getSongId());
            } else {

                while (mList.size() <= songPos) {
                    mList.add(null);
                    mSongID.add(null);
                }

                if (songPos == -1) {
                    throw new IllegalStateException("Media server protocol error: songPos not " +
                            "included with the playlist changes included with the following " +
                            "music. Path:" + music.getFullPath() + " Name: " + music.getName());
                }

                mList.set(songPos, music);
                mSongID.set(songPos, music.getSongId());
            }
        }
    }


    Music getByIndex(final int index) {
        Music result = null;

        if (index >= 0 && mList.size() > index) {
            result = mList.get(index);
        }

        return result;
    }


    List<Music> getMusic() {
        return Collections.unmodifiableList(mList);
    }

    @Override
    public Iterator<Music> iterator() {
        return mList.iterator();
    }


    void manipulate(final Iterable<Music> musicList, final int listCapacity) {
        for (final Music music : musicList) {

            add(music);
        }


        synchronized (mList) {
            final int listSize = mList.size();
            final int songIDSize = mSongID.size();
            if (listSize < listCapacity) {
                throw new IllegalStateException("List store: " + listSize + " and playlistLength: " + listCapacity + " size differs.");
            }

            mList.subList(listCapacity, listSize).clear();
            mSongID.subList(listCapacity, songIDSize).clear();

            if (songIDSize != listSize) {
                throw new IllegalStateException("List store: " + listSize + " and SongID: " + songIDSize + " size differs.");
            }
        }
    }


    void replace(final Collection<Music> collection) {

        synchronized (mList) {
            mList.clear();
            mList.addAll(collection);

            mSongID.clear();
            for (final Music track : collection) {
                mSongID.add(track.getSongId());
            }
        }
    }

    int size() {
        return mList.size();
    }
}
