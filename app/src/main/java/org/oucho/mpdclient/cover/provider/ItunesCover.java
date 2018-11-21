package org.oucho.mpdclient.cover.provider;

import org.oucho.mpdclient.cover.AbstractWebCover;
import org.oucho.mpdclient.helpers.AlbumInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class ItunesCover extends AbstractWebCover {

    private static final String TAG = "ItunesCover";

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) {
        final String response;
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        String coverUrl;
        JSONObject jsonObject;

        try {
            response = callCover("https://itunes.apple.com/search?term=" + albumInfo.getAlbum() + ' ' + albumInfo.getArtist() + "&limit=5&media=music&entity=album");
            jsonRootObject = new JSONObject(response);
            jsonArray = jsonRootObject.getJSONArray("results");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                coverUrl = jsonObject.getString("artworkUrl100");
                if (coverUrl != null) {
                    // Based on some tests even if the cover art size returned
                    // is 100x100
                    // Bigger versions also exists.
                    return new String[]{
                            coverUrl.replace("100x100", "600x600")
                    };
                }
            }

        } catch (final Exception e) {
            Log.e(TAG, "Failed to get cover URL from " + getName(), e);

        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "ITUNES";
    }
}
