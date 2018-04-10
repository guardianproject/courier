package info.guardianproject.securereaderinterface;

import java.util.ArrayList;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereader.SecureShareContentProvider;
import info.guardianproject.securereaderinterface.widgets.CustomFontCheckableButton;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


public class AddFeedActivity extends AppActivity
{
	public static final String LOGTAG = "AddFeedActivity";
	public static final boolean LOGGING = false;
	
	AddFeedPagerAdapter mPagerAdapter;
	ViewPager mViewPager;
	
	private ArrayList<View> mTabs;

	@SuppressLint("NewApi") @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_feed);
		setMenuIdentifier(R.menu.activity_add_feed);
		setDisplayHomeAsUp(true);
		
		mPagerAdapter = new AddFeedPagerAdapter(getSupportFragmentManager());

		setActionBarTitle(getString(R.string.title_activity_add_feed));
		
		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setAdapter(mPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab.
		// We can also use ActionBar.Tab#select() to do this if we have a
		// reference to the
		// Tab.
		mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
		{
			@Override
			public void onPageSelected(int position)
			{
				setSelectedTab(position);
			}
		});

		ViewGroup mTabContainer = (ViewGroup) findViewById(R.id.tab_container);
		mTabs = new ArrayList<>();
		
		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mPagerAdapter.getCount(); i++)
		{
			// Create a tab with text corresponding to the page title defined by
			// the adapter.
			// Also specify this Activity object, which implements the
			// TabListener interface, as the
			// listener for when this tab is selected.
			CharSequence title = mPagerAdapter.getPageTitle(i);
			CustomFontCheckableButton tab = (CustomFontCheckableButton) LayoutInflater.from(this).inflate(R.layout.actionbar_tab, mTabContainer, false);
			tab.setText(title);
			mTabs.add(tab);
			tab.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int index = mTabs.indexOf(v);
					mViewPager.setCurrentItem(index, false);
				}
			});
			mTabContainer.addView(tab);
		}
		mTabs.get(0).setSelected(true);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_search)
		{
			Intent newIntent = new Intent(this, AddFeedSearchActivity.class);
			startActivityForResult(newIntent, 1);
			return true;
		} else if (item.getItemId() == R.id.menu_share) {
			exportOPML();
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onUnlockedActivityResult(requestCode, resultCode, data);

		// When returning from a search, go to "following" tab
		if (requestCode == 1)
			mViewPager.setCurrentItem(0, false);
	}
	
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the primary sections of the app.
	 */
	public class AddFeedPagerAdapter extends FragmentPagerAdapter
	{
		AddFeedFragment mFragmentFollowing;
		AddFeedFragment mFragmentExplore;

		AddFeedPagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		@Override
		public Fragment getItem(int i)
		{
			Bundle args = new Bundle();
			if (i == 0)
			{
				if (mFragmentFollowing == null)
				{
					args.putBoolean("following", true);
					mFragmentFollowing = new AddFeedFragment();
					mFragmentFollowing.setArguments(args);
				}
				return mFragmentFollowing;
			}
			else if (i == 1)
			{
				if (mFragmentExplore == null)
				{
					args.putBoolean("following", false);
					mFragmentExplore = new AddFeedFragment();
					mFragmentExplore.setArguments(args);
				}
				return mFragmentExplore;
			}
			return null;
		}

		@Override
		public int getCount()
		{
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position)
		{
			switch (position)
			{
			case 0:
				return getString(R.string.add_feed_tab_following).toUpperCase();
			case 1:
				return getString(R.string.add_feed_tab_explore).toUpperCase();
			}
			return null;
		}

		void updateAdapterFollowing()
		{
			if (mFragmentFollowing != null)
				mFragmentFollowing.updateList();
		}
		
		void updateAdapterExplore()
		{
			if (mFragmentExplore != null)
				mFragmentExplore.updateList();
		}

	}

//	@Override
//	protected void onResume()
//	{
//		super.onResume();
//		if (mPagerAdapter != null)
//		{
//			mPagerAdapter.updateAdapterFollowing();
//			mPagerAdapter.updateAdapterExplore();
//		}
//	}

	private void setSelectedTab(int index)
	{
		for (int i = 0; i < mTabs.size(); i++)
		{
			View view = mTabs.get(i);
			view.setSelected(i == index);
		}
		if (index == 0)
			this.mPagerAdapter.updateAdapterFollowing();
		else if (index == 1)
			this.mPagerAdapter.updateAdapterExplore();			
	}

	private void exportOPML() {
		String opmlFileName = getString(R.string.app_name) + ".opml";
		File sharedFile = App.getInstance().socialReader.exportSubscribedFeedsAsOPML(opmlFileName);
		if (sharedFile != null) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			Uri sharedUri = Uri.parse(SecureShareContentProvider.CONTENT_URI + "opml/" + opmlFileName);
			intent.putExtra(Intent.EXTRA_STREAM, sharedUri);
			intent.setType("text/x-opml");
			Intent chooser = Intent.createChooser(intent, "Export OPML");
			startActivity(chooser);
		}
	}
}
