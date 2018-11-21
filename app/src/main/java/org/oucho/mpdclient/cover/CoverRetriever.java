package org.oucho.mpdclient.cover;

import org.oucho.mpdclient.helpers.AlbumInfo;

public interface CoverRetriever {

    String[] getCoverUrl(AlbumInfo albumInfo);

    String getName();

    boolean isCoverLocal();
}
