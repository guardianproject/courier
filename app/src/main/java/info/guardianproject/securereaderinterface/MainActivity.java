package info.guardianproject.securereaderinterface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.PopupWindowCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.facebook.device.yearclass.YearClass;
import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;

import java.util.ArrayList;

import info.guardianproject.securereader.FeedFetcher;
import info.guardianproject.securereader.ModeSettings;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SyncService;
import info.guardianproject.securereader.SyncStatus;
import info.guardianproject.securereaderinterface.adapters.ItemCursor;
import info.guardianproject.securereaderinterface.adapters.ItemCursorRecyclerViewAdapter;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.profiler.ProfilerWizardActivity;
import info.guardianproject.securereaderinterface.ui.ActionProviderShare;
import info.guardianproject.securereaderinterface.ui.ItemAdapterListener;
import info.guardianproject.securereaderinterface.ui.UIBroadcaster;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.widgets.AppearingRelativeLayout;

// HockeyApp SDK
//import net.hockeyapp.android.CrashManager;
//import net.hockeyapp.android.UpdateManager;

public class MainActivity extends ItemExpandActivity implements ItemAdapterListener
{
	public static String INTENT_EXTRA_SHOW_THIS_FEED_SELECTION = "info.guardianproject.securereaderinterface.showThisFeedSelection";
	public static String INTENT_EXTRA_SHOW_THIS_ITEM = "info.guardianproject.securereaderinterface.showThisItemId";

	public static final boolean LOGGING = false;
	public static final String LOGTAG = "MainActivity";
	
	// HockeyApp SDK
	//public static String APP_ID = "3fa04d8b0a135d7f3bf58026cb125866";

	private boolean mIsInitialized;
	private ItemViewModel mShowItemId;
	SocialReader socialReader;

	/*
	 * The action bar menu item for the "TAG" option. Only show this when a feed
	 * filter is set.
	 */
	MenuItem mMenuItemTag;
	boolean mShowTagMenuItem;
	MenuItem mMenuItemShare;
	MenuItem mMenuItemFeed;

	ActionProviderShare mShareActionProvider;

	SwipeRefreshLayout swipeRefreshLayout;
	RecyclerView itemsRecyclerView;
	AppearingRelativeLayout errorLayout;
	TextView tagResultsHeader;
	View tagResultsCloseButton;

	boolean mIsLoading;
	private FeedSelection mCurrentShownFeedSelection = FeedSelection.EMPTY;
	private boolean mBackShouldOpenAllFeeds;
	private ItemCursorRecyclerViewAdapter mAdapter;
	private UpdateErrorsTask mTaskUpdateErrors;
	private Snackbar snackbarOptimizationInfo;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	
		//getSupportActionBar().hide();

		setContentView(R.layout.activity_main);
		setMenuIdentifier(R.menu.activity_main);

		initializeViews();

