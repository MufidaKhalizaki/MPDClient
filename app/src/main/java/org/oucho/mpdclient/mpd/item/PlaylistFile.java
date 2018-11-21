package org.oucho.mpdclient.mpd.item;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlaylistFile extends Item implements FilesystemTreeEntry {

    private static final Pattern PLAYLIST_FILE_REGEXP = Pattern.compile("^.*/(.+)\\.(\\w+)$");

    private final String mFullPath;

    public PlaylistFile(final String path) {
        super();
        mFullPath = path;
    }

    @Override
    public String getFullPath() {
        return mFullPath;
    }

    @Override
    public String getName() {
        String result = "";

        if (mFullPath != null) {
            final Matcher matcher = PLAYLIST_FILE_REGEXP.matcher(mFullPath);
            if (matcher.matches()) {
                result = matcher.replaceAll("[$2] $1.$2");
            } else {
                result = mFullPath;
            }
        }
        return result;
    }
}
