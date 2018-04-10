package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.uiutil.FontManager;
import android.content.Context;
import android.util.AttributeSet;

public class CustomFontCheckableButton extends CheckableButton {

	@SuppressWarnings("unused")
	private CustomFontTextViewHelper mHelper;

	public CustomFontCheckableButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs);
	}

	public CustomFontCheckableButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs);
	}

	public CustomFontCheckableButton(Context context)
	{
		super(context);
		init(null);
	}
	
	private void init(AttributeSet attrs)
	{
		mHelper = new CustomFontTextViewHelper(this, attrs);
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
