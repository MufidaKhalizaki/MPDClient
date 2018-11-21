package org.oucho.mpdclient.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import org.oucho.mpdclient.BuildConfig;
import org.oucho.mpdclient.R;

public class About extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder about = new AlertDialog.Builder(getActivity());

        @SuppressLint("InflateParams")
        View dialoglayout = getActivity().getLayoutInflater().inflate(R.layout.alertdialog_about, null);
        String versionName = BuildConfig.VERSION_NAME;
        TextView versionView = dialoglayout.findViewById(R.id.version);
        versionView.setText(versionName);

        about.setView(dialoglayout);

        return about.create();
    }

}
