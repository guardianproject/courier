package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.models.ViewPagerIndicator;
import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;


public class NestedViewPager extends ViewPager
{
	private ViewPagerIndicator mViewPagerIndicator;
	private float mPositionOffset;

	public NestedViewPager(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		super.addOnPageChangeListener(new OnPageChangeListener()
		{
			@Override
			public void onPageScrollStateChanged(int arg0)
			{
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
			{
				mPositionOffset = positionOffset;
			}

			@Override
			public void onPageSelected(int arg0)
			{
				if (mViewPagerIndicator != null)
				{
					mViewPagerIndicator.onCurrentChanged(arg0);
				}
			}
		});
	}

	@Override
	public boolean canScrollHorizontally(int direction)
	{
		if (direction < 0 && getCurrentItem() == 0 && mPositionOffset == 0)
		{
			return false;
		}
		else if (direction > 0 && mPositionOffset == 0)
		{
			if (getAdapter() != null)
			{
				int current = getCurrentItem();
				int count = getAdapter().getCount();
				if (current >= (count - 1))
				{
					return false;
				}
			}
		}
		return true;
	}

	public void setViewPagerIndicator(ViewPagerIndicator viewPagerIndicator)
	{
		mViewPagerIndicator = viewPagerIndicator;
		updateViewPagerIndicator();
	}

	/**
	 * If a ViewPagerIndicator is set (using
	 * {@link #setViewPagerIndicator(ViewPagerIndicator)}) this will update it
	 * with total number of items and the current item.
	 */
	private void updateViewPagerIndicator()
	{
		if (mViewPagerIndicator != null)
		{
			if (getAdapter() != null)
				mViewPagerIndicator.onTotalChanged(getAdapter().getCount());
			else
				mViewPagerIndicator.onTotalChanged(0);
			mViewPagerIndicator.onCurrentChanged(this.getCurrentItem());
		}
	}

	private final DataSetObserver mDataSetObserver = new DataSetObserver()
	{
		@Override
		public void onChanged()
		{
			if (mViewPagerIndicator != null)
			{
				if (getAdapter() != null)
					mViewPagerIndicator.onTotalChanged(getAdapter().getCount());
			}
		}
	};

	@Override
	public void setAdapter(PagerAdapter adapter)
	{
		// Unregister old, if any
		if (getAdapter() != null)
			getAdapter().unregisterDataSetObserver(mDataSetObserver);
		if (adapter != null)
			adapter.registerDataSetObserver(mDataSetObserver);
		super.setAdapter(adapter);

		updateViewPagerIndicator();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		if (getAdapter() != null)
			getAdapter().notifyDataSetChanged();
	}
}
