package info.guardianproject.securereaderinterface.views.media;

import info.guardianproject.securereaderinterface.R;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.tinymission.rss.MediaContent;

public class MediaVideoPreviewView extends FrameLayout implements MediaPreviewView
{
	public static final String LOGTAG = "MediaVideoPreviewView";
	public static final boolean LOGGING = false;
	
	private ImageView mImageView;
	private Handler mHandler;
	private TextView mTvDisplayName;

	public MediaVideoPreviewView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public MediaVideoPreviewView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public MediaVideoPreviewView(Context context)
	{
		super(context);
		initView(context);
	}

	public void setScaleType(ScaleType scaleType)
	{
		if (mImageView != null)
			mImageView.setScaleType(scaleType);
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.media_preview_video, this);

		mImageView = (ImageView) findViewById(R.id.image);
		View mPlayView = findViewById(R.id.btnPlay);
		if (mPlayView != null)
			mPlayView.setVisibility(View.GONE);
		mTvDisplayName = (TextView)findViewById(R.id.mediaDisplayName);
	}

	@Override
	public void setDataSource(MediaContent mediaContent, info.guardianproject.iocipher.File mediaFile)
	{
		if (mTvDisplayName != null && mediaContent.getExpression() != null) {
			mTvDisplayName.setText(mediaContent.getExpression());
		}

		if (mediaFile == null)
		{
			if (LOGGING)
				Log.v(LOGTAG, "Failed to download media, no file.");
			return;
		}

		//if (mImageView == null) {
		//}

		//TODO - fix this!
/*		mHandler = new Handler();
		Runnable getOrientationRunnable = new Runnable()
		{
			@Override
			public void run()
			{
				if (LOGGING)
					Log.v(LOGTAG, "getOrientationThread");
				
				Bitmap preview = ThumbnailUtils.createVideoThumbnail(mMediaFile.getPath(), Video.Thumbnails.MINI_KIND);

				Runnable reportRunnable = new Runnable()
				{
					private Bitmap mBitmap;

					@Override
					public void run()
					{
						if (!mUseThisThread)
							AnimationHelpers.fadeOut(mImageView, 0, 0, false);
						if (mBitmap != null)
						{
							mImageView.setScaleType(ScaleType.CENTER_CROP);
							mImageView.setImageBitmap(mBitmap);
						}
						else
						{
							mImageView.setScaleType(ScaleType.CENTER_CROP);
							mImageView.setImageResource(R.drawable.img_placeholder);
						}
						if (!mUseThisThread)
							AnimationHelpers.fadeIn(mImageView, 500, 0, false);
						else
							AnimationHelpers.fadeIn(mImageView, 0, 0, false);
					}

					private Runnable init(Bitmap bitmap)
					{
						mBitmap = bitmap;
						return this;
					}

				}.init(preview);

				if (mUseThisThread)
					reportRunnable.run();
				else
					mHandler.post(reportRunnable);
			}
		};

		if (mUseThisThread)
		{
			getOrientationRunnable.run();
		}
		else
		{
			Thread getOrientationThread = new Thread(getOrientationRunnable);
			getOrientationThread.start();
		}*/
	}

	public void recycle()
	{
		if (mImageView != null)
			mImageView.setImageBitmap(null);
	}
}
