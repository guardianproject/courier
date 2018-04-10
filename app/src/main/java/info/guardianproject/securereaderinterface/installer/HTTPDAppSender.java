//https://gist.github.com/komamitsu/1893396
package info.guardianproject.securereaderinterface.installer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import info.guardianproject.securereaderinterface.AppActivity;
import info.guardianproject.securereaderinterface.R;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class HTTPDAppSender extends AppActivity
{
	public static final String LOGTAG = "HTTPDAppSender";
	public static final boolean LOGGING = false;
	
	private static class AppInfo
	{
		public String packageName;
		public int resIdIcon;
		public String title;
		public String description;

		public AppInfo(String packageName, String title, String description, int resIdIcon)
		{
			this.packageName = packageName;
			this.title = title;
			this.description = description;
			this.resIdIcon = resIdIcon;
		}
	}

	public AppInfo[] APPS_TO_DISPLAY;
	private static final HashMap<String, Integer> IMAGE_MAP;
	static
	{
		IMAGE_MAP = new HashMap<>();
		IMAGE_MAP.put("background_debut_light.png", R.drawable.background_debut_light);
		IMAGE_MAP.put("ic_action_logo.png", R.drawable.ic_action_logo);
		IMAGE_MAP.put("ic_share_promo.png", R.drawable.ic_share_promo);
	}

	private static final int PORT = 8080;

	private final Handler handler = new Handler();
	private TextView textView;
	private MyHTTPD server;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		APPS_TO_DISPLAY = new AppInfo[] {
					new AppInfo("org.torproject.android", "", "", 0),
					null,
					new AppInfo("info.guardianproject.mrapp", "", "", 0),
					new AppInfo("info.guardianproject.otr.app.im", "", "", 0),
					new AppInfo("info.guardianproject.browser", "", "", 0) };
		APPS_TO_DISPLAY[1] = new AppInfo(getPackageName(), "", "", 0); 
		
		setContentView(R.layout.httpd_app_sender);
		setDisplayHomeAsUp(true);
		textView = (TextView) findViewById(R.id.tvUrl);

		setMenuIdentifier(R.menu.activity_httpd_app_sender);
		setActionBarTitle(getString(R.string.title_activity_httpd_app_sender));
	}

	private CharSequence getHtmlTemplate() throws IOException
	{
		InputStream inputStream = this.getResources().getAssets().open("app_share.html");

		BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder total = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null)
		{
			total.append(line);
		}
		return total;
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		// This could be the answer to setting it up automatically:
		// http://www.whitebyte.info/android/android-wifi-hotspot-manager-class
		// https://github.com/nickrussler/Android-Wifi-Hotspot-Manager-Class

		// This doesn't work with tethering
		WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
		if (LOGGING) 
			Log.v(LOGTAG, "WifiManager Raw IP:" + ipAddress);
		final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
		if (LOGGING)
			Log.v(LOGTAG,"WifiManager IP: " + formatedIpAddress);
		textView.setText("http://" + formatedIpAddress + ":" + PORT );

		// WifiManager not giving info about hotspot, loop through networks, look for 192.
		if (ipAddress < 1) {
			try
			{
				List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
				for (NetworkInterface intf : interfaces)
				{
					List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
					for (InetAddress addr : addrs)
					{
						if (!addr.isLoopbackAddress() && addr.getAddress().length == 4 
								&& !addr.getHostAddress().contains("0.0.0.0") 
								&& addr.getHostAddress().startsWith("192"))
						{
							textView.setText("http://" + addr.getHostAddress() + ":" + PORT);	
							break;
						}
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		try
		{
			server = new MyHTTPD();
			server.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (server != null)
			server.stop();
	}

	private class MyHTTPD extends NanoHTTPD
	{
		public MyHTTPD() throws IOException
		{
			super(PORT);
		}

		@Override
		public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files)
		{
			if (LOGGING) 
				Log.v(LOGTAG, "Request for: " + uri);

			if (uri.equals("/"))
			{
				StringBuilder responseText = new StringBuilder();

				try
				{
					CharSequence template = getHtmlTemplate();
					String templateStr = template.toString();

					templateStr = templateStr.replaceAll("<!-- APPLICATION_NAME -->", getString(R.string.app_name));
					
					Pattern pattern = Pattern.compile("<!-- BEGIN_APP_ROW -->(.*?)<!-- END_APP_ROW -->", Pattern.DOTALL);
					Matcher matcher = pattern.matcher(templateStr);
					// StringBuffer buffer = new StringBuffer();
					if (matcher.find())
					{
						String rowTemplate = matcher.group(1);

						StringBuilder strRows = new StringBuilder();

						PackageManager packageManager = HTTPDAppSender.this.getPackageManager();
						List<PackageInfo> packages = packageManager.getInstalledPackages(0);
						for (PackageInfo packageInfo : packages)
						{
							// Is it one of our packages?
							AppInfo info = getAppInfoFromPackageName(packageInfo.packageName);
							if (info != null)
							{
								strRows.append(generateRow(rowTemplate, packageInfo));
							}
						}

						templateStr = templateStr.replace(matcher.group(), strRows);
					}

					responseText.append(templateStr);

					if (LOGGING)
						Log.v(LOGTAG, responseText.toString());
					return new Response(Status.OK, "text/html", responseText.toString());
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (uri.startsWith("/image/"))
			{
				try
				{
					uri = uri.substring(7);
					if (IMAGE_MAP.containsKey(uri))
					{
						int resId = IMAGE_MAP.get(uri);

						InputStream is = getResources().openRawResource(resId);
						long cb = 0;
						byte buf[] = new byte[1024];
						int len;
						while ((len = is.read(buf)) > 0)
							cb += len;
						is.close();
						is = getResources().openRawResource(resId);

						Response response = new Response(Status.OK, "image/png", is);
						response.addHeader("Content-Length", "" + cb);
						return response;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else if (uri.startsWith("/icon/"))
			{
				try
				{
					uri = uri.substring(6);
					PackageInfo packageInfo = this.getPackageInfoFromPackageName(uri);
					if (packageInfo != null)
					{
						Drawable logo = packageInfo.applicationInfo.loadLogo(getPackageManager());
						if (logo == null)
							logo = packageInfo.applicationInfo.loadIcon(getPackageManager());

						Bitmap bmp = drawableToBitmap(logo);
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						if (bmp.compress(CompressFormat.PNG, 100, stream))
						{
							long cb = stream.size();
							InputStream is = new ByteArrayInputStream(stream.toByteArray());

							Response response = new Response(Status.OK, "image/png", is);
							response.addHeader("Content-Length", "" + cb);
							return response;
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				PackageManager packageManager = HTTPDAppSender.this.getPackageManager();
				List<PackageInfo> packages = packageManager.getInstalledPackages(0);

				String requestedPackage = "";
				if (uri.length() > 1)
				{
					// Remove the "/"
					requestedPackage = uri.substring(1);
				}
				
				// Strip the .apk we added to the request off
				if (requestedPackage.endsWith(".apk")) {
					requestedPackage = requestedPackage.substring(0, requestedPackage.length() - 4);
				}

				PackageInfo packageInfo = this.getPackageInfoFromPackageName(requestedPackage);
				if (packageInfo != null)
				{
					ApplicationInfo appInfo = packageInfo.applicationInfo;
					String pathToApk = appInfo.sourceDir;

					if (LOGGING) 
						Log.v(LOGTAG, pathToApk);
					try
					{
						File apk = new File(pathToApk);
						FileInputStream fin = new FileInputStream(pathToApk);

						Response response = new Response(Status.OK, "application/vnd.android.package-archive", fin);
						response.addHeader("Content-Length", "" + apk.length());

						return response;

					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		private AppInfo getAppInfoFromPackageName(String requestedPackage)
		{
			if (requestedPackage == null || requestedPackage.length() == 0)
				return null;

			for (AppInfo appInfo : APPS_TO_DISPLAY)
			{
				String allowedPackage = appInfo.packageName;
				if (requestedPackage.equals(allowedPackage))
				{
					return appInfo;
				}
			}
			return null;
		}

		private PackageInfo getPackageInfoFromPackageName(String requestedPackage)
		{
			AppInfo info = getAppInfoFromPackageName(requestedPackage);
			if (info == null)
				return null; // Not one of our packages!

			PackageManager packageManager = HTTPDAppSender.this.getPackageManager();
			List<PackageInfo> packages = packageManager.getInstalledPackages(0);
			for (PackageInfo packageInfo : packages)
			{
				if (requestedPackage.equals(packageInfo.packageName))
				{
					return packageInfo;
				}
			}
			return null;
		}

		private String generateRow(String templateOriginal, PackageInfo packageInfo)
		{
			String appPath = packageInfo.applicationInfo.sourceDir;
			File appFile = new File(appPath);

			String title = (String) packageInfo.applicationInfo.loadLabel(getPackageManager());
			if (title == null || title.length() == 0)
				title = packageInfo.applicationInfo.name;
			if (title == null || title.length() == 0)
				title = appFile.getName();
						
			CharSequence description = packageInfo.applicationInfo.loadDescription(getPackageManager());
			if (description == null)
				description = "";
			
			String downloadUrl = packageInfo.packageName + ".apk";

			String template = templateOriginal;
			template = template.replaceAll("<!-- BEGIN_APP_TITLE -->(.*?)<!-- END_APP_TITLE -->(?s)", title);
			template = template.replaceAll("<!-- BEGIN_APP_INFO -->(.*?)<!-- END_APP_INFO -->(?s)", description.toString());
			template = template.replaceAll("<!-- APP_ICON -->(?s)", "icon/" + packageInfo.packageName);
			template = template.replaceAll("<!-- APP_PACKAGE -->(?s)", "" + downloadUrl);

			return template;
		}

		public Bitmap drawableToBitmap(Drawable drawable)
		{
			if (drawable instanceof BitmapDrawable)
			{
				return ((BitmapDrawable) drawable).getBitmap();
			}

			Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);

			return bitmap;
		}

		// public InputStream bitmapToInputStream(Bitmap bitmap)
		// {
		// int size = bitmap.getHeight() * bitmap.getRowBytes();
		// ByteBuffer buffer = ByteBuffer.allocate(size);
		// bitmap.copyPixelsToBuffer(buffer);
		// return new ByteArrayInputStream(buffer.array());
		// }
	}
}
