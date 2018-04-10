package info.guardianproject.securereaderinterface;
		
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.facebook.device.yearclass.YearClass;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.tinymission.rss.Feed;
import com.tinymission.rss.Item;
import com.tinymission.rss.MediaContent;

import net.etuldan.sparss.utils.ArticleTextExtractor;
import net.etuldan.sparss.utils.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Exchanger;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.securereader.ModeSettings;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SocialReader.SocialReaderLockListener;
import info.guardianproject.securereader.SocialReporter;
import info.guardianproject.securereaderinterface.adapters.DrawerMenuRecyclerViewAdapter;
import info.guardianproject.securereaderinterface.installer.HTTPDAppSender;
import info.guardianproject.securereaderinterface.installer.SecureBluetoothReceiverFragment;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.ui.ContentFormatter;
import info.guardianproject.securereaderinterface.ui.ProxyMediaStreamServer;
import info.guardianproject.securereaderinterface.ui.SupportedLanguage;
import info.guardianproject.securereaderinterface.ui.UIBroadcaster;
import info.guardianproject.securereaderinterface.widgets.AnimatedRelativeLayout;
import info.guardianproject.securereaderinterface.widgets.CustomFontButton;
import info.guardianproject.securereaderinterface.widgets.CustomFontEditText;
import info.guardianproject.securereaderinterface.widgets.CustomFontRadioButton;
import info.guardianproject.securereaderinterface.widgets.CustomFontTextView;

public class App extends Application implements OnSharedPreferenceChangeListener, SocialReaderLockListener, SocialReader.SocialReaderFeedPreprocessor, SocialReader.SocialReaderFullTextPreprocessor {
	public static final String LOGTAG = "App";
	public static final boolean LOGGING = false;
	
	public static final String EXIT_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.exit.action";
	public static final String SET_UI_LANGUAGE_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.setuilanguage.action";
	public static final String WIPE_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.wipe.action";
	public static final String LOCKED_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.lock.action";
	public static final String UNLOCKED_BROADCAST_ACTION = "info.guardianproject.securereaderinterface.unlock.action";

	public static final String FRAGMENT_TAG_RECEIVE_SHARE = "FragmentReceiveShare";
	public static final String FRAGMENT_TAG_SEND_BT_SHARE = "FragmentSendBTShare";

	private static App m_singleton;

	public SettingsUI settings;

	public SocialReader socialReader;
	public SocialReporter socialReporter;
	
	private String mCurrentLanguage;
	private FeedSelection mCurrentFeedSelection = FeedSelection.ALL_FEEDS;
	private Feed mCurrentFeed;

	private boolean mIsWiping = false;
	private ProxyMediaStreamServer mProxyMediaStreamServer;

	@Override
	public void onCreate()
	{
		m_singleton = this;
		settings = new SettingsUI(this);

		// Check language is valid
		String code = settings.uiLanguageCode();
		if (!SupportedLanguage.isSupportedLanguageCode(code)) {
			settings.setUiLanguage(SupportedLanguage.getDefaultSupportedLanguage());
		}

		super.onCreate();

		// Load images from secure storage
		//
		Picasso.setSingletonInstance(new Picasso.Builder(this)
				.addRequestHandler(new RequestHandler() {
					@Override
					public boolean canHandleRequest(Request data) {
						return true;
					}

					@Override
					public Result load(Request request, int networkPolicy) throws IOException {
						FileInputStream is = new FileInputStream(request.uri.toString());
						return new Result(is, Picasso.LoadedFrom.NETWORK);
					}
				})
				//.indicatorsEnabled(true)
				.defaultBitmapConfig(Bitmap.Config.RGB_565)
				.build());

		socialReader = SocialReader.getInstance(this.getApplicationContext());
		socialReader.setLockListener(this);
		socialReader.setFeedPreprocessor(this);
		socialReader.setFullTextPreprocessor(this);
		socialReporter = new SocialReporter(socialReader);
		//applyPassphraseTimeout();
		
		settings.registerChangeListener(this);
		
		mCurrentLanguage = getBaseContext().getResources().getConfiguration().locale.getLanguage();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(UIBroadcaster.BROADCAST_ACTION_FEED_SELECTED);
		LocalBroadcastManager.getInstance(this).registerReceiver(mCallbacksReceiver, intentFilter);
	}

