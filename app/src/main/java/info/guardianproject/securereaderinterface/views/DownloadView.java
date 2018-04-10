package info.guardianproject.securereaderinterface.views;

import info.guardianproject.securereaderinterface.AppActivity;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * A special FrameLayout that will inset itself by the current toolbar height
 */
public class DownloadView extends FrameLayout implements AppBarLayout.OnOffsetChangedListener {

	public DownloadView(Context context) {
		super(context);
	}

	public DownloadView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DownloadView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (getContext() instanceof AppActivity)
		{
			AppActivity activity = (AppActivity) getContext();
			Toolbar toolbar = activity.getToolbar();
			((AppBarLayout) toolbar.getParent()).addOnOffsetChangedListener(this);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		if (getContext() instanceof AppActivity)
		{
			AppActivity activity = (AppActivity) getContext();
			Toolbar toolbar = activity.getToolbar();
			((AppBarLayout) toolbar.getParent()).removeOnOffsetChangedListener(this);
		}
		super.onDetachedFromWindow();
	}

	private void setTopOffset(final int offset)
	{
		if (!ViewCompat.isInLayout(this)) {
			setPadding(getPaddingLeft(), offset, getPaddingRight(), getPaddingBottom());
		}
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
		int visible = appBarLayout.getHeight() + verticalOffset;
		setTopOffset(visible);
	}
}
