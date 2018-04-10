package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import info.guardianproject.securereaderinterface.R;

public class HeightLimitedLinearLayout extends LinearLayout
{
	private float mHeightLimit;
	private int mDrawHeightLimit;
	private Rect mClipRect;

	public HeightLimitedLinearLayout(Context context)
	{
		super(context);
		initView(context, null);
	}

	public HeightLimitedLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context, attrs);
	}

	private void initView(Context context, AttributeSet attrs)
	{
		mHeightLimit = 0;
		mDrawHeightLimit = -1;
		if (attrs != null)
		{
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.HeightLimitedLinearLayout);
			mHeightLimit = a.getFloat(R.styleable.HeightLimitedLinearLayout_height_limit, 0);
			a.recycle();
		}
	}

	public void setHeightLimit(float heightLimit)
	{
		mHeightLimit = heightLimit;
	}

	/**
	 * Use this to "cut off" the view when drawing, i.e. apply a clip so that it
	 * is at most drawHeightLimit pixels high.
	 * 
	 * @param drawHeightLimit
	 *            Max height in pixels.
	 */
	public void setDrawHeightLimit(int drawHeightLimit)
	{
		mDrawHeightLimit = drawHeightLimit;
		invalidate();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = this.getMeasuredWidth();
		if (mHeightLimit != 0 && width > 0)
		{
			int height = (int) (width / mHeightLimit);
			ViewGroup.LayoutParams lp = getLayoutParams();
			if (lp != null && lp.width == LayoutParams.MATCH_PARENT)
				super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
			else
				super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
		}
	}

	@Override
	public void draw(Canvas canvas)
	{
		if (mDrawHeightLimit != -1)
		{
			if (mClipRect == null)
				mClipRect = new Rect(0, 0, getWidth(), mDrawHeightLimit);
			mClipRect.bottom = mClipRect.top + mDrawHeightLimit;
			canvas.clipRect(mClipRect, Op.REPLACE);
		}
		super.draw(canvas);
	}
}
