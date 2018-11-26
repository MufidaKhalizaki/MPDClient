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
