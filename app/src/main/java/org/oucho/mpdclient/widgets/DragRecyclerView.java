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

package org.oucho.mpdclient.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.oucho.mpdclient.widgets.fastscroll.FastScrollRecyclerView;


public class DragRecyclerView extends FastScrollRecyclerView {
    private boolean mDragging = false;
    private boolean mAnimating = false;
    private int mCurrentTop;
    private int mCurrentBottom;
    private int mCurrentPosition;
    private int mAnimationDuration;
    private Drawable mHandleDrawable;
    private final Rect mHandleBounds = new Rect();
    private View mDraggedView;

    private OnItemMovedListener mOnItemMovedListener = null;

    public DragRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public DragRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DragRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setOnItemMovedListener(OnItemMovedListener listener) {
        mOnItemMovedListener = listener;
    }


    private void triggerListener(int oldPosition, int newPosition) {
        if (mOnItemMovedListener != null) {
            mOnItemMovedListener.onItemMoved(oldPosition, newPosition);
        }
    }

    private void init(Context context) {


        mAnimationDuration = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        addOnItemTouchListener(new ItemTouchListener());
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);

        if (mDragging || mAnimating) {
            mHandleDrawable.draw(c);
        }
    }


    public void startDrag(View childView) {

        Context context = getContext();
        mDragging = true;

        mDraggedView = childView;

        mCurrentTop = mDraggedView.getTop();
        mCurrentBottom = mDraggedView.getBottom();
        mCurrentPosition = getChildAdapterPosition(mDraggedView);

        Bitmap bitmap = Bitmap.createBitmap(mDraggedView.getWidth(), mDraggedView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mDraggedView.draw(canvas);
        mHandleDrawable = new BitmapDrawable(context.getResources(), bitmap);

        mHandleBounds.left = mDraggedView.getLeft();
        mHandleBounds.top = mCurrentTop;
        mHandleBounds.right = mHandleBounds.left + mDraggedView.getWidth();
        mHandleBounds.bottom = mHandleBounds.top + mDraggedView.getHeight();

        mHandleDrawable.setBounds(mHandleBounds);

        mDraggedView.setVisibility(View.INVISIBLE);
    }


    public interface OnItemMovedListener {
        void onItemMoved(int oldPosition, int newPosition);
    }




    private class ItemTouchListener implements RecyclerView.OnItemTouchListener {
        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean b) {
            // This constructor is intentionally empty, pourquoi ? parce que !
        }


        @Override
        public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent ev) {

            return mDragging;
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent ev) {
            if (!mDragging) {
                return;
            }
            float y = ev.getY();

            View v = recyclerView.findChildViewUnder((float) recyclerView.getWidth() / 2, y);
            if (v == null) {
                return;
            }
            int position = recyclerView.getChildAdapterPosition(v);

            switch (ev.getAction()) {
                case MotionEvent.ACTION_MOVE:

                    mHandleBounds.offsetTo(mHandleBounds.left, (int) (y - (float) mHandleBounds.height() / 2));
                    mHandleDrawable.setBounds(mHandleBounds);

                    if (mCurrentPosition != position
                            && (y < mCurrentTop || y > mCurrentBottom)) {

                        if (position > mCurrentPosition) {
                            for (int i = mCurrentPosition; i < position; i++) {
                                triggerListener(i, i + 1);
                            }
                        } else {
                            for (int i = mCurrentPosition; i > position; i--) {
                                triggerListener(i, i - 1);
                            }
                        }

                        mCurrentPosition = position;
                        mCurrentTop = v.getTop();
                        mCurrentBottom = v.getBottom();
                    }

                    invalidate();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:

                    ValueAnimator anim = ValueAnimator.ofInt(mHandleBounds.top, mCurrentTop).setDuration(mAnimationDuration);
                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mAnimating = true;
                            invalidate();
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mAnimating = false;
                            invalidate();
                            mDraggedView.setVisibility(View.VISIBLE);
                        }

                    });
                    anim.addUpdateListener(animation -> {
                        mHandleBounds.offsetTo(mHandleBounds.left, (Integer) animation.getAnimatedValue());
                        mHandleDrawable.setBounds(mHandleBounds);
                        invalidate();
                    });
                    anim.start();
                    if (mCurrentPosition != position) {


                        if (position > mCurrentPosition) {
                            for (int i = mCurrentPosition; i < position; i++) {
                                triggerListener(i, i + 1);
                            }
                        } else {
                            for (int i = mCurrentPosition; i > position; i--) {
                                triggerListener(i, i - 1);
                            }
                        }
                    }

                    mCurrentPosition = position;
                    mDragging = false;
                    invalidate();

                    break;
                default: //do nothing
                    break;
            }

        }




    }
}
