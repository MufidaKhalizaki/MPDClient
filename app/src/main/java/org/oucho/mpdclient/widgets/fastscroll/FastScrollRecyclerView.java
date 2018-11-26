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

package org.oucho.mpdclient.widgets.fastscroll;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.oucho.mpdclient.R;
import org.oucho.mpdclient.widgets.fastscroll.FastScroller.SectionIndexer;

public class FastScrollRecyclerView extends RecyclerView {

    private FastScroller mFastScroller;

    public FastScrollRecyclerView(Context context) {
        super(context);
        layout(context, null);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        layout(context, attrs);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);

        if (adapter instanceof SectionIndexer) {
            setSectionIndexer((SectionIndexer) adapter);
        } else if (adapter == null) {
            setSectionIndexer(null);
        }
    }


    private void setSectionIndexer(SectionIndexer sectionIndexer) {
        mFastScroller.setSectionIndexer(sectionIndexer);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFastScroller.attachRecyclerView(this);

        ViewParent parent = getParent();

        if (parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            viewGroup.addView(mFastScroller);
            mFastScroller.setLayoutParams(viewGroup);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mFastScroller.detachRecyclerView();
        super.onDetachedFromWindow();
    }

    private void layout(Context context, AttributeSet attrs) {
        mFastScroller = new FastScroller(context, attrs);
        mFastScroller.setId(R.id.fastscroller);
    }
}
