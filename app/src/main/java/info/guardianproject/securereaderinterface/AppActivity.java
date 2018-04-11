package info.guardianproject.securereaderinterface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchUIUtil;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinymission.rss.Feed;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import info.guardianproject.securereader.ModeSettings;
import info.guardianproject.securereader.Settings.ProxyType;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.adapters.DrawerMenuRecyclerViewAdapter;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.ui.LocaleHelper;
import info.guardianproject.securereaderinterface.ui.UIBroadcaster;
import info.guardianproject.securereaderinterface.uiutil.ActivitySwitcher;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.CheckableButton;

/**
 * The application base activity. Handles stuff like the drawer menu, etc.
 */
public class AppActivity extends LockableActivity implements DrawerMenuRecyclerViewAdapter.DrawerMenuCallbacks {
    public static final String LOGTAG = "AppActivity";
    public static final boolean LOGGING = false;

    public static final String EXTRA_USE_ROTATION_ANIMATION = "use_rotation_animation";

    private KillReceiver mKillReceiver;
    private SetUiLanguageReceiver mSetUiLanguageReceiver;
    private WipeReceiver mWipeReceiver;
    private int mIdMenu;
    private Menu mOptionsMenu;
    private boolean mDisplayHomeAsUp = false;

    /**
     * Set a timeout if you want the toolbar to hide itself automatically. Milliseconds.
     */
    private int mToolbarTimeout = 0; // default don't autohide

    /**
     * The main menu that will host all content links.
     */
    protected View mLeftSideMenu;
    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;

    private ArrayList<Runnable> mDeferredCommands = new ArrayList<>();
    protected Toolbar mToolbar;
    private LayoutInflater mInflater;
    private LayoutFactoryWrapper mLayoutFactoryWrapper;

    private boolean useRotationAnimation;


    protected void setMenuIdentifier(int idMenu) {
        mIdMenu = idMenu;
    }

    protected boolean useLeftSideMenu() {
        return true;
    }

    @Override
    public void setContentView(int layoutResID) {
        View view = LayoutInflater.from(this).inflate(R.layout.activity_base, null);
        ViewStub stub = (ViewStub) view.findViewById(R.id.content_root);
        if (stub != null) {
            stub.setLayoutResource(layoutResID);
            stub.inflate();
            super.setContentView(view);
        } else {
            super.setContentView(layoutResID);
        }
    }

