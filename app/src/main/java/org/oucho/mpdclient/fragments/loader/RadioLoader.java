package org.oucho.mpdclient.fragments.loader;


import android.content.Context;
import android.util.Log;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.mpd.MPDCommand;
import org.oucho.mpdclient.mpd.exception.MPDException;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.mpd.item.Stream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RadioLoader extends BaseLoader<ArrayList<Stream>> implements MPDConfig {


    private static final String TAG = "RadioLoader";

    private static final String FILE_NAME = "streams.xml";

    public RadioLoader(Context context) {
        super(context);
    }


    @Override
    public ArrayList<Stream> loadInBackground() {

        ArrayList<Stream> mStreams = new ArrayList<>();

        List<Music> mpdStreams = null;
        int iterator = 0;

        /* Many users have playlist support disabled, no need for an exception. */
        if (MPDApplication.getInstance().oMPDAsyncHelper.oMPD.isCommandAvailable(MPDCommand.MPD_CMD_LISTPLAYLISTS)) {
            try {
                mpdStreams = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getSavedStreams();
            } catch (final IOException | MPDException ignore) {
                Log.w(TAG, "Failed to retrieve saved streams.");
            }
        } else {
            Log.w(TAG, "Streams fragment can't load streams, playlist support not enabled.");
            mpdStreams = Collections.emptyList();
        }

        if (null != mpdStreams) {
            for (final Music stream : mpdStreams) {
                mStreams.add(new Stream(stream.getName(), stream.getFullPath(), iterator));
                iterator++;
            }
        }

        final List<Stream> oldStreams = loadOldStreams();
        if (null != oldStreams) {
            for (final Stream stream : mStreams) {
                if (!mStreams.contains(stream)) {
                    try {
                        MPDApplication.getInstance().oMPDAsyncHelper.oMPD.saveStream(stream.getUrl(), stream.getName());
                    } catch (final IOException | MPDException e) {
                        Log.e(TAG, "Failed to save a stream.", e);
                    }

                    stream.setPos(mStreams.size());
                    mStreams.add(stream);
                }
            }
        }
        Collections.sort(mStreams);

        return mStreams;
    }


    private List<Stream> loadOldStreams() {
        AbstractList<Stream> oldStreams = null;

        try {
            final InputStream in = MPDApplication.getInstance().openFileInput(FILE_NAME);
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            final XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(in, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("stream".equals(xpp.getName())) {
                        if (null == oldStreams) {
                            oldStreams = new ArrayList<>();
                        }
                        oldStreams.add(new Stream(xpp.getAttributeValue("", "name"), xpp
                                .getAttributeValue("", "url"), -1));
                    }
                }
                eventType = xpp.next();
            }

            MPDApplication.getInstance().deleteFile(FILE_NAME);

        } catch (final FileNotFoundException ignored) {
        } catch (final Exception e) {
            Log.e(TAG, "Error while loading streams", e);
        }
        return oldStreams;
    }

}
