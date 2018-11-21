package org.oucho.mpdclient.search.bordel;

import org.oucho.mpdclient.R;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;


public class SeparatedListAdapter extends BaseAdapter {

    private static final int TYPE_CONTENT = 0;

    private static final int TYPE_SEPARATOR = 1;

    private final SeparatedListDataBinder mBinder; // The content -> view 'binding'

    private final LayoutInflater mInflater;

    private final List<?> mItems; // Content

    private final int mSeparatorLayoutId;

    private int mViewId = -1; // The view to be displayed

    private SeparatedListAdapter(final Context context, @LayoutRes final int separatorViewId, final SeparatedListDataBinder binder, final List<?> items) {
        super();
        mViewId = R.layout.search_list_item;
        mBinder = binder;
        mItems = Collections.unmodifiableList(items);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSeparatorLayoutId = separatorViewId;
    }

    public SeparatedListAdapter(final Context context, final SeparatedListDataBinder binder, final List<?> items) {
        this(context, R.layout.list_separator, binder, items);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(final int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return (long) position;
    }

    @Override
    public int getItemViewType(final int position) {
        final int viewType;

        if (mItems.get(position) instanceof String) {
            viewType = TYPE_SEPARATOR;
        } else {
            viewType = TYPE_CONTENT;
        }

        return viewType;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        final int itemType = getItemViewType(position);
        final View view;

        if (convertView == null) {
            if (itemType == TYPE_SEPARATOR) {
                view = mInflater.inflate(mSeparatorLayoutId, parent, false);
            } else {
                view = mInflater.inflate(mViewId, parent, false);
            }
        } else {
            view = convertView;
        }

        if (itemType == TYPE_SEPARATOR) {
            final CharSequence separator = (CharSequence) mItems.get(position);

            ((TextView) view.findViewById(R.id.separator_title)).setText(separator);
        } else {
            mBinder.onDataBind(view, mItems.get(position));
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public boolean isEnabled(final int position) {
        return getItemViewType(position) != TYPE_SEPARATOR && mBinder.isEnabled();
    }

}
