package org.oucho.mpdclient.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.mpd.exception.MPDException;

import java.io.IOException;


public class NewRadioReceiver extends BroadcastReceiver implements MPDConfig{

    @Override
    public void onReceive(Context context, Intent intent) {

        final String TAG = "NewRadioReceiver";
        String etat = intent.getAction();

        if ( "org.oucho.MPDclient.ADD_RADIO".equals(etat) ) {

            String name = intent.getStringExtra("name");
            String url = intent.getStringExtra("url");

            try {

                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.saveStream(url, name);

                String text = context.getResources().getString(R.string.addRadio_fromApp, name);
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

                Toast.makeText(context, "Radio reçu.", Toast.LENGTH_SHORT).show();

            } catch (final IOException | MPDException e) {

                Toast.makeText(context, "Failed to save stream.", Toast.LENGTH_SHORT).show();

                Log.e(TAG, "Failed to save stream.", e);
            }
        }
    }
}
