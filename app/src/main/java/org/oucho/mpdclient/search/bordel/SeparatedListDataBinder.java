package org.oucho.mpdclient.search.bordel;

import android.view.View;

interface SeparatedListDataBinder {

    boolean isEnabled();

    void onDataBind(View targetView, Object item);
}
