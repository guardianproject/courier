package info.guardianproject.securereaderinterface.adapters;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SyncStatus;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.ui.UIBroadcaster;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

import java.util.ArrayList;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tinymission.rss.Feed;

public class FeedListAdapter extends BaseAdapter
{
	public final static String LOGTAG = "FeedListAdapter";
	public static final boolean LOGGING = false;

	public interface FeedListAdapterListener
	{
		void onFeedUnfollow(Feed feed);
		void onFeedDelete(Feed feed);
	}

	protected final Context mContext;
	private final FeedListAdapterListener mListener;
	protected final LayoutInflater mInflater;

	protected final ArrayList<Feed> mItems;
	protected final ArrayList<Feed> mItemsUnfiltered;

	private FeedSwipeListener mCurrentOperationsView; // Only allow operations for one view
	// at a time!
	protected int mOperationButtonsOffsetMax;

	public FeedListAdapter(Context context, FeedListAdapterListener listener, ArrayList<Feed> feeds)
	{
		super();
		mContext = context;
		mListener = listener;
		mItemsUnfiltered = feeds;
		mItems = new ArrayList<>();
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		filterItems();
		mOperationButtonsOffsetMax = UIHelpers.dpToPx(136, context);
		if (App.getInstance().isRTL())
			mOperationButtonsOffsetMax = -mOperationButtonsOffsetMax;
	}

	protected void filterItems()
	{
		mItems.clear();
		for (Feed feed : mItemsUnfiltered)
		{
			if (feed.isSubscribed())
				mItems.add(feed);
		}
	}
	
