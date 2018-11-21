package org.oucho.mpdclient.cover.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.cover.CachedCover;
import org.oucho.mpdclient.cover.CoverRetriever;
import org.oucho.mpdclient.cover.LocalCover;
import org.oucho.mpdclient.cover.provider.DeezerCover;
import org.oucho.mpdclient.cover.provider.ItunesCover;
import org.oucho.mpdclient.helpers.AlbumInfo;
import org.oucho.mpdclient.tools.MultiMap;
import org.oucho.mpdclient.tools.Utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static android.text.TextUtils.isEmpty;


public final class CoverManager implements MPDConfig {


    private static final String PREFERENCE_DOWNLOAD_COVER = "enableDownloadCover";

    public static final String PREFERENCE_LOCALSERVER = "enableLocalCover";

    private static final Pattern BLOCK_IN_COMBINING_DIACRITICAL_MARKS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private static final String COVERS_FILE_NAME = "covers.bin";

    private static final String[] DISC_REFERENCES = {
            "disc", "cd", "disque"
    };

    private static final int MAX_REQUESTS = 20;

    private static final String TAG = "CoverManager";

    private static final Pattern TEXT_PATTERN = Pattern.compile("[^\\w .-]+");

    private static final String WRONG_COVERS_FILE_NAME = "wrong-covers.bin";

    private static CoverManager sInstance = null;

    private final ExecutorService mCacheCoverFetchExecutor = Executors.newFixedThreadPool(1);

    private final ExecutorService mCreateBitmapExecutor = mCacheCoverFetchExecutor;

    private final ThreadPoolExecutor mCoverFetchExecutor = getCoverFetchExecutor();

    private final MultiMap<CoverInfo, CoverDownloadListener> mHelpersByCoverInfo = new MultiMap<>();

    private final ExecutorService mPriorityCoverFetchExecutor = Executors.newFixedThreadPool(1);

    private final ExecutorService mRequestExecutor = Executors.newFixedThreadPool(1);

    private final BlockingDeque<CoverInfo> mRequests = new LinkedBlockingDeque<>();

    private final List<CoverInfo> mRunningRequests = Collections.synchronizedList(new ArrayList<CoverInfo>());

    private boolean mActive = true;

    private CoverRetriever[] mCoverRetrievers = null;

    private Map<String, String> mCoverUrlMap = null;

    private Set<String> mNotFoundAlbumKeys;

    private MultiMap<String, String> mWrongCoverUrlMap = null;

    private CoverManager() {
        super();
        mRequestExecutor.submit(new RequestProcessorTask());
        setCoverRetrieversFromPreferences();
        initializeCoverData();
    }


    private static URL buildURLForConnection(final String incomingRequest) {
        URL url = null;
        String request = null;

        if (incomingRequest != null) {
            request = incomingRequest.trim();
        }

        if (isEmpty(request)) {
            return null;
        }
        request = request.replace(" ", "%20");

        try {
            url = new URL(request);
        } catch (final MalformedURLException e) {
            Log.w(TAG, "Failed to parse the URL string for URL object generation.", e);
        }

        return url;
    }

    private static String cleanGetRequest(final CharSequence text) {
        String processedText = null;

        if (text != null) {
            processedText = TEXT_PATTERN.matcher(text).replaceAll(" ");

            processedText = Normalizer.normalize(processedText, Normalizer.Form.NFD);

            processedText = BLOCK_IN_COMBINING_DIACRITICAL_MARKS.matcher(processedText).replaceAll("");
        }

        return processedText;
    }


    private static boolean doesUrlExist(final HttpURLConnection connection) {
        int statusCode = 0;

        if (connection == null) {
            Log.d(TAG, "Cannot find out if URL exists with a null connection.");
            return false;
        }

        try {
            statusCode = connection.getResponseCode();
        } catch (final IOException e) {
            Log.e(TAG, "Failed to get a valid response code.", e);
        }

        return doesUrlExist(statusCode);
    }


