package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.FeedFetcher.FeedFetchedCallback;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapter;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapterExplore;
import info.guardianproject.securereaderinterface.adapters.FeedListAdapterExplore.FeedListAdapterExploreListener;
import info.guardianproject.securereaderinterface.uiutil.HttpTextWatcher;
import info.guardianproject.securereaderinterface.widgets.UrlInputEditText;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListView;

import info.guardianproject.securereaderinterface.R;
import com.tinymission.rss.Feed;

public class AddFeedFragment extends Fragment implements FeedFetchedCallback, info.guardianproject.securereaderinterface.adapters.FeedListAdapter.FeedListAdapterListener, FeedListAdapterExploreListener
{
	public static final String LOGTAG = "AddFeedFragment";
	public static final boolean LOGGING = false;
	
	private ListView mListFeeds;
	private UrlInputEditText mEditManualUrl;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View rootView = inflater.inflate(R.layout.add_feed_fragment, container, false);
		mListFeeds = (ListView) rootView.findViewById(R.id.listFeeds);
		Button mBtnAddManualUrl = (Button) rootView.findViewById(R.id.btnAddManualUrl);
		mBtnAddManualUrl.setEnabled(false);
		mBtnAddManualUrl.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				Handler threadHandler = new Handler();
				if (!imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS, new ResultReceiver(threadHandler)
				{
					@Override
					protected void onReceiveResult(int resultCode, Bundle resultData)
					{
						super.onReceiveResult(resultCode, resultData);
						doAddFeed();
					}
				}))
				{
					doAddFeed(); // Keyboard not open
				}
			}
		});
		mEditManualUrl = (UrlInputEditText) rootView.findViewById(R.id.editManualUrl);
		mEditManualUrl.setHintRelativeSize(0.7f);
		mEditManualUrl.addTextChangedListener(new HttpTextWatcher(rootView.getContext(), mBtnAddManualUrl));
		
		Intent intent = getActivity().getIntent();
		String action = intent.getAction();
		if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null)
		{
			mEditManualUrl.setText(intent.getData().toString());
		}

		
		return rootView;
	}

	private void doAddFeed()
	{
		App.getInstance().socialReader.addFeedByURL(mEditManualUrl.getText().toString(), AddFeedFragment.this);
		updateList();
		mEditManualUrl.setText("");
	}

	@Override
	public void onResume()
	{
		super.onResume();
		updateList();
	}

	public void updateList()
	{
		ArrayList<Feed> feeds = App.getInstance().socialReader.getFeedsList();
		boolean following = getArguments().getBoolean("following");
		if (following)
			mListFeeds.setAdapter(new FeedListAdapter(mListFeeds.getContext(), this, feeds));
		else
			mListFeeds.setAdapter(new FeedListAdapterExplore(mListFeeds.getContext(), this, feeds));
	}

	@Override
	public void feedFetched(Feed _feed)
	{
		// We have now downloaded information about manually added feed, so
		// update list!
		if (LOGGING)
			Log.v(LOGTAG, "Feed " + _feed.getFeedURL() + " loaded, update list");
		App.getInstance().socialReader.subscribeFeed(_feed);
		updateList();
	}

	@Override
	public void feedError(Feed _feed) {
		//TODO refactoring
	}


	@Override
	public void onFeedUnfollow(Feed feed) {
		App.getInstance().socialReader.unsubscribeFeed(feed);
	}

	@Override
	public void onFeedDelete(Feed feed) {
		App.getInstance().socialReader.removeFeed(feed);
		updateList();
	}

	@Override
	public void onFeedFollow(Feed feed) {
		App.getInstance().socialReader.subscribeFeed(feed);
	}
}
