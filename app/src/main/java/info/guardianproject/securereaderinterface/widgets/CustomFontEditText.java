package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.uiutil.FontManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class CustomFontEditText extends EditText {

	private CustomFontTextViewHelper mHelper;

	public CustomFontEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public CustomFontEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public CustomFontEditText(Context context) {
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs)
	{
		mHelper = new CustomFontTextViewHelper(this, attrs);
		addTextChangedListener(mHelper);
	}
	
	@Override
	public void setText(CharSequence text, BufferType type)
	{
		super.setText(FontManager.transformText(this, text), type);
	}
	
	@Override
	public void setTextAppearance(Context context, int resid)
	{
		super.setTextAppearance(context, resid);
		mHelper.setTextAppearance(resid);
	}
}
