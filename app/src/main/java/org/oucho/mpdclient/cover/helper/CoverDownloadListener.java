package org.oucho.mpdclient.cover.helper;

import org.oucho.mpdclient.helpers.AlbumInfo;

public interface CoverDownloadListener {

    void onCoverDownloadStarted(CoverInfo cover);

    void onCoverDownloaded(CoverInfo cover);

    void onCoverNotFound(CoverInfo coverInfo);

    void tagAlbumCover(AlbumInfo albumInfo);

}
