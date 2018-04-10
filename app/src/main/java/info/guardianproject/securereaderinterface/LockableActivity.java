package info.guardianproject.securereaderinterface;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

@SuppressLint("Registered")
public class LockableActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener
{
	public static final String LOGTAG = "LockableActivity";
	public static final boolean LOGGING = false;
	
	private boolean mLockedInOnPause;
	private boolean mResumed;
	private boolean mNeedToRecreate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LocalBroadcastManager.getInstance(this).registerReceiver(mLockReceiver, new IntentFilter(App.LOCKED_BROADCAST_ACTION));
		LocalBroadcastManager.getInstance(this).registerReceiver(mUnlockReceiver, new IntentFilter(App.UNLOCKED_BROADCAST_ACTION));
		addSettingsChangeListener();
		applyEnableScreenshotsSetting();
	}
	
	@Override
	protected void onDestroy()
	{
		removeSettingsChangeListener();
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mLockReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mUnlockReceiver);
	}

	@Override
	protected void onStart()
	{
		if (!mLockedInOnPause)
			App.getInstance().onActivityResume(this);
		super.onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if (!mLockedInOnPause)
			App.getInstance().onActivityPause(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mResumed = false;
		PowerManager pm =(PowerManager) getSystemService(Context.POWER_SERVICE);
		if (pm != null && !pm.isScreenOn())
		{
			mLockedInOnPause = true;
			App.getInstance().onActivityPause(this);
		}
	}

	@Override
	protected void onResume()
	{
		if (mLockedInOnPause)
			App.getInstance().onActivityResume(this);
		mLockedInOnPause = false;
		super.onResume();
		mResumed = true;
		if (mNeedToRecreate)
		{
			recreateNowOrOnResume();
		}
	}
	
	private void addSettingsChangeListener()
	{
		App.getSettings().registerChangeListener(this);
	}

	private void removeSettingsChangeListener()
	{
		App.getSettings().unregisterChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (SettingsUI.KEY_BLOCK_SCREENSHOTS.equals(key)) {
			if (LOGGING) 
				Log.v(LOGTAG, "The block screenshots setting has changed.");
			recreateNowOrOnResume();
		}
		if (SettingsUI.KEY_SHOW_PANIC_BUTTON.equals(key)) {
		    invalidateOptionsMenu();
        }
	}
	
	/**
	 * Based on the enable screenshots setting, set relevant window flags so that screen capturing will be enabled/disabled.
	 */
	private void applyEnableScreenshotsSetting()
	{
		if (!App.getSettings().blockScreenshots())
		{
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
		}
		else {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
					WindowManager.LayoutParams.FLAG_SECURE);
		}
	}
	
	BroadcastReceiver mLockReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			LockableActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					onLocked();
				}
			});
		}
	};
	
	BroadcastReceiver mUnlockReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			LockableActivity.this.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					onUnlocked();
				}
			});
		}
	};
	private View mContentView;

	private boolean mHasResult;

	private int mRequestCode;

	private int mResultCode;

	private Intent mReturnedIntent;
	
	protected void onLocked()
	{
		mContentView.setVisibility(View.INVISIBLE);
	}
	
	protected void onUnlocked()
	{
		mContentView.setVisibility(View.VISIBLE);
		if (mHasResult)
		{
			mHasResult = false;
			onUnlockedActivityResult(mRequestCode, mResultCode, mReturnedIntent);
		}
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		ViewGroup parent = (ViewGroup) (getWindow().getDecorView());
		mContentView = parent.getChildAt(0);
	}

	@Override
	public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
		canvas.drawColor(Color.BLACK);
		return true;
	}

	@Override
	final protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent)
	{
		super.onActivityResult(requestCode, resultCode, returnedIntent);
		if (App.getInstance().isActivityLocked())
		{
			mHasResult = true;
			mRequestCode = requestCode;
			mResultCode = resultCode;
			mReturnedIntent = returnedIntent;
		}
		else
		{
			onUnlockedActivityResult(requestCode, resultCode, returnedIntent);
		}
	}
	
	protected void onUnlockedActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent)
	{
		// Call our fragments
		//
		if (getSupportFragmentManager().getFragments() != null)
		{
			for (Fragment f : getSupportFragmentManager().getFragments())
			{
				if (f instanceof LockableFragment)
				{
					((LockableFragment) f).onUnlockedActivityResult(requestCode, resultCode, imageReturnedIntent);
				}
			}
		}
	}
	
	public void recreateNowOrOnResume()
	{
		if (!mResumed)
		{
			mNeedToRecreate = true;
		}
		else
		{
			mNeedToRecreate = false;
			Intent intentThis = getIntent();

			Bundle b = new Bundle();
			onSaveInstanceState(b);
			intentThis.putExtra("savedInstance", b);
			intentThis.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			finish();
			overridePendingTransition(0, 0);
			startActivity(intentThis);
			overridePendingTransition(0, 0);
		}
	}

	protected boolean isActivityResumed() {
		return mResumed;
	}
}