    private static boolean doesUrlExist(final int statusCode) {
        final int temporaryRedirect = 307; /* No constant for 307 exists */

        return statusCode == HttpURLConnection.HTTP_OK || statusCode == temporaryRedirect || statusCode == HttpURLConnection.HTTP_MOVED_TEMP;
    }

    private static byte[] download(final String textUrl) {

        final URL url = buildURLForConnection(textUrl);
        final HttpURLConnection connection = getHttpConnection(url);
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;
        byte[] buffer = null;
        int len;

        if (!doesUrlExist(connection)) {
            return null;
        }

        try {
            assert connection != null;
            bis = new BufferedInputStream(connection.getInputStream(), 8192);
            baos = new ByteArrayOutputStream();
            buffer = new byte[1024];
            while ((len = bis.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            buffer = baos.toByteArray();
        } catch (final Exception e) {
            Log.e(TAG, "Failed to download cover.", e);

        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close the BufferedInputStream.", e);
                }
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Failed to close the BufferedArrayOutputStream.", e);
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
        return buffer;
    }

    private static byte[] getCoverBytes(final String[] coverUrls, final CoverInfo coverInfo) {

        byte[] coverBytes = null;

        for (final String url : coverUrls) {

            try {

                if (coverInfo.getState() == CoverInfo.STATE.CACHE_COVER_FETCH) {

                    coverBytes = readBytes(new URL("file://" + url).openStream());

                } else if (coverInfo.getState() == CoverInfo.STATE.WEB_COVER_FETCH) {
                    coverBytes = download(url);
                }

                if (coverBytes != null) {
                    break;
                }

            } catch (final Exception e) {
                Log.w(TAG, "Cover get bytes failure.", e);
            }
        }


        return coverBytes;
    }

    private static ThreadPoolExecutor getCoverFetchExecutor() {
        return new ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public static String getCoverFileName(final AlbumInfo albumInfo) {
        return albumInfo.getKey() + ".jpg";
    }

    private static String getCoverFolder() {

        final File cacheDir = MPDApplication.getInstance().getCacheDir();

        if (cacheDir == null) {
            return null;
        }
        return cacheDir.getAbsolutePath() + "/covers/";
    }


    private static HttpURLConnection getHttpConnection(final URL url) {
        HttpURLConnection connection = null;

        if (url == null) {
            return null;
        }

        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            Log.w(TAG, "Failed to execute cover get request.", e);
        }

        if (connection != null) {
            connection.setUseCaches(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
        }

        return connection;
    }

    public static synchronized CoverManager getInstance() {
        if (sInstance == null) {
            sInstance = new CoverManager();
        }
        return sInstance;
    }

    private static AlbumInfo getNormalizedAlbumInfo(final AlbumInfo albumInfo) {
        final String artist = cleanGetRequest(albumInfo.getArtist());
        String album = cleanGetRequest(albumInfo.getAlbum());
        album = removeDiscReference(album);

        return new AlbumInfo(album, artist, albumInfo.getPath(), albumInfo.getFilename());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> loadCovers() {
        Map<String, String> wrongCovers;
        ObjectInputStream objectInputStream = null;

        try {

            final File file = new File(getCoverFolder(), COVERS_FILE_NAME);

            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            wrongCovers = (Map<String, String>) objectInputStream.readObject();

        } catch (final Exception e) {
            Log.e(TAG, "Cannot load cover history file.", e);
            wrongCovers = new HashMap<>();

        } finally {

            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover history file.", e);

                }
            }
        }
        return wrongCovers;
    }

    @SuppressWarnings("unchecked")
    private static MultiMap<String, String> loadWrongCovers() {
        MultiMap<String, String> wrongCovers;
        ObjectInputStream objectInputStream = null;

        try {
            final File file = new File(getCoverFolder(), WRONG_COVERS_FILE_NAME);
            objectInputStream = new ObjectInputStream(new FileInputStream(file));
            wrongCovers = (MultiMap<String, String>) objectInputStream.readObject();
        } catch (final Exception e) {
            Log.e(TAG, "Cannot load cover blacklist.", e);
            wrongCovers = new MultiMap<>();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover blacklist.", e);

                }
            }
        }

        return wrongCovers;
    }

