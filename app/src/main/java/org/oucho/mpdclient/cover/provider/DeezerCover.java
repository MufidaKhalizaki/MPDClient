package org.oucho.mpdclient.cover.provider;

import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.cover.AbstractWebCover;
import org.oucho.mpdclient.helpers.AlbumInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;


public class DeezerCover extends AbstractWebCover implements MPDConfig {

    private static final String TAG = "DeezerCover";

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) {

        final String deezerResponse;
        final JSONObject jsonRootObject;
        final JSONArray jsonArray;
        StringBuilder coverUrl = new StringBuilder();
        JSONObject jsonObject;

        try {
            deezerResponse = callCover("http://api.deezer.com/search/album?q=" + albumInfo.getAlbum() + ' ' + albumInfo.getArtist() + "&nb_items=1&output=json");
            jsonRootObject = new JSONObject(deezerResponse);
            jsonArray = jsonRootObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = jsonArray.getJSONObject(i);
                coverUrl.setLength(0);
                coverUrl.append(jsonObject.getString("cover"));
                if (coverUrl.length() != 0) {
                    coverUrl.append("&size=big");
                    return new String[]{
                            coverUrl.toString()
                    };
                }
            }

        } catch (final Exception e) {
            Log.e(TAG, "Failed to get cover URL from Deezer", e);
        }

        return new String[0];
    }

    @Override
    public String getName() {
        return "DEEZER";
    }
}
