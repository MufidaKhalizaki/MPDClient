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

package org.oucho.mpdclient.settings;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.event.StatusChangeListener;

public class SettingsActivity extends AppCompatActivity implements MPDConfig, StatusChangeListener {

    private SettingsFragment mSettingsFragment;

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {
        mSettingsFragment.onConnectionStateChanged();
    }

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        Resources res = getResources();

        setTitre(res.getString(R.string.settings));

        mSettingsFragment = new SettingsFragment();
        MPDApplication.getInstance().oMPDAsyncHelper.addStatusChangeListener(this);
        getFragmentManager().beginTransaction().replace(R.id.content_frame, mSettingsFragment).commit();
    }

    private void setTitre(String titre) {

        int couleurTitre = ContextCompat.getColor(MPDApplication.getInstance().getApplicationContext(), R.color.colorAccent);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + " </font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + " </font>"));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MPDApplication.getInstance().oMPDAsyncHelper.removeStatusChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        MPDApplication.getInstance().setActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MPDApplication.getInstance().unsetActivity(this);
    }

    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {

    }

    @Override
    public void randomChanged(final boolean random) {

    }

    @Override
    public void repeatChanged(final boolean repeating) {

    }

    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {

    }

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {

    }

    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {

    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {

    }
}
