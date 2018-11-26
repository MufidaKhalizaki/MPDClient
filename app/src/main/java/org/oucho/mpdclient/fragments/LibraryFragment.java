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

package org.oucho.mpdclient.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.oucho.mpdclient.MPDApplication;
import org.oucho.mpdclient.MPDConfig;
import org.oucho.mpdclient.MainActivity;
import org.oucho.mpdclient.R;

public class LibraryFragment extends Fragment implements MPDConfig {

    private boolean pagerVisible = false;

    private TabLayout tabLayout;

    private final Handler mHandler = new Handler();

    private static int currentTab = 0;

    private SectionsPagerAdapter mSectionsPagerAdapter = null;

    private boolean receiver = false;

    private ViewPager mViewPager = null;

    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        if (getViewPager() != null) {

            try {
                mViewPager.setAdapter(mSectionsPagerAdapter);
            } catch (NullPointerException ignore) {}
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.activity_main_content, container, false);

        MainActivity activity = (MainActivity) getActivity();

        mContext = activity.getApplicationContext();

        setViewPager(view.findViewById(R.id.pager));

        if (mSectionsPagerAdapter != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mViewPager.addOnPageChangeListener(mViewPagerChangeListener);
        }

        tabLayout = view.findViewById(R.id.tab_layout_indicator);
        tabLayout.setupWithViewPager(mViewPager, true);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (receiver) {
            mContext.unregisterReceiver(mServiceListener);
            receiver = false;
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if (!receiver) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(INTENT_BACK);

            mContext.registerReceiver(mServiceListener, filter);
            receiver = true;
        }
    }


    private final ViewPager.OnPageChangeListener mViewPagerChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(final int position) {}

        @Override
        public void onPageScrollStateChanged(int state) {

            if (!pagerVisible) {
                Animation anime = AnimationUtils.loadAnimation(mContext, R.anim.pager_fade_in);
                tabLayout.startAnimation(anime);
                tabLayout.setVisibility(View.VISIBLE);
                pagerVisible = true;
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            mHandler.removeCallbacks(removeDot);
            mHandler.postDelayed(removeDot, 500);

            setTitre(position);
        }

    };

    public static int getPosition() {
        return currentTab;
    }

    public static void setTitre(int position) {

        currentTab = position;
        String tri = mSettings.getString("tri", "");

        if (tri.equals("year")) {
            tri = MPDApplication.getInstance().getString(R.string.menu_sort_by_year);
        } else if (tri.equals("artist")) {
            tri = MPDApplication.getInstance().getString(R.string.menu_sort_by_artist);
        }

        if ( position == 0 ) {
            if (!MPDApplication.getFragmentAlbumSong()) {
                Intent intent = new Intent();
                intent.setAction("org.oucho.mdpclient.setTitle");
                intent.putExtra("Titre", MPDApplication.getInstance().getString(R.string.albums));
                intent.putExtra("Second", tri);
                MPDApplication.getInstance().sendBroadcast(intent);
            }

        } else if ( position == 1 ) {

            Intent intent = new Intent();
            intent.setAction("org.oucho.mdpclient.setTitle");
            intent.putExtra("Titre", MPDApplication.getInstance().getString(R.string.streams));
            intent.putExtra("Second", "");
            MPDApplication.getInstance().sendBroadcast(intent);

        } else if (position == 2) {

            Intent intent = new Intent();
            intent.setAction("org.oucho.mdpclient.setTitle");
            intent.putExtra("Titre", MPDApplication.getInstance().getString(R.string.playlist));
            intent.putExtra("Second", "");
            MPDApplication.getInstance().sendBroadcast(intent);
        }
    }

    private final Runnable removeDot = new Runnable() {
        @Override
        public void run() {

            if (pagerVisible) {
                Animation anime = AnimationUtils.loadAnimation(mContext, R.anim.pager_fade_out);
                tabLayout.startAnimation(anime);
                tabLayout.setVisibility(View.GONE);
                pagerVisible = false;
            }
        }
    };

    private void backToPrevious() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }


    private static class SectionsPagerAdapter extends FragmentPagerAdapter {

        private SectionsPagerAdapter(final FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            final Fragment fragment;

            switch (position) {
                case 0:
                    fragment = AlbumListFragment.newInstance();
                    break;
                case 1:
                    fragment = RadioFragment.newInstance();
                    break;
                case 2:
                    fragment = PlaylistFragment.newInstance();
                    break;
                default:
                    fragment = null;
                    break;
            }

            return fragment;
        }
    }

    private void setViewPager(ViewPager value) {
        mViewPager = value;
    }

    private ViewGroup getViewPager() {
        return mViewPager;
    }


    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            assert receiveIntent != null;
            if (receiveIntent.equals(INTENT_BACK)) {
                backToPrevious();
            }
        }
    };
}
