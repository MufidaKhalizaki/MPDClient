package org.oucho.mpdclient.fragments.loader;

import android.content.Context;
import android.util.Log;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Item;

import java.io.IOException;
import java.util.List;

public class AlbumSongLoader extends BaseLoader<List<? extends Item>> implements MPDConfig {

    private Album mAlbum = null;
    private List<? extends Item> mItems = null;

    public AlbumSongLoader(Context context, Album album) {
        super(context);

        mAlbum = album;
    }

    @Override
    public List<? extends Item> loadInBackground() {

        try {
            mItems = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getSongs(mAlbum);
        } catch (final IOException | MPDException e) {
            Log.e("Album Song Loader", "Failed to update.", e);
        }
        return mItems;
    }
}
