package info.guardianproject.securereaderinterface.views;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.NestedScrollView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.ItemExpandActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.ViewHolderItem;
import info.guardianproject.securereaderinterface.models.ItemViewModel;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.ui.ItemAdapterListener;
//import info.guardianproject.securereaderinterface.ui.MediaViewCollection;
//import info.guardianproject.securereaderinterface.ui.MediaViewCollection.OnMediaLoadedListener;
import info.guardianproject.securereaderinterface.widgets.AnimatedRelativeLayout;

public class ItemDetailView extends NestedScrollView implements ItemAdapterListener {
	public static final String LOGTAG = "ItemView";
	public static final boolean LOGGING = false;

	public interface ItemViewListener {
		void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem);
	}

	private final ItemViewModel mItem;
	private final int adapterPosition;
	private final ViewHolderItem mViewHolder;

	private ItemViewListener mListener;

	public ItemDetailView(Context context, ItemViewModel item, int adapterPosition)
	{
		super(context);
		mItem = item;
		this.adapterPosition = adapterPosition;
		LayoutInflater.from(context).inflate(R.layout.item_detail, this, true);
		mViewHolder = new ViewHolderItem(this);
		update();
		if (mViewHolder.mediaLayout != null) {
			List<MediaItemViewModel> downloadedMedia = mItem.getMediaItems(true);
			if (downloadedMedia == null || downloadedMedia.size() == 0) {
				ViewGroup.LayoutParams params = mViewHolder.mediaLayout.getLayoutParams();
				params.height = getResources().getDimensionPixelSize(R.dimen.download_media_view_height);
				mViewHolder.mediaLayout.setLayoutParams(params);
			}
		}
	}

	public void update() {
		mViewHolder.bindModel(mItem, false, false, this);
	}

	public void setListener(ItemViewListener listener) {
		mListener = listener;
	}

	public ItemViewModel getItem()
	{
		return mItem;
	}

	public int getAdapterPosition() { return adapterPosition; }

	private AnimatedRelativeLayout getAnimatedRoot()
	{
		if (mViewHolder != null)
		{
			return (AnimatedRelativeLayout) mViewHolder.itemView.findViewById(R.id.animatedRoot);
		}
		return null;
	}

	/**
	 * Use this method to set optional initial starting positions for the views.
	 *
	 * @param storedPositions
	 */
	public void setStoredPositions(SparseArray<Rect> storedPositions)
	{
		// Animations?
		AnimatedRelativeLayout animatedRoot = getAnimatedRoot();
		if (animatedRoot != null)
		{
			animatedRoot.setAnimationDuration(ItemExpandActivity.EXPANSION_ANIMATION_DURATION);
			animatedRoot.setStartPositions(storedPositions);
		}
	}

	public void resetToStoredPositions(SparseArray<Rect> storedPositions, int duration)
	{
		AnimatedRelativeLayout animatedRoot = getAnimatedRoot();
		if (animatedRoot != null)
		{
			animatedRoot.setStartPositions(storedPositions);
			animatedRoot.setAnimationDuration(duration);
			animatedRoot.animateToStartPositions();
		}
	}

	private SparseArray<Rect> getStoredPositions()
	{
		SparseArray<Rect> positions = null;
		if (mViewHolder != null)
		{
			AnimatedRelativeLayout animatedRoot = (AnimatedRelativeLayout) mViewHolder.itemView.findViewById(R.id.animatedRoot);
			if (animatedRoot != null)
			{
				positions = new SparseArray<>();
				for (int iChild = 0; iChild < animatedRoot.getChildCount(); iChild++)
				{
					View child = animatedRoot.getChildAt(iChild);
					if (child.getId() != View.NO_ID)
					{
						Rect currentRect = new Rect(child.getLeft(), child.getTop(), child.getLeft() + child.getWidth(), child.getTop() + child.getHeight());
						positions.put(child.getId(), currentRect);
					}
				}
			}
		}
		return positions;
	}

	public void recycle() {
		if (LOGGING)
			Log.d(LOGTAG, "Recycling ItemView: " + toString());
		mListener = null;
		mViewHolder.unbindModel();
	}

	@Override
	public void onItemSelected(ItemViewModel item) {

	}

	@Override
	public void onTagSelected(String tag) {

	}

	@Override
	public void onSourceSelected(long feedId) {

	}

	@Override
	public void onMediaSelected(ItemViewModel item, MediaItemViewModel mediaItem) {
		final Bundle mediaData = new Bundle();
		mediaData.putSerializable("media", mediaItem);
		App.getInstance().onCommand(getContext(), R.integer.command_view_media, mediaData);
	}

	@Override
	public void onMediaDownloaded(ItemViewModel item, MediaItemViewModel mediaItem) {
		if (LOGGING)
			Log.v(LOGTAG, "Media content has requested relayout.");

		// At least one downloaded, reset to full height
		//
		if (mViewHolder.mediaLayout != null) {
			ViewGroup.LayoutParams params = mViewHolder.mediaLayout.getLayoutParams();
			params.height = ViewGroup.LayoutParams.MATCH_PARENT;
			mViewHolder.mediaLayout.setLayoutParams(params);
		}

		AnimatedRelativeLayout animatedRoot = getAnimatedRoot();
		if (animatedRoot != null)
		{
			animatedRoot.setStartPositions(getStoredPositions());
			animatedRoot.requestLayout();
		}

		if (mListener != null) {
			mListener.onMediaDownloaded(item, mediaItem);
		}
	}

	@Override
	public void onCursorUpdated() {

	}

	public boolean showFullText() {
		return mViewHolder.showFullText();
	}

	public void setShowFullText(boolean show) {
		mViewHolder.setShowFullText(show);
	}
}