	@Override
	public int getCount()
	{
		return mItems.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mItems.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public void notifyDataSetChanged()
	{
		filterItems();
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Feed feed = (Feed)getItem(position);

		View view;
		if (convertView == null)
		{
			view = mInflater.inflate(R.layout.add_feed_list_item_following, parent, false);
		}
		else
		{
			view = convertView;
		}

		// ImageView iv = (ImageView) view.findViewById(R.id.ivFeedIcon);
		// if (feedModel.feed.getImageManager() != null)
		// feedModel.feed.getImageManager().download(feedModel.feed.,
		// imageView)
		// App.getInstance().socialReader.loadDisplayImageMediaContent(feedModel.feed.getImageManager(),
		// iv);

		// Name
		TextView tv = (TextView) view.findViewById(R.id.tvFeedName);
		tv.setText(feed.getTitle());
		tv.setTextColor(ContextCompat.getColor(mContext, R.color.feed_list_title_normal));

		SyncStatus feedError = App.getInstance().socialReader.syncStatus(feed);
		if (!feedError.equals(SyncStatus.OK)) {
			tv.setText(R.string.add_feed_error_loading);
			tv.setTextColor(ContextCompat.getColor(mContext, R.color.feed_list_title_error));
		} else if (TextUtils.isEmpty(feed.getTitle())) {
			tv.setText(R.string.add_feed_not_loaded);
		}

		// Description
		tv = (TextView) view.findViewById(R.id.tvFeedDescription);
		tv.setText(feed.getDescription());
		if (TextUtils.isEmpty(feed.getTitle()) || tv.getText().length() == 0)
			tv.setText(feed.getFeedURL());

		// Icon
		final ImageView feedIcon = (ImageView) view.findViewById(R.id.ivFeedIcon);
		if (feedIcon != null) {
			File feedIconFile = new File(App.getInstance().socialReader.getFileSystemDir(), SocialReader.FEED_ICON_FILE_PREFIX + feed.getDatabaseId());
			ViewGroup.LayoutParams size = feedIcon.getLayoutParams();
			Picasso.get().load(Uri.parse(feedIconFile.getAbsolutePath()))
					.resize(size.width, size.height)
					.placeholder(R.drawable.ic_filter_logo_placeholder)
					.into(feedIcon);
		}

		// Operation?
		View operationButtons = view.findViewById(R.id.llOperationButtons);
		
		View btnRemove = view.findViewById(R.id.btnRemove);
		View btnDelete = view.findViewById(R.id.btnDelete);
		btnRemove.setTag(feed);
		btnRemove.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Feed feed = (Feed) v.getTag();
				if (mListener != null)
					mListener.onFeedUnfollow(feed);
				notifyDataSetChanged();
			}
		});
		btnDelete.setTag(feed);
		btnDelete.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Feed feed = (Feed) v.getTag();
				if (mListener != null)
					mListener.onFeedDelete(feed);
				mItemsUnfiltered.remove(feed);
				notifyDataSetChanged();
			}
		});
		view.setOnClickListener(new FeedClickListener(feed));
		view.setOnTouchListener(new View.OnTouchListener() {
			GestureDetector gestureDetector;
			FeedSwipeListener swipeListener;
			public View.OnTouchListener init(View view, Feed feed, View operationsView)
			{
				swipeListener = new FeedSwipeListener(mContext, mOperationButtonsOffsetMax, view, feed, operationsView);
				gestureDetector = new GestureDetector(mContext, swipeListener);
				return this;
			}
			
            public boolean onTouch(View v, MotionEvent event) {
            	if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
            		swipeListener.animateToOpenOrClosed();
                return gestureDetector.onTouchEvent(event);
            }
        }.init(view, feed, operationButtons));
		return view;
	}

	protected class FeedClickListener implements OnClickListener
	{
		private Feed mFeed;
		
		public FeedClickListener(Feed feed)
		{
			mFeed = feed;
		}

		@Override
		public void onClick(View v) {
			UIBroadcaster.setFeedFilter(v.getContext(), new FeedSelection(mFeed.getDatabaseId()));
			App.getInstance().onCommand(FeedListAdapter.this.mContext, R.integer.command_news_list, null);
		}
	}	
	
	protected class FeedSwipeListener extends SimpleOnGestureListener
	{
		private View mView;
		private Feed mFeed;
		private final View mOperationsView;
	
		int swipeMinDistance;
		int swipeThresholdVelocity;
		int swipeMaxOffPath;

		float translateOffsetMax;
		float translateOffsetAtStart;
		float translateOffsetCurrent;
		
		public FeedSwipeListener(Context context, float offsetMax, View view, Feed feed, View operationsView)
		{
			mFeed = feed;
			mView = view;
			mOperationsView = operationsView;
			translateOffsetMax = offsetMax;
			AnimationHelpers.translateX(mOperationsView, 0, translateOffsetMax, 0);
			translateOffsetCurrent = translateOffsetMax;
			ViewConfiguration vc = ViewConfiguration.get(context);
			swipeMinDistance = vc.getScaledPagingTouchSlop();
			swipeThresholdVelocity = vc.getScaledMinimumFlingVelocity();
			swipeMaxOffPath = vc.getScaledTouchSlop();
		}
		
		
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
        	if (this != mCurrentOperationsView)
        	{
        		if (mCurrentOperationsView != null)
        			mCurrentOperationsView.hideOperationButtons();
        		mCurrentOperationsView = this;
        	}
        	
        	float translate;
			if (App.getInstance().isRTL()) {
				if (translateOffsetAtStart == 0) {
					float dx = Math.min(0, e2.getX() - e1.getX());
					translate = Math.max(translateOffsetMax, dx);
				} else {
					float dx = Math.min(0, e1.getX() - e2.getX());
					translate = Math.min(0, translateOffsetMax - dx);
				}
			} else {
				if (translateOffsetAtStart == 0) {
					float dx = Math.max(0, e2.getX() - e1.getX());
					translate = Math.min(translateOffsetMax, dx);
				} else {
					float dx = Math.max(0, e1.getX() - e2.getX());
					translate = Math.max(0, translateOffsetMax - dx);
				}
			}
        	AnimationHelpers.translateX(mOperationsView, 0, translate, 0);
        	translateOffsetCurrent = translate;
			return false;
		}

		@Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
            	float dx = e1.getX() - e2.getX();
            	float dy = Math.abs(e1.getY() - e2.getY()); 
            	if (dy > this.swipeMaxOffPath)
            	{
            		animateToOpenOrClosed();
            		return false;
            	}

				if (App.getInstance().isRTL())
					dx = -dx;

                // right to left swipe
                if(dx > swipeMinDistance && Math.abs(velocityX) > swipeThresholdVelocity)
                {
                	if (this != mCurrentOperationsView)
                	{
                		if (mCurrentOperationsView != null)
                			mCurrentOperationsView.hideOperationButtons();
                		mCurrentOperationsView = this;
                	}
                	showOperationButtons();
                }
                else if (-dx > swipeMinDistance && Math.abs(velocityX) > swipeThresholdVelocity)
                {
                	if (this == mCurrentOperationsView)
            		{
                		mCurrentOperationsView = null;
                		hideOperationButtons();
                	}
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

        @Override
		public boolean onSingleTapUp(MotionEvent e) {
			mView.performClick();
        	return true;
		}

		@Override
        public boolean onDown(MotionEvent e) 
		{
			translateOffsetAtStart = translateOffsetCurrent;
			return true;
        }
		
		public void animateToOpenOrClosed()
		{
			if (translateOffsetAtStart == 0)
			{
				if (Math.abs(translateOffsetCurrent)  > this.swipeMinDistance)
				{
					hideOperationButtons();
				}
				else
				{
					showOperationButtons();
				}
			}
			else
			{
				if (Math.abs((translateOffsetMax - translateOffsetCurrent))  < this.swipeMinDistance)
				{
					hideOperationButtons();
				}
				else
				{
					showOperationButtons();
				}
			}
		}
			
		public void showOperationButtons()
		{
			long time = Math.abs((long)((500.0f * translateOffsetCurrent) / translateOffsetMax));
			AnimationHelpers.translateX(mOperationsView, translateOffsetCurrent, 0, time);
    		translateOffsetCurrent = 0;
		}
		
		public void hideOperationButtons()
		{
			long time = Math.abs((long)((500.0f * (translateOffsetMax - translateOffsetCurrent)) / translateOffsetMax));
    		AnimationHelpers.translateX(mOperationsView, translateOffsetCurrent, translateOffsetMax, time);
    		translateOffsetCurrent = translateOffsetMax;
		}
    }
}
