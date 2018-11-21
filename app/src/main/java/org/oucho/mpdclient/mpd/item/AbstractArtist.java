package org.oucho.mpdclient.mpd.item;

import org.oucho.mpdclient.mpd.Tools;

import java.util.Arrays;
import java.util.Locale;


abstract class AbstractArtist extends Item {

    private final String mName;

    private final String mSort;

    AbstractArtist(final String name) {
        super();
        mName = name;
        if (null != name && name.toLowerCase(Locale.getDefault()).startsWith("the ")) {
            mSort = name.substring(4);
        } else {
            mSort = null;
        }
    }

    AbstractArtist(final String name, final String sort) {
        super();

        mName = name;
        mSort = sort;
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
            final AbstractArtist artist = (AbstractArtist) o;

            assert artist != null;
            if (Tools.isNotEqual(mName, artist.mName) || Tools.isNotEqual(mSort, artist.mSort)) {
                isEqual = Boolean.FALSE;
            }
        }

        if (isEqual == null) {
            isEqual = Boolean.TRUE;
        }

        return isEqual;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{mName, mSort});
    }

    @Override
    String sortText() {
        final String result;

        if (mSort == null && mName == null) {
            result = "";
        } else if (mSort == null) {
            result = super.sortText();
        } else {
            result = mSort;
        }

        return result;
    }
}
