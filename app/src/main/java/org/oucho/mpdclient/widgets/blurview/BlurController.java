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

package org.oucho.mpdclient.widgets.blurview;

import android.graphics.Canvas;

interface BlurController {
    float DEFAULT_SCALE_FACTOR = 8f;
    float DEFAULT_BLUR_RADIUS = 16f;

    void destroy();

    void updateBlurViewSize();
    void onDrawEnd();
    void setBlurRadius(float radius);

    void drawBlurredContent(Canvas canvas);

    void setBlurAutoUpdate(boolean enabled);

    void setBlurAlgorithm(BlurAlgorithm algorithm);
}