		socialReader = ((App) getApplicationContext()).socialReader;
		mAdapter = new ItemCursorRecyclerViewAdapter(this);
		mAdapter.setListener(this);
		itemsRecyclerView.setAdapter(mAdapter);

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SyncService.BROADCAST_SYNCSERVICE_FEED_STATUS);
		intentFilter.addAction(SyncService.BROADCAST_SYNCSERVICE_FEED_ICON_STATUS);
		LocalBroadcastManager.getInstance(this).registerReceiver(mSyncEventReceiver, intentFilter);

		intentFilter = new IntentFilter();
		intentFilter.addAction(UIBroadcaster.BROADCAST_ACTION_FEED_SELECTED);
		intentFilter.addAction(UIBroadcaster.BROADCAST_ACTION_ITEM_FAVORITE_CHANGED);
		intentFilter.addAction(UIBroadcaster.BROADCAST_ACTION_FEED_RESYNC_REQUESTED);
		intentFilter.addAction(UIBroadcaster.BROADCAST_ACTION_COMMAND);
		LocalBroadcastManager.getInstance(this).registerReceiver(mCallbacksReceiver, intentFilter);

		// Saved what we were looking at?
		if (savedInstanceState != null && savedInstanceState.containsKey("FeedSelection"))
		{
			FeedSelection type = (FeedSelection) savedInstanceState.getSerializable("FeedSelection");
			UIBroadcaster.setFeedFilter(this, type);
		}
		else
		{
			UIBroadcaster.setFeedFilter(this, App.getInstance().getCurrentFeedSelection());
		}

		// HockeyApp SDK
		//checkForUpdates();
	}

	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncEventReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallbacksReceiver);
		super.onDestroy();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// If we are in the process of displaying the lock screen avoid extra work!
		if (!App.getInstance().isActivityLocked()) {
			if (!mIsInitialized) {
				mIsInitialized = true;
				UIBroadcaster.setFeedFilter(this, App.getInstance().getCurrentFeedSelection());
				//getSupportActionBar().show();
			}
		}

		// Called with flags of which item to show?
		Intent intent = getIntent();
		FeedSelection feedSelection = null;
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_ITEM)) {
			this.mShowItemId = (ItemViewModel) intent.getSerializableExtra(INTENT_EXTRA_SHOW_THIS_ITEM);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_ITEM);
		}
		if (intent.hasExtra(INTENT_EXTRA_SHOW_THIS_FEED_SELECTION)) {
			feedSelection = (FeedSelection) intent.getSerializableExtra(INTENT_EXTRA_SHOW_THIS_FEED_SELECTION);
			getIntent().removeExtra(INTENT_EXTRA_SHOW_THIS_FEED_SELECTION);
		} else if (socialReader.getDefaultFeedId() >= 0) {
			feedSelection = new FeedSelection(socialReader.getDefaultFeedId());
		}

		if (feedSelection != null)
		{
			if (LOGGING) 
				Log.d(LOGTAG, "INTENT_EXTRA_SHOW_THIS_TYPE was set, show type " + feedSelection.toString());
			
			UIBroadcaster.setFeedFilter(this, feedSelection);
		}
		// HockeyApp SDK
		//checkForCrashes();
		if (!App.getInstance().isActivityLocked()) {
			if (socialReader.getFeedsList().size() > 0)
			{
				refreshList();
			}
		}

		if (!App.getInstance().isActivityLocked() && !App.getSettings().hasShownOptimizationInfo()) {
			itemsRecyclerView.postDelayed(new Runnable() {
				@Override
				public void run() {
					snackbarOptimizationInfo = Snackbar.make(itemsRecyclerView, R.string.optimization_info_text, Snackbar.LENGTH_INDEFINITE);
					snackbarOptimizationInfo.setAction(R.string.optimization_info_more, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(MainActivity.this, ProfilerWizardActivity.class);
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
							startActivity(intent);
						}
					});
					snackbarOptimizationInfo.setActionTextColor(Color.YELLOW);
					snackbarOptimizationInfo.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
						@Override
						public void onDismissed(Snackbar transientBottomBar, int event) {
							super.onDismissed(transientBottomBar, event);
							App.getSettings().setHasShownOptimizationInfo(true);
						}
					});
					snackbarOptimizationInfo.show();
				}
			}, 35000);
		}
		if (snackbarOptimizationInfo != null) {
			snackbarOptimizationInfo.dismiss();
			snackbarOptimizationInfo = null;
		}
	}
	
	// HockeyApp SDK
	/*private void checkForCrashes() {
		CrashManager.register(this, APP_ID);
	}*/	
	
	// HockeyApp SDK
	/*private void checkForUpdates() {
		//Remove this for store builds!
		UpdateManager.register(this, APP_ID);
	}*/

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	private void initializeViews() {
		swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
		swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				swipeRefreshLayout.setRefreshing(false);
				FeedSelection feedSelection = getCurrentFeedSelection();
				if (feedSelection == FeedSelection.SHARED || feedSelection == FeedSelection.FAVORITES || feedSelection == FeedSelection.EMPTY)
					refreshList();
				else
					onResync(feedSelection, true);
			}
		});
		itemsRecyclerView = (RecyclerView) findViewById(R.id.itemsRecyclerView);
		itemsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
		itemsRecyclerView.setAdapter(mAdapter);
		errorLayout = (AppearingRelativeLayout) findViewById(R.id.frameError);
		if (errorLayout != null) {
			errorLayout.setVisibility(View.GONE);
			errorLayout.findViewById(R.id.ivErrorClose).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (errorLayout != null) {
						errorLayout.collapse();
					}
				}
			});
		}

		tagResultsHeader = (TextView) findViewById(R.id.tvTagResults);
		tagResultsHeader.setVisibility(View.GONE);
		tagResultsCloseButton = findViewById(R.id.btnCloseTagSearch);
		tagResultsCloseButton.setVisibility(View.GONE);
		tagResultsCloseButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Clear tag filter
				setTagFilter(null);
			}
		});

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		
		// save index and top position
		//
		Point storedScrollPos = null;
		LinearLayoutManager llm = (LinearLayoutManager) itemsRecyclerView.getLayoutManager();
		int pos = llm.findFirstVisibleItemPosition();
		if (pos != RecyclerView.NO_POSITION) {
			RecyclerView.ViewHolder vh = itemsRecyclerView.findViewHolderForAdapterPosition(pos);
			if (vh != null) {
				storedScrollPos = new Point(pos, llm.getDecoratedTop(vh.itemView));
			}
		}

		// Change the content and re-initialize the views
		setContentView(R.layout.activity_main);
		initializeViews();

		// Restore scroll position, if previously set
		if (storedScrollPos != null) {
			llm = (LinearLayoutManager) itemsRecyclerView.getLayoutManager(); // Get LLM from new recyclerview
			llm.scrollToPositionWithOffset(storedScrollPos.x, storedScrollPos.y);
		}
	}
	
	private void syncSpinnerToCurrentItem()
	{
		if (getCurrentFeedSelection() == FeedSelection.ALL_FEEDS)
			setActionBarTitle(getString(R.string.feed_filter_all_feeds));
		else if (getCurrentFeedSelection() == FeedSelection.POPULAR)
			setActionBarTitle(getString(R.string.feed_filter_popular));
		else if (getCurrentFeedSelection() == FeedSelection.SHARED)
			setActionBarTitle(getString(R.string.feed_filter_shared_stories));
		else if (getCurrentFeedSelection() == FeedSelection.FAVORITES)
			setActionBarTitle(getString(R.string.feed_filter_favorites));
		else if (getCurrentFeed() != null)
			setActionBarTitle(getCurrentFeed().getTitle());
		else
			setActionBarTitle(getString(R.string.feed_filter_all_feeds));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean ret = super.onCreateOptionsMenu(menu);

		// Find the tag menu item. Only to be shown when feed filter is set!
		mMenuItemTag = menu.findItem(R.id.menu_tag);
		mMenuItemTag.setVisible(mShowTagMenuItem);

		// Locate MenuItem with ShareActionProvider
		mMenuItemShare = menu.findItem(R.id.menu_share);
		if (mMenuItemShare != null && getSupportActionBar() != null)
		{
			mShareActionProvider = new ActionProviderShare(getSupportActionBar().getThemedContext());
			mShareActionProvider.setFeed(getCurrentFeed());
			MenuItemCompat.setActionProvider(mMenuItemShare, mShareActionProvider);
		}
		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.menu_tag:
		{
			View view = mToolbar.findViewById(item.getItemId());
			showTagSearchPopup(view);
			return true;
		}
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("NewApi")
	private void setTagItemVisible(boolean bVisible)
	{
		mShowTagMenuItem = bVisible;
		if (mMenuItemTag != null)
		{
			mMenuItemTag.setVisible(mShowTagMenuItem);
			if (Build.VERSION.SDK_INT >= 11)
				invalidateOptionsMenu();
		}
	}

	private void showTagSearchPopup(View anchorView)
	{
		try
		{
			LayoutInflater inflater = getLayoutInflater();
			final PopupWindow mMenuPopup = new PopupWindow(inflater.inflate(R.layout.search_by_tag, null, false), this.itemsRecyclerView.getWidth(),
					this.itemsRecyclerView.getHeight(), true);

			ListView lvTags = (ListView) mMenuPopup.getContentView().findViewById(R.id.lvTags);

			String[] rgTags = new String[0];
			// rgTags[0] = "#one";
			// rgTags[1] = "#two";
			// rgTags[2] = "#three";
			// rgTags[3] = "#four";

			ListAdapter adapter = new ArrayAdapter<>(this, R.layout.search_by_tag_item, R.id.tvTag, rgTags);
			lvTags.setAdapter(adapter);
			lvTags.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id)
				{
					String tag = (String) arg0.getAdapter().getItem(position);
					setTagFilter(tag);
					mMenuPopup.dismiss();
				}
			});

			EditText editTag = (EditText) mMenuPopup.getContentView().findViewById(R.id.editTag);
			editTag.setOnEditorActionListener(new OnEditorActionListener()
			{
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
				{
					if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_SEARCH)
					{
						setTagFilter(v.getText().toString());
						mMenuPopup.dismiss();
						return true;
					}
					return false;
				}
			});

			mMenuPopup.setOutsideTouchable(true);
			mMenuPopup.setBackgroundDrawable(new ColorDrawable(0x80ffffff));
			PopupWindowCompat.showAsDropDown(mMenuPopup, anchorView, 0, 0, Gravity.TOP);
			mMenuPopup.getContentView().setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					mMenuPopup.dismiss();
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void onResync(FeedSelection feedSelection, boolean showLoadingSpinner)
	{
		if (socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_PROXY)
		{
			socialReader.connectProxy(this);
		}

		if (socialReader.isOnline() == SocialReader.ONLINE)
		{
			setIsLoading(showLoadingSpinner);
			boolean isSyncing;
			if (feedSelection == FeedSelection.ALL_FEEDS)
				isSyncing = socialReader.manualSyncSubscribedFeeds(mManualSyncCallback);
			else
				isSyncing = socialReader.manualSyncFeed(App.getInstance().getFeedById(feedSelection.Value), mManualSyncCallback);
			if (!isSyncing && showLoadingSpinner) {
				// No resync started, maybe offline?
				setIsLoading(false);
			}
		}
	}

	@Override
	protected void configureActionBarForFullscreen(boolean fullscreen)
	{
		super.configureActionBarForFullscreen(fullscreen);
		if (mMenuItemFeed != null)
			mMenuItemFeed.setVisible(!fullscreen);
		if (mMenuItemShare != null)
			mMenuItemShare.setVisible(!fullscreen);
		if (mMenuItemTag != null)
			mMenuItemTag.setVisible(!fullscreen && mShowTagMenuItem);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowCustomEnabled(!fullscreen);
		}
		setDisplayHomeAsUp(fullscreen);
	}

	private void setIsLoading(boolean isLoading)
	{
		// Are we sure we are not loading anything?
		//if (!isLoading && mAdapter != null && mAdapter.getItemCount() == 0) {
			if (getCurrentFeedSelection() == FeedSelection.ALL_FEEDS ||
					getCurrentFeedSelection().isNormalFeed()) {
				if (socialReader.getSyncService() != null) {
					isLoading = socialReader.getSyncService().anyFeedSyncing();
				}
			}
		//}
//					for (SyncService.SyncTask task : socialReader.getSyncService().syncList) {
//						if (task.type == SyncService.SyncTask.TYPE_FEED) {
//							if (task.status == SyncService.SyncTask.CREATED ||
//									task.status == SyncService.SyncTask.QUEUED ||
//									task.status == SyncService.SyncTask.STARTED) {
//								if (getCurrentFeedSelection() == FeedSelection.ALL_FEEDS ||
//										(task.feed != null && getCurrentFeedSelection().equals(task.feed.getDatabaseId()))) {
//									isLoading = true; // No, one is actually syncing!
//									break;
//								}
//							}
//						}
//					}
//				}
//			}
//		}

		// Is all initialized?
/*		if (!isLoading &&
				getCurrentFeedSelection() == FeedSelection.ALL_FEEDS &&
				itemsRecyclerView != null && mAdapter != null && mAdapter.getItemCount() == 0) {
			ArrayList<Feed> allFeeds = socialReader.getFeedsList();
			if (allFeeds == null || allFeeds.isEmpty() || allFeeds.get(0).getNetworkPullDate() == null) {
				// No feeds, so still loading!
				isLoading = true;
			}
		}*/

		mIsLoading = isLoading;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					swipeRefreshLayout.setRefreshing(mIsLoading);
				}
			});
		}
		refreshLeftSideMenu();
	}

	private void showError(String error) {
		/* Disable the error panel for now
		if (errorLayout != null) {
			TextView tv = (TextView) errorLayout.findViewById(R.id.tvError);
			if (tv != null)
				tv.setText(error);
			if (TextUtils.isEmpty(error)) {
				errorLayout.collapse();
			} else {
				errorLayout.expand();
			}
		}*/
	}

	@SuppressLint({ "InlinedApi", "NewApi" })
	private void updateList(boolean isUpdate)
	{
		if (!isUpdate)
			setIsLoading(true);
		mAdapter.setFeedSelection(getCurrentFeedSelection(), isUpdate);
		if (!isUpdate)
			itemsRecyclerView.scrollToPosition(0);
		syncSpinnerToCurrentItem();
		if (mShareActionProvider != null)
			mShareActionProvider.setFeed(getCurrentFeed());
		updateErrors();
	}

	private void refreshList()
	{
		updateList(true);
	}
	
	private void refreshListIfCurrent(Feed feed)
	{
		if (getCurrentFeedSelection() == FeedSelection.ALL_FEEDS)
		{
			refreshList();
		}
		else if (getCurrentFeedSelection().isNormalFeed() && getCurrentFeedSelection().equals(feed.getDatabaseId()))
		{
			//TODO - this seems a little ugly
			App.getInstance().updateCurrentFeed(feed);
			refreshList();
		}
	}
	
	private void checkShowStoryFullScreen()
	{
		if (mShowItemId != null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Loaded feed and INTENT_EXTRA_SHOW_THIS_ITEM was set to " + mShowItemId.item.getTitle() + ". Try to show it");
			openStoryFullscreen(itemsRecyclerView, mShowItemId);
			mShowItemId = null;
		}
	}

	//TODO
