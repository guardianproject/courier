package info.guardianproject.securereaderinterface.uiutil;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;

import java.util.HashMap;

public class FontManager
{
	public static final String LOGTAG = "FontManager";
	public static final boolean LOGGING = false;
	
	private static HashMap<String, Typeface> gFonts = new HashMap<>();
	
	public static Typeface getFontByName(Context context, String name)
	{
		// Content font is a font that can be changed in settings!
		if (name.equalsIgnoreCase("contentFont")) {
			name = "Lucida Grande";
		}

		if (gFonts.containsKey(name))
			return gFonts.get(name);

		try
		{
			Typeface font = Typeface.createFromAsset(context.getAssets(), "fonts/" + name + ".ttf");
			if (font != null)
			{
				gFonts.put(name, font);
			}
			return font;
		}
		catch (Exception ex)
		{
			if (LOGGING)
				Log.e(LOGTAG, "Failed to get font: " + name);
		}
		return null;
	}

	public static CharSequence transformText(TextView view, CharSequence text)
	{
		return text;
	}
	

}
