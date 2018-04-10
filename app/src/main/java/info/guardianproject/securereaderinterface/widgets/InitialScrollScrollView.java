package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * This class is a normal ScrollView with the additional functionality of supporting
 * initial scroll to a given child id. Using the normal ScrollView the only way this
 * can be done is through post:ing a call to scrollTo(), but this has the negative
 * side effect of the view first being shown, then being scrolled - causing flickering. 
 */
public class InitialScrollScrollView extends ScrollView
{
	private int mScrollToViewId = View.NO_ID;
	private int mScrollToViewOffset = 0;

	public InitialScrollScrollView(Context context)
	{
		super(context);
	}

	public InitialScrollScrollView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public InitialScrollScrollView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}
	
	public void setInitialPosition(int id, int offset)
	{
		mScrollToViewId = id;
		mScrollToViewOffset = offset;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		super.onLayout(changed, l, t, r, b);
		if (getChildCount() > 0 && mScrollToViewId != View.NO_ID && getChildAt(0) instanceof ViewGroup)
		{
			// Find the child
			ViewGroup content = (ViewGroup) getChildAt(0);
			View child = content.findViewById(mScrollToViewId);
			if (child != null)
			{
				int posChild = UIHelpers.getRelativeTop(child);
				int posParent = UIHelpers.getRelativeTop(this);
				int offsetNow = posChild - posParent;
				scrollTo(0, offsetNow - mScrollToViewOffset);
				mScrollToViewId = View.NO_ID; //reset
			}
		}
	}
}
