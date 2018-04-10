package info.guardianproject.securereaderinterface;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.tinymission.rss.MediaContent;

import info.guardianproject.securereaderinterface.models.MediaItemViewModel;
import info.guardianproject.securereaderinterface.ui.ProxyMediaStreamServer;
import info.guardianproject.securereaderinterface.views.media.MediaImageView;
import info.guardianproject.securereaderinterface.views.media.MediaVideoView;

public class ViewMediaActivity extends AppActivity // implements
// OnTouchListener
{
	public static final String LOGTAG = "ViewMediaActivity";
	public static final boolean LOGGING = false;
	
	@Override
	protected boolean useLeftSideMenu()
	{
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_media);
		setMenuIdentifier(R.menu.activity_view_media);
		AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
		lp.setScrollFlags(0);
		mToolbar.setLayoutParams(lp);

		this.setDisplayHomeAsUp(true);
		//TODO - if we allow this to hide, we can never get it back!
		//this.setToolbarTimeout(5000);

		Bundle parameters = getIntent().getBundleExtra("parameters");
		if (parameters != null) {
			MediaItemViewModel mediaItem = (MediaItemViewModel) parameters.getSerializable("media");
			if (mediaItem != null) {
				ViewGroup rootView = (ViewGroup) findViewById(R.id.view_media_root);
				final View mediaItemView = createMediaItemView(mediaItem);
				rootView.addView(mediaItemView);
			}
		}
	}

	public View createMediaItemView(MediaItemViewModel mediaItem)
	{
		boolean isVideo = (mediaItem.media.getMediaContentType() == MediaContent.MediaContentType.VIDEO);
		boolean isAudio = (mediaItem.media.getMediaContentType() == MediaContent.MediaContentType.AUDIO);
		if (isVideo || isAudio)
		{
			MediaVideoView vmc = new MediaVideoView(this);
			ProxyMediaStreamServer proxyMediaServer = App.getInstance().getProxyMediaStreamServer();
			if (proxyMediaServer != null) {
				vmc.setContentUri(Uri.parse(proxyMediaServer.getProxyUrlForMediaContent(mediaItem.media)));
			}
			return vmc;
		}
		else
		{
			MediaImageView imc = new MediaImageView(this);
			imc.setDataSource(mediaItem.getFile());
			return imc;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
