package info.guardianproject.securereaderinterface;

import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers;
import info.guardianproject.securereaderinterface.uiutil.AnimationHelpers.FadeInFadeOutListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SplashActivity extends Activity
{
	protected boolean _active = true;
	protected int _splashTime = 3000; // time to display the splash screen in ms

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		Thread splashTread = new Thread()
		{
			@Override
			public void run()
			{
				try
				{
					int waited = 0;
					while (_active && waited < _splashTime)
					{
						sleep(100);
						waited += 100;
					}
				}
				catch (InterruptedException e)
				{
					// do nothing
				}
				finally
				{
					View root = findViewById(R.id.rlRoot);
					AnimationHelpers.fadeOut(root, 300, 0, false, new FadeInFadeOutListener()
					{
						@Override
						public void onFadeInStarted(View view) {
						}

						@Override
						public void onFadeInEnded(View view) {
						}

						@Override
						public void onFadeOutStarted(View view) {
						}

						@Override
						public void onFadeOutEnded(View view) {
							close();
						}
					});
				}
			}
		};
		splashTread.start();
	}
	
	private void close()
	{
		Intent i = new Intent(SplashActivity.this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(i);
		finish();
	}
}
