package info.guardianproject.securereaderinterface.uiutil;

import java.util.Locale;

import android.content.Context;
import android.graphics.Rect;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.TransformationMethod;
import android.view.View;

public class AllCapsTransformation implements TransformationMethod
{
	private Locale mLocale;

	public AllCapsTransformation(Context context)
	{
		 mLocale = context.getResources().getConfiguration().locale;
	}
	
	@Override
	public CharSequence getTransformation(CharSequence source, View view)
	{
		CharSequence ret = source;
		if (ret != null)
		{
			if (source instanceof Spanned)
			{
				ret = new SpannableString(ret.toString().toUpperCase(mLocale));
				TextUtils.copySpansFrom((Spanned)source, 0, source.length(), Object.class, (Spannable) ret, 0);
			}
			else
			{
				ret = ret.toString().toUpperCase(mLocale);
			}
		}
		return ret;
	}

	@Override
	public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect)
	{
	}
}