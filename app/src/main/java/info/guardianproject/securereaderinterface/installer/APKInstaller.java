package info.guardianproject.securereaderinterface.installer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class APKInstaller {

	public static final String LOGTAG = "APKInstaller";
	public static final boolean LOGGING = false;
	
	public static int APK_INSTALL_CODE = 1;
	
	Context applicationContext;
	
	public APKInstaller(Context _context) {
		applicationContext = _context;
	}
	
	public void installAPK(int apk) {
		
        try {

	        String tempFileName = "apktoinstall.apk";
			InputStream in = applicationContext.getResources().openRawResource(apk);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			int size;
			byte[] buffer = new byte[1024];
			while((size = in.read(buffer,0,1024)) >= 0)
			{
				baos.write(buffer,0,size);
			}
			in.close();

			FileOutputStream fout = applicationContext.openFileOutput(tempFileName, Context.MODE_WORLD_READABLE);
			fout.write(baos.toByteArray());
			fout.close();
			
	        File tempFile = applicationContext.getFileStreamPath(tempFileName);
	        tempFile.setReadable(true);
	        
	        installAPK(tempFile);
	    }

	    catch (Exception ex) {
	        ex.printStackTrace();
	    }
	}
	
	public void installAPK(File apkFile)
	{
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        //applicationContext.startActivityForResult(Intent.createChooser(intent, "install"), APK_INSTALL_CODE);
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
        applicationContext.startActivity(Intent.createChooser(intent, "install"));
	}
}
