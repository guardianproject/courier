package info.guardianproject.securereaderinterface.views.media;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.iocipher.File;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.tinymission.rss.MediaContent;

public class MediaApplicationPreviewView extends FrameLayout implements MediaPreviewView
{
	public static final String LOGTAG = "MediaApplicationPreviewView";
	public static final boolean LOGGING = false;

	public MediaApplicationPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public MediaApplicationPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public MediaApplicationPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.media_preview_application, this);
	}

	@Override
	public void setDataSource(MediaContent mediaContent, File mediaFile) {
	}

	@Override
	public void recycle()
	{
		// Do nothing
	}
}