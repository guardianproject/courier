package com.example.securesharertest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {

	public static final String LOGTAG = "SecureShareTest"; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(LOGTAG,"starting");
		
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
	    
	    if (intent != null && type != null && action != null) {
	    	Log.v(LOGTAG,"intent: " + intent.toString());
	    	Log.v(LOGTAG,"action: " + action.toString());
	    	Log.v(LOGTAG,"type: " + type.toString());
	    }
	    
	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("application/x-bigbuffalo-bundle".equals(type)) {
	            //Uri streamUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
	            //Log.v(LOGTAG,"Received: " + streamUri.toString());
	        	
	        	Uri streamUri = (Uri) intent.getData();
	        	Log.v(LOGTAG,"Received: " + streamUri.toString());
	        	
	        	BufferedOutputStream outstream = null;
	        	InputStream instream = null;
	        	File outfile = new File(Environment.getExternalStorageDirectory(), "temp.bbb");
	        	Log.v(LOGTAG,outfile.getAbsolutePath());
	            try {
					instream = getContentResolver().openInputStream(streamUri);
					outstream = new BufferedOutputStream(new java.io.FileOutputStream(outfile));
				
					int count;
					byte[] buffer = new byte[256];
					while ((count = instream.read(buffer, 0, buffer.length)) != -1) {
						Log.v(LOGTAG,"Read " + count + " bytes");
					    outstream.write(buffer,0,count);
					}
					
				// When that's done, present it back to Big Buffalo
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setDataAndType(Uri.fromFile(outfile), "application/x-bigbuffalo-bundle");
					this.startActivity(i);
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (instream != null) {
						try {
							instream.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (outstream != null) {
						try {
							outstream.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
	            

	            
	        }
	    }
	}

}
