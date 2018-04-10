package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

public class AnimatedRelativeLayout extends RelativeLayout
{
	public interface AnimatedRelativeLayoutListener {
		void onAnimatedToEndPositions();
		void onAnimatedToStartPositions();
	}

	private int mAnimationDuration = 1000; // default
	private SparseArray<Rect> mStartPositions;
	private SparseArray<Rect> mEndPositions;
	private float mAnimationValue;
	private boolean mAnimating;
	private boolean mAttached;
	private boolean mLaidOut;
	private AnimatedRelativeLayoutListener mListener;

	public AnimatedRelativeLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public AnimatedRelativeLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public AnimatedRelativeLayout(Context context)
	{
		super(context);
	}

	public void setStartPositions(SparseArray<Rect> startPositions)
	{
		mStartPositions = startPositions;
	}

	public void animateToStartPositions()
	{
		applyAnimation(true, mAnimationDuration);
	}

	public void setListener(AnimatedRelativeLayoutListener listener) {
		mListener = listener;
	}

	public void setAnimationDuration(int milliseconds) {
		if (milliseconds < 0)
			mAnimationDuration = 1000;
		else
			mAnimationDuration = milliseconds;
	}

	public boolean isAnimating()
	{
		return mAnimating;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (changed)
		{
			mLaidOut = true;
			applyAnimation(false, mAnimationDuration);
		}
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		mAttached = true;
		applyAnimation(false, mAnimationDuration);
	}
	
	@Override
	protected void onDetachedFromWindow() 
	{
		super.onDetachedFromWindow();
		mAttached = false;
	}

	private void applyAnimation(final boolean reversed, int duration)
	{
		if (mStartPositions != null && mStartPositions.size() > 0 && !mAnimating && mAttached && mLaidOut)
		{
			mEndPositions = new SparseArray<>();
			for (int index = mStartPositions.size() - 1; index >= 0; index--)
			{
				int id = mStartPositions.keyAt(index);

				View view = findViewById(id);
				if (view != null)
				{
					Rect currentRect = new Rect(view.getLeft(), view.getTop(), view.getLeft() + view.getWidth(), view.getTop() + view.getHeight());

					// Adjust start rect with paddings for current view
					Rect startRect = mStartPositions.get(id);
					startRect.offset(-view.getPaddingLeft(), -view.getPaddingTop());
					startRect.right += view.getPaddingLeft() + view.getPaddingRight();
					startRect.bottom += view.getPaddingTop() + view.getPaddingBottom();
					mStartPositions.put(id, startRect);
					
					if (startRect.equals(currentRect))
						mStartPositions.remove(id);
					else
						mEndPositions.put(id, currentRect);
				}
			}

			final LayoutAnim anim = new LayoutAnim(reversed);
			anim.setDuration(duration);
			anim.setFillBefore(true);
			anim.setFillAfter(true);
			anim.setAnimationListener(new AnimationListener()
			{
				@Override
				public void onAnimationEnd(Animation animation)
				{
					post(new Runnable()
					{
						@Override
						public void run()
						{
							if (!reversed)
								resetAnimatedProperties();
							if (mListener != null) {
								if (!reversed) {
									mListener.onAnimatedToEndPositions();
								} else {
									mListener.onAnimatedToStartPositions();
								}
							}
						}
					});
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
			mAnimationValue = reversed ? 1.0f : 0;
			mAnimating = true;
			this.startAnimation(anim);
		} else if (mListener != null) {
			if (!reversed) {
				mListener.onAnimatedToEndPositions();
			} else {
				mListener.onAnimatedToStartPositions();
			}
		}
	}
	
	private void resetAnimatedProperties()
	{
		mAnimating = false;
		mStartPositions = null;
		this.clearAnimation();
	}

	public class LayoutAnim extends Animation
	{
		private boolean mIsReversed;

		public LayoutAnim(boolean reversed)
		{
			mIsReversed = reversed;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t)
		{
			mAnimationValue = mIsReversed ? (1.0f - interpolatedTime) : interpolatedTime;
			invalidate();
		}

		@Override
		public boolean willChangeBounds()
		{
			return false;
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime)
	{
		boolean clipSet = false;
		int sc = canvas.save();

		if (isAnimating())
		{
			if (child.getId() != View.NO_ID)
			{
				if (mStartPositions != null && mStartPositions.indexOfKey(child.getId()) >= 0)
				{
					Rect startRect = mStartPositions.get(child.getId());
					Rect endRect = mEndPositions.get(child.getId());
					if (startRect != null && endRect != null) {
						int height = (int) (startRect.height() + (mAnimationValue * (endRect.height() - startRect.height())));

						int leftDelta = (int) ((1 - mAnimationValue) * (startRect.left - endRect.left));
						int topDelta = (int) ((1 - mAnimationValue) * (startRect.top - endRect.top));
						clipSet = true;
						canvas.clipRect(child.getLeft() + leftDelta, child.getTop() + topDelta, child.getRight() + leftDelta, child.getTop() + topDelta + height,
								Op.INTERSECT);
						canvas.translate(leftDelta, topDelta);
					}
				}
			}
		}
		if (!clipSet)
		{
			canvas.clipRect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom(),
				Op.INTERSECT);
		}
		boolean ret = super.drawChild(canvas, child, drawingTime);
		canvas.restoreToCount(sc);

		return ret;
	}
}
