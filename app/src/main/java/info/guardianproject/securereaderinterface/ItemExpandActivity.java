package info.guardianproject.securereaderinterface;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.TranslateAnimation;
import android.widget.SeekBar;

import com.tinymission.rss.Item;

import info.guardianproject.securereaderinterface.adapters.ItemCursorRecyclerViewAdapter;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.ui.ActionProviderShareItem;
import info.guardianproject.securereaderinterface.ui.UIBroadcaster;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.views.ItemDetailContainerView;
import info.guardianproject.securereaderinterface.widgets.AnimatedRelativeLayout;

public class ItemExpandActivity extends AppActivity implements ItemDetailContainerView.ItemDetailContainerViewListener {
	public static final String LOGTAG = "ItemExpandActivity";
	public static final boolean LOGGING = false;

	public static final int EXPANSION_ANIMATION_DURATION = 500;

	private AnimatedRelativeLayout mDetailViewContainer;
	private ItemDetailContainerView mDetailView;
	private RecyclerView mItemsRecyclerView;
	private int mDetailViewOpeningOffset;
	private boolean mInFullScreenMode;

	private MenuItem mMenuItemShareItem;
	private ActionProviderShareItem mShareItemActionProvider;

	@Override
	protected void onResume()
	{
		super.onResume();
		configureActionBarForFullscreen(isInFullScreenMode());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		// Locate MenuItem with ShareActionProvider
		mMenuItemShareItem = menu.findItem(R.id.menu_share_item);
		if (mMenuItemShareItem != null && getSupportActionBar() != null)
		{
			mShareItemActionProvider = new ActionProviderShareItem(getSupportActionBar().getThemedContext());
			//mShareItemActionProvider.setFeed(getCurrentFeed());
			MenuItemCompat.setActionProvider(mMenuItemShareItem, mShareItemActionProvider);
		}
		return ret;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean ret = super.onPrepareOptionsMenu(menu);
		configureActionBarForFullscreen(isInFullScreenMode());

		MenuItem item = menu.findItem(R.id.menu_show_full_text);
		if (item != null) {
			if (mDetailView != null && (isInFullScreenMode() || findViewById(R.id.storyContainer) != null)) {
				item.setVisible(mDetailView.canShowFullText());
				item.setChecked(mDetailView.showingFullText());
			} else {
				item.setVisible(false);
			}
		}

		item = menu.findItem(R.id.menu_share_item);
		if (item != null) {
			if (mDetailView != null && (isInFullScreenMode() || findViewById(R.id.storyContainer) != null) && mDetailView.getCurrentStory() != null) {
				mShareItemActionProvider.setItem(mDetailView.getCurrentStory().item);
				item.setVisible(true);
			} else {
				item.setVisible(false);
			}
		}

		item = menu.findItem(R.id.menu_favorite);
		if (item != null) {
			if (mDetailView != null && (isInFullScreenMode() || findViewById(R.id.storyContainer) != null) && mDetailView.getCurrentStory() != null) {
				Item dataItem = mDetailView.getCurrentStory().item;
				item.setChecked(dataItem.getFavorite());
				item.setIcon(item.isChecked() ? R.drawable.ic_bookmark_white_24dp : R.drawable.ic_bookmark_border_white_24dp);
				item.setVisible(true);
			} else {
				item.setVisible(false);
			}
		}

		item = menu.findItem(R.id.menu_font_size);
		if (item != null) {
			if (mDetailView != null && (isInFullScreenMode() || findViewById(R.id.storyContainer) != null)) {
				item.setVisible(true);
			} else {
				item.setVisible(false);
			}
		}
		return ret;
	}

	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		updateItemView();
	}

	@Override
	public void setContentView(View view) {
		super.setContentView(view);
		updateItemView();
	}

	@Override
	public void setContentView(View view, LayoutParams params) {
		super.setContentView(view, params);
		updateItemView();
	}

	private void updateItemView() 
	{
		if (mDetailView != null)
		{
			ViewGroup container = (ViewGroup)findViewById(R.id.storyContainer);
			if (container != null && isInFullScreenMode()) {
				if (mDetailView.getParent() != null)
					((ViewGroup) mDetailView.getParent()).removeView(mDetailView);

				// Close old full screen view
				//
				configureActionBarForFullscreen(false);
				this.removeDetailViewContainer(false);
				this.mInFullScreenMode = false;
				scrollToCurrentItem();
				container.addView(mDetailView);
			} else if (container == null && !isInFullScreenMode()) {
				if (mDetailView.getParent() != null)
					((ViewGroup) mDetailView.getParent()).removeView(mDetailView);
			}
		}
	}

	private void createFullScreenContainer(ItemDetailContainerView content) {
		ViewGroup screenFrame = getTopFrame();
		if (screenFrame != null) {
			// Disable drag of the left side menu
			//
			mDrawerLayout.closeDrawers();

			// Remove old view (if set) from view tree
			//
			removeDetailViewContainer(false);

			mDetailViewContainer = new AnimatedRelativeLayout(this);
			content.setId(R.id.content);
			mDetailViewContainer.addView(content);
			mDetailViewContainer.setAnimationDuration(EXPANSION_ANIMATION_DURATION);

			mInFullScreenMode = true;

			screenFrame.addView(mDetailViewContainer);

			mDetailViewContainer.setListener(new AnimatedRelativeLayout.AnimatedRelativeLayoutListener() {
				@Override
				public void onAnimatedToEndPositions() {
					configureActionBarForFullscreen(true);

					// Minimize overdraw by hiding list
					mItemsRecyclerView.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onAnimatedToStartPositions() {
					removeDetailViewContainer(true);
				}
			});
		}
	}

	public void openStoryFullscreen(RecyclerView recyclerView, ItemViewModel item) {
		if (recyclerView == null || recyclerView.getAdapter() == null || item == null || getTopFrame() == null) {
			return;
		}
		ItemCursorRecyclerViewAdapter adapter = (ItemCursorRecyclerViewAdapter) recyclerView.getAdapter();

		// Clean up old
		if (mDetailView != null) {
			mDetailView.setListener(null);
			mDetailView = null;
		}
		mDetailView = createDetailView();
		mDetailView.setListener(this);

		RecyclerView.ViewHolder vh = recyclerView.findViewHolderForItemId(item.item.getDatabaseId());
		adapter.getCursor().setCurrentItemId(item.item.getDatabaseId());
		mDetailView.setStory(adapter, (vh == null) ? null :
					getStoredPositions((ViewGroup) vh.itemView));

		mItemsRecyclerView = recyclerView;

		ViewGroup container = (ViewGroup)findViewById(R.id.storyContainer);
		if (container != null) {
			// We already have a container in the UI, landscape mode!
			container.addView(mDetailView);
		} else {
			createFullScreenContainer(mDetailView);
			if (vh != null) {
				setCollapsedSizeToStoryViewSize(vh.itemView);
			}
			animateRecyclerViewItem(recyclerView, item.item.getDatabaseId(), true);
		}
		invalidateOptionsMenu();
	}

	protected ItemDetailContainerView createDetailView() {
		return new ItemDetailContainerView(this);
	}

	private void animateRecyclerViewItem(RecyclerView recyclerView, long itemId, boolean expand) {
		if (recyclerView == null || recyclerView.getAdapter() == null || itemId <= 0) {
			return;
		}
		RecyclerView.ViewHolder vh = recyclerView.findViewHolderForItemId(itemId);
		if (vh != null && vh.getAdapterPosition() != RecyclerView.NO_POSITION) {

			int[] storyViewScreenLocation = new int[2];
			vh.itemView.getLocationOnScreen(storyViewScreenLocation);
			int[] fullStoryViewScreenLocation = new int[2];
			getTopFrame().getLocationOnScreen(fullStoryViewScreenLocation);

			int storyViewTop = storyViewScreenLocation[1];
			int storyViewBottom = storyViewTop + vh.itemView.getHeight();
			int frameTop = fullStoryViewScreenLocation[1];
			int frameBottom = frameTop + getTopFrame().getHeight();

			int topDistance = storyViewTop - frameTop;
			int bottomDistance = frameBottom - storyViewBottom;

			for (int i = 0; i < recyclerView.getChildCount(); i++) {
				View child = recyclerView.getChildAt(i);
				int childPosition = recyclerView.getChildAdapterPosition(child);
				if (childPosition < vh.getAdapterPosition()) {
					TranslateAnimation t;
					if (expand) {
						t = new TranslateAnimation(0, 0, 0, -topDistance);
					} else {
						t = new TranslateAnimation(0, 0, -topDistance, 0);
					}
					t.setDuration(EXPANSION_ANIMATION_DURATION);
					child.startAnimation(t);
				} else if (childPosition > vh.getAdapterPosition()) {
					TranslateAnimation t;
					if (expand) {
						t = new TranslateAnimation(0, 0, 0, bottomDistance);
					} else {
						t = new TranslateAnimation(0, 0, bottomDistance, 0);
					}
					t.setDuration(EXPANSION_ANIMATION_DURATION);
					child.startAnimation(t);
				}
				//else {
				// Current item, do nothing, will be obscured by the expanding item detail view
				//}
			}
		}
	}


	private void setCollapsedSizeToStoryViewSize(View storyView)
	{
		int[] recyclerViewScreenLocation = new int[2];
		mItemsRecyclerView.getLocationOnScreen(recyclerViewScreenLocation);
		int[] storyViewScreenLocation = new int[2];
		storyView.getLocationOnScreen(storyViewScreenLocation);
		int[] fullStoryViewScreenLocation = new int[2];
		getTopFrame().getLocationOnScreen(fullStoryViewScreenLocation);

		// The view we are expanding from is this distance from top of RecyclerView
		mDetailViewOpeningOffset = storyViewScreenLocation[1] - recyclerViewScreenLocation[1];

		SparseArray<Rect> startPositions = new SparseArray<>();
		int yInFullStoryCoords = storyViewScreenLocation[1] - fullStoryViewScreenLocation[1];

		Rect itemRect = new Rect(0, yInFullStoryCoords, storyView.getWidth(), yInFullStoryCoords + storyView.getHeight());
		startPositions.put(R.id.content, itemRect);
		mDetailViewContainer.setStartPositions(startPositions);
	}
	
	private void getStoredPositionForViewWithId(ViewGroup parent, int viewId, SparseArray<Rect> positions)
	{
		View view = parent.findViewById(viewId);
		if (view != null)
		{
			Rect rect = UIHelpers.getRectRelativeToView(parent, view);
			rect.offset(view.getPaddingLeft(), view.getPaddingTop());
			rect.right -= (view.getPaddingRight() + view.getPaddingLeft());
			rect.bottom -= (view.getPaddingBottom() + view.getPaddingTop());
			positions.put(view.getId(), rect);
		}
	}
	
	private SparseArray<Rect> getStoredPositions(ViewGroup viewGroup)
	{
		if (viewGroup == null || viewGroup.getChildCount() == 0)
			return null;

		SparseArray<Rect> positions = new SparseArray<>();

		getStoredPositionForViewWithId(viewGroup, R.id.layout_media, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.tvTitle, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.tvContent, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.layout_source, positions);
		getStoredPositionForViewWithId(viewGroup, R.id.layout_author, positions);
		return positions;
	}

	private ViewGroup getTopFrame()
	{
		return (ViewGroup) findViewById(R.id.layout_root);
	}

	private void removeDetailViewContainer(boolean animated)
	{
		if (mDetailViewContainer != null)
		{
			try
			{
				if (animated)
					AnimationHelpers.fadeOut(mDetailViewContainer, 500, 0, true);
				else
					((ViewGroup) mDetailViewContainer.getParent()).removeView(mDetailViewContainer);
				mDetailViewContainer.setListener(null);
				mDetailViewContainer = null;
				mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
			}
			catch (Exception ex)
			{
				if (LOGGING)
					Log.e(LOGTAG, "Failed to remove full story view from view tree: " + ex.toString());
			}
		}
	}

	@Override
	public void onBackPressed()
	{
		if (isInFullScreenMode())
		{
			exitFullScreenMode();
		}
		else
		{
			// If the user is not currently in full screen story mode, allow the
			// system to handle the
			// Back button. This calls finish() on this activity and pops the
			// back stack.
			super.onBackPressed();
		}
	}

	protected void configureActionBarForFullscreen(boolean isFullscreen)
	{
		setToolbarTimeout(isFullscreen ? 5000 : 0);
	}

	private boolean isInFullScreenMode()
	{
		return mInFullScreenMode;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			if (isInFullScreenMode())
			{
				exitFullScreenMode();
				return true;
			}
			case R.id.menu_show_full_text:
				item.setChecked(!item.isChecked());
				if (mDetailView != null) {
					mDetailView.showFullText(item.isChecked());
				}
				break;

			case R.id.menu_favorite:
				if (mDetailView != null && mDetailView.getCurrentStory() != null) {
					item.setChecked(!item.isChecked());
					item.setIcon(item.isChecked() ? R.drawable.ic_bookmark_white_24dp : R.drawable.ic_bookmark_border_white_24dp);
                    Item dataItem = mDetailView.getCurrentStory().item;
					App.getInstance().socialReader.markItemAsFavorite(dataItem, item.isChecked());
                    UIBroadcaster.itemFavoriteStatusChanged(this, dataItem);
				}
				break;

			case R.id.menu_font_size:
				showTextSizePanel();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void exitFullScreenMode()
	{
		mInFullScreenMode = false;
		configureActionBarForFullscreen(false);
		mItemsRecyclerView.setVisibility(View.VISIBLE);
		mItemsRecyclerView.getAdapter().notifyDataSetChanged();
		scrollToCurrentItem();
	}

	private void doExitFullScreenMode() {
		mItemsRecyclerView.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (mDetailView != null)
					mDetailView.onCollapse(EXPANSION_ANIMATION_DURATION);
				if (mDetailViewContainer != null) {
					mDetailViewContainer.animateToStartPositions();
				}
			}
		});

	}

	private void scrollToCurrentItem() {
		if (mItemsRecyclerView != null && mItemsRecyclerView.getLayoutManager() instanceof LinearLayoutManager &&
				mItemsRecyclerView.getAdapter() instanceof ItemCursorRecyclerViewAdapter) {

			final ItemCursorRecyclerViewAdapter adapter = (ItemCursorRecyclerViewAdapter) mItemsRecyclerView.getAdapter();
			LinearLayoutManager layoutManager = (LinearLayoutManager) mItemsRecyclerView.getLayoutManager();

			int adapterPosition = adapter.getCursor().getCurrentIndex() + adapter.getHeaderOffset();
			if (adapterPosition != RecyclerView.NO_POSITION) {
				mItemsRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
					@Override
					public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
						mItemsRecyclerView.removeOnLayoutChangeListener(this);
						long currentItem = adapter.getCursor().getCurrentItemId();
						if (currentItem >= 0) {
							RecyclerView.ViewHolder vh = mItemsRecyclerView.findViewHolderForItemId(currentItem);
							if (vh != null) {
								setCollapsedSizeToStoryViewSize(vh.itemView);
							}
							animateRecyclerViewItem(mItemsRecyclerView, currentItem, false);
						}
						doExitFullScreenMode();
					}
				});
				layoutManager.scrollToPositionWithOffset(adapterPosition, mDetailViewOpeningOffset);
			} else {
				doExitFullScreenMode();
			}
		} else {
			doExitFullScreenMode();
		}
	}

	@Override
	public void onCurrentUpdated() {
		invalidateOptionsMenu();
	}

	private void showTextSizePanel() {
		View contentView = LayoutInflater.from(this).inflate(R.layout.text_size_panel, null, false);

		final SeekBar seekBarTextSize = (SeekBar) contentView.findViewById(R.id.seekBarTextSize);
		seekBarTextSize.setMax(150);
		int adjustment = App.getSettings().getContentFontSizeAdjustment();
		seekBarTextSize.setProgress(adjustment + 50);
		seekBarTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
										  boolean fromUser) {
				if (fromUser)
				{
					progress -= 50;
					int adjustment = App.getSettings().getContentFontSizeAdjustment();
					if (adjustment != progress)
					{
						App.getSettings().setContentFontSizeAdjustment(progress);
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		AlertDialog.Builder alert = new AlertDialog.Builder(this)
				.setPositiveButton(R.string.pref_done, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.show();
	}
}