/*	@Override
	public void onHeaderCreated(View headerView, int resIdHeader)
	{
		if (resIdHeader == R.layout.story_list_hint_proxy)
		{
			mProxyView = (StoryListHintProxyView) headerView;
			updateProxyView();
		}
	}*/

//TODO
/*	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		// Probably opening a popup (Feed Spinner). Remember what sync mode was
		// set to when we open.
		if (!hasFocus)
		{
			mCurrentSyncMode = App.getSettings().getCurrentMode().syncMode();
		}
		else
		{
			if (mCurrentSyncMode != App.getSettings().getCurrentMode().syncMode())
			{
				mCurrentSyncMode = App.getSettings().getCurrentMode().syncMode();
				refreshList();
			}
		}
	}*/

	@Override
	protected void onWipe()
	{
		super.onWipe();
		UIBroadcaster.setFeedFilter(this, FeedSelection.EMPTY);
	}

	@Override
	protected void onUnlocked() {
		super.onUnlocked();
		socialReader = ((App) getApplicationContext()).socialReader;
		UIBroadcaster.setFeedFilter(this, App.getInstance().getCurrentFeedSelection());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		// Save what we are currently looking at, so we can restore that later
		//
		outState.putSerializable("FeedSelection", App.getInstance().getCurrentFeedSelection());
	}
	
	@Override
	public void onBackPressed()
	{
		if (mBackShouldOpenAllFeeds)
		{
			mBackShouldOpenAllFeeds = false;
			UIBroadcaster.setFeedFilter(this, FeedSelection.ALL_FEEDS);
		}
		else
		{
			super.onBackPressed();
		}
	}

	private BroadcastReceiver mSyncEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getAction()) {
				case SyncService.BROADCAST_SYNCSERVICE_FEED_STATUS: {
					if (LOGGING)
						Log.v(LOGTAG, "Got a syncEvent");

					Feed feed = (Feed) intent.getSerializableExtra(SyncService.EXTRA_SYNCSERVICE_FEED);
					//SyncStatus status = (SyncStatus) intent.getSerializableExtra(SyncService.EXTRA_SYNCSERVICE_STATUS);
					refreshListIfCurrent(feed);
					refreshLeftSideMenu();
				}
				break;
				case SyncService.BROADCAST_SYNCSERVICE_FEED_ICON_STATUS: {
					SyncStatus status = (SyncStatus) intent.getSerializableExtra(SyncService.EXTRA_SYNCSERVICE_STATUS);
					if (status.equals(SyncStatus.OK.Value) && mAdapter != null && itemsRecyclerView != null && itemsRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
						LinearLayoutManager llm = (LinearLayoutManager) itemsRecyclerView.getLayoutManager();
						int first = llm.findFirstVisibleItemPosition();
						int last = llm.findLastVisibleItemPosition();
						if (first != RecyclerView.NO_POSITION && last != RecyclerView.NO_POSITION) {
							mAdapter.notifyItemRangeChanged(first, last - first + 1);
						}
					}
				}
				break;
			}
		}
	};

	private BroadcastReceiver mCallbacksReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (UIBroadcaster.BROADCAST_ACTION_FEED_SELECTED.equalsIgnoreCase(intent.getAction())) {
				FeedSelection feed = (FeedSelection) intent.getSerializableExtra(UIBroadcaster.EXTRAS_FEED_SELECTION);
				if (feed == null) {
					return;
				}

				boolean visibleTags = false;
				if (feed.isNormalFeed()) {
					visibleTags = BuildConfig.UI_ENABLE_TAGS;
				}
				setTagItemVisible(visibleTags);
				if (mAdapter != null) {
					mAdapter.setShowTags(visibleTags);
				}
				boolean isUpdate = false;
				if (feed.equals(mCurrentShownFeedSelection))
					isUpdate = true;
				if (!isUpdate) {
					setTagFilter(null);
				}

				mCurrentShownFeedSelection = feed;

				// If clicking story source to filter feed, "back" should not close app but get us back to "ALL FEEDS" (task #4292)
				//
				mBackShouldOpenAllFeeds = false;
				// TODO - filter on source
				//if (mCurrentShownFeedSelection.isNormalFeed()) {
					//if (source instanceof ItemRecyclerViewAdapter || source instanceof ItemView)
					//	mBackShouldOpenAllFeeds = true;
				//}

				updateList(isUpdate);


			} else if (UIBroadcaster.BROADCAST_ACTION_ITEM_FAVORITE_CHANGED.equalsIgnoreCase(intent.getAction())) {
				// An item has been marked/unmarked as favorite. Update the list
				// to pick up this change!
				refreshList();
			} else if (UIBroadcaster.BROADCAST_ACTION_FEED_RESYNC_REQUESTED.equalsIgnoreCase(intent.getAction())) {
				FeedSelection feed = (FeedSelection) intent.getSerializableExtra(UIBroadcaster.EXTRAS_FEED_SELECTION);
				if (feed == null) {
					return;
				}
				// Only show spinner if updating current feed
				boolean showLoader = getCurrentFeedSelection().equals(feed);
				onResync(feed, showLoader);
			} else if (UIBroadcaster.BROADCAST_ACTION_COMMAND.equalsIgnoreCase(intent.getAction())) {
				int command = intent.getIntExtra(UIBroadcaster.EXTRAS_COMMAND, -1);
				if (command == R.integer.command_add_feed_manual) {
					// First add it to reader!
					Bundle commandParameters = intent.getBundleExtra(UIBroadcaster.EXTRAS_COMMAND_OPTIONS);
					App.getInstance().socialReader.addFeedByURL(commandParameters.getString("uri"), null);
					refreshList();
				}
			}
		}
	};

	@Override
	public void onItemSelected(ItemViewModel item) {
		openStoryFullscreen(itemsRecyclerView, item);
	}

	@Override
	public void onTagSelected(String tag) {
		setTagFilter(tag);
	}

	@Override
	public void onSourceSelected(long feedId) {
		UIBroadcaster.setFeedFilter(this, new FeedSelection(feedId));
	}

	@Override
	public void onMediaSelected(ItemViewModel item, MediaItemViewModel mediaItem) {
		openStoryFullscreen(itemsRecyclerView, item);
	}

	@Override
	public void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem) {

	}

	@Override
	public void onCursorUpdated() {
		setIsLoading(false);
	}

	private void setTagFilter(String tag) {
		if (tag == null)
		{
			tagResultsHeader.setVisibility(View.GONE);
			tagResultsCloseButton.setVisibility(View.GONE);
		}
		else
		{
			tagResultsHeader.setText(UIHelpers.setSpanBetweenTokens(getString(R.string.story_item_short_tag_results, tag), "##",
					new ForegroundColorSpan(ContextCompat.getColor(this, R.color.accent))));
			tagResultsHeader.setVisibility(View.VISIBLE);
			tagResultsCloseButton.setVisibility(View.VISIBLE);
		}
		if (mAdapter != null) {
			mAdapter.setFilterString(tag);
		}
	}

	// Callback for manual feed sync. The only thing we need to so here is hide the loading
	// spinner. All else handled by the broadcast listener.
	private final FeedFetcher.FeedFetchedCallback mManualSyncCallback = new FeedFetcher.FeedFetchedCallback()
	{
		@Override
		public void feedFetched(Feed _feed)
		{
			setIsLoading(false);
		}

		@Override
		public void feedError(Feed _feed) {
			setIsLoading(false);
		}
	};

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		super.onSharedPreferenceChanged(sharedPreferences, key);
		//TODO