	private BroadcastReceiver mCallbacksReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (UIBroadcaster.BROADCAST_ACTION_FEED_SELECTED.equalsIgnoreCase(intent.getAction())) {
				mCurrentFeedSelection = (FeedSelection) intent.getSerializableExtra(UIBroadcaster.EXTRAS_FEED_SELECTION);
				if (mCurrentFeedSelection.isNormalFeed())
					mCurrentFeed = getFeedById(mCurrentFeedSelection.Value);
				else
					mCurrentFeed = null;
			}
		}
	};

	public static App getInstance()
	{
		return m_singleton;
	}

	public static SettingsUI getSettings()
	{
		return getInstance().settings;
	}

	private LockScreenActivity mLockScreen;

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (!mIsWiping)
		{
			if (key.equals(Settings.KEY_UI_LANGUAGE))
			{
				// Notify activities (if any)
				LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
			}
			else if (key.equals(Settings.KEY_AUTOLOCK))
			{
				applyPassphraseTimeout();
			}
		}
	}

	private void applyPassphraseTimeout()
	{
		socialReader.setCacheWordTimeout(settings.autoLock());
	}

	public void wipe(Context context, Settings.PanicAction action, boolean restart)
	{
		mIsWiping = true;
		socialReader.doWipe(action);
		mLastResumed = null;
		mLockScreen = null;

		// Notify activities (if any)
		LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.WIPE_BROADCAST_ACTION));
		mIsWiping = false;

		if (restart) {
			mCurrentFeedSelection = FeedSelection.ALL_FEEDS;
			Intent intent = new Intent(context, SplashActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}
	
	public boolean isWiping()
	{
		return mIsWiping;
	}
	
	public static View createView(String name, Context context, AttributeSet attrs)
	{
		View returnView = null;

		int id = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "id", -1);

		if (name.equals("TextView") || name.endsWith("DialogTitle"))
		{
			return new CustomFontTextView(context, attrs);
		}
		else if (name.equals("Button"))
		{
			return new CustomFontButton(context, attrs);
		}
		else if (name.equals("RadioButton"))
		{
			return new CustomFontRadioButton(context, attrs);
		}
		else if (name.equals("EditText"))
		{
			return new CustomFontEditText(context, attrs);
		}

		// API 17 still has some trouble with handling RTL layouts automatically.
		else if (name.equals("FrameLayout") && Build.VERSION.SDK_INT == 17 && getInstance().isRTL()) {
			returnView = new FrameLayout(context, attrs);
			returnView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
			returnView.setTextDirection(View.TEXT_DIRECTION_RTL);
		}
		else if (name.equals("LinearLayout") && Build.VERSION.SDK_INT == 17 && getInstance().isRTL()) {
			returnView = new LinearLayout(context, attrs);
			returnView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
			returnView.setTextDirection(View.TEXT_DIRECTION_RTL);
		}
		else if ((name.equals("RelativeLayout") || name.endsWith("AnimatedRelativeLayout")) && Build.VERSION.SDK_INT == 17 && getInstance().isRTL()) {
			if (name.equals("RelativeLayout")) {
				returnView = new RelativeLayout(context, attrs);
			} else {
				returnView = new AnimatedRelativeLayout(context, attrs);
			}
			RelativeLayout relativeLayout = (RelativeLayout) returnView;
			relativeLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
			returnView.setTextDirection(View.TEXT_DIRECTION_RTL);
			relativeLayout.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
				@Override
				public void onChildViewAdded(View parent, View child) {
					RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) child.getLayoutParams();
					if (lp != null) {
						int[] rules = lp.getRules();
						if (rules[RelativeLayout.START_OF] != 0) {
							lp.removeRule(RelativeLayout.LEFT_OF);
						}
						if (rules[RelativeLayout.END_OF] != 0) {
							lp.removeRule(RelativeLayout.RIGHT_OF);
						}
						if (rules[RelativeLayout.ALIGN_PARENT_START] != 0) {
							lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
						}
						if (rules[RelativeLayout.ALIGN_PARENT_END] != 0) {
							lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
						}
					}
				}

				@Override
				public void onChildViewRemoved(View parent, View child) {
				}
			});
		}
		return returnView;
	}

	public boolean isRTL()
	{
		if (Build.VERSION.SDK_INT >= 17)
		{
			return (getBaseContext().getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
		}
		else
		{
			// Handle old devices by looking at current language
			Configuration config = getBaseContext().getResources().getConfiguration();
			if (config.locale != null)
			{
				String language = config.locale.getLanguage();
				if (language.startsWith("ar") || language.startsWith("fa"))
					return true;
			}
			return false;
		}
	}

	private int mnResumed = 0;
	private Activity mLastResumed;
	private boolean mIsLocked = true;
	
	public void onActivityPause(Activity activity)
	{
		mnResumed--;
		if (mnResumed == 0)
			socialReader.onPause();
		if (mLastResumed == activity)
			mLastResumed = null;
	}

	public void onActivityResume(Activity activity)
	{
		mLastResumed = activity;
		mnResumed++;
		if (mnResumed == 1)
			socialReader.onResume();
		showLockScreenIfLocked();
	}
	
	public boolean isActivityLocked()
	{
		return mIsLocked;
	}
	
	private void showLockScreenIfLocked()
	{
		if (mIsLocked && mLastResumed != null && mLockScreen == null && !mIsWiping)
		{
			Intent intent = new Intent(App.this, LockScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.putExtra("originalIntent", mLastResumed.getIntent());
			mLastResumed.startActivity(intent);
			mLastResumed.overridePendingTransition(0, 0);
			mLastResumed = null;
		}
	}
	
	@Override
	public void onLocked()
	{
		mIsLocked = true;
		showLockScreenIfLocked();
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(LOCKED_BROADCAST_ACTION));
	}

	@Override
	public void onUnlocked()
	{
		mIsLocked = false;
		if (mLockScreen != null)
			mLockScreen.onUnlocked();
		mLockScreen = null;
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(UNLOCKED_BROADCAST_ACTION));
	}

	public void onLockScreenResumed(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = lockScreenActivity;
	}

	public void onLockScreenPaused(LockScreenActivity lockScreenActivity)
	{
		mLockScreen = null;
	}

	// TODO - should be moved to the library!
	public Feed getFeedById(long idFeed)
	{
		return socialReader.getFeedById(idFeed);
	}

	public FeedSelection getCurrentFeedSelection()
	{
		if (mCurrentFeedSelection == null)
			return FeedSelection.EMPTY;
		return mCurrentFeedSelection;
	}

	public Feed getCurrentFeed() {
		return mCurrentFeed;
	}

	/**
	 * Update the current feed property. Why is this needed? Because if the feed was just
	 * updated from the network a new Feed object will have been created and we want to
	 * pick up changes to the network pull date (and possibly other changes) here.
	 * @param feed
	 */
	public void updateCurrentFeed(Feed feed)
	{
		//TODO - need this?
		if (mCurrentFeed != null && mCurrentFeed.getDatabaseId() == feed.getDatabaseId())
			mCurrentFeed = feed;
	}

	private Resources mOverrideResources;

	public void setOverrideResources(Resources r)
	{
		mOverrideResources = r;
	}

	@Override
	public Resources getResources() {
		if (mOverrideResources != null)
			return mOverrideResources;
		return super.getResources();
	}

	public Class getDrawerMenuAdapterClass() {
		return DrawerMenuRecyclerViewAdapter.class;
	}

	public ContentFormatter getItemContentFormatter() { return null; }

	protected void onPrepareOptionsMenu(Activity activity, Menu menu) {
	}

	protected boolean onOptionsItemSelected(Activity activity, int itemId) {
		return false;
	}

	public ProxyMediaStreamServer getProxyMediaStreamServer() {
		if (mProxyMediaStreamServer == null || !mProxyMediaStreamServer.isAlive()) {
			if (mProxyMediaStreamServer != null) // If we have a dead server, call stop before we replace it
				mProxyMediaStreamServer.stop();
			mProxyMediaStreamServer = ProxyMediaStreamServer.createMediaServer();
		}
		return mProxyMediaStreamServer;
	}

	public void onCommand(Context context, int command, Bundle commandParameters) {
		UIBroadcaster.onCommand(context, command, commandParameters);
		switch (command) {
			case R.integer.command_news_list: {
				Intent intent = new Intent(context, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(intent);
				break;
			}

			case R.integer.command_feed_add: {
				Intent intent = new Intent(context, AddFeedActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(intent);
				((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
				break;
			}

			case R.integer.command_settings: {
				Intent intent = new Intent(context, SettingsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(intent);
				break;
			}

			case R.integer.command_toggle_online: {
				if (App.getInstance().socialReader.isOnline() == SocialReader.NOT_ONLINE_NO_PROXY)
					App.getInstance().socialReader.connectProxy((Activity) context);
				// else
				// Not sure this makes sense
				// App.getInstance().socialReader.goOffline();

				break;
			}

			case R.integer.command_view_media: {
				if (LOGGING)
					Log.v(LOGTAG, "command_view_media");
				if (commandParameters != null && commandParameters.containsKey("media")) {
					MediaItemViewModel mediaItem = (MediaItemViewModel) commandParameters.getSerializable("media");
					if (LOGGING)
						Log.v(LOGTAG, "MediaContent " + (mediaItem != null ? mediaItem.media.getType() : "unknown"));

					if (mediaItem != null && mediaItem.media.getType().startsWith("application/vnd.android.package-archive")) {
						// This is an application package. View means
						// "ask for installation"...
                        // TODO - filename is what?
                        Uri shareUri = socialReader.addFileToSecureShare(mediaItem.getFile(), "apk", "sharedapk.apk");
						if (shareUri != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setDataAndType(shareUri, mediaItem.media.getType());
                            context.startActivity(intent);
                        }
					} else if (mediaItem != null && mediaItem.media.getType().startsWith("application/epub+zip")) {
						if (LOGGING)
							Log.v(LOGTAG, "MediaContent is epub");

						// TODO - filename is what?
						Uri shareUri = socialReader.addFileToSecureShare(mediaItem.getFile(), "epub", "shared.epub");
						if (shareUri != null) {
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							intent.setDataAndType(shareUri, mediaItem.media.getType());

							PackageManager packageManager = context.getPackageManager();
							List list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
							if (list.size() > 0) {
								if (LOGGING)
									Log.v(LOGTAG, "Launching epub reader" + shareUri.toString());
								context.startActivity(intent);
							} else {
								if (LOGGING)
									Log.v("UIBroadcaster", "No application found" + shareUri.toString());

								// Download epub reader?
								int numShown = App.getSettings().downloadEpubReaderDialogShown();
								if (numShown < 1) {
									App.getSettings().setDownloadEpubReaderDialogShown(numShown + 1);
									intent = new Intent(context, DownloadEpubReaderActivity.class);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
									context.startActivity(intent);
									((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
								}
							}
						}
					} else {
						Intent intent = new Intent(context, ViewMediaActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
						intent.putExtra("parameters", commandParameters);
						context.startActivity(intent);
					}
				} else {
					if (LOGGING)
						Log.e(LOGTAG, "Invalid parameters to command command_view_media.");
				}
				break;
			}

			case R.integer.command_downloads: {
				Intent intent = new Intent(context, DownloadsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(intent);
				((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
				break;
			}

			case R.integer.command_receiveshare: {
				if (context instanceof FragmentActivity) {
					if (LOGGING)
						Log.v(LOGTAG, "Calling receive share fragment dialog");
					FragmentManager fm = ((FragmentActivity) context).getSupportFragmentManager();
					SecureBluetoothReceiverFragment dialogReceiveShare = new SecureBluetoothReceiverFragment();
					dialogReceiveShare.show(fm, App.FRAGMENT_TAG_RECEIVE_SHARE);
				}
				break;
			}

			case R.integer.command_shareapp: {
				if (LOGGING)
					Log.v(LOGTAG, "Calling HTTPDAppSender");
				Intent intent = new Intent(context, HTTPDAppSender.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				context.startActivity(intent);
				((Activity) context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
				break;
			}

			case R.integer.command_read_more: {
				if (LOGGING)
					Log.v(LOGTAG, "Read More");
				if (commandParameters != null && commandParameters.containsKey("url")) {
					try {
						Uri uri = Uri.parse(commandParameters.getString("url"));

						// Use tor or not?
						if (info.guardianproject.securereaderinterface.App.getSettings().proxyType() != Settings.ProxyType.None) {
							Intent intent = new Intent(Intent.ACTION_VIEW, uri);
							String thisPackageName = info.guardianproject.securereaderinterface.App.getInstance().getPackageName();

							// Instead of using built in functionality, we create our own chooser so that we
							// can remove ourselves from the list (opening the story in this app would actually
							// take us to the AddFeed page, so it does not make sense to have it as an option)
							List<Intent> targetedShareIntents = new ArrayList<>();
							List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
							if (resInfo != null && resInfo.size() > 0) {
								for (ResolveInfo resolveInfo : resInfo) {
									String packageName = resolveInfo.activityInfo.packageName;

									Intent targetedShareIntent = (Intent) intent.clone();
									targetedShareIntent.setPackage(packageName);
									if (!packageName.equals(thisPackageName)) // Remove
									// ourselves
									{
										targetedShareIntents.add(targetedShareIntent);
									}
								}

								if (targetedShareIntents.size() > 0) {
									Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), null);
									chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
									context.startActivity(chooserIntent);
								}
							}
						} else {
							// Open
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
							context.startActivity(browserIntent);
						}
					} catch (Exception e) {
						if (LOGGING)
							Log.d(LOGTAG, "Error trying to open read more link: " + commandParameters.getString("url"));
					}
				}
				break;
			}
		}
	}

	@Override
	public String onGetFeedURL(Feed feed) {
		return null;
	}

	@Override
	public InputStream onFeedDownloaded(Feed feed, InputStream content, Map<String, String> headers) {
		return null;
	}

	@Override
	public void onFeedParsed(Feed feed) {
		if (LOGGING) {
			Log.v(LOGTAG, String.format("onFeedParsed: %s : %s", feed.getTitle(), feed.getFeedURL()));
		}
		// Sort media items on size and group and remove unused ones.
		for (Item item : feed.getItems()) {
			if (item.getNumberOfMediaContent() > 0) {
				ArrayList<MediaContent> mediaContents = item.getMediaContent();

				// For groupless items, try to match URLs to see if they are indeed the same
				// media item in different configs.
				int maxGroupFound = 0;
				HashMap<String,ArrayList<MediaContent>> groupMap = new HashMap<>(mediaContents.size());
				for (MediaContent mc : mediaContents) {
					if (mc.getMediaGroup() == 0) {
						try {
							URL url = new URL(mc.getUrl());
							String path = url.getPath().toLowerCase();
							if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) {
								if (groupMap.containsKey(path)) {
									groupMap.get(path).add(mc);
								} else {
									ArrayList<MediaContent> newlist = new ArrayList<>();
									newlist.add(mc);
									groupMap.put(path, newlist);
								}
							}
						} catch (Exception ignored) {
						}
					} else {
						maxGroupFound = Math.max(maxGroupFound, mc.getMediaGroup());
					}
				}
				int group = maxGroupFound + 1;
				for (String key : groupMap.keySet()) {
					ArrayList<MediaContent> content = groupMap.get(key);
					if (content.size() > 1) {
						for (MediaContent mc :content) {
							mc.setMediaGroup(group);
						}
						group++;
					}
				}

				// Sort items according to media group, and then width (descending)
				// If first item has a group, sort "groupless" at the end, otherwise
				// sort groupless at the front.
				final boolean firstItemHasGroup = mediaContents.get(0).getMediaGroup() > 0;
				Collections.sort(mediaContents, new Comparator<MediaContent>() {
					@Override
					public int compare(MediaContent item1, MediaContent item2) {
						if (item1.getMediaGroup() == 0 && item2.getMediaGroup() > 0)
							return firstItemHasGroup ? 1 : -1;
						else if (item2.getMediaGroup() == 0 && item1.getMediaGroup() > 0)
							return firstItemHasGroup ? -1 : 1;
						if (item1.getMediaGroup() == item2.getMediaGroup()) {
							// Sort by width
							if (item1.getWidth() == item2.getWidth())
								return 0;
							else if (item1.getWidth() < item2.getWidth())
								return 1;
							return -1;
						}
						else if (item1.getMediaGroup() > item2.getMediaGroup())
							return 1;
						return -1;
					}
				});

				// Part 2 - Just keep 1 item in each group (but keep all groupless)
				//
				for (int i = mediaContents.size() - 1; i > 0; i--) {
					group = mediaContents.get(i).getMediaGroup();
					if (group != 0 && mediaContents.get(i - 1).getMediaGroup() == group) {
						// We have a better one in this group, remove this
						mediaContents.remove(i);
						if (LOGGING) {
							Log.v(LOGTAG, String.format("Item %s removing media from group", item.getTitle()));
						}
					}
				}
			}
		}
	}

	@Override
	public String onFullTextDownloaded(Item item, MediaContent content, File file) {
		String ret = null;
		try {
			String contentIndicator = null;

			String articleAbstract = item.getCleanMainContent().toString();
			if (articleAbstract.length() > 60) {
				contentIndicator = articleAbstract.substring(20, 40);
			}

			InputStream is = new FileInputStream(file);

			ret = ArticleTextExtractor.extractContent(is, contentIndicator);
			if (ret != null) {
				ret = HtmlUtils.improveHtmlContent(ret, item.getLink());
			}
			is.close();
		} catch (Exception ignored) {}
		return ret;
	}

	public void detectDeviceCapsForOptimizedMode(ModeSettings modeOptimized) {
		// Connected to wifi?
		boolean onWifi = false;
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (networkInfo != null && networkInfo.isConnected()) {
			onWifi = true;
		}

		if (onWifi) {
			Set<ModeSettings.Sync> values = new HashSet<>(Arrays.asList(ModeSettings.Sync.Summary));
			modeOptimized.setSyncData(values);
			values = new HashSet<>(Arrays.asList(ModeSettings.Sync.Summary, ModeSettings.Sync.FullText, ModeSettings.Sync.Media));
			modeOptimized.setSyncWifi(values);
		} else {
			Set<ModeSettings.Sync> values = new HashSet<>(Arrays.asList(ModeSettings.Sync.Summary, ModeSettings.Sync.Media));
			modeOptimized.setSyncData(values);
			values = new HashSet<>(Arrays.asList(ModeSettings.Sync.Summary, ModeSettings.Sync.FullText, ModeSettings.Sync.Media));
			modeOptimized.setSyncWifi(values);
		}

		// Screen size
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int smallestWidth = Math.min(size.x, size.y);

		// Memory
		java.io.File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		long memorySize = totalBlocks * blockSize;
		float memorySizeGigs = (float)memorySize / (1024 * 1024 * 1024);

		modeOptimized.setPowersavePercentage(20);
		if (smallestWidth < 720 && memorySizeGigs <= 16) {
			modeOptimized.setPowerSaveEnabled(true);
		} else {
			modeOptimized.setPowerSaveEnabled(false);
		}
	}
}
