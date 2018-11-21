package org.oucho.mpdclient.widgets;


import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;


public class CustomGridLayoutManager extends GridLayoutManager {
    private static final float MILLISECONDS_PER_INCH = 40f;

    private final Context mContext;

    public CustomGridLayoutManager(Context context, int value) {
        super(context, value);

        mContext = context;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, final int position) {

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(mContext) {

            @Override
            public PointF computeScrollVectorForPosition
            (int targetPosition) {
                return CustomGridLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected float calculateSpeedPerPixel (DisplayMetrics displayMetrics) {
                return MILLISECONDS_PER_INCH/displayMetrics.densityDpi;
            }
        };

        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }
}

