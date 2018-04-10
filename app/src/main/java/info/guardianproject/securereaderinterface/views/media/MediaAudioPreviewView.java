package info.guardianproject.securereaderinterface.views.media;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.tinymission.rss.MediaContent;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereaderinterface.R;

public class MediaAudioPreviewView extends FrameLayout implements MediaPreviewView
{
	public static final String LOGTAG = "MediaAudioPreviewView";
	public static final boolean LOGGING = false;

	private TextView mTvDisplayName;

	public MediaAudioPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public MediaAudioPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public MediaAudioPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.media_preview_audio, this);
		mTvDisplayName = (TextView)findViewById(R.id.mediaDisplayName);
	}

	public void setDataSource(MediaContent mediaContent, File mediaFile)
	{
		if (mTvDisplayName != null && mediaContent.getExpression() != null) {
			mTvDisplayName.setText(mediaContent.getExpression());
		}
	}

	public void recycle()
	{
	}
}