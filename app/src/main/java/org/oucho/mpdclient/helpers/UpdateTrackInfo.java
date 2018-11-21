package org.oucho.mpdclient.helpers;

import android.os.AsyncTask;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.R;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.item.Music;


public class UpdateTrackInfo implements MPDConfig {

    private static boolean mForceCoverUpdate = false;

    private FullTrackInfoUpdate mFullTrackInfoListener = null;

    private static String mLastAlbum = null;

    private static String mLastArtist = null;

    private TrackInfoUpdate mTrackInfoListener = null;

    public UpdateTrackInfo() {

    }

    public final void addCallback(final FullTrackInfoUpdate listener) {
        mFullTrackInfoListener = listener;
    }

    public final void addCallback(final TrackInfoUpdate listener) {
        mTrackInfoListener = listener;
    }

    public final void refresh(final MPDStatus mpdStatus) {
        refresh(mpdStatus, false);
    }

    public final void refresh(final MPDStatus mpdStatus, final boolean forceCoverUpdate) {
        mForceCoverUpdate = forceCoverUpdate;
        new UpdateTrackInfoAsync(mTrackInfoListener, mFullTrackInfoListener).execute(mpdStatus);
    }

    public final void removeCallback() {
        mFullTrackInfoListener = null;
    }

    public final void removeAll() {
        mTrackInfoListener = null;
        mFullTrackInfoListener = null;
    }

    public interface FullTrackInfoUpdate {

        void onCoverUpdate(AlbumInfo albumInfo);

        void onTrackInfoUpdate(Music updatedSong, CharSequence album, CharSequence artist, CharSequence date, CharSequence title);
    }

    public interface TrackInfoUpdate {

        void onCoverUpdate();

        void onTrackInfoUpdate(CharSequence artist, CharSequence title);
    }

    private static class UpdateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Void> {

        private String mAlbum = null;

        private AlbumInfo mAlbumInfo = null;

        private String mArtist = null;

        private Music mCurrentTrack = null;

        private String mDate = null;

        private boolean mHasCoverChanged = false;

        private String mTitle = null;

        final TrackInfoUpdate trackInfoListener;
        final FullTrackInfoUpdate fullTrackInfoListener;

        UpdateTrackInfoAsync(TrackInfoUpdate trackInfo, FullTrackInfoUpdate fullTrackInfo) {
            this.trackInfoListener = trackInfo;
            this.fullTrackInfoListener = fullTrackInfo;
        }


        @Override
        protected final Void doInBackground(final MPDStatus... params) {
            final int songPos = params[0].getSongPos();
            mCurrentTrack = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);

            if (mCurrentTrack != null) {
                if (mCurrentTrack.isStream()) {
                    if (mCurrentTrack.hasTitle()) {
                        mAlbum = mCurrentTrack.getName();
                        mTitle = mCurrentTrack.getTitle();
                    } else {
                        mTitle = mCurrentTrack.getName();
                    }

                    mArtist = mCurrentTrack.getArtist();
                    mAlbumInfo = new AlbumInfo(mArtist, mAlbum);
                } else {
                    mAlbum = mCurrentTrack.getAlbum();

                    mDate = Long.toString(mCurrentTrack.getDate());
                    if (mDate.isEmpty() || mDate.charAt(0) == '-') {
                        mDate = "";
                    } else {
                        mDate = " - " + mDate;
                    }

                    mTitle = mCurrentTrack.getTitle();
                    setArtist();
                    mAlbumInfo = new AlbumInfo(mCurrentTrack);
                }
                mHasCoverChanged = hasCoverChanged();

            }

            mLastAlbum = mAlbum;
            mLastArtist = mArtist;

            return null;
        }


        private boolean hasCoverChanged() {
            final boolean invalid = mArtist == null || mAlbum == null;
            return invalid || !mArtist.equals(mLastArtist) || !mAlbum.equals(mLastAlbum);
        }


        @Override
        protected final void onPostExecute(final Void result) {
            super.onPostExecute(result);

            final boolean sendCoverUpdate = mHasCoverChanged || mCurrentTrack == null
                    || mForceCoverUpdate;

            if (mCurrentTrack == null) {
                mTitle = MPDApplication.getInstance().getResources().getString(R.string.noSongInfo);
            }

            if (fullTrackInfoListener != null) {
                fullTrackInfoListener.onTrackInfoUpdate(mCurrentTrack, mAlbum, mArtist, mDate, mTitle);

                if (sendCoverUpdate) {
                    fullTrackInfoListener.onCoverUpdate(mAlbumInfo);
                }
            }

            if (trackInfoListener != null) {
                trackInfoListener.onTrackInfoUpdate(mAlbum, mTitle);

                if (sendCoverUpdate) {
                    trackInfoListener.onCoverUpdate();
                }
            }
        }


        private void setArtist() {
            final String albumArtist = mCurrentTrack.getAlbumArtist();

            mArtist = mCurrentTrack.getArtist();
            if (mArtist == null || mArtist.isEmpty()) {
                mArtist = albumArtist;
            } else if (albumArtist != null && !mArtist.toLowerCase().contains(albumArtist.toLowerCase())) {
                mArtist = albumArtist + " / " + mArtist;
            }
        }
    }
}
