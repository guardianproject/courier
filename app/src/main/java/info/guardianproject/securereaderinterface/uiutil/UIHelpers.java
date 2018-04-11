package info.guardianproject.securereaderinterface.uiutil;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;

public class UIHelpers
{
	public static final String LOGTAG = "UIHelpers";
	public static final boolean LOGGING = false;	
	
	public static int dpToPx(int dp, Context ctx)
	{
		Resources r = ctx.getResources();
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
	}

	public static String dateDateDisplayString(Date date, Context context)
	{
		if (date == null)
			return "";
		return DateFormat.getDateInstance().format(date);
	}

	public static String dateTimeDisplayString(Date date, Context context)
	{
		if (date == null)
			return "";
		return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
	}
	
	public static String dateDiffDisplayString(Date date, Context context, int idStringNever, int idStringRecently, int idStringMinutes, int idStringMinute,
			int idStringHours, int idStringHour, int idStringDays, int idStringDay)
	{
		if (date == null)
			return "";

		Date todayDate = new Date();
		double ti = todayDate.getTime() - date.getTime();
		if (ti < 0)
			ti = -ti;
		ti = ti / 1000; // Convert to seconds
		if (ti < 1)
		{
			return context.getString(idStringNever);
		}
		else if (ti < 60)
		{
			return context.getString(idStringRecently);
		}
		else if (ti < 3600 && (int) Math.round(ti / 60) < 60)
		{
			int diff = (int) Math.round(ti / 60);
			if (diff == 1)
				return context.getString(idStringMinute, diff);
			return context.getString(idStringMinutes, diff);
		}
		else if (ti < 86400 && (int) Math.round(ti / 60 / 60) < 24)
		{
			int diff = (int) Math.round(ti / 60 / 60);
			if (diff == 1)
				return context.getString(idStringHour, diff);
			return context.getString(idStringHours, diff);
		}
		else
		// if (ti < 2629743)
		{
			int diff = (int) Math.round(ti / 60 / 60 / 24);
			if (diff == 1)
				return context.getString(idStringDay, diff);
			return context.getString(idStringDays, diff);
		}
		// else
		// {
		// return context.getString(idStringNever);
		// }
	}

	public static int getRelativeLeft(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getLeft();
		else
			return myView.getLeft() + UIHelpers.getRelativeLeft((View) myView.getParent());
	}

	public static int getRelativeTop(View myView)
	{
		if (myView.getParent() == myView.getRootView())
			return myView.getTop();
		else
			return myView.getTop() + UIHelpers.getRelativeTop((View) myView.getParent());
	}

	/**
	 * Get the coordinates of a view relative to another anchor view. The anchor
	 * view is assumed to be in the same view tree as this view.
	 * 
	 * @param anchorView
	 *            View relative to which we are getting the coordinates
	 * @param view
	 *            The view to get the coordinates for
	 * @return A Rect containing the view bounds
	 */
	public static Rect getRectRelativeToView(View anchorView, View view)
	{
		Rect ret = new Rect(getRelativeLeft(view) - getRelativeLeft(anchorView), getRelativeTop(view) - getRelativeTop(anchorView), 0, 0);
		ret.right = ret.left + view.getWidth();
		ret.bottom = ret.top + view.getHeight();
		return ret;
	}

	// From.
	// http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
	/**
	 * Given either a Spannable String or a regular String and a token, apply
	 * the given CharacterStyle to the span between the tokens, and also remove
	 * tokens.
	 * <p>
	 * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##",
	 * new ForegroundColorSpan(0xFFFF0000));} will return a CharSequence
	 * {@code "Hello world!"} with {@code world} in red.
	 * 
	 * @param text
	 *            The text, with the tokens, to adjust.
	 * @param token
	 *            The token string; there should be at least two instances of
	 *            token in text.
	 * @param cs
	 *            The style to apply to the CharSequence. WARNING: You cannot
	 *            send the same two instances of this parameter, otherwise the
	 *            second call will remove the original span.
	 * @return A Spannable CharSequence with the new style applied.
	 * 
	 * @see http://developer.android.com/reference/android/text/style/CharacterStyle
	 *      .html
	 */
	public static CharSequence setSpanBetweenTokens(CharSequence text, String token, CharacterStyle... cs)
	{
		// Start and end refer to the points where the span will apply
		int tokenLen = token.length();
		int start = text.toString().indexOf(token) + tokenLen;
		int end = text.toString().indexOf(token, start);

		if (start > -1 && end > -1)
		{
			// Copy the spannable string to a mutable spannable string
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			for (CharacterStyle c : cs)
				ssb.setSpan(c, start, end, 0);

			// Delete the tokens before and after the span
			ssb.delete(end, end + tokenLen);
			ssb.delete(start - tokenLen, start);

			text = ssb;
		}

		return text;
	}

	public static void colorizeDrawable(Context context, int idAttr, Drawable drawable)
	{
		if (drawable == null)
			return;

		TypedValue outValue = new TypedValue();
		context.getTheme().resolveAttribute(idAttr, outValue, true);
		colorizeDrawableWithColor(context, outValue.data, drawable);
	}

	public static void colorizeDrawableWithColor(Context context, int color, Drawable drawable)
	{
		if (drawable == null)
			return;
		if ((color & 0xff000000) != 0)
			drawable.setColorFilter(color, Mode.SRC_ATOP);
		else
			drawable.setColorFilter(null);
	}
	
	public static void hideSoftKeyboard(Activity activity)
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		View view = activity.getCurrentFocus();
		if (view != null)
			inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static void populateContainerWithSVG(View rootView, int idSVG, int idContainer) {
		try {
			SVG svg = SVG.getFromResource(rootView.getContext(), idSVG);

			SVGImageView svgImageView = new SVGImageView(rootView.getContext());
			svgImageView.setSVG(svg);
			svgImageView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			ViewGroup layout = (ViewGroup) rootView.findViewById(idContainer);
			layout.addView(svgImageView);
		} catch (SVGParseException e) {
			e.printStackTrace();
		}
	}
}
