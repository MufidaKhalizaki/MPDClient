package org.oucho.mpdclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.oucho.mpdclient.dialog.About;
import org.oucho.mpdclient.fragments.LibraryFragment;
import org.oucho.mpdclient.fragments.PlayerFragment;
import org.oucho.mpdclient.fragments.QueueFragment;
import org.oucho.mpdclient.helpers.MPDControl;
import org.oucho.mpdclient.helpers.UpdateTrackInfo;
import org.oucho.mpdclient.mpd.MPDStatus;
import org.oucho.mpdclient.mpd.event.StatusChangeListener;
import org.oucho.mpdclient.mpd.item.Music;
import org.oucho.mpdclient.search.SearchActivity;
import org.oucho.mpdclient.settings.SettingsActivity;
import org.oucho.mpdclient.widgets.ProgressBar;
import org.oucho.mpdclient.widgets.blurview.BlurView;
import org.oucho.mpdclient.widgets.blurview.RenderScriptBlur;

import static org.oucho.mpdclient.MPDConfig.INTENT_QUEUE_BLUR;
import static org.oucho.mpdclient.MPDConfig.INTENT_SET_MENU;


public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        OnBackStackChangedListener,
        PopupMenu.OnMenuItemClickListener,
        StatusChangeListener,
        UpdateTrackInfo.TrackInfoUpdate {

    private static final int SETTINGS = 5;
    private static final int CONNECT = 8;

    private static final String FRAGMENT_TAG_LIBRARY = "library";

    private static final String TAG = "MainActivity";

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;

    private FragmentManager mFragmentManager;

    private AppBarLayout mAppBarLayout;

    private BlurView queueBlurView;

    private ViewGroup rootView;

    private Context mContext;

    private static String currentSong;

    private static boolean queueView = false;

    private Menu menu;

    static {

        final StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
    }

    private static boolean playStatus;

    private RelativeLayout mPlaybar;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        setPlayStatus(false);

        SharedPreferences mSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

        setContentView(R.layout.activity_main);

        final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        getWindow().getDecorView().setSystemUiVisibility(mUIFlag);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();

        queueBlurView = findViewById(R.id.queueBlurView);

        mPlaybar = findViewById(R.id.playbar);

        mAppBarLayout = findViewById(R.id.appBar);
        mDrawerLayout = findViewById(R.id.drawer_layout);

        Resources res = mContext.getResources();
        float dp = res.getDimension(R.dimen.app_bar_elevation);
        mAppBarLayout.setElevation(dp);


        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
            toggle.setDrawerIndicatorEnabled(true);
            mDrawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        setNavigationMenu();

        String tri = mSettings.getString("tri", "");

        if (tri.equals(""))
            mSettings.edit().putString("tri", "a-z").apply();

        String titre = getString(R.string.app_name);
        setTitre(titre, null);

        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);

        initializeLibraryFragment();

        rootView = findViewById(R.id.toolbar_layout);

        initPlaybar();
    }


    private void afficheBlurView(boolean value) {

        if (value) {
            queueBlurView.setVisibility(View.VISIBLE);
           // setBlurView();

        } else {
            queueBlurView.setVisibility(View.GONE);
        }

        // evite la montée en charge du CPU
        new Handler().postDelayed(this::setBlurView, 200);

    }

    private void setBlurView() {
        final float radius = 5f;

        queueBlurView.setupWith(rootView)
                .blurAlgorithm(new RenderScriptBlur(MPDApplication.getInstance(), true))
                .blurRadius(radius);
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////// Playbar listeners //////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////

    private ImageButton mButtonPlayPause;
    private ImageButton mButtonPlayPause0;

    private ImageButton mShuffleB;
    private ImageButton mRepeatB;


    private TextView mSongArtist;

    private TextView mSongTitle;

    private ImageView mShuffle;

    private ImageView mRepeat;

    private ProgressBar mProgressBar;

    private RelativeLayout layoutA;
    private RelativeLayout layoutB;


    private final Handler mHandler = new Handler();

    private void initPlaybar() {

        layoutA = findViewById(R.id.track_info);
        layoutB = findViewById(R.id.track_info0);

        layoutA.setOnClickListener(v -> goPlayer());

        mSongTitle = findViewById(R.id.song_title);
        mSongArtist = findViewById(R.id.song_artist);

        ImageButton buttonPrev = findViewById(R.id.prev);
        buttonPrev.setOnClickListener(mButtonClickListener);

        ImageButton buttonPrev0 = findViewById(R.id.prev0);
        buttonPrev0.setOnClickListener(mButtonClickListener);

        ImageButton buttonNext = findViewById(R.id.next);
        buttonNext.setOnClickListener(mButtonClickListener);

        ImageButton buttonNext0 = findViewById(R.id.next0);
        buttonNext0.setOnClickListener(mButtonClickListener);

        mButtonPlayPause = findViewById(R.id.playpause);
        mButtonPlayPause.setOnClickListener(mButtonClickListener);


        mRepeat = findViewById(R.id.barA_repeat);
        mShuffle = findViewById(R.id.barA_shuffle);

        mRepeatB = findViewById(R.id.barB_repeat);
        mRepeatB.setOnClickListener(mButtonClickListener);

        mShuffleB = findViewById(R.id.barB_shuffle);
        mShuffleB.setOnClickListener(mButtonClickListener);

        mButtonPlayPause0 = findViewById(R.id.playpause0);
        mButtonPlayPause0.setOnClickListener(mButtonClickListener);

        mProgressBar = findViewById(R.id.progress_bar);

        if (MPDApplication.getInstance().updateTrackInfo == null) {
            MPDApplication.getInstance().updateTrackInfo = new UpdateTrackInfo();
        }
        MPDApplication.getInstance().updateTrackInfo.addCallback(this);
        MPDApplication.getInstance().oMPDAsyncHelper.addStatusChangeListener(this);

        //MPDApplication.getInstance().setActivity(this);

        mHandler.post(mUpdateProgressBar);
    }


    private final Runnable mUpdateProgressBar = new Runnable() {

        @Override
        public void run() {

            final MPDStatus status = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus();

            mProgressBar.setMax((int) status.getTotalTime());
            mProgressBar.setProgress((int) status.getElapsedTime());

            mHandler.postDelayed(mUpdateProgressBar, 250);
        }
    };

    @Override
    public void connectionStateChanged(final boolean connected, final boolean connectionLost) {

        if (connected) {
            MPDApplication.getInstance().updateTrackInfo.refresh(MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus(), true);

        }

        if (!connected) {
            mSongTitle.setText(R.string.notConnected);
            mSongArtist.setText("");
        }
    }


    @Override
    public final void onTrackInfoUpdate(final CharSequence artist, final CharSequence title) {

        setCurrentSong(String.valueOf(title));

        mSongArtist.setText(artist);
        mSongTitle.setText(title);

        Intent intent = new Intent();
        intent.setAction("org.oucho.mdpclient.onTrackInfoUpdate");
        intent.putExtra("Titre", title);
        intent.putExtra("Artiste", artist);

        mContext.sendBroadcast(intent);
    }


    @Override
    public void playlistChanged(final MPDStatus mpdStatus, final int oldPlaylistVersion) {

        final int songPos = mpdStatus.getSongPos();
        final Music currentSong = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);

        if (currentSong != null && currentSong.isStream() || mpdStatus.isState(MPDStatus.STATE_STOPPED))
            MPDApplication.getInstance().updateTrackInfo.refresh(mpdStatus, true);
    }


    @Override
    public void stateChanged(final MPDStatus mpdStatus, final int oldState) {

        MPDApplication.getInstance().updateTrackInfo.refresh(mpdStatus);

        updatePlayPauseButton(mpdStatus);
    }



    @Override
    public void trackChanged(final MPDStatus mpdStatus, final int oldTrack) {

        MPDApplication.getInstance().updateTrackInfo.refresh(mpdStatus);
    }



    private void updatePlayPauseButton(final MPDStatus status) {

        final int state = status.getState();

        if (state == MPDStatus.STATE_PLAYING) {
            setPlayStatus(true);
            mButtonPlayPause.setImageResource(R.drawable.ic_pause_circle_filled_grey_600_48dp);
            mButtonPlayPause0.setImageResource(R.drawable.ic_pause_circle_filled_grey_600_48dp);

        } else {
            setPlayStatus(true);
            mButtonPlayPause.setImageResource(R.drawable.ic_play_circle_filled_grey_600_48dp);
            mButtonPlayPause0.setImageResource(R.drawable.ic_play_circle_filled_grey_600_48dp);
        }
    }

    @Override
    public void randomChanged(final boolean random) {

        Log.i(TAG, "randomChanged");
        if (random) {
            mShuffle.setVisibility(View.VISIBLE);
            mShuffleB.setImageResource(R.drawable.ic_shuffle_grey_600_24dp);
        } else {
            mShuffle.setVisibility(View.GONE);
            mShuffleB.setImageResource(R.drawable.ic_shuffle_grey_400_24dp);
        }
    }

    @Override
    public void repeatChanged(final boolean repeating) {
        Log.i(TAG, "repeatChanged");

        if (repeating) {
            mRepeat.setVisibility(View.VISIBLE);
            mRepeatB.setImageResource(R.drawable.ic_repeat_grey_600_24dp);
        } else {
            mRepeat.setVisibility(View.GONE);
            mRepeatB.setImageResource(R.drawable.ic_repeat_grey_400_24dp);

        }
    }

    @Override
    public void volumeChanged(final MPDStatus mpdStatus, final int oldVolume) {
        //Toast.makeText(getApplicationContext(), "Volume", Toast.LENGTH_SHORT).show();
    }

    @Override
    public final void onCoverUpdate() {}

    @Override
    public void stickerChanged(final MPDStatus mpdStatus) {}

    @Override
    public void libraryStateChanged(final boolean updating, final boolean dbChanged) {}


    private final View.OnClickListener mButtonClickListener = v -> {

        // TODO repeat ne fonctionne pas
        MPDControl.run(v.getId());
    };

    ////////////////////////////////////////////////////////////////////////////////////////

    private boolean playbarB = false;


    private void goPlayer() {

        if (!playbarB) {

            float tailleBarre = getResources().getDimension(R.dimen.barre_lecture);

            Fragment fragment = PlayerFragment.newInstance();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction().addToBackStack("player");
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom);
            ft.replace(R.id.player, fragment);
            ft.commit();

            TranslateAnimation animate = new TranslateAnimation(0, 0, tailleBarre, 0);
            animate.setDuration(400);
            animate.setFillAfter(true);
            layoutB.startAnimation(animate);
            layoutB.setVisibility(View.VISIBLE);

            TranslateAnimation animate2 = new TranslateAnimation(0, 0, 0, -tailleBarre);
            animate2.setDuration(400);
            animate2.setFillAfter(true);
            layoutA.startAnimation(animate2);
            layoutA.setVisibility(View.GONE);

            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setInterpolator(new AccelerateInterpolator());
            fadeOut.setDuration(400);
            mProgressBar.setAnimation(fadeOut);
            mProgressBar.setVisibility(View.GONE);

            layoutA.setElevation(0);


            mHandler.postDelayed(() -> playbarB = true, 400);


        }

    }


    private void setNavigationMenu() {
        mNavigationView.inflateMenu(R.menu.navigation);
    }


    /* *********************************************************************************************
     * Navigation Drawer
     * ********************************************************************************************/

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.nav_settings:
                final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS);
                break;

            case R.id.nav_about:
                About dialog = new About();
                dialog.show(getSupportFragmentManager(), "about");
                break;

            case R.id.nav_exit:
                exit();
                break;

            default:
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void initializeLibraryFragment() {
        LibraryFragment fragment = (LibraryFragment) mFragmentManager.findFragmentByTag(FRAGMENT_TAG_LIBRARY);

        if (fragment == null) {
            fragment = new LibraryFragment();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.replace(R.id.library_root_frame, fragment, FRAGMENT_TAG_LIBRARY);
            ft.commit();
        }
    }

    @Override
    public void onBackPressed() {

        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);

        } else if (mFragmentManager.getBackStackEntryCount() > 0) {
            setQueueView(false);
            super.onBackPressed();

        } else {

            exit();
        }
    }

    private void exit() {
        MPDApplication.getInstance().unsetActivity(this);

        MPDApplication.getInstance().stopDisconnectScheduler();

        MPDApplication.getInstance().updateTrackInfo.removeAll();

        MPDApplication.getInstance().oMPDAsyncHelper.disconnect();

        MPDApplication.getInstance().oMPDAsyncHelper.removeStatusChangeListener(this);

        MPDApplication.getInstance().stopAlertDialog();

        finish();
    }

    @Override
    public void onBackStackChanged() {

        if (playbarB) {
            float tailleBarre = getResources().getDimension(R.dimen.barre_lecture);

            // TranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta)
            TranslateAnimation animate2 = new TranslateAnimation(0, 0, -tailleBarre, 0);
            animate2.setDuration(400);
            animate2.setFillAfter(true);
            layoutA.startAnimation(animate2);
            layoutA.setVisibility(View.VISIBLE);

            TranslateAnimation animate = new TranslateAnimation(0, 0, 0, tailleBarre);
            animate.setDuration(400);
            animate.setFillAfter(true);
            layoutB.startAnimation(animate);

            // bug GONE, reste actif si pas de clearAnimation
            mHandler.postDelayed(() -> {
                layoutB.clearAnimation();
                layoutB.setVisibility(View.GONE);
            }, 400);

            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new AccelerateInterpolator());
            fadeIn.setDuration(400);
            mProgressBar.setAnimation(fadeIn);
            mProgressBar.setVisibility(View.VISIBLE);

            layoutA.setElevation(5);
            playbarB = false;

        }
    }

    public boolean dispatchKeyEvent(KeyEvent event){
        int keyCode = event.getKeyCode();
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch (keyCode) {

                case KeyEvent.KEYCODE_VOLUME_UP:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_UP);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    MPDControl.run(MPDControl.ACTION_VOLUME_STEP_DOWN);
                    return true;

                default:
                    return super.dispatchKeyEvent(event);
            }
        }
        return super.dispatchKeyEvent(event);
    }


    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        return onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        this.menu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = true;

            switch (item.getItemId()) {
                case R.id.menu_search:

                    Intent i = new Intent(this, SearchActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(i);

                    break;
                case R.id.action_view_queue:
                    viewQueue();
                    break;
                case CONNECT:
                    MPDApplication.getInstance().connect();
                    break;
                default:
                    result = super.onOptionsItemSelected(item);
                    break;
            }

        return result;
    }


    private void viewQueue() {

        if (getQueueView()) {
            onBackPressed();
        } else {

            Fragment fragment = QueueFragment.newInstance();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction().addToBackStack("queue");
            ft.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
            ft.replace(R.id.queue, fragment);
            ft.commit();
            setQueueView(true);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        mHandler.removeCallbacks(mUpdateProgressBar);

        unregisterReceiver(mServiceListener);

    }


    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        // Reminder: Never disable buttons that are shown as actionbar actions here.
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        final MPDStatus status = MPDApplication.getInstance().oMPDAsyncHelper.oMPD.getStatus();

        updatePlayPauseButton(status);
        MPDApplication.getInstance().updateTrackInfo.refresh(status, true);

        mHandler.post(mUpdateProgressBar);


        IntentFilter filter = new IntentFilter();
        filter.addAction("org.oucho.mdpclient.setTitle");
        filter.addAction(INTENT_QUEUE_BLUR);
        filter.addAction(INTENT_SET_MENU);
        registerReceiver(mServiceListener, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
        MPDApplication.getInstance().setActivity(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        MPDApplication.getInstance().unsetActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit();
    }

    private final BroadcastReceiver mServiceListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();
            String titre = intent.getStringExtra("Titre");
            String second = intent.getStringExtra("Second");
            boolean elevation = intent.getBooleanExtra("elevation", true);
            boolean playbar = intent.getBooleanExtra("playbar", true);


            assert receiveIntent != null;
            if (receiveIntent.equals("org.oucho.mdpclient.setTitle")) {

                try {
                    if (!(titre.equals(" ") && second.equals("none"))) {
                        setTitre(titre, second);
                    }
                } catch (NullPointerException ignore) {

                }

                Resources res = mContext.getResources();

                if (elevation) {
                    float dp = res.getDimension(R.dimen.app_bar_elevation);
                    if (!MPDApplication.getFragmentAlbumSong())
                        mAppBarLayout.setElevation(dp);
                } else {
                    mAppBarLayout.setElevation(0);
                }

                if (playbar) {
                    float dp = res.getDimension(R.dimen.playbar_elevation);
                    mPlaybar.setElevation(dp);
                } else {
                    mPlaybar.setElevation(0);
                }
            }

            if (receiveIntent.equals(INTENT_QUEUE_BLUR)) {
                boolean value = intent.getBooleanExtra("blur", false);
                afficheBlurView(value);
            }

            if (receiveIntent.equals(INTENT_SET_MENU)) {
                boolean value = intent.getBooleanExtra("menu", false);
                menu.setGroupVisible(R.id.main_menu_group, value);
            }
        }
    };


    private void setTitre(String tite, String value) {

        Resources res = mContext.getResources();
        float dp = res.getDimension(R.dimen.app_bar_elevation);

        if (!MPDApplication.getFragmentAlbumSong())
            mAppBarLayout.setElevation(dp);

        try {
            if (value.equals("year")) {
                value = MPDApplication.getInstance().getString(R.string.menu_sort_by_year);
            } else if (value.equals("artist")) {
                value = MPDApplication.getInstance().getString(R.string.menu_sort_by_artist);
            }

        } catch (NullPointerException ignore) {}

        int couleurTitre = ContextCompat.getColor(mContext.getApplicationContext(), R.color.red_600);
        int couleurSecond = ContextCompat.getColor(mContext.getApplicationContext(), R.color.grey_400);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + tite + " " + " " + " </font> <small> <font color='" + couleurSecond + "'>"
                    + value + "</small></font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            setTitle(Html.fromHtml("<font color='" + couleurTitre + "'>" + tite + " " + " "
                    + " </font> <small> <font color='" + couleurSecond + "'>" + value + "</small></font>"));
        }

    }


    private static void setPlayStatus(boolean value) {
        playStatus = value;
    }

    public static boolean getPlayStatus() {
        return playStatus;
    }

    private static void setCurrentSong(String value) {
        currentSong = value;
    }

    public static String getCurrentSong() {
        return currentSong;
    }

    private static void setQueueView(boolean value) {
        queueView = value;
    }

    private static boolean getQueueView() {
        return queueView;
    }

}
