package org.oucho.mpdclient.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.fragments.AlbumListFragment;
import org.oucho.mpdclient.fragments.AlbumSongsFragment;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Album;
import org.oucho.mpdclient.mpd.item.Item;
import org.oucho.mpdclient.mpd.item.Music;

import java.io.IOException;


public class CreatePlaylistDialog extends DialogFragment implements MPDConfig {

    private static final String TAG = "CreatePlaylistDialog";
    private OnPlaylistCreatedListener mListener;

    public static CreatePlaylistDialog newInstance() {
        return new CreatePlaylistDialog();
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final View layout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_playlist,
                new LinearLayout(getActivity()), false);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity())
                .setTitle(R.string.create_playlist)
                .setView(layout)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {

                            EditText editText = layout.findViewById(R.id.playlist_name);

                            final String playlistName = editText.getText().toString();

                            addToPlaylist(playlistName);

                            if (mListener != null) {
                                mListener.onPlaylistCreated();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> {
                            // This constructor is intentionally empty, pourquoi ? parce que !
                        });
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

        return builder.create();
    }

    private void addToPlaylist(String playlistName) {

        if (MPDApplication.getPlaylistTypeAlbum()) {
            Item item = AlbumListFragment.getAlbumItem();

            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.addToPlaylist(playlistName, (Album) item);

            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to add.", e);
            }
        } else {
            Item item = AlbumSongsFragment.getSongItem();

            try {
                MPDApplication.getInstance().oMPDAsyncHelper.oMPD.addToPlaylist(playlistName, (Music) item);
            } catch (final IOException | MPDException e) {
                Log.e(TAG, "Failed to add.", e);
            }
        }

    }

    public void setOnPlaylistCreatedListener(OnPlaylistCreatedListener listener) {
        mListener = listener;
    }


    interface OnPlaylistCreatedListener {
        void onPlaylistCreated();
    }

}
