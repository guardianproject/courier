package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.Checkable;

public class CheckableButton extends Button implements Checkable
{
	private boolean mChecked;

	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	public CheckableButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public CheckableButton(Context context)
	{
		super(context);
	}

	public CheckableButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public int[] onCreateDrawableState(int extraSpace)
	{
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked())
		{
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

	public void toggle()
	{
		setChecked(!mChecked);
	}

	public boolean isChecked()
	{
		return mChecked;
	}

	public void setChecked(boolean checked)
	{
		if (mChecked != checked)
		{
			mChecked = checked;
			refreshDrawableState();
		}
	}
}
