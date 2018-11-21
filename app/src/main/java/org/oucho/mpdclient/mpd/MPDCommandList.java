package org.oucho.mpdclient.mpd;


public interface MPDCommandList {

    /**
     * MPD default TCP port.
     */
    int MAX_VOLUME = 100;

    int MIN_VOLUME = 0;

    String MPD_CMD_CONSUME = "consume";

    String MPD_CMD_COUNT = "count";

    String MPD_CMD_PLAYLIST_ADD = "playlistadd";  // supports file:///

    String MPD_CMD_SEARCH = "search";

    String MPD_CMD_IDLE = "idle";

    String MPD_CMD_FIND = "find";

    /** Added in MPD protocol 0.16.0 */
    String MPD_CMD_FIND_ADD = "findadd";

    String MPD_CMD_GROUP = "group";

    String MPD_CMD_LISTALLINFO = "listallinfo";

    String MPD_CMD_LISTPLAYLISTS = "listplaylists";

    String MPD_CMD_LIST_TAG = "list";

    char MPD_CMD_NEWLINE = '\n';

    String MPD_CMD_NEXT = "next";

    String MPD_CMD_PASSWORD = "password";

    String MPD_CMD_PAUSE = "pause";

    String MPD_CMD_PLAY = "play";

    String MPD_CMD_PLAYLIST_DEL = "playlistdelete";

    String MPD_CMD_PLAYLIST_INFO = "listplaylistinfo";

    String MPD_CMD_PLAYLIST_MOVE = "playlistmove";

    String MPD_CMD_PLAY_ID = "playid";

    String MPD_CMD_PREV = "previous";

    String MPD_CMD_RANDOM = "random";

    String MPD_CMD_REFRESH = "update";

    String MPD_CMD_REPEAT = "repeat";

    /** Added in MPD protocol 0.17.0. */
    String MPD_CMD_SEARCH_ADD_PLAYLIST = "searchaddpl";

    String MPD_CMD_SEEK_ID = "seekid";

    String MPD_CMD_SET_VOLUME = "setvol";

    String MPD_CMD_SINGLE = "single";

    String MPD_CMD_STATISTICS = "stats";

    String MPD_CMD_STATUS = "status";

    String MPD_CMD_STOP = "stop";

    /** Added in MPD protocol 0.20.0. */
    // deprecated commands
    String MPD_SEARCH_ALBUM = "album";

    String MPD_TAG_ALBUM = "album";

    String MPD_TAG_ALBUM_ARTIST = "albumartist";

    String MPD_TAG_ARTIST = "artist";

}
