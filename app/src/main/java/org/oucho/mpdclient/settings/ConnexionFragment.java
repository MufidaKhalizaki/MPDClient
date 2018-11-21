package org.oucho.mpdclient.settings;


import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.InputType;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;

public class ConnexionFragment extends PreferenceFragment implements MPDConfig {


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.connectionsettings);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final PreferenceCategory masterCategory = (PreferenceCategory) preferenceScreen.findPreference("connectionCategory");

        createDynamicSettings(masterCategory);

        Resources res = getResources();

        setTitre(res.getString(R.string.defaultSettings));


    }


    private void setTitre(String titre) {

        int couleurTitre = ContextCompat.getColor(getContext(), R.color.colorAccent);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            getActivity().setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + " </font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            getActivity().setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + titre + " </font>"));
        }

    }

    private void createDynamicSettings(PreferenceCategory toCategory) {

        final EditTextPreference prefHost = new EditTextPreference(getContext());
        prefHost.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        prefHost.setDialogTitle(R.string.host);
        prefHost.setTitle(R.string.host);
        prefHost.setSummary(R.string.hostDescription);
        prefHost.setDefaultValue("192.168.0.1");
        prefHost.setKey("hostname");
        toCategory.addPreference(prefHost);

        final EditTextPreference prefPort = new EditTextPreference(getContext());
        prefPort.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        prefPort.setDialogTitle(R.string.port);
        prefPort.setTitle(R.string.port);
        prefPort.setSummary(R.string.portDescription);
        prefPort.setDefaultValue("6600");
        prefPort.setKey("port");
        toCategory.addPreference(prefPort);

        final EditTextPreference prefPassword = new EditTextPreference(getContext());
        prefPassword.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        prefPassword.setDialogTitle(R.string.password);
        prefPassword.setTitle(R.string.password);
        prefPassword.setSummary(R.string.passwordDescription);
        prefPassword.setDefaultValue("");
        prefPassword.setKey("password");
        toCategory.addPreference(prefPassword);

        getActivity().onContentChanged();
    }
}
