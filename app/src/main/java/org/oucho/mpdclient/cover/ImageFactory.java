/*
 * MPDclient - Music Player Daemon (MPD) remote for android
 * Copyright (C) 2017  Old-Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
