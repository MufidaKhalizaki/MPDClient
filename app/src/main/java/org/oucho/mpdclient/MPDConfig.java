package org.oucho.mpdclient;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public interface MPDConfig {

    SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(MPDApplication.getInstance());

    String INTENT_BACK = "org.oucho.mpdclient.LOCK_ViewPager";
    String INTENT_QUEUE_BLUR = "org.oucho.mpdclient.QUEUE_BLUR";

    String INTENT_SET_MENU = "org.oucho.mpdclient.SET_MENU";


}
