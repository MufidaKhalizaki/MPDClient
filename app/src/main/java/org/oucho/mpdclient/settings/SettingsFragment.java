package org.oucho.mpdclient.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.cover.CachedCover;
import org.oucho.mpdclient.cover.helper.CoverManager;
import org.oucho.mpdclient.mpd.exception.MPDException;

import java.io.IOException;

public class SettingsFragment extends PreferenceFragment implements MPDConfig {

    private static final String TAG = "SettingsFragment";


    private EditTextPreference mCacheUsage;

    private boolean mPreferencesBound;

    public SettingsFragment() {
        super();
        mPreferencesBound = false;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        refreshDynamicFields();

    }

    public void onConnectionStateChanged() {
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mCacheUsage = (EditTextPreference) findPreference("cacheUsage");

        mPreferencesBound = true;
        refreshDynamicFields();
    }



    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            @NonNull final Preference preference) {

        if (preference.getKey() == null) {
            return false;
        }

        if ("refreshMPDDatabase".equals(preference.getKey())) {
            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.refreshDatabase();
                Toast.makeText(getContext(), "Database update send", Toast.LENGTH_SHORT).show();
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to refresh the database.", e);
            }
            return true;
        } else if ("clearLocalCoverCache".equals(preference.getKey())) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.clearLocalCoverCache)
                    .setMessage(R.string.clearLocalCoverCachePrompt)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {

                        CoverManager.getInstance().clear();
                        mCacheUsage.setSummary("0.00B");
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        // do nothing
                    })
                    .show();
            return true;

        }


        return false;

    }

    private void refreshDynamicFields() {
        if (getActivity() == null || !mPreferencesBound) {
            return;
        }
        onConnectionStateChanged();

        final long size = new CachedCover().getCacheUsage();
        final String usage = Formatter.formatFileSize(getContext(), size);

        mCacheUsage.setSummary(usage);
    }

}
