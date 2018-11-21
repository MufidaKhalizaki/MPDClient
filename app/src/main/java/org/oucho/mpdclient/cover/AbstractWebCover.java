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
