package org.oucho.mpdclient.widgets.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;


class BlockingBlurController implements BlurController {

    //Bitmap size should be divisible by 16 to meet stride requirement
    private static final int ROUNDING_VALUE = 16;

    private static final float scaleFactor = DEFAULT_SCALE_FACTOR;
    private float blurRadius = DEFAULT_BLUR_RADIUS;
    private float roundingWidthScaleFactor = 1f;
    private float roundingHeightScaleFactor = 1f;

    private BlurAlgorithm blurAlgorithm;
    private Canvas internalCanvas;

    private Bitmap internalBitmap;

    private final View blurView;
    private final ViewGroup rootView;
    private final Rect relativeViewBounds = new Rect();

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {

            if (!isMeDrawingNow) {
                updateBlur();
            }
            return true;
        }
    };

    //Used to distinct parent draw() calls from Controller's draw() calls
    private boolean isMeDrawingNow;

    //must be set from message queue
    private final Runnable onDrawEndTask = new Runnable() {
        @Override
        public void run() {

            isMeDrawingNow = false;
        }
    };

    private boolean shouldTryToOffsetCoords = true;


    BlockingBlurController(@NonNull View blurView, @NonNull ViewGroup rootView) {
        this.rootView = rootView;
        this.blurView = blurView;
        this.blurAlgorithm = new RenderScriptBlur(blurView.getContext(), true);

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        if (isZeroSized(measuredWidth, measuredHeight)) {
            deferBitmapCreation();
            return;
        }

        init(measuredWidth, measuredHeight);
    }

    private int downScaleSize(float value) {

        return (int) Math.ceil(value / scaleFactor);
    }

    private int roundSize(int value) {

        if (value % ROUNDING_VALUE == 0) {
            return value;
        }
        return value - (value % ROUNDING_VALUE) + ROUNDING_VALUE;
    }

    private void init(int measuredWidth, int measuredHeight) {

        if (isZeroSized(measuredWidth, measuredHeight)) {
            blurView.setWillNotDraw(true);
            setBlurAutoUpdate(false);
            return;
        }
        blurView.setWillNotDraw(false);
        allocateBitmap(measuredWidth, measuredHeight);
        internalCanvas = new Canvas(internalBitmap);
        setBlurAutoUpdate(true);
    }

    private boolean isZeroSized(int measuredWidth, int measuredHeight) {

        return downScaleSize(measuredHeight) == 0 || downScaleSize(measuredWidth) == 0;
    }

    private void updateBlur() {

        isMeDrawingNow = true;
        blurView.invalidate();
    }


    private void deferBitmapCreation() {

        blurView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                blurView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int measuredWidth = blurView.getMeasuredWidth();
                int measuredHeight = blurView.getMeasuredHeight();

                init(measuredWidth, measuredHeight);
            }

        });
    }

    private void allocateBitmap(int measuredWidth, int measuredHeight) {

        //downscale overlay (blurred) bitmap
        int nonRoundedScaledWidth = downScaleSize(measuredWidth);
        int nonRoundedScaledHeight = downScaleSize(measuredHeight);

        int scaledWidth = roundSize(nonRoundedScaledWidth);
        int scaledHeight = roundSize(nonRoundedScaledHeight);

        roundingHeightScaleFactor = (float) nonRoundedScaledHeight / scaledHeight;
        roundingWidthScaleFactor = (float) nonRoundedScaledWidth / scaledWidth;

        internalBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, blurAlgorithm.getSupportedBitmapConfig());
    }

    //draw starting from blurView's position
    private void setupInternalCanvasMatrix() {

        blurView.getDrawingRect(relativeViewBounds);

        if (shouldTryToOffsetCoords) {
            try {
                rootView.offsetDescendantRectToMyCoords(blurView, relativeViewBounds);
            } catch (IllegalArgumentException e) {
                shouldTryToOffsetCoords = false;
            }
        }

        float scaleFactorX = scaleFactor * roundingWidthScaleFactor;
        float scaleFactorY = scaleFactor * roundingHeightScaleFactor;

        float scaledLeftPosition = -relativeViewBounds.left / scaleFactorX;
        float scaledTopPosition = -relativeViewBounds.top / scaleFactorY;

        float scaledTranslationX = blurView.getTranslationX() / scaleFactorX;
        float scaledTranslationY = blurView.getTranslationY() / scaleFactorY;

        internalCanvas.translate(scaledLeftPosition - scaledTranslationX, scaledTopPosition - scaledTranslationY);
        internalCanvas.scale(1f / scaleFactorX, 1f / scaleFactorY);
    }

    private void drawUnderlyingViews() {
        rootView.draw(internalCanvas);
    }

    @Override
    public void drawBlurredContent(Canvas canvas) {

        isMeDrawingNow = true;

        internalCanvas.save();
        setupInternalCanvasMatrix();
        drawUnderlyingViews();
        internalCanvas.restore();

        blurAndSave();
        draw(canvas);
    }

    private void draw(Canvas canvas) {
        canvas.save();
        canvas.scale(scaleFactor * roundingWidthScaleFactor, scaleFactor * roundingHeightScaleFactor);
        canvas.drawBitmap(internalBitmap, 0, 0, null);
        canvas.restore();
    }

    @Override
    public void onDrawEnd() {
        blurView.post(onDrawEndTask);
    }

    private void blurAndSave() {
        internalBitmap = blurAlgorithm.blur(internalBitmap, blurRadius);
    }

    @Override
    public void updateBlurViewSize() {
        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        init(measuredWidth, measuredHeight);
    }

    @Override
    public void destroy() {
        setBlurAutoUpdate(false);
        blurAlgorithm.destroy();
        if (internalBitmap != null) {
            internalBitmap.recycle();
        }
    }

    @Override
    public void setBlurRadius(float radius) {
        this.blurRadius = radius;
    }

    @Override
    public void setBlurAlgorithm(BlurAlgorithm algorithm) {
        this.blurAlgorithm = algorithm;
    }

    @Override
    public void setBlurAutoUpdate(boolean enabled) {

        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        if (enabled) {
            blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
        }
    }
}
