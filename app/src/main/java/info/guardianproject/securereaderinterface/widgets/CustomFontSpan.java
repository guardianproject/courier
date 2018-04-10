package info.guardianproject.securereaderinterface.widgets;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class CustomFontSpan extends MetricAffectingSpan
{
	private final Typeface newType;

	public CustomFontSpan(Typeface type)
	{
		super();
		newType = type;
	}

	@Override
	public void updateDrawState(TextPaint ds)
	{
		applyCustomTypeFace(ds, newType);
	}

	@Override
	public void updateMeasureState(TextPaint paint)
	{
		applyCustomTypeFace(paint, newType);
	}

	private static void applyCustomTypeFace(Paint paint, Typeface tf)
	{
        final Typeface oldTypeface = paint.getTypeface();
        final int oldStyle = oldTypeface != null ? oldTypeface.getStyle() : 0;
        final int fakeStyle = oldStyle & ~tf.getStyle();

        if ((fakeStyle & Typeface.BOLD) != 0)
        {
            paint.setFakeBoldText(true);
        }

        if ((fakeStyle & Typeface.ITALIC) != 0)
        {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);

	}
}
