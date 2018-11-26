/*
 * MPDclient - Music Player Daemon (MPD) remote for android
 * Copyright (C) 2017  Old-Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oucho.mpdclient.cover;

import android.net.Uri;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.helpers.AlbumInfo;

import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;

public class LocalCover implements CoverRetriever, MPDConfig {

    public static final String RETRIEVER_NAME = "User's HTTP Server";

    private static final String URL_PREFIX = "http://";


    private static void appendPathString(Uri.Builder builder, String baseString) {

        if (baseString != null && !baseString.isEmpty()) {
            final String[] components = baseString.split("/");
            for (final String component : components) {
                builder.appendPath(component);
            }
        }
    }

    private static String buildCoverUrl(String serverName, String musicPath, String path) {

        if (musicPath.startsWith(URL_PREFIX)) {
            int hostPortEnd = musicPath.indexOf(URL_PREFIX.length(), '/');
            if (hostPortEnd == -1) {
                hostPortEnd = musicPath.length();
            }
            serverName = musicPath.substring(URL_PREFIX.length(), hostPortEnd);
            musicPath = musicPath.substring(hostPortEnd);
        }
        final Uri.Builder uriBuilder = Uri.parse(URL_PREFIX + serverName).buildUpon();
        appendPathString(uriBuilder, musicPath);
        appendPathString(uriBuilder, path);
        appendPathString(uriBuilder, "cover.jpg");

        final Uri uri = uriBuilder.build();
        return uri.toString();
    }

    @Override
    public String[] getCoverUrl(final AlbumInfo albumInfo) {

        if (isEmpty(albumInfo.getPath())) {
            return new String[0];
        }


        String url;

        final List<String> urls = new ArrayList<>();

        final String serverName = MPDApplication.getInstance().oMPDAsyncHelper.getConnectionSettings().server;

        url = buildCoverUrl(serverName, "/", albumInfo.getPath());

        if (!urls.contains(url)) {
            urls.add(url);
        }

        return urls.toArray(new String[urls.size()]);

    }

    @Override
    public String getName() {
        return RETRIEVER_NAME;
    }

    @Override
    public boolean isCoverLocal() {
        return false;
    }

}
