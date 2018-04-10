package info.guardianproject.securereaderinterface.uiutil;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;

public class ViewExpander
{
	public static final int DEFAULT_EXPAND_DURATION = 500;
	public static final int DEFAULT_COLLAPSE_DURATION = 500;

	private int mCurrentTop = -1;
	private View mView;

	public ViewExpander(View view)
	{
		mView = view;
	}

	public void prepareDraw(Canvas canvas)
	{
		if (mCurrentTop != 0)
		{
			canvas.translate(0, -mCurrentTop);
			canvas.clipRect(new Rect(0, mCurrentTop, mView.getWidth(), mView.getHeight()), Op.REPLACE);
		}
	}

	public void setSize(int top)
	{
		mCurrentTop = top;
		mView.invalidate();
	}

	/**
	 * Expand the view from the collapsed size (need to call
	 * {@link #setCollapsedSize(int, int, int)} first) using the default
	 * duration.
	 */
	public void expand()
	{
		expand(DEFAULT_EXPAND_DURATION);
	}

	/**
	 * Expand the view from the collapsed size (need to call
	 * {@link #setCollapsedSize(int, int, int)} first) using the given duration.
	 * 
	 * @param duration
	 *            Duration in milliseconds.
	 */
	public void expand(int duration)
	{
		if (mCurrentTop == 0)
			mCurrentTop = mView.getHeight();
		final ExpandAnim anim = new ExpandAnim(mCurrentTop, 0);
		anim.setDuration(duration);
		mView.startAnimation(anim);
		mView.setVisibility(View.VISIBLE);
	}

	public void collapse()
	{
		collapse(DEFAULT_COLLAPSE_DURATION);
	}

	public void collapse(int duration)
	{
		final ExpandAnim anim = new ExpandAnim(mCurrentTop, mView.getHeight());
		anim.setDuration(duration);
		anim.setAnimationListener(new AnimationListener()
		{

			@Override
			public void onAnimationEnd(Animation animation)
			{
				mView.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
			}

			@Override
			public void onAnimationStart(Animation animation)
			{
			}
		});
		mView.startAnimation(anim);
	}

	public class ExpandAnim extends Animation
	{
		int fromTop;
		int toTop;

		public ExpandAnim(int fromTop, int toTop)
		{
			this.fromTop = fromTop;
			this.toTop = toTop;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			int newTop;
			newTop = (int) (fromTop + ((toTop - fromTop) * interpolatedTime));
			setSize(newTop);
		}

		@Override
		public boolean willChangeBounds()
		{
			return true;
		}
	}
}
