package org.oucho.mpdclient.fragments.loader;


import android.content.Context;
import android.util.Log;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Item;

import java.io.IOException;
import java.util.List;

public class PlaylistContentLoader extends BaseLoader<List<? extends Item>> implements MPDConfig {

    private List<? extends Item> mItems = null;

    private final String mPlaylistName;


    public PlaylistContentLoader(Context context, String playlist) {
        super(context);

        mPlaylistName = playlist;
    }


    @Override
    public List<? extends Item> loadInBackground() {

        try {
            mItems = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylistSongs(mPlaylistName);
        } catch (final IOException | MPDException e) {
            Log.e("PlayListLoader", "Failed to update.", e);
        }

        return mItems;
    }

}