/*		if (key.equals(KEY_SYNC_MODE) && App.getSettings().getCurrentMode().syncMode() == ModeSettings.SyncMode.LetItFlow) {
			// Reload the recycler view with images!
			mAdapter.notifyDataSetChanged();
		}*/
	}

	private void updateErrors() {
		showError(null); // First hide
		if (mTaskUpdateErrors != null) {
			mTaskUpdateErrors.cancel(true);
		}
		mTaskUpdateErrors = new UpdateErrorsTask();
		mTaskUpdateErrors.execute();
	}

	private class UpdateErrorsTask extends ThreadedTask<Void, Void, String>
	{
		private final FeedSelection mFeedSelection;

		UpdateErrorsTask()
		{
			mFeedSelection = getCurrentFeedSelection();
		}

		@Override
		protected String doInBackground(Void... values)
		{
			if (LOGGING)
				Log.v(LOGTAG, "UpdateErrorsTask: doInBackground");

			SocialReader socialReader = App.getInstance().socialReader;

			if (mFeedSelection == FeedSelection.SHARED) {
				return null;
			} else if (mFeedSelection == FeedSelection.FAVORITES) {
				return null;
			} else if (mFeedSelection == FeedSelection.ALL_FEEDS || mFeedSelection == null) {
				if (LOGGING)
					Log.v(LOGTAG, "UpdateErrorsTask: all subscribed");
				int errorCount = 0;
				for (Feed feed : socialReader.getSubscribedFeedsList()) {
					if (!feed.getStatus().equals(SyncStatus.OK)) {
						errorCount++;
					}
				}
				if (errorCount == 0) {
					return null;
				} else if (errorCount == 1) {
					return getString(R.string.error_1_feed);
				} else {
					return getString(R.string.error_n_feeds, errorCount);
				}
			} else {
				if (LOGGING)
					Log.v(LOGTAG, "UpdateErrorsTask");
				String retString = null;

				Feed feedSelection = new Feed(mFeedSelection.Value, null, null);
				SyncStatus status = socialReader.syncStatus(feedSelection);
				if (status.equals(SyncStatus.ERROR_NOT_FOUND)) {
					retString = getString(R.string.error_feed_404);
				} else if (status.equals(SyncStatus.ERROR_BAD_URL)) {
					retString = getString(R.string.error_feed_bad_url);
				} else if (!status.equals(SyncStatus.OK)) {
					retString = getString(R.string.error_feed_unknown);
				}
				return retString;
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mTaskUpdateErrors == this)
				mTaskUpdateErrors = null;
		}

		@Override
		protected void onPostExecute(String error)
		{
			synchronized (MainActivity.this) {
				if (mTaskUpdateErrors == this)
					mTaskUpdateErrors = null;
				if (LOGGING)
					Log.v(LOGTAG, "UpdateErrorsTask: finished");
				showError(error);
			}
		}
	}
}
