package info.guardianproject.securereaderinterface.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.KeyEvent;
import android.widget.EditText;

import java.util.regex.Matcher;

/**
 * This is a variation of the EditText class that will hide the hint text when it has
 * focus (normal EditText will always show the hint if the input text is empty, even when
 * is has focus).
 * Also, the hint text size can be adjusted relative to the set text size of the widget
 * by calling {@link #setHintRelativeSize(float)}.
 *
 */
public class UrlInputEditText extends EditText
{
	private CharSequence mHint;
	private float mHintRelativeSize = 1.0f;

	public UrlInputEditText(Context context)
	{
		super(context);
		init();
	}

	public UrlInputEditText(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public UrlInputEditText(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init();
	}

	/**
	 * Get the hint size as a fraction of the normal text size. Default is 1.0 (hint text
	 * is the same size as input text).
	 * @return hint size as a fraction of the normal text size.
	 * @see #setHintRelativeSize(float)
	 */
	public float getHintRelativeSize()
	{
		return mHintRelativeSize;
	}

	/**
	 * Set the hint size as a fraction of the normal text size.
	 * @param relativeSize hint size as a fraction of the normal text size.
	 * @see #getHintRelativeSize()
	 */
	public void setHintRelativeSize(float relativeSize)
	{
		mHintRelativeSize = relativeSize;
		
		// If we have focus, the change will be picked up later, otherwise need to
		// change it now.
		if (!hasFocus())
			setAndResizeHint();
	}
	
	private void init()
	{
		mHint = getHint();
		setAndResizeHint();
		InputFilter filter = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
				// Is result ok?
				SpannableStringBuilder ssb = new SpannableStringBuilder(dest);
				ssb.replace(dstart, dend, source, start, end);

				Matcher m = Patterns.WEB_URL.matcher(ssb.toString());
				if (!m.matches() && !m.hitEnd()) {
					return "";
				}
				return null;
			}
		};
		setFilters(new InputFilter[] { filter });
	}

	private void setAndResizeHint()
	{
		if (!TextUtils.isEmpty(mHint))
		{
			SpannableString span = new SpannableString(mHint);
			span.setSpan(new RelativeSizeSpan(getHintRelativeSize()), 0, mHint.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			super.setHint(span);
		}
	}
	
	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
	{
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (focused)
			super.setHint("");
		else
			setAndResizeHint();
	}
	
    @Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
        {
           this.clearFocus();
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
