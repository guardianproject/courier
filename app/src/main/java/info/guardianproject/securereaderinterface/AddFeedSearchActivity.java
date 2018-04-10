package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.widgets.CustomFontTextView;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class AddFeedSearchActivity extends AppActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentViewNoBase(R.layout.activity_add_feed_search);

		// Display home as up
		setDisplayHomeAsUp(true);
		
		final SearchView searchView = (SearchView) findViewById(R.id.toolbar_search);
		searchView.setOnQueryTextListener(new OnQueryTextListener()
		{
			@Override
			public boolean onQueryTextSubmit(String query) {
				InputMethodManager imm = (InputMethodManager) searchView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				Handler threadHandler = new Handler();
				if (!imm.hideSoftInputFromWindow(searchView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS, new ResultReceiver(threadHandler)
				{
					private String query;
					ResultReceiver init(String query)
					{
						this.query = query;
						return this;
					}
					
					@Override
					protected void onReceiveResult(int resultCode, Bundle resultData)
					{
						super.onReceiveResult(resultCode, resultData);
						doSearch(query);
					}
				}.init(query)))
				{
					doSearch(query);
				}
		
				return true;
			}
		
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
			
		});
		
		CustomFontTextView title = (CustomFontTextView) findViewById(R.id.toolbar_title);
		title.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Click title to start new search!
				enterSearch();
			}
		});
	}

	@Override
	protected boolean useLeftSideMenu()
	{
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		return true; // No menu, but need to return true to get "home" in onOptionsItemSelected below.
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			exitSearch();
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	
	
	@Override
	public void onBackPressed() {
		exitSearch();
		super.onBackPressed();
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		finish();
	}

	private void doSearch(String term)
	{
		CustomFontTextView title = (CustomFontTextView) findViewById(R.id.toolbar_title);
		title.setText(term);
		SearchView sv = (SearchView)findViewById(R.id.toolbar_search);
		sv.setVisibility(View.GONE);
		title.setVisibility(View.VISIBLE);
		AddFeedSearchFragment f = (AddFeedSearchFragment) getSupportFragmentManager().findFragmentByTag("search");
		if (f != null)
			f.search(term);
		else
		{
			f = AddFeedSearchFragment.newInstance(term);
			getSupportFragmentManager().beginTransaction().add(R.id.results_container, f, "search").addToBackStack("search").commit();
		}
	}
	
	private void enterSearch()
	{
		exitSearch();
		CustomFontTextView title = (CustomFontTextView) findViewById(R.id.toolbar_title);
		SearchView sv = (SearchView)findViewById(R.id.toolbar_search);
		title.setVisibility(View.GONE);
		sv.setVisibility(View.VISIBLE);
		sv.requestFocus();
	}
	
	private void exitSearch()
	{
		AddFeedSearchFragment f = (AddFeedSearchFragment) getSupportFragmentManager().findFragmentByTag("search");
		if (f != null)
			f.saveResults();
	}
}

