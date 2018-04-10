package info.guardianproject.securereaderinterface.views.media;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.tinymission.rss.MediaContent;

import info.guardianproject.iocipher.File;

public class MediaImagePreviewView extends ImageView implements MediaPreviewView
{
	public static final String LOGTAG = "MediaImagePreviewView";
	public static final boolean LOGGING = false;
	
	private File mMediaFile;
	private boolean mHasSetDrawable;

	public MediaImagePreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public MediaImagePreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public MediaImagePreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		this.setScaleType(ScaleType.CENTER_CROP);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		setBitmapIfDownloaded();
	}

	@Override
	public void setDataSource(MediaContent mediaContent, File mediaFile) {
		mMediaFile = mediaFile;
		mHasSetDrawable = false;
		setBitmapIfDownloaded();
	}

	public void recycle()
	{
		setImageDrawable(null);
	}

	private synchronized void setBitmapIfDownloaded() {
		int w = getWidth();
		int h = getHeight();
		if (mMediaFile != null && w > 0 && h > 0 && !mHasSetDrawable) {
			mHasSetDrawable = true;
			Picasso.with(getContext())
					.load(Uri.parse(mMediaFile.getAbsolutePath()))
					.centerCrop()
					.resize(w, h)
					.into(this);
		}
	}
}
