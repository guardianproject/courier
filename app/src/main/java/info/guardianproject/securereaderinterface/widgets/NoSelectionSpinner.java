package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.widgets.compat.Spinner;
import android.content.Context;
import android.util.AttributeSet;

public class NoSelectionSpinner extends Spinner
{
	public NoSelectionSpinner(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public NoSelectionSpinner(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public NoSelectionSpinner(Context context)
	{
		super(context);
	}

	// TODO - Now the base Spinner widget is actually a "no selection" spinner
	// implementation already. If and when the base class  is updated to handle selection we are gonna need the code below again.
	// @Override
	// public void setSelection(int position, boolean animate)
	// {
	// setSelection(position);
	// }
	//
	// @Override
	// public void setSelection(int position)
	// {
	// if (this.getOnItemSelectedListener() != null)
	// {
	// if (position == INVALID_POSITION)
	// this.getOnItemSelectedListener().onNothingSelected(this);
	// else
	// this.getOnItemSelectedListener().onItemSelected(this, null, position,
	// getItemIdAtPosition(position));
	// }
	// }

	// @Override
	// public Object getSelectedItem()
	// {
	// return null;
	// }
	//
	// @Override
	// public long getSelectedItemId()
	// {
	// return INVALID_ROW_ID;
	// }
	//
	// @Override
	// public int getSelectedItemPosition()
	// {
	// return INVALID_POSITION;
	// }

}
