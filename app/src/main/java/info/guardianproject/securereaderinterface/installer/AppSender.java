package info.guardianproject.securereaderinterface.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

public class AppSender {
		
	public static final String LOGTAG = "AppSender";
	public static final boolean LOGGING = false;
	
	Context applicationContext;
		
	public AppSender(Context _context) {
			applicationContext = _context;
	}
		
	public void sendThisApp() {
	
		try {
			PackageManager packageManager = applicationContext.getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(applicationContext.getPackageName(), 0);
			ApplicationInfo appInfo = packageInfo.applicationInfo;
			String pathToApk = appInfo.sourceDir;
						
			shareFileViaBluetooth(pathToApk);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}	
	}
		
	public void sendOtherApp(String appPackageName) {
			
		PackageManager packageManager = applicationContext.getPackageManager();
		List<PackageInfo> packages = packageManager.getInstalledPackages(0); 
		for (int i = 0; i < packages.size(); i++) {
	         PackageInfo pInfo = packages.get(i);
	         ApplicationInfo aInfo = pInfo.applicationInfo; 
	         if (aInfo.packageName.equals(appPackageName)) {
	        	 shareFileViaBluetooth(aInfo.sourceDir);
	        	 break;
	         }
	      }		
	}
	
	private void shareFileViaBluetooth(String pathToFile)
	{

		String tempFileName = "temp.jpg";
		
		try {
			FileOutputStream fos = applicationContext.openFileOutput(tempFileName,Context.MODE_WORLD_READABLE);
			FileInputStream fin = new FileInputStream(pathToFile);

			byte[] buffer = new byte[4096];
			long total = 0L;
			int read;
			while ((read = fin.read(buffer)) != -1) 
			{
				fos.write(buffer, 0, read);
				total += read;
			}
			  
			fin.close();
			fos.close();
			  
	        File tempFile = applicationContext.getFileStreamPath(tempFileName);
	        tempFile.setReadable(true);
	        
			Intent intent = new Intent();  
			intent.setAction(Intent.ACTION_SEND);  
			//intent.setType("application/vnd.android.package-archive");
			intent.setType("image/jpeg");
			intent.setComponent(new ComponentName("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity"));		
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
			applicationContext.startActivity(intent);
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}