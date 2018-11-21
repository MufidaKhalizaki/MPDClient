package org.oucho.mpdclient.mpd.item;

import android.os.Parcel;
import android.os.Parcelable;


public class Artist extends AbstractArtist implements Parcelable {

    @SuppressWarnings("unused")
    public static final Creator<Artist> CREATOR = new Creator<Artist>() {

                @Override
                public Artist createFromParcel(final Parcel source) {
                    return new Artist(source);
                }

                @Override
                public Artist[] newArray(final int size) {
                    return new Artist[size];
                }
            };

    public Artist(final String name) {
        super(name);
    }

    private Artist(final Parcel in) {
        super(in.readString(), /* name */
                in.readString()); /* sort */
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(getName());
        dest.writeString(sortText());
    }
}
