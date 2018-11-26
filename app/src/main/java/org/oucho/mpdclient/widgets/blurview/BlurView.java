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

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.oucho.mpdclient.R;


public class BlurView extends FrameLayout {

    private static final String TAG_LOG = "BlurView";

    private BlurController blurController = createStubController();

    @ColorInt
    private int overlayColor;

    public BlurView(Context context) {
        super(context);

        init();
    }

    public BlurView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public BlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {

        overlayColor =  ContextCompat.getColor(getContext(), R.color.colorBlurViewOverlay);

        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        //draw only on system's hardware accelerated canvas
        if (canvas.isHardwareAccelerated()) {
            blurController.drawBlurredContent(canvas);
            drawColorOverlay(canvas);
            super.draw(canvas);
        } else if (!isHardwareAccelerated()) {
            //if view is in a not hardware accelerated window, don't draw blur
            super.draw(canvas);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        blurController.updateBlurViewSize();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        blurController.onDrawEnd();
    }

    private void drawColorOverlay(Canvas canvas) {
        canvas.drawColor(overlayColor);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.w(TAG_LOG, "onDetachedFromWindow()");


        blurController.setBlurAutoUpdate(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.w(TAG_LOG, "onAttachedToWindow()");

        if (!isHardwareAccelerated()) {
            Log.w(TAG_LOG, "BlurView can't be used in not hardware-accelerated window!");
        } else {
            blurController.setBlurAutoUpdate(true);
        }
    }

    private void setBlurController(@NonNull BlurController blurController) {
        Log.w(TAG_LOG, "setBlurController()");

        this.blurController.destroy();
        this.blurController = blurController;
    }


    public ControllerSettings setupWith(@NonNull ViewGroup rootView) {

        Log.w(TAG_LOG, "setupWith()");

        BlurController blurController = new BlockingBlurController(this, rootView);
        setBlurController(blurController);

        if (!isHardwareAccelerated()) {

            blurController.setBlurAutoUpdate(false);
        }

        return new ControllerSettings(blurController);
    }

    public static class ControllerSettings {
        final BlurController blurController;

        ControllerSettings(BlurController blurController) {
            this.blurController = blurController;
        }


        @SuppressWarnings("UnusedReturnValue")
        public ControllerSettings blurRadius(float radius) {
            blurController.setBlurRadius(radius);
            return this;
        }

        public ControllerSettings blurAlgorithm(BlurAlgorithm algorithm) {
            blurController.setBlurAlgorithm(algorithm);
            return this;
        }

    }

    //Used in edit mode and in case if no BlurController was set
    private BlurController createStubController() {

        Log.w(TAG_LOG, "createStubController()");

        return new BlurController() {
            @Override
            public void drawBlurredContent(Canvas canvas) {}

            @Override
            public void updateBlurViewSize() {}

            @Override
            public void onDrawEnd() {}

            @Override
            public void setBlurRadius(float radius) {}

            @Override
            public void setBlurAlgorithm(BlurAlgorithm algorithm) {}

            @Override
            public void destroy() {}

            @Override
            public void setBlurAutoUpdate(boolean enabled) {}
        };
    }

}
