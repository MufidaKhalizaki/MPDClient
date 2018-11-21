package org.oucho.mpdclient.library;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.fragments.AlbumListFragment;
import org.oucho.mpdclient.fragments.AlbumSongsFragment;
import org.oucho.mpdclient.fragments.RadioFragment;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Artist;

public class SimpleLibraryActivity extends AppCompatActivity implements MPDConfig {


    private String debugIntent(final Intent intent) {
        final ComponentName callingActivity = getCallingActivity();
        final StringBuilder stringBuilder = new StringBuilder();
        final Bundle extras = intent.getExtras();

        stringBuilder.append("SimpleLibraryActivity started with invalid extra");

        if (callingActivity != null) {
            stringBuilder.append(", calling activity: ");
            stringBuilder.append(callingActivity.getClassName());
        }

        if (extras != null) {
            for (final String what : extras.keySet()) {
                stringBuilder.append(", intent extra: ");
                stringBuilder.append(what);
            }
        }

        stringBuilder.append('.');
        return stringBuilder.toString();
    }

    private Fragment getRootFragment() {
        final Intent intent = getIntent();
        final Fragment rootFragment;

        String EXTRA_STREAM = "streams";
        String EXTRA_ARTIST = "artist";
        String EXTRA_ALBUM = "album";
        if (intent.hasExtra(EXTRA_ALBUM)) {

            final Album album = intent.getParcelableExtra(EXTRA_ALBUM);

            //noinspection AccessStaticViaInstance
            rootFragment = new AlbumSongsFragment().newInstance(album);

        } else if (intent.hasExtra(EXTRA_ARTIST)) {

            final Artist artist = intent.getParcelableExtra(EXTRA_ARTIST);

            rootFragment = new AlbumListFragment().newInstance(artist);

        } else if (intent.hasExtra(EXTRA_STREAM)) {

            rootFragment = new RadioFragment();

        } else {
            throw new IllegalStateException(debugIntent(intent));
        }

        return rootFragment;
    }



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs);


        final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(mUIFlag);

        if (savedInstanceState == null) {
            final Fragment rootFragment = getRootFragment();

            if (rootFragment == null) {
                throw new RuntimeException("Error : SimpleLibraryActivity root fragment is null");
            }

            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.root_frame, rootFragment);
            ft.commit();
        }
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = false;

        if (item.getItemId() == android.R.id.home) {
            finish();
            result = true;
        }

        return result;
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("org.oucho.mdpclient.setTitle");

        registerReceiver(mServiceListener, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mServiceListener);

    }


    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();
            String titre = intent.getStringExtra("Titre");

            assert receiveIntent != null;
            if (receiveIntent.equals("org.oucho.mdpclient.setTitle")) {
                setTitre(titre);
            }
        }
    };


    private void setTitre(String tite) {

        int couleurTitre = ContextCompat.getColor(MPDApplication.getInstance().getApplicationContext(), R.color.colorAccent);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + tite + " " + " " + " </font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + tite + " " + " " + " </font>"));
        }

    }


}
