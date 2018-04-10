package info.guardianproject.securereaderinterface.widgets;

import info.guardianproject.securereaderinterface.uiutil.AllCapsTransformation;
import info.guardianproject.securereaderinterface.uiutil.FontManager;
import info.guardianproject.securereaderinterface.R;
import android.annotation.SuppressLint;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;

public class CustomFontTextViewHelper implements TextWatcher {
	private TextView mView;
	private boolean mInTransform;
	
	@SuppressLint({ "InlinedApi", "NewApi" })
	public CustomFontTextViewHelper(TextView view, AttributeSet attrs)
	{
		mView = view;
		boolean mUseAllCaps = false;
		if (attrs != null)
		{
			TypedArray a = mView.getContext().obtainStyledAttributes(attrs, R.styleable.CustomFontTextView);
			String fontName = a.getString(R.styleable.CustomFontTextView_fontName);
			if (fontName != null && !mView.isInEditMode())
			{
				Typeface font = FontManager.getFontByName(mView.getContext(), fontName);
				if (font != null)
					mView.setTypeface(font);
			}
			a.recycle();
			
			a = mView.getContext().obtainStyledAttributes(attrs, new int[] { android.R.attr.textAllCaps });
			mUseAllCaps = a.getBoolean(0, false);
			a.recycle();
		}
		
		// All caps does not work with Spanned text, so turn it off and handle in setText()
		if (mUseAllCaps)
		{
			if (Build.VERSION.SDK_INT >= 14)
				mView.setAllCaps(false);
			mView.setTransformationMethod(new AllCapsTransformation(mView.getContext()));
		}
		
		if (mView.getHint() != null)
		{
			mView.setHint(FontManager.transformText(mView, mView.getHint()));
		}
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if (!mInTransform)
		{
			mInTransform = true;
			CharSequence t = FontManager.transformText(mView, s);
			if (t != s)
			{
				int start = mView.getSelectionStart();
				int end = mView.getSelectionEnd();
				int len = s.length();
				int delta = len - t.length();
				mView.setText(t);
				if (mView instanceof EditText)
				{
					((EditText) mView).setSelection(Math.max(-1, start - delta), Math.max(-1, end - delta));
				}
			}
			mInTransform = false;
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{
	}
	
	public void setTextAppearance(int resid)
	{
		TypedArray a = mView.getContext().getTheme().obtainStyledAttributes(resid, R.styleable.CustomFontTextView);
		if (a != null)
		{
			String fontName = a.getString(R.styleable.CustomFontTextView_fontName);
			if (fontName != null)
			{
				Typeface font = FontManager.getFontByName(mView.getContext(), fontName);
				if (font != null)
				{
					mView.setTypeface(font);
					mView.setText(FontManager.transformText(mView, mView.getText()));
					if (mView.getHint() != null)
					{
						mView.setHint(FontManager.transformText(mView, mView.getHint()));
					}
				}
			}
			a.recycle();
		}
	}
}
