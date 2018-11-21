package org.oucho.mpdclient.tools;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.StringRes;
import android.widget.Toast;

public final class Utils implements MPDConfig {


    private Utils() {
    }

    private static int calculateInSampleSize(final BitmapFactory.Options options, final int reqWidth, final int reqHeight) {

        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }


    public static Bitmap decodeSampledBitmapFromBytes(final byte[] bytes, final int reqWidth, final int reqHeight) {

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    public static void notifyUser(@StringRes final int resId, final Object... format) {
        final String formattedString = MPDApplication.getInstance().getResources().getString(resId, format);
        Toast.makeText(MPDApplication.getInstance(), formattedString, Toast.LENGTH_SHORT).show();
    }

    public static void notifyUser(@StringRes final int resId) {
        Toast.makeText(MPDApplication.getInstance(), resId, Toast.LENGTH_SHORT).show();
    }

    public static void notifyUser(final CharSequence message) {
        Toast.makeText(MPDApplication.getInstance(), message, Toast.LENGTH_SHORT).show();
    }

    public static Object[] toObjectArray(final Object... args) {
        return args;
    }

}
