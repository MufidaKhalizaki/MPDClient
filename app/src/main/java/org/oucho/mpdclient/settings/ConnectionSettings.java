package org.oucho.mpdclient.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.oucho.mpdclient.MPDConfig;

public class ConnectionSettings extends AppCompatActivity implements MPDConfig {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new ConnexionFragment()).commit();
    }

}