    protected void setContentViewNoBase(int layoutResID) {
        super.setContentView(layoutResID);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        //App.getInstance().setCurrentLanguageInConfig(this);
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(this);
        mInflater = inflater.cloneInContext(this);
        mLayoutFactoryWrapper = new LayoutFactoryWrapper(inflater.getFactory());
        LayoutInflaterCompat.setFactory(mInflater, mLayoutFactoryWrapper);

        // TODO - unify these into one receiver
        mKillReceiver = new KillReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mKillReceiver, new IntentFilter(App.EXIT_BROADCAST_ACTION));
        mSetUiLanguageReceiver = new SetUiLanguageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mSetUiLanguageReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
        mWipeReceiver = new WipeReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mWipeReceiver, new IntentFilter(App.WIPE_BROADCAST_ACTION));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase, App.getSettings().uiLanguageCode(), App.getSettings().uiRegionCode()));
    }

    protected LayoutFactoryWrapper getLayoutFactoryWrapper() {
        return mLayoutFactoryWrapper;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            // If parent is AppBarLayout, listen to changes
            if (mToolbar.getParent() instanceof AppBarLayout) {
                ((AppBarLayout) mToolbar.getParent()).addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                    @Override
                    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                        if (verticalOffset == 0) {
                            mToolbar.removeCallbacks(mHideToolbarRunnable);
                            if (mToolbarTimeout != 0) {
                                mToolbar.postDelayed(mHideToolbarRunnable, mToolbarTimeout);
                            }
                        }
                    }
                });
            }
            setSupportActionBar(mToolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                setActionBarTitle(getSupportActionBar().getTitle());
            }
        }
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        if (mDrawerLayout != null) {
            if (!useLeftSideMenu()) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            } else {
                mMenuViewHolder = null;
                mLeftSideMenu = mDrawerLayout.findViewById(R.id.left_drawer);
                mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0) {
                    private boolean isClosed = true;

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        super.onDrawerClosed(drawerView);
                        runDeferredCommands();
                        isClosed = true;
                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {
                        super.onDrawerSlide(drawerView, slideOffset);
                        if (isClosed && slideOffset > 0) {
                            isClosed = false;
                            updateLeftSideMenu();
                            if (mMenuViewHolder != null) {
                                mMenuViewHolder.recyclerView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mMenuViewHolder.recyclerView.scrollToPosition(0);
                                    }
                                });
                            }
                        }
                    }
                };
                mDrawerLayout.addDrawerListener(mDrawerToggle);
                mDrawerToggle.syncState();
            }
        }

    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    public void setDisplayHomeAsUp(boolean displayHomeAsUp) {
        mDisplayHomeAsUp = displayHomeAsUp;
        if (displayHomeAsUp) {
            if (mDrawerToggle != null) {
                mDrawerToggle.setDrawerIndicatorEnabled(false);
            } else {
                if (getSupportActionBar() != null)
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if (mDrawerToggle != null) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            } else {
                if (getSupportActionBar() != null)
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    public void setActionBarTitle(CharSequence title) {
        if (mToolbar != null) {
            TextView tvTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
            if (tvTitle != null) {
                tvTitle.setText(title);
                tvTitle.setSelected(true);
            }
        }
    }

    public void setActionBarColor(int color, boolean showShadow) {
        if (mToolbar != null) {
            mToolbar.setBackgroundColor(color);
            AppBarLayout layout = (AppBarLayout)mToolbar.getParent();
            layout.setBackgroundColor(color);
            if (!showShadow) {
                ViewCompat.setElevation(layout, 0);
            } else {
                ViewCompat.setElevation(layout, UIHelpers.dpToPx(4, layout.getContext()));
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        if (mDrawerLayout != null) {
            if (useRotationAnimation) {
                useRotationAnimation = false;
                final View container = (View)mDrawerLayout.getParent();
                int type = container.getLayerType();
                container.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                ActivitySwitcher.animationIn(container,
                        getWindowManager(), new ActivitySwitcher.AnimationFinishedListener() {
                            @Override
                            public void onAnimationFinished() {
                                container.setLayerType(View.LAYER_TYPE_NONE, null);
                            }
                        });
            }
            ((ViewGroup) mDrawerLayout.getParent()).setVisibility(View.VISIBLE);
        }
        super.onResume();
        if (!isFinishing()) {
            invalidateOptionsMenu();
            updateLeftSideMenu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDrawerLayout != null) {
            ((ViewGroup) mDrawerLayout.getParent()).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getBooleanExtra(EXTRA_USE_ROTATION_ANIMATION, false)) {
            useRotationAnimation = true;
        }
    }

    private final class KillReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    private final class SetUiLanguageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Handler().post(new Runnable() {

                @Override
                public void run() {
                    recreateNowOrOnResume();
                }
            });
        }
    }

    private final class WipeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onWipe();
                }
            });
        }
    }

    /**
     * Override this to react to a wipe!
     */
    protected void onWipe() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mKillReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSetUiLanguageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWipeReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIdMenu == 0)
            return false;
        mOptionsMenu = menu;
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(mIdMenu, menu);

        if (shouldAddDefaultOptionsItems()) {
            getMenuInflater().inflate(R.menu.overflow_main, menu);
        }

        colorizeMenuItems();
        return true;
    }

    protected boolean shouldAddDefaultOptionsItems() {
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null && App.getSettings().autoLock() == 0) {
            // Remove lock app option
            MenuItem item = menu.findItem(R.id.menu_lock_app);
            if (item != null)
                item.setVisible(false);
        }
        if (menu != null && !App.getSettings().showPanicButton()) {
            // Remove panic button option
            MenuItem item = menu.findItem(R.id.menu_panic);
            if (item != null)
                item.setVisible(false);
        }

        // Allow app to respond
        App.getInstance().onPrepareOptionsMenu(this, menu);

        return super.onPrepareOptionsMenu(menu);
    }

    private void colorizeMenuItems() {
        if (mOptionsMenu == null || getSupportActionBar() == null)
            return;
        for (int i = 0; i < mOptionsMenu.size(); i++) {
            MenuItem item = mOptionsMenu.getItem(i);
            Drawable d = item.getIcon();
            if (d != null) {
                d.mutate();
                UIHelpers.colorizeDrawable(getSupportActionBar().getThemedContext(), R.attr.colorControlNormal, d);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDisplayHomeAsUp && item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            this.startActivity(intent);
            this.overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
            return true;
        }

        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item))
            return true;

        // Allow app to respond
        if (App.getInstance().onOptionsItemSelected(this, item.getItemId())) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_panic: {
                Intent intent = new Intent(this, PanicActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }

            case R.id.menu_media_downloads: {
                App.getInstance().onCommand(this, R.integer.command_downloads, null);
                return true;
            }

            case R.id.menu_manage_feeds: {
                App.getInstance().onCommand(this, R.integer.command_feed_add, null);
                return true;
            }

            case R.id.menu_preferences: {
                App.getInstance().onCommand(this, R.integer.command_settings, null);
                return true;
            }

            case R.id.menu_share_app: {
                App.getInstance().onCommand(this, R.integer.command_shareapp, null);
                return true;
            }

            case R.id.menu_lock_app: {
                App.getInstance().socialReader.lockApp();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class MenuViewHolder {
        //public CheckableButton btnTorStatus;
        //public CheckableButton btnShowPhotos;
        public RecyclerView recyclerView;
    }

    private MenuViewHolder mMenuViewHolder;

    protected void updateLeftSideMenu() {
        if (mLeftSideMenu != null) {
            createMenuViewHolder();
            new UpdateLeftSideMenuTask().execute();
        }
    }

    protected void refreshLeftSideMenu() {
        if (mLeftSideMenu != null && mMenuViewHolder != null) {
            if (mMenuViewHolder.recyclerView.getAdapter() != null) {
                mMenuViewHolder.recyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    private void createMenuViewHolder() {
        if (mMenuViewHolder == null) {
            mMenuViewHolder = new MenuViewHolder();
            View menuView = mLeftSideMenu;
            //mMenuViewHolder.btnTorStatus = (CheckableButton) menuView.findViewById(R.id.btnMenuTor);
            //mMenuViewHolder.btnShowPhotos = (CheckableButton) menuView.findViewById(R.id.btnMenuPhotos);
            mMenuViewHolder.recyclerView = (RecyclerView) menuView.findViewById(R.id.drawerMenuRecyclerView);
            mMenuViewHolder.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

            Class adapterClass = App.getInstance().getDrawerMenuAdapterClass();
            try {
                Constructor cons = adapterClass.getConstructor(Context.class, DrawerMenuRecyclerViewAdapter.DrawerMenuCallbacks.class);
                if (cons != null) {
                    Object instance = cons.newInstance(this, this);
                    if (instance instanceof RecyclerView.Adapter) {
                        mMenuViewHolder.recyclerView.setAdapter((RecyclerView.Adapter) instance);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Hookup events
            /*
            mMenuViewHolder.btnTorStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (App.getSettings().proxyType() != ProxyType.None) {
                        if (App.getInstance().socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_PROXY) {
                            mDrawerLayout.closeDrawers();
                            App.getInstance().socialReader.connectProxy(AppActivity.this);
                        }
                    }
                }
            });
*/
/*            mMenuViewHolder.btnShowPhotos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO

                    if (App.getSettings().getCurrentMode().syncMode() == ModeSettings.SyncMode.LetItFlow)
                        App.getSettings().getCurrentMode().setSyncMode(ModeSettings.SyncMode.BitWise);
                    else
                        App.getSettings().getCurrentMode().setSyncMode(ModeSettings.SyncMode.LetItFlow);
                    mMenuViewHolder.btnShowPhotos.setChecked(App.getSettings().getCurrentMode().syncMode() == ModeSettings.SyncMode.LetItFlow);

                }
            });*/
        }
    }

    class UpdateLeftSideMenuTask extends ThreadedTask<Void, Void, Void> {
        private ArrayList<Feed> feeds;
        private boolean isUsingProxy;
        private boolean isUsingPsiphon;
        private boolean showImages;
        private boolean isOnline;

        @Override
        protected Void doInBackground(Void... values) {
            createMenuViewHolder();
            feeds = App.getInstance().socialReader.getSubscribedFeedsList();
            isUsingProxy = App.getInstance().socialReader.useProxy();
            isUsingPsiphon = (App.getSettings().proxyType() == ProxyType.Psiphon);
            isOnline = App.getInstance().socialReader.isProxyOnline();
            showImages = (App.getInstance().socialReader.syncSettingsForCurrentNetwork().contains(ModeSettings.Sync.Media));

            // Update adapter
            RecyclerView.Adapter adapter = mMenuViewHolder.recyclerView.getAdapter();
            if (adapter instanceof DrawerMenuRecyclerViewAdapter) {
                ((DrawerMenuRecyclerViewAdapter)adapter).recalculateData();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            RecyclerView.Adapter adapter = mMenuViewHolder.recyclerView.getAdapter();
            if (adapter instanceof DrawerMenuRecyclerViewAdapter) {
                ((DrawerMenuRecyclerViewAdapter)adapter).update(feeds);
            } else {
                adapter.notifyDataSetChanged();
            }

            // Update TOR connection status
            //
/*            if (isUsingProxy) {
                mMenuViewHolder.btnTorStatus.setChecked(isOnline);
                if (isUsingPsiphon)
                    mMenuViewHolder.btnTorStatus.setText(isOnline ? R.string.menu_psiphon_connected : R.string.menu_psiphon_not_connected);
                else
                    mMenuViewHolder.btnTorStatus.setText(isOnline ? R.string.menu_tor_connected : R.string.menu_tor_not_connected);
                mMenuViewHolder.btnTorStatus.setCompoundDrawablesWithIntrinsicBounds(null,
                        ContextCompat.getDrawable(mMenuViewHolder.btnTorStatus.getContext(), isUsingPsiphon ? R.drawable.button_psiphon_icon_selector : R.drawable.button_tor_icon_selector), null, null);
            } else {
                mMenuViewHolder.btnTorStatus.setChecked(false);
                mMenuViewHolder.btnTorStatus.setText(R.string.menu_tor_not_connected);
                mMenuViewHolder.btnTorStatus.setCompoundDrawablesWithIntrinsicBounds(null,
                        ContextCompat.getDrawable(mMenuViewHolder.btnTorStatus.getContext(), R.drawable.button_tor_icon_selector), null, null);
            }
            mMenuViewHolder.btnShowPhotos.setChecked(showImages);*/
        }
    }

    @Override
    protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent data) {
        super.onUnlockedActivityResult(requestCode, resultCode, data);
        if (requestCode == UIBroadcaster.RequestCode.CREATE_CHAT_ACCOUNT.Value) {
            if (resultCode == RESULT_OK) {
                App.getSettings().setChatUsernamePasswordSet();
                // Then redirect somewhere?
            }
        }
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mInflater != null)
                return mInflater;
        }
        return super.getSystemService(name);
    }


    @Override
    public void runAfterMenuClose(Runnable runnable) {
        mDeferredCommands.add(runnable);
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            runDeferredCommands();
        }
    }

    private void runDeferredCommands() {
        for (Runnable r : mDeferredCommands)
            r.run();
        mDeferredCommands.clear();
    }

    /**
     * This is a shortcut to {@link App#getCurrentFeedSelection()} }
     *
     * @return the currently displayed feed (if any)
     */
    protected FeedSelection getCurrentFeedSelection() {
        return App.getInstance().getCurrentFeedSelection();
    }

    /**
     * This is a shortcut to {@link App#getCurrentFeed()} }
     *
     * @return the currently displayed feed (if any)
     */
    protected Feed getCurrentFeed() {
        return App.getInstance().getCurrentFeed();
    }

    /**
     * Get number of milliseconds before toolbar hides itself (0 = never hide)
     * @return Milliseconds before toolbar is hidden. 0 for no timeout.
     */
    public int getToolbarTimeout() {
        return mToolbarTimeout;
    }

    /**
     * Set number of milliseconds before toolbar hides itself (0 = never hide)
     * @param toolbarTimeout Milliseconds before toolbar is hidden. 0 for no timeout.
     */
    public void setToolbarTimeout(int toolbarTimeout) {
        mToolbarTimeout = toolbarTimeout;
        if (mToolbarTimeout == 0) {
            // Show the bar!
            mToolbar.removeCallbacks(mHideToolbarRunnable);
            ((AppBarLayout) mToolbar.getParent()).setExpanded(true, true);
        } else {
            mToolbar.postDelayed(mHideToolbarRunnable, mToolbarTimeout);
        }
    }

    private final Runnable mHideToolbarRunnable = new Runnable() {
        @Override
        public void run() {
            ((AppBarLayout) mToolbar.getParent()).setExpanded(false, true);
        }
    };
}
