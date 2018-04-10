package info.guardianproject.securereaderinterface.ui;

import android.content.Context;

public class PackageHelper extends info.guardianproject.securereader.PackageHelper {

	public static final String URI_CHATSECURE = "info.guardianproject.otr.app.im";
	public static final String URI_CHATSECURE_PLAY = "market://search?q=pname:info.guardianproject.otr.app.im";

	public static final String URI_ORWEB = "info.guardianproject.browser";
	public static final String URI_ORWEB_PLAY = "market://search?q=pname:info.guardianproject.browser";

	public static final String URI_FBREADER_PLAY = "https://play.google.com/store/apps/details?id=org.geometerplus.zlibrary.ui.android";
	public static final String URI_FBREADER_WEB = "http://fbreader.org/FBReaderJ";
	
	public static boolean isChatSecureInstalled(Context context) {
		return isAppInstalled(context, URI_CHATSECURE);
	}

	public static boolean isOrwebInstalled(Context context)
	{
		return isAppInstalled(context, URI_ORWEB);
	}
}
