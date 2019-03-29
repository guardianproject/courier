package info.guardianproject.securereaderinterface.adapters;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereader.SyncStatus;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import java.util.ArrayList;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.tinymission.rss.Feed;

public class FeedListAdapterExplore extends FeedListAdapter
{
	public final static String LOGTAG = "FeedListAdapterExplore";
	public static final boolean LOGGING = false;

	public interface FeedListAdapterExploreListener
	{
		void onFeedFollow(Feed feed);
		void onFeedUnfollow(Feed feed);
		void onFeedDelete(Feed feed);
	}

	private final FeedListAdapterExploreListener mListener;

	public FeedListAdapterExplore(Context context, FeedListAdapterExploreListener listener, ArrayList<Feed> feeds)
	{
		super(context, null, feeds);
		mListener = listener;
		mOperationButtonsOffsetMax = UIHelpers.dpToPx(68, context);
		if (App.getInstance().isRTL())
			mOperationButtonsOffsetMax = -mOperationButtonsOffsetMax;
	}
	
	protected void filterItems()
	{
		mItems.clear();
		mItems.addAll(mItemsUnfiltered);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		Feed feed = (Feed)getItem(position);

		View view;
		if (convertView == null)
		{
			view = mInflater.inflate(R.layout.add_feed_list_item_explore, parent, false);
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
					.resize(0, size.height)
					.placeholder(R.drawable.ic_filter_logo_placeholder)
					.into(feedIcon);
		}

		View btnOff = view.findViewById(R.id.btnOff);
		View btnOn = view.findViewById(R.id.btnOn);
		btnOn.setVisibility(feed.isSubscribed() ? View.VISIBLE : View.GONE);
		btnOff.setVisibility(feed.isSubscribed() ? View.GONE : View.VISIBLE);
		btnOff.setTag(feed);
		btnOff.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Feed feed = (Feed) v.getTag();
				if (mListener != null)
					mListener.onFeedFollow(feed);
				notifyDataSetChanged();
			}
		});
		btnOn.setTag(feed);
		btnOn.setOnClickListener(new View.OnClickListener()
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
		
		
		// Operation?
		View operationButtons = view.findViewById(R.id.llOperationButtons);
		
		View btnDelete = view.findViewById(R.id.btnDelete);
		btnDelete.setTag(feed);
		btnDelete.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Feed feed = (Feed) v.getTag();
				if (mListener != null)
					mListener.onFeedDelete(feed);
				mItems.remove(feed);
				notifyDataSetChanged();
			}
		});
		
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
}
