package info.guardianproject.securereaderinterface.views.media;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.tinymission.rss.MediaContent;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class MediaPreviewLayout extends FrameLayout implements View.OnClickListener {
	public interface MediaPreviewLayoutListener {
		void onMediaDownloaded(MediaItemViewModel mediaItem);
	}
	private final MediaItemViewModel mediaItem;
	private final MediaPreviewView mediaView;
	private View downloadView;
	private MediaPreviewLayoutListener listener;

	public MediaPreviewLayout(Context context, MediaItemViewModel mediaItem) {
		super(context);
		this.mediaItem = mediaItem;
		this.mediaView = createMediaView();
		if (!mediaItem.getMediaData(this, false)) {
			// Will not download, show download view
			createDownloadView();
		}
	}

	public void setListener(MediaPreviewLayoutListener listener) {
		this.listener = listener;
	}

	public MediaItemViewModel getMediaItem() {
		return mediaItem;
	}

	public void addToContainer(ViewGroup container) {
		if (mediaItem.isDownloaded()) {
			container.addView((View)mediaView);
		} else {
			container.addView(this);
		}
	}

	public void removeFromContainer(ViewGroup container) {
		if (((View)mediaView).getParent() == container) {
			container.removeView((View)mediaView);
		} else {
			container.removeView(this);
			removeView((View)mediaView);
		}
	}

	public boolean isViewFromObject(View view) {
		return view.equals(this) || view.equals(mediaView);
	}

	public void recycle() {
		mediaView.recycle();
	}

	private MediaPreviewView createMediaView()
	{
		MediaPreviewView mediaView;

		// Create a view for it
		switch (mediaItem.media.getMediaContentType()) {
			case VIDEO:
				mediaView = new MediaVideoPreviewView(getContext());
				// TODO
				//vmc.setScaleType(mDefaultScaleType);
				break;

			case APPLICATION:
				mediaView = new MediaApplicationPreviewView(getContext());
				break;

			case EPUB:
				mediaView = new MediaEPubPreviewView(getContext());
				break;

			case AUDIO:
				mediaView = new MediaAudioPreviewView(getContext());
				break;

			default:
				mediaView = new MediaImagePreviewView(getContext());
				//TODO imc.setScaleType(mDefaultScaleType);
				break;
		}
		((View)mediaView).setOnClickListener(this);
		return mediaView;
	}

	public void onDataDownloading() {
		createDownloadingView();
	}

	public void onDataDownloadError() {
		createDownloadView();
	}

	public void onDataDownloaded() {
		if (listener != null) {
			listener.onMediaDownloaded(mediaItem);
		}
	}
	public void onDataAvailable() {
		if (downloadView != null) {
			removeView(downloadView);
			downloadView = null;
		}
		// If we are attached to container, add us to this view.
		if (getParent() != null) {
			View v = (View) mediaView;
			if (v.getParent() != null) {
				((ViewGroup)v.getParent()).removeView(v);
			}
			addView(v);
		}
		mediaView.setDataSource(mediaItem.media, mediaItem.getFile());
	}

	private void createDownloadView()
	{
		downloadView = LayoutInflater.from(getContext()).inflate(R.layout.media_download, this, false);
		downloadView.setOnClickListener(this);
		ImageView iv = ((ImageView) downloadView.findViewById(R.id.ivDownloadIcon));
		Drawable icon = ContextCompat.getDrawable(getContext(), placeholderIcon()).mutate();
		UIHelpers.colorizeDrawableWithColor(getContext(), Color.argb(77, 0, 0, 0), icon);
		iv.setImageDrawable(icon);
		TextView tv = (TextView) downloadView.findViewById(R.id.tvDownload);
		if (tv != null)
			tv.setText(placeholderText());
		//mDownloadView.setBackgroundColor(placeholderBackground());
		this.addView(downloadView);
		downloadView.bringToFront();
		iv.clearAnimation();
	}

	private void createDownloadingView()
	{
		if (downloadView != null) {
			removeView(downloadView);
		}
		downloadView = LayoutInflater.from(getContext()).inflate(R.layout.media_downloading, this, false);
		//TODO cancel? mDownloadView.setOnClickListener(null);
		ImageView iv = ((ImageView) downloadView.findViewById(R.id.ivDownloadIcon));
		iv.setImageResource(R.drawable.ic_context_load);
		TextView tv = (TextView) downloadView.findViewById(R.id.tvDownload);
		if (tv != null)
			tv.setText(null);
		//mDownloadView.setBackgroundColor(placeholderBackground());
		this.addView(downloadView);
		iv.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.rotate));
	}

	public int placeholderIcon()
	{
		switch (mediaItem.media.getMediaContentType()) {
			case VIDEO:
				return R.drawable.ic_load_video;
			case EPUB:
				return R.drawable.ic_content_epub;
			case APPLICATION:
				return R.drawable.ic_content_app;
			default:
				return R.drawable.ic_load_photo;
		}
	}

	//TODO
/*	public int placeholderBackground()
	{
		if (mMediaViewCollection != null)
		{
			MediaContentLoadInfo info = mMediaViewCollection.getFirstLoadInfo();
			if (info != null)
			{
				if (info.isVideo())
					return Color.parseColor("#D98BC34A");
			}
		}
		return this.getResources().getColor(R.color.grey_light_light);
	}*/

	public CharSequence placeholderText()
	{
		if (mediaItem.media.getMediaContentType() == MediaContent.MediaContentType.EPUB) {
			return getContext().getText(R.string.download_epub_hint);
		}
		return null;
	}

	@Override
	public void onClick(View view) {
		if (view == downloadView) {
			// Download this media item!
			mediaItem.getMediaData(MediaPreviewLayout.this, true);
		} else if (view == mediaView && mediaItem.isDownloaded()) {
			performClick();
		}
	}
}
