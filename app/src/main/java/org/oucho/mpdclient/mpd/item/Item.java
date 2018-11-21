package org.oucho.mpdclient.mpd.item;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.R;

import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Item implements Comparable<Item> {


    public static <T extends Item> List<T> merged(final List<T> albumArtists, final List<T> artists) {
        int jStart = albumArtists.size() - 1;
        // remove "" from albumArtists, because the Unknown
        // AlbumArtist would fall back to an Artist, the "Unknown"
        // Entry must come from the Artists.
        for (int j = jStart; j >= 0; j--) { // album artists
            if (albumArtists.get(j).getName().equals("")) {
                albumArtists.remove(j);
                jStart--;
            }
        }
        for (int i = artists.size() - 1; i >= 0; i--) { // artists
            for (int j = jStart; j >= 0; j--) { // album artists
                if (albumArtists.get(j).doesNameExist(artists.get(i))) {
                    jStart = j;
                    artists.remove(i);
                    break;
                }
            }
        }
        artists.addAll(albumArtists);
        Collections.sort(artists);
        return artists;
    }


    @Override
    public int compareTo(@NonNull Item another) {
        final int comparisonResult;
        final String sorted = sortText();
        final String anotherSorted = another.sortText();

        // sort "" behind everything else
        if (sorted == null || sorted.isEmpty()) {
            if (anotherSorted == null || anotherSorted.isEmpty()) {
                comparisonResult = 0;
            } else {
                comparisonResult = 1;
            }
        } else if (anotherSorted == null || anotherSorted.isEmpty()) {
            comparisonResult = -1;
        } else {
            comparisonResult = Collator.getInstance().compare(sorted, anotherSorted);
        }

        return comparisonResult;
    }

    boolean doesNameExist(final Item o) {
        boolean nameExists = false;
        final String name = getName();

        if (name != null && o != null) {
            nameExists = name.equals(o.getName());
        }

        return nameExists;
    }

    public abstract String getName();

    public boolean isUnknown() {
        final String name = getName();

        return name == null || name.isEmpty();
    }


    public String mainText() {
        String mainText = getName();

        Context context = MPDApplication.getInstance().getApplicationContext();

        String album =  context.getString(R.string.mpd_unknown_album);
        String artist =  context.getString(R.string.mpd_unknown_artist);
        String genre =  context.getString(R.string.mpd_unknown_genre);

        Bundle bundle = new Bundle();
        bundle.putString("UnknownMetadataAlbum", album);
        bundle.putString("UnknownMetadataArtist", artist);
        bundle.putString("UnknownMetadataGenre", genre);

        if (mainText == null || mainText.isEmpty()) {
            final String unknownKey = "UnknownMetadata";
            final String key = unknownKey + getClass().getSimpleName();

            if (bundle.containsKey(key)) {
                mainText = bundle.getString(key);
            }
        }

        return mainText;
    }

    String sortText() {
        String name = getName();

        if (name != null) {
            name = name.toLowerCase(Locale.getDefault());
        }

        return name;
    }

    @Override
    public String toString() {
        return mainText();
    }

}
