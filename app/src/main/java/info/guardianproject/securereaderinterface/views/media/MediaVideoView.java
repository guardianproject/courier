package info.guardianproject.securereaderinterface.views.media;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.VideoView;

import info.guardianproject.securereaderinterface.R;

public class MediaVideoView extends FrameLayout implements OnErrorListener, OnPreparedListener, OnCompletionListener, OnSeekBarChangeListener
{
	private VideoView mVideoView;
	private MediaController mMediaController;
	private MediaPlayer mMP;
	private View mControllerView;
	private View mViewLoading;
	private View mBtnPlay;
	private View mBtnPause;
	private SeekBar mSeekbar;
	private boolean mIsTrackingThumb;

	public MediaVideoView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	public MediaVideoView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public MediaVideoView(Context context)
	{
		super(context);
		initView(context);
	}

	private void initView(Context context)
	{
		View.inflate(context, R.layout.media_video, this);

		mVideoView = (VideoView) findViewById(R.id.content);
		mViewLoading = findViewById(R.id.frameLoading);

		mControllerView = LayoutInflater.from(getContext()).inflate(R.layout.media_video_controller, this, false);

		if (!isInEditMode())
		{
			mViewLoading.setVisibility(View.INVISIBLE);

			mMediaController = new InternalMediaController(getContext());
			mMediaController.setAnchorView(mVideoView);
			mVideoView.setMediaController(mMediaController);

			mVideoView.setOnErrorListener(this);
			mVideoView.setOnPreparedListener(this);
			mVideoView.setOnCompletionListener(this);
		}

		View btnCollapse = mControllerView.findViewById(R.id.btnCollapse);
		btnCollapse.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				((Activity) v.getContext()).finish();
			}
		});

		mBtnPlay = mControllerView.findViewById(R.id.btnPlay);
		mBtnPlay.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mVideoView.start();
				mBtnPlay.setVisibility(View.INVISIBLE);
				mBtnPause.setVisibility(View.VISIBLE);
			}
		});

		mBtnPause = mControllerView.findViewById(R.id.btnPause);
		mBtnPause.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mVideoView.pause();
				mBtnPlay.setVisibility(View.VISIBLE);
				mBtnPause.setVisibility(View.INVISIBLE);
			}
		});

		mSeekbar = (SeekBar) mControllerView.findViewById(R.id.seekbar);
	}

	public void setContentUri(Uri uri)
	{
		mVideoView.setVideoURI(uri);
		mViewLoading.setVisibility(View.VISIBLE);
	}

	private class InternalMediaController extends MediaController
	{
		public InternalMediaController(Context context)
		{
			super(context);
		}

		@Override
		public void setAnchorView(View view)
		{
			super.setAnchorView(view);
			this.removeAllViews();
			addView(mControllerView);
		}
	}

	public MediaPlayer getMediaPlayer()
	{
		return mMP;
	}

	@Override
	public void onPrepared(final MediaPlayer mp)
	{
		mMP = mp;
		mViewLoading.setVisibility(View.GONE);
		mp.start();
		mBtnPlay.setVisibility(View.INVISIBLE);
		mBtnPause.setVisibility(View.VISIBLE);
		mMediaController.show();

		mSeekbar.setOnSeekBarChangeListener(this);

		mSeekbar.setMax(mp.getDuration());
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					while (mp.getCurrentPosition() < mp.getDuration())
					{
						if (!mIsTrackingThumb)
							mSeekbar.setProgress(mp.getCurrentPosition());
						Message msg = new Message();
						int millis = mp.getCurrentPosition();

						msg.obj = millis / 1000;

						try
						{
							Thread.sleep(100);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}).start();

	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		// TODO - show error
		mViewLoading.setVisibility(View.GONE);
		mBtnPlay.setVisibility(View.VISIBLE);
		mBtnPause.setVisibility(View.INVISIBLE);
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer mp)
	{
		mBtnPlay.setVisibility(View.VISIBLE);
		mBtnPause.setVisibility(View.INVISIBLE);
		mVideoView.seekTo(0);
		mSeekbar.setProgress(0);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		if (mIsTrackingThumb)
			mMediaController.show();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{
		mIsTrackingThumb = true;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		if (getMediaPlayer() != null)
			getMediaPlayer().seekTo(seekBar.getProgress());
		mIsTrackingThumb = false;
	}
}