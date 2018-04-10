package info.guardianproject.securereaderinterface.uiutil;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

/**
 * A TextWatcher that makes sure that "http://" is added to input if a correct
 * scheme is not given.
 * 
 */
public class HttpTextWatcher implements TextWatcher
{
	private boolean mHasAddedScheme;
	private boolean mChanging;
	private final View mEnableDisableThisViewOnInput;

	public HttpTextWatcher(Context context, View enableDisableThisViewOnInput)
	{
		mEnableDisableThisViewOnInput = enableDisableThisViewOnInput;
		mHasAddedScheme = false;
	}

	@Override
	public void afterTextChanged(Editable s)
	{
		if (!mChanging)
		{
			String ss = s.toString();
			if (ss.indexOf('.') != -1 && !ss.contains("://"))
			{
				mChanging = true;
				mHasAddedScheme = true;
				s.insert(0, "http://");
				mChanging = false;
			}
		}
		if (mEnableDisableThisViewOnInput != null)
			mEnableDisableThisViewOnInput.setEnabled(s.length() > 0);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after)
	{

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count)
	{

	}
}