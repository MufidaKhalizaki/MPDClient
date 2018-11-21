package org.oucho.mpdclient.mpd.item;

import android.os.Parcel;
import android.os.Parcelable;


public class Music extends AbstractMusic implements Parcelable {

    @SuppressWarnings("unused")
    public static final Creator<Music> CREATOR = new Creator<Music>() {
        @Override
        public Music createFromParcel(final Parcel source) {
            return new Music(source);
        }

        @Override
        public Music[] newArray(final int size) {
            return new Music[size];
        }
    };

    Music(final String album, final String artist, final String albumArtist,
            final String composer, final String fullPath, final int disc, final long date,
            final String genre, final long time, final String title, final int totalTracks,
            final int track, final int songId, final int songPos, final String name) {
        super(album, artist, albumArtist, composer, fullPath, disc, date, genre, time, title,
                totalTracks, track, songId, songPos, name);
    }


    private Music(final Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readString(), in.readString(),
                in.readInt(), in.readLong(), in.readString(), in.readLong(), in.readString(),
                in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(mAlbum);
        dest.writeString(mArtist);
        dest.writeString(mAlbumArtist);
        dest.writeString(mComposer);
        dest.writeString(mFullPath);
        dest.writeInt(mDisc);
        dest.writeLong(mDate);
        dest.writeString(mGenre);
        dest.writeLong(mTime);
        dest.writeString(mTitle);
        dest.writeInt(mTotalTracks);
        dest.writeInt(mTrack);
        dest.writeInt(mSongId);
        dest.writeInt(mSongPos);
        dest.writeString(mName);
    }
}
