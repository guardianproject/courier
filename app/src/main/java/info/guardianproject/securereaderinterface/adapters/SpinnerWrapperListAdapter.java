package info.guardianproject.securereaderinterface.adapters;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

public class SpinnerWrapperListAdapter implements ListAdapter
{
	public static final String LOGTAG = "SpinnerWrapperListAdapter";
	public static final boolean LOGGING = false;	

	private SpinnerAdapter mAdapter;

	public SpinnerWrapperListAdapter(SpinnerAdapter adapter)
	{
		mAdapter = adapter;
	}
	
	@Override
	public void registerDataSetObserver(DataSetObserver observer)
	{
		mAdapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer)
	{
		mAdapter.unregisterDataSetObserver(observer);
	}

	@Override
	public int getCount()
	{
		return mAdapter.getCount();
	}

	@Override
	public Object getItem(int position)
	{
		return mAdapter.getItem(position);
	}

	@Override
	public long getItemId(int position)
	{
		return mAdapter.getItemId(position);
	}

	@Override
	public boolean hasStableIds()
	{
		return mAdapter.hasStableIds();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		return mAdapter.getDropDownView(position, convertView, parent);
	}

	@Override
	public int getItemViewType(int position)
	{
		return mAdapter.getItemViewType(position);
	}

	@Override
	public int getViewTypeCount()
	{
		return mAdapter.getViewTypeCount();
	}

	@Override
	public boolean isEmpty()
	{
		return mAdapter.isEmpty();
	}

	@Override
	public boolean areAllItemsEnabled()
	{
		return true;
	}

	@Override
	public boolean isEnabled(int position)
	{
		return true;
	}

}
