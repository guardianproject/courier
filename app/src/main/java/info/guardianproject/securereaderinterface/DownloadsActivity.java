package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.adapters.DownloadsAdapter;
import info.guardianproject.securereaderinterface.models.FeedSelection;
import info.guardianproject.securereaderinterface.models.ItemViewModel;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class DownloadsActivity extends AppActivity implements DownloadsAdapter.DownloadsAdapterListener {
	public static final boolean LOGGING = false;
	public static final String LOGTAG = "DownloadsActivity";

	private DownloadsAdapter downloadsAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_downloads);
		setMenuIdentifier(R.menu.activity_downloads);

		// Display home as up
		setDisplayHomeAsUp(true);
		
		// Set up the action bar.
		setActionBarTitle(getString(R.string.downloads_title));

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.lvRoot);
		recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
		downloadsAdapter = new DownloadsAdapter(this);
		downloadsAdapter.setListener(this);
		recyclerView.setAdapter(downloadsAdapter);
	}

	@Override
	protected void onWipe()
	{
		super.onWipe();
		downloadsAdapter.notifyDataSetChanged();
	}

	@Override
	protected boolean useLeftSideMenu()
	{
		return true;
	}

	@Override
	public void onItemSelected(ItemViewModel item) {
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_ITEM, item);
		intent.putExtra(MainActivity.INTENT_EXTRA_SHOW_THIS_FEED_SELECTION, new FeedSelection(item.item.getFeedId()));
		startActivity(intent);
	}
}
