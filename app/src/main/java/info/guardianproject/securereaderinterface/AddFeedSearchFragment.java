package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.OPMLParser;
import info.guardianproject.securereader.OPMLParser.OPMLOutline;

import java.util.ArrayList;
import java.util.regex.Pattern;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.adapters.FeedListSearchAdapter;

import com.tinymission.rss.Feed;

public class AddFeedSearchFragment extends Fragment implements Filterable
{
	public static final String LOGTAG = "AddFeedSearchResultsFragment";
	public static final boolean LOGGING = false;
	public static final String SEARCH_URL_FORMAT = "?lang=%1$s&term=%2$s&desc=1";

	private ListView mListFeeds;
	private FeedListSearchAdapter mFeedListSearchAdapter;
	private ProgressBar mSpinnerSearch;
	private TextView mTvNotFound;

	private String mSearchTerm;
	private Filter mFilter;
	
	private boolean mSearchingRemote;
	private boolean mSearchingLocal;
	
    public static AddFeedSearchFragment newInstance(String searchTerm) {

    	AddFeedSearchFragment fragment = new AddFeedSearchFragment();

        Bundle arguments = new Bundle();
        arguments.putString("term", searchTerm);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchTerm = getArguments().getString("term");
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View mRootView = inflater.inflate(R.layout.add_feed_search_fragment, container, false);
		mSpinnerSearch = (ProgressBar) mRootView.findViewById(R.id.spinnerSearch);
		mSpinnerSearch.setVisibility(View.GONE);
		mListFeeds = (ListView) mRootView.findViewById(R.id.listFeeds);
        mFeedListSearchAdapter = new FeedListSearchAdapter(mListFeeds.getContext());
		mListFeeds.setAdapter(mFeedListSearchAdapter);
		mTvNotFound = (TextView) mRootView.findViewById(R.id.tvNotFound);		
		doSearch();
		return mRootView;
	}

	public void search(String term)
	{
		mFeedListSearchAdapter.clear();
		mSearchTerm = term;
		doSearch();
	}
	
	private void doSearch()
	{
		mTvNotFound.setVisibility(View.GONE);
		mListFeeds.setVisibility(View.GONE);
		if (mSearchTerm.length() > 0)
		{
			mSearchingLocal = true;
			mSearchingRemote = true;
			mSpinnerSearch.setVisibility(View.VISIBLE);
			
			getFilter().filter(mSearchTerm);
			
			String term = Uri.encode(mSearchTerm);
			String lang = App.getSettings().uiLanguageCode();
			String searchUrl = getString(R.string.feed_search_url) + String.format(SEARCH_URL_FORMAT, lang, term);
			OPMLParser mParser = new OPMLParser(App.getInstance().socialReader, searchUrl, new OPMLParser.OPMLParserListener() {
				@Override
				public void opmlParsed(ArrayList<OPMLOutline> outlines) {

					ArrayList<Feed> result = new ArrayList<>();

					if (LOGGING)
						Log.v(LOGTAG, "Finished Parsing search OPML");
					if (outlines != null) {
						for (int i = 0; i < outlines.size(); i++) {
							OPMLOutline outlineElement = outlines.get(i);
							Feed newFeed = new Feed(outlineElement.text, outlineElement.xmlUrl);
							newFeed.setSubscribed(false);
							newFeed.setDescription(outlineElement.description);
							newFeed.setCategory(outlineElement.category);
							result.add(newFeed);
						}
					} else {
						if (LOGGING)
							Log.e(LOGTAG, "Received null search OPML");
					}

					mFeedListSearchAdapter.setRemoteMatches(result);
					mSearchingRemote = false;
					checkSearchComplete();
				}
			});
		}
	}

	@Override
	public Filter getFilter()
	{
		if (mFilter == null)
		{
			mFilter = new Filter()
			{
				@Override
				protected FilterResults performFiltering(CharSequence constraint)
				{
					ArrayList<Feed> filteredFeeds = null;

					ArrayList<Feed> allUnfollowedFeeds = App.getInstance().socialReader.getUnsubscibedFeedsList();
						if (allUnfollowedFeeds != null)
						{
							Pattern pattern = Pattern.compile(Pattern.quote(constraint.toString()), Pattern.CASE_INSENSITIVE);
							filteredFeeds = new ArrayList<>();
							for (Feed feed : allUnfollowedFeeds)
							{
								boolean titleMatches = (!TextUtils.isEmpty(feed.getTitle()) && pattern.matcher(feed.getTitle()).find());
								boolean descriptionMatches = (!TextUtils.isEmpty(feed.getDescription()) && pattern.matcher(feed.getDescription()).find());
								if (titleMatches || descriptionMatches)
								{
									filteredFeeds.add(feed);
								}
							}
						}
					FilterResults results = new FilterResults();
					results.count = (filteredFeeds != null) ? filteredFeeds.size() : 0;
					results.values = filteredFeeds;
					return results;
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint, FilterResults results)
				{
					if (results.count > 0)
						mFeedListSearchAdapter.setLocalMatches((ArrayList<Feed>) results.values);
					else
						mFeedListSearchAdapter.setLocalMatches(null);
					mSearchingLocal = false;
					checkSearchComplete();
				}
			};
		}

		return mFilter;
	}
	
	private void checkSearchComplete()
	{
		if (!mSearchingLocal && !mSearchingRemote)
		{
			mSpinnerSearch.setVisibility(View.GONE);
			if (mFeedListSearchAdapter.getCount() == 0)
				mTvNotFound.setVisibility(View.VISIBLE);
		}
		if (mFeedListSearchAdapter.getCount() > 0)
			mListFeeds.setVisibility(View.VISIBLE);
	}
	
	public void saveResults() {
		if (mFeedListSearchAdapter != null)
		{
			for (Feed feedToAdd : this.mFeedListSearchAdapter.getSelectedFeeds())
			{
				App.getInstance().socialReader.subscribeFeed(feedToAdd);
			}
		}
	}


}