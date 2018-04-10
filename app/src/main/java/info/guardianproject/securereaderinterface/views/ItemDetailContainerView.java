package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.BuildConfig;
import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.adapters.ItemCursor;
import info.guardianproject.securereaderinterface.adapters.ItemCursorRecyclerViewAdapter;
import info.guardianproject.securereaderinterface.adapters.ShareSpinnerAdapter;
import info.guardianproject.securereaderinterface.installer.SecureBluetoothSenderFragment;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.ui.UIBroadcaster;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import info.guardianproject.securereaderinterface.widgets.CheckableImageView;
import info.guardianproject.securereaderinterface.widgets.compat.Spinner;
import info.guardianproject.securereaderinterface.R;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.tinymission.rss.Feed;

public class ItemDetailContainerView extends FrameLayout
{
	protected static final String LOGTAG = "FullScreenStoryItemView";
	public static final boolean LOGGING = false;

	public static final int SHOW_TEXTSIZE_PANEL_DURATION = 2000;
	private static final long MARK_AS_READ_TIMEOUT = 5000;

	public interface ItemDetailContainerViewListener {
		void onCurrentUpdated();
	}

	private ViewPager mContentPager;
	private ContentPagerAdapter mContentPagerAdapter;
	
	private ItemCursorRecyclerViewAdapter mItemAdapter;
	private ItemCursor mItemCursor;
	private SparseArray<Rect> mInitialViewPositions;
	private SparseArray<Rect> mFinalViewPositions;
	private Handler mHandler;
	private ItemDetailContainerViewListener mListener;

	public ItemDetailContainerView(Context context)
	{
		super(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.item_detail_container, this);
		initialize();
	}

