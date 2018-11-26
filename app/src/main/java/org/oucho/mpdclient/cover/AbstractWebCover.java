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

import android.widget.Toast;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public abstract class AbstractWebCover implements CoverRetriever, MPDConfig {

    @Override
    public boolean isCoverLocal() {
        return false;
    }


    protected static String callCover(String urlstring){

        String data=null;

        String httpRequest = urlstring.replace(" ", "%20");

        try {
            URL url = new URL(httpRequest);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            InputStream stream = conn.getInputStream();

            data = convertStreamToString(stream);

            stream.close();

        }catch(SocketTimeoutException e){

            Toast.makeText(MPDApplication.getInstance(), "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return data;

    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
