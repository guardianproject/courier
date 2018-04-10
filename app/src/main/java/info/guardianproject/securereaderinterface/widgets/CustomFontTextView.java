package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.SettingsUI;
import info.guardianproject.securereaderinterface.uiutil.FontManager;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;
import info.guardianproject.securereaderinterface.R;

public class CustomFontTextView extends AppCompatTextView implements SharedPreferences.OnSharedPreferenceChangeListener {
	private Rect mBounds;
	private Shader mShader;
	private CustomFontTextViewHelper mHelper;
	private boolean mAllowSizeAdjustment;
	private float mOriginalTextSize;

	public CustomFontTextView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public CustomFontTextView(Context context)
	{
		this(context, null, 0);
	}

	public CustomFontTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		mBounds = new Rect();
		mHelper = new CustomFontTextViewHelper(this, attrs);
		mOriginalTextSize = getTextSize();

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomFontTextView, defStyle, 0);
		mAllowSizeAdjustment = a.getBoolean(R.styleable.CustomFontTextView_allow_size_adjustment, false);
		a.recycle();
		mShader = getPaint().getShader();
		updateTextSize();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (heightMeasureSpec == MeasureSpec.UNSPECIFIED || heightMeasureSpec == MeasureSpec.AT_MOST)
		{
			if (getLineCount() > 0)
			{
				int bottom = 0;
				int n = getVisibleLines(this.getMeasuredHeight());
				if (n > 0)
				{
					this.getLineBounds(n - 1, mBounds);
					bottom = mBounds.bottom;
				}
				super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(bottom + this.getPaddingBottom(), MeasureSpec.EXACTLY));
			}
		}
	}

	public int getVisibleLines()
	{
		return getVisibleLines(getHeight());
	}

	private int getVisibleLines(int height)
	{
		if (getLineCount() > 0 && height > 0 && getLayout() != null)
		{
			Rect bounds = new Rect();
			for (int i = 0; i < getLineCount(); i++)
			{
				getLineBounds(i, bounds);
				if ((bounds.bottom) > (height - this.getPaddingBottom()))
				{
					return i;
				}
			}
			return getLineCount(); // All fit
		}
		return 0;
	}

	@Override
	public void setText(CharSequence text, BufferType type)
	{
		super.setText(FontManager.transformText(this, text), type);
		updateTextSize();
	}

	@Override
	public void setTextAppearance(Context context, int resid)
	{
		super.setTextAppearance(context, resid);
		mHelper.setTextAppearance(resid);
	}

	@Override
	public void setTextSize(float size) {
		super.setTextSize(size);
		mOriginalTextSize = super.getTextSize();
		updateTextSize();
	}

	@Override
	public void setTextSize(int unit, float size) {
		super.setTextSize(unit, size);
		mOriginalTextSize = getTextSize();
		updateTextSize();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (mAllowSizeAdjustment) {
			App.getSettings().registerChangeListener(this);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		if (mAllowSizeAdjustment) {
			App.getSettings().unregisterChangeListener(this);
		}
		super.onDetachedFromWindow();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (SettingsUI.KEY_CONTENT_FONT_SIZE_ADJUSTMENT.equalsIgnoreCase(key)) {
			updateTextSize();
		}
	}

	void updateTextSize() {
		if (mAllowSizeAdjustment) {
			float adjustment = App.getSettings().getContentFontSizeAdjustment();
			adjustment /= 100f; // Now -0.5 -> 1.0
			super.setTextSize(TypedValue.COMPLEX_UNIT_PX, mOriginalTextSize + mOriginalTextSize * adjustment);
		}
	}
}