	public ItemDetailContainerView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.item_detail_container, this);
		initialize();
	}

	public ItemDetailContainerViewListener getListener() {
		return mListener;
	}

	public void setListener(ItemDetailContainerViewListener listener) {
		this.mListener = listener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		super.onTouchEvent(event);
		return true;
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig)
	{
		if (mContentPager != null) {
			if (mContentPagerAdapter != null) {
				mContentPagerAdapter.notifyDataSetChanged();
			}
			mContentPager.setAdapter(null);
		}
		this.removeAllViews();
		super.onConfigurationChanged(newConfig);
		LayoutInflater inflater = LayoutInflater.from(getContext());
		inflater.inflate(R.layout.item_detail_container, this);
		initialize();
		setCurrentStoryIndex((mItemCursor != null) ? mItemCursor.getCurrentIndex() : 0);
		refresh();
	}

	private void initialize()
	{
		mHandler = new Handler();
		setBackgroundResource(R.drawable.background_detail);
		mContentPager = (ViewPager) findViewById(R.id.horizontalPagerContent);
		mContentPagerAdapter = new ContentPagerAdapter();
		mContentPager.setAdapter(mContentPagerAdapter);
	}

	public ItemViewModel getCurrentStory()
	{
		if (mItemAdapter == null || mItemCursor == null)
			return null;
		return mItemAdapter.getDataItem( mItemCursor.getCurrentIndex());
	}

	public void setStory(ItemCursorRecyclerViewAdapter itemAdapter, SparseArray<Rect> initialViewPositions)
	{
		mItemAdapter = itemAdapter;
		mItemAdapter.registerAdapterDataObserver(observer);
		mItemCursor = itemAdapter.getCursor();
		setCurrentStoryIndex((mItemCursor != null) ? mItemCursor.getCurrentIndex() : 0);
		mInitialViewPositions = initialViewPositions;
		mFinalViewPositions = initialViewPositions;
		mContentPager.setCurrentItem((mItemCursor != null) ? mItemCursor.getCurrentIndex() : 0);
		refresh();
	}

	@Override
	protected void onDetachedFromWindow() {
		if (mItemAdapter != null) {
			mItemAdapter.unregisterAdapterDataObserver(observer);
			mItemAdapter = null;
		}
		super.onDetachedFromWindow();
	}

	public boolean canShowFullText() {
		ItemDetailView itemDetailView = mContentPagerAdapter.getCurrentView();
		if (itemDetailView != null) {
			return App.getInstance().socialReader.hasFullTextForItem(itemDetailView.getItem().item);
		}
		return false;
	}

	public boolean showingFullText() {
		ItemDetailView itemDetailView = mContentPagerAdapter.getCurrentView();
		if (itemDetailView != null) {
			return itemDetailView.showFullText();
		}
		return false;
	}

	public void showFullText(boolean show) {
		ItemDetailView itemDetailView = mContentPagerAdapter.getCurrentView();
		if (itemDetailView != null) {
			itemDetailView.setShowFullText(show);
		}
	}

	private void setCurrentStoryIndex(int index)
	{
		if (mItemCursor != null) {
			mItemCursor.setCurrentIndex(index);
		}
		ItemViewModel current = getCurrentStory();
		if (current != null)
		{
			mHandler.removeCallbacks(markAsReadRunnable);
			mHandler.postDelayed(markAsReadRunnable, MARK_AS_READ_TIMEOUT);
			DownloadsAdapter.viewed(current);
		}
		if (getListener() != null) {
			getListener().onCurrentUpdated();
		}
	}

	private Runnable markAsReadRunnable = new Runnable() {
		@Override
		public void run() {
			markCurrentItemAsRead();
		}
	};

	public void refresh()
	{
		mContentPagerAdapter.notifyDataSetChanged();
	}

	private void markCurrentItemAsRead() {
		if (ViewCompat.isAttachedToWindow(this)) {
			ItemViewModel itemViewModel = getCurrentStory();
			if (itemViewModel != null && itemViewModel.item != null) {
				App.getInstance().socialReader.addToItemViewCount(itemViewModel.item);
			}
		}
	}

	public void onCollapse(int duration)
	{
		ItemDetailView itemDetailView = mContentPagerAdapter.getCurrentView();
		if (itemDetailView != null)
			itemDetailView.resetToStoredPositions(mFinalViewPositions, duration);
	}
	
	private class ContentPagerAdapter extends PagerAdapter
	{
		private ItemDetailView mCurrentView;
		private boolean mUseReverseSwipe;

		public ContentPagerAdapter()
		{
			super();
			mUseReverseSwipe = App.getInstance().isRTL();
		}

		public ItemDetailView getCurrentView()
		{
			return mCurrentView;
		}
		
		@Override
		public void setPrimaryItem(ViewGroup container, int position,
				Object object) {
			super.setPrimaryItem(container, position, object);
			if (mCurrentView == null || position != ((mItemCursor != null) ? mItemCursor.getCurrentIndex() : -1))
			{
				setCurrentStoryIndex(position);
				mCurrentView = (ItemDetailView) object;
			}
		}

		@Override
		public int getItemPosition(@NonNull Object object) {
			ItemDetailView view = (ItemDetailView)object;
			if (view != null) {
				return view.getAdapterPosition();
			}
			return POSITION_NONE;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1)
		{
			return arg0.equals(arg1);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			ItemViewModel itemViewModel;
			int adapterPosition = position;
			if (mUseReverseSwipe) {
				adapterPosition = mItemAdapter.getDataItemCount() - position - 1;
			}
			itemViewModel = mItemAdapter.getDataItem(adapterPosition);

			final ItemDetailView itemDetailView = new ItemDetailView(getContext(), itemViewModel, adapterPosition);
			if (mInitialViewPositions != null && position == mContentPager.getCurrentItem())
			{
				itemDetailView.setStoredPositions(mInitialViewPositions);
				mInitialViewPositions = null;
			}
			itemDetailView.setListener(new ItemDetailView.ItemViewListener() {
				@Override
				public void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem) {
					if (itemDetailView.getVisibility() == View.VISIBLE) {
						DownloadsAdapter.viewed(item);    // Tell the downloads adapter that we saw this
					}
				}
			});
			container.addView(itemDetailView);
			return itemDetailView;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			ItemDetailView itemDetailView = (ItemDetailView)object;
			container.removeView(itemDetailView);
			itemDetailView.recycle();
		}

		@Override
		public int getCount()
		{
			return (mItemAdapter == null) ? 0 : mItemAdapter.getDataItemCount();
		}
	}

	private RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
		@Override
		public void onChanged() {
			super.onChanged();
			mContentPagerAdapter.notifyDataSetChanged();
		}

		@Override
		public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
			super.onItemRangeChanged(positionStart, itemCount, payload);
			mContentPagerAdapter.notifyDataSetChanged();
			if (mContentPagerAdapter.getCurrentView() != null)
				mContentPagerAdapter.getCurrentView().update();
		}

		@Override
		public void onItemRangeInserted(int positionStart, int itemCount) {
			super.onItemRangeInserted(positionStart, itemCount);
			mContentPagerAdapter.notifyDataSetChanged();
		}

		@Override
		public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
			super.onItemRangeMoved(fromPosition, toPosition, itemCount);
			mContentPagerAdapter.notifyDataSetChanged();
		}

		@Override
		public void onItemRangeRemoved(int positionStart, int itemCount) {
			super.onItemRangeRemoved(positionStart, itemCount);
			mContentPagerAdapter.notifyDataSetChanged();
		}
	};
}