    private static byte[] readBytes(final InputStream inputStream) throws IOException {
        try {

            final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            final int bufferSize = 1024;
            final byte[] buffer = new byte[bufferSize];

            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            return byteBuffer.toByteArray();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }


    private static String removeDiscReference(final String album) {
        String cleanedAlbum = album.toLowerCase();
        for (final String discReference : DISC_REFERENCES) {
            cleanedAlbum = cleanedAlbum.replaceAll(discReference + "\\s*\\d+", " ");
        }
        return cleanedAlbum;
    }

    private static void saveCovers(final String fileName, final Object object) {
        ObjectOutputStream outputStream = null;
        try {
            final File file = new File(getCoverFolder(), fileName);
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(object);
        } catch (final Exception e) {
            Log.e(TAG, "Cannot save covers.", e);
        } finally {

            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close cover file.", e);

                }
            }
        }
    }


    void addCoverRequest(final CoverInfo coverInfo) {
        mRequests.add(coverInfo);
    }

    public void clear() {
        final CachedCover cachedCover = getCacheRetriever();
        if (cachedCover != null) {
            cachedCover.clear();
        }
        initializeCoverData();
    }

    public void clear(final AlbumInfo albumInfo) {

        final CachedCover cachedCover = getCacheRetriever();

        if (cachedCover != null) {
            cachedCover.delete(albumInfo);
        }

        //mCoverUrlMap.remove(albumInfo);
        mCoverUrlMap.remove(albumInfo.getKey());
        mWrongCoverUrlMap.remove(albumInfo.getKey());
        mNotFoundAlbumKeys.remove(albumInfo.getKey());
    }

    private static void setInstance() {
        sInstance = null;
    }

    @Override
    protected void finalize() throws Throwable {
        stopExecutors();
        setInstance();
        super.finalize();
    }

    private CachedCover getCacheRetriever() {
        for (final CoverRetriever retriever : mCoverRetrievers) {
            if (retriever instanceof CachedCover && retriever.isCoverLocal()) {
                return (CachedCover) retriever;
            }
        }
        return null;
    }

    private CoverInfo getExistingRequest(final CoverInfo coverInfo) {
        return mRunningRequests.get(mRunningRequests.indexOf(coverInfo));
    }

    private void initializeCoverData() {
        mWrongCoverUrlMap = loadWrongCovers();
        mCoverUrlMap = loadCovers();
        mNotFoundAlbumKeys = new HashSet<>();
    }


    private boolean isBlacklistedCoverUrl(final String url, final String albumKey) {

            return mWrongCoverUrlMap.get(albumKey).contains(url);
    }

    private boolean isLastCoverRetriever(final CoverRetriever retriever) {

        for (int r = 0; r < mCoverRetrievers.length; r++) {
            if (mCoverRetrievers[r].equals(retriever)) {
                if (r < mCoverRetrievers.length - 1) {
                    return false;
                }
            }
        }
        return true;
    }



    private void notifyListeners(CoverInfo coverInfo) {

        if (mHelpersByCoverInfo.containsKey(coverInfo)) {
            final Iterator<CoverDownloadListener> listenerIterator = mHelpersByCoverInfo.get(coverInfo).iterator();

            while (listenerIterator.hasNext()) {
                final CoverDownloadListener listener = listenerIterator.next();

                switch (coverInfo.getState()) {
                    case COVER_FOUND:
                        removeRequest(coverInfo);

                        listener.onCoverDownloaded(coverInfo);

                        // Do a copy for the other listeners (not to share
                        // bitmaps between views because of the recycling)
                        if (listenerIterator.hasNext()) {
                            coverInfo = new CoverInfo(coverInfo);
                            final Bitmap copyBitmap = coverInfo.getBitmap()[0].copy(coverInfo.getBitmap()[0].getConfig(), coverInfo.getBitmap()[0].isMutable());

                            coverInfo.setBitmap(new Bitmap[]{
                                    copyBitmap
                            });
                        }
                        break;
                    case COVER_NOT_FOUND:

                        if (!coverInfo.isRequestGivenUp() && !isEmpty(coverInfo.getPath())) {
                            mNotFoundAlbumKeys.add(coverInfo.getKey());
                        }

                        removeRequest(coverInfo);

                        listener.onCoverNotFound(coverInfo);
                        break;
                    case WEB_COVER_FETCH:
                        listener.onCoverDownloadStarted(coverInfo);
                        break;
                    default:
                        break;
                } // switch
            } //while
        } // if
    }

    private void removeRequest(final CoverInfo coverInfo) {
        mRunningRequests.remove(coverInfo);
        mHelpersByCoverInfo.remove(coverInfo);
    }

    private void saveCovers() {
        saveCovers(COVERS_FILE_NAME, mCoverUrlMap);
    }

    private void saveWrongCovers() {
        saveCovers(WRONG_COVERS_FILE_NAME, mWrongCoverUrlMap);
    }

    private void setCoverRetrievers(final List<CoverRetrievers> whichCoverRetrievers) {
        if (whichCoverRetrievers == null) {
            mCoverRetrievers = new CoverRetriever[0];
        }
        assert whichCoverRetrievers != null;
        mCoverRetrievers = new CoverRetriever[whichCoverRetrievers.size()];
        for (int i = 0; i < whichCoverRetrievers.size(); i++) {
            switch (whichCoverRetrievers.get(i)) {
                case CACHE:
                    mCoverRetrievers[i] = new CachedCover();
                    break;
                case LOCAL:
                    mCoverRetrievers[i] = new LocalCover();
                    break;
                case DEEZER:
                    mCoverRetrievers[i] = new DeezerCover();
                    break;
                case ITUNES:
                    mCoverRetrievers[i] = new ItunesCover();
                    break;
            }
        }
    }

    void setCoverRetrieversFromPreferences() {

        final List<CoverRetrievers> enabledRetrievers = new ArrayList<>();

        // There is a cover provider order, respect it.
        enabledRetrievers.add(CoverRetrievers.CACHE);


        if (mSettings.getBoolean(PREFERENCE_LOCALSERVER, false)) {
                enabledRetrievers.add(CoverRetrievers.LOCAL);
            }

            if (mSettings.getBoolean(PREFERENCE_DOWNLOAD_COVER, true)) {
                enabledRetrievers.add(CoverRetrievers.ITUNES);
                enabledRetrievers.add(CoverRetrievers.DEEZER);
            }

        setCoverRetrievers(enabledRetrievers);
    }

    private void stopExecutors() {
        try {

            mActive = false;
            mPriorityCoverFetchExecutor.shutdown();
            mRequestExecutor.shutdown();
            mCreateBitmapExecutor.shutdown();
            mCoverFetchExecutor.shutdown();
            mCacheCoverFetchExecutor.shutdown();
        } catch (final Exception ex) {
            Log.e(TAG, "Failed to shutdown cover executors.", ex);
        }

    }

    private enum CoverRetrievers {
        CACHE,
        LOCAL,
        DEEZER,
        ITUNES
    }

    private class CreateBitmapTask implements Runnable {

        private final CoverInfo mCoverInfo;

        private CreateBitmapTask(final CoverInfo coverInfo) {
            super();
            mCoverInfo = coverInfo;
        }

        @Override
        public void run() {

            final Bitmap[] bitmaps;

            if (mCoverInfo.getCoverRetriever().isCoverLocal()) {
                int maxSize = mCoverInfo.getCoverMaxSize();
                if (mCoverInfo.getCachedCoverMaxSize() != CoverInfo.MAX_SIZE) {
                    maxSize = mCoverInfo.getCachedCoverMaxSize();
                }
                if (maxSize == CoverInfo.MAX_SIZE) {

                    bitmaps = new Bitmap[]{
                            BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0, mCoverInfo.getCoverBytes().length)
                    };

                    mCoverInfo.setBitmap(bitmaps);
                } else {

                    bitmaps = new Bitmap[]{
                            Utils.decodeSampledBitmapFromBytes(mCoverInfo.getCoverBytes(), maxSize, maxSize)
                    };

                    mCoverInfo.setBitmap(bitmaps);
                }
            } else {
                final BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0, mCoverInfo.getCoverBytes().length, o);

                int scale = 1;
                if (mCoverInfo.getCoverMaxSize() != CoverInfo.MAX_SIZE || o.outHeight > mCoverInfo.getCoverMaxSize() || o.outWidth > mCoverInfo.getCoverMaxSize()) {

                    scale = (int) Math.pow(2.0, (double) (int) Math.round(Math.log((double) mCoverInfo.getCoverMaxSize() / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
                }

                o.inSampleSize = 1;
                o.inJustDecodeBounds = false;

                final Bitmap fullBmp = BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0, mCoverInfo.getCoverBytes().length, o);
                final Bitmap bmp;

                if (scale == 1) {
                    // This can cause some problem (a bitmap being freed will
                    // free both references)
                    // But the only use is to save it in the cache so it's okay.
                    bmp = fullBmp;
                } else {
                    o.inSampleSize = scale;
                    o.inJustDecodeBounds = false;
                    bmp = BitmapFactory.decodeByteArray(mCoverInfo.getCoverBytes(), 0, mCoverInfo.getCoverBytes().length, o);
                }

                bitmaps = new Bitmap[]{bmp, fullBmp};

                mCoverInfo.setBitmap(bitmaps);
                mCoverInfo.setCoverBytes(null);

                final CoverRetriever cacheRetriever;
                cacheRetriever = getCacheRetriever();

                if (cacheRetriever != null && !mCoverInfo.getCoverRetriever().equals(cacheRetriever)) {
                    // Save the fullsize bitmap
                    getCacheRetriever().save(mCoverInfo, fullBmp);

                    // Release the cover immediately if not used
                    if (!bitmaps[0].equals(bitmaps[1])) {
                        bitmaps[1].recycle();
                        bitmaps[1] = null;
                    }
                }
            }

            mRequests.addLast(mCoverInfo);
        }

    }

    private class FetchCoverTask implements Runnable {

        private final CoverInfo mCoverInfo;

        private FetchCoverTask(final CoverInfo coverInfo) {
            super();
            mCoverInfo = coverInfo;
        }

        @Override
        public void run() {
            String[] coverUrls;
            boolean remote;
            boolean local;
            boolean canStart = true;
            byte[] coverBytes;

            if (mCoverInfo.getState() != CoverInfo.STATE.WEB_COVER_FETCH || mCoverFetchExecutor.getQueue().size() < MAX_REQUESTS) {

                if (mCoverInfo.getCoverRetriever() != null) {

                    canStart = false;
                }

                for (final CoverRetriever coverRetriever : mCoverRetrievers) {
                    try {

                        if (coverRetriever == null) {
                            continue;
                        }

                        if (canStart) {
                            remote = mCoverInfo.getState() == CoverInfo.STATE.WEB_COVER_FETCH && !coverRetriever.isCoverLocal();
                            local = mCoverInfo.getState() == CoverInfo.STATE.CACHE_COVER_FETCH && coverRetriever.isCoverLocal();

                            if (remote || local) {

                                mCoverInfo.setCoverRetriever(coverRetriever);

                                coverUrls = coverRetriever.getCoverUrl(mCoverInfo);

                                if (!(coverUrls != null && coverUrls.length > 0) && remote && !(coverRetriever.getName().equals(LocalCover.RETRIEVER_NAME))) {
                                    final AlbumInfo normalizedAlbumInfo = getNormalizedAlbumInfo(mCoverInfo);

                                    if (!normalizedAlbumInfo.equals(mCoverInfo)) {

                                        coverUrls = coverRetriever.getCoverUrl(normalizedAlbumInfo);
                                    }
                                }

                                if (coverUrls != null && coverUrls.length > 0) {
                                    final List<String> wrongUrlsForCover = mWrongCoverUrlMap.get(mCoverInfo.getKey());

                                    if (wrongUrlsForCover == null || !isBlacklistedCoverUrl(coverUrls[0], mCoverInfo.getKey())) {

                                        coverBytes = getCoverBytes(coverUrls, mCoverInfo);

                                        if (coverBytes != null && coverBytes.length > 0) {
                                            if (!coverRetriever.isCoverLocal()) {
                                                mCoverUrlMap.put(mCoverInfo.getKey(), coverUrls[0]);
                                            }
                                            mCoverInfo.setCoverBytes(coverBytes);
                                            mRequests.addLast(mCoverInfo);
                                            return;
                                        }

                                    }
                                }

                            }

                        } else {

                            canStart = coverRetriever.equals(mCoverInfo.getCoverRetriever());
                        }

                    } catch (final Exception e) {
                        Log.e(TAG, "Fetch cover failure.", e);
                    }

                }

            } else {
                mCoverInfo.setRequestGivenUp();
                Log.w(TAG, "Too many requests, giving up this one : " + mCoverInfo.getAlbum());
            }

            mRequests.addLast(mCoverInfo);
        }

    }

    private class RequestProcessorTask implements Runnable {

        @SuppressWarnings("ConstantConditions")
        @Override
        public void run() {

            CoverInfo coverInfo;

            while (mActive) {

                try {
                    coverInfo = mRequests.take();

                    if (coverInfo == null || coverInfo.getListener() == null) {
                        return;
                    }


                    switch (coverInfo.getState()) {
                        case NEW:

                            mHelpersByCoverInfo.put(coverInfo, coverInfo.getListener());

                            if (mRunningRequests.contains(coverInfo)) {
                                final CoverInfo existingRequest = getExistingRequest(coverInfo);
                                existingRequest.setPriority(existingRequest.isPriority()
                                        || coverInfo.isPriority());
                                notifyListeners(existingRequest);
                                break;

                            } else {

                                if (!coverInfo.isValid() || mNotFoundAlbumKeys.contains(coverInfo.getKey())) {

                                    coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                    notifyListeners(coverInfo);
                                } else {
                                    mRunningRequests.add(coverInfo);
                                    coverInfo.setState(CoverInfo.STATE.CACHE_COVER_FETCH);
                                    mCacheCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                }
                                break;

                            }
                        case CACHE_COVER_FETCH:
                            if (coverInfo.getCoverBytes() == null || coverInfo.getCoverBytes().length == 0) {
                                coverInfo.setState(CoverInfo.STATE.WEB_COVER_FETCH);
                                notifyListeners(coverInfo);
                                if (coverInfo.isPriority()) {mPriorityCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                } else {
                                    mCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                                }
                                break;
                            } else {
                                coverInfo.setState(CoverInfo.STATE.CREATE_BITMAP);
                                mCreateBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
                                break;
                            }
                        case WEB_COVER_FETCH:
                            if (coverInfo.getCoverBytes() != null && coverInfo.getCoverBytes().length > 0) {
                                coverInfo.setState(CoverInfo.STATE.CREATE_BITMAP);
                                notifyListeners(coverInfo);
                                mCreateBitmapExecutor.submit(new CreateBitmapTask(coverInfo));
                                break;
                            } else {
                                coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                notifyListeners(coverInfo);
                                break;
                            }
                        case CREATE_BITMAP:
                            if (coverInfo.getBitmap() != null) {
                                coverInfo.setState(CoverInfo.STATE.COVER_FOUND);
                                notifyListeners(coverInfo);
                            } else if (isLastCoverRetriever(coverInfo.getCoverRetriever())) {

                                mCoverFetchExecutor.submit(new FetchCoverTask(coverInfo));
                            } else {
                                coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                                notifyListeners(coverInfo);
                            }
                            break;
                        default:
                            Log.e(TAG, "Unknown request : " + coverInfo);
                            coverInfo.setState(CoverInfo.STATE.COVER_NOT_FOUND);
                            notifyListeners(coverInfo);
                            break;
                    }

                    if (mRunningRequests.isEmpty()) {
                        saveCovers();
                        saveWrongCovers();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Cover request processing failure.", e);

                }
            }

        }
    }
}
