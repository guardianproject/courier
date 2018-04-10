package info.guardianproject.securereaderinterface.ui;

import java.io.IOException;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;

public class SingleRangeFileInputStream extends FileInputStream
{
	private long mFileLength;
	private long mRangeStart;
	private long mRangeEnd;
	private long mAvailable;

	public SingleRangeFileInputStream(File file, String rangeHeader) throws IOException
	{
		super(file);
		
		mFileLength = file.length();
		mRangeStart = 0;
		mRangeEnd = 0;
		String rangeVal = rangeHeader.trim().substring("bytes=".length());
		if (rangeVal.startsWith("-"))
		{
	        mRangeEnd = mFileLength - 1;
	        mRangeStart = mFileLength - 1 - Long.parseLong(rangeVal.substring(1));			
		}
		else
		{
	        String[] range = rangeVal.split("-");
	        mRangeStart = Long.parseLong(range[0]);
	        mRangeEnd = range.length > 1 ? Long.parseLong(range[1]) : mFileLength - 1;
		}
	    if (mRangeEnd > mFileLength - 1) 
	    {
	        mRangeEnd = mFileLength - 1;
	    }
	    
	    if (validRange())
	    {
			long skipped = 0;
			while (skipped < mRangeStart) {
				skipped += super.skip(mRangeStart - skipped);
			}
	    	mAvailable = (mRangeEnd - mRangeStart + 1);
	    }
	    else
	    {
	    	mAvailable = 0;
	    }
	}
	
	public boolean validRange()
	{
		return mRangeStart <= mRangeEnd;
	}

	@Override
	public int available() throws IOException
	{
		return (int) mAvailable;
	}

	@Override
	public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException
	{
		int read = super.read(buffer, byteOffset, byteCount);
		mAvailable -= read;
		return read;
	}

	public String getContentLengthHeader()
	{
		long contentLength = mRangeEnd - mRangeStart + 1;
		return "" + contentLength;
	}

	public String getContentRangeHeader()
	{
		return "bytes " + mRangeStart + "-" + mRangeEnd + "/" + mFileLength;
	}
}
