package info.guardianproject.securereaderinterface.views.media;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import info.guardianproject.iocipher.File;
import info.guardianproject.securereaderinterface.R;
import uk.co.senab.photoview.PhotoViewAttacher;

public class MediaImageView extends FrameLayout
{
	private static final String LOGTAG = "MediaImageView";
	public static final boolean LOGGING = false;
	private ImageView imageView;

	public MediaImageView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public MediaImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public MediaImageView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		LayoutInflater.from(context).inflate(R.layout.media_image, this, true);
		imageView = (ImageView) findViewById(R.id.content);
	}

	public void setDataSource(File mediaFile)
	{
		if (mediaFile != null && imageView != null) {
			Picasso.with(getContext())
					.load(Uri.parse(mediaFile.getAbsolutePath()))
					.into(imageView, new Callback() {
						@Override
						public void onSuccess() {
							new PhotoViewAttacher(imageView);
						}

						@Override
						public void onError() {

						}
					});
		}
	}

	// Need to catch errors, see https://github.com/chrisbanes/PhotoView
	//
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		try {
			return super.onInterceptTouchEvent(ev);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}