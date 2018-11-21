package org.oucho.mpdclient.cover;

import android.graphics.Bitmap;
import android.graphics.Matrix;


class ImageFactory {

    static Bitmap resizedBitmap(Bitmap image) {
        // redimessione l'image par matrice (plus propre qu'un simple redimenssionnement)

        // Largeur max
        int logoSize = 600;

        if (image.getWidth() > logoSize) {

            int newWidth;
            int newHeight;

            if (image.getWidth() > image.getHeight()) {
                float aspectRatio = image.getWidth() / (float) image.getHeight();
                newWidth = logoSize;
                newHeight = Math.round(newWidth / aspectRatio);
            } else {
                float aspectRatio = image.getHeight() / (float) image.getWidth();
                newHeight = logoSize;
                newWidth = Math.round(newHeight / aspectRatio);
            }

            int width = image.getWidth();
            int height = image.getHeight();

            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;

            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);

            return Bitmap.createBitmap(image, 0, 0, width, height, matrix, false);

        } else {

            return image;

        }
    }

}
