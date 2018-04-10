package info.guardianproject.securereaderinterface;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.LayoutInflaterCompat;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.security.GeneralSecurityException;
import java.util.UUID;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.models.LockScreenCallbacks;
import info.guardianproject.securereaderinterface.onboarding.OnboardingFragmentListener;
import info.guardianproject.securereaderinterface.ui.LayoutFactoryWrapper;
import info.guardianproject.securereaderinterface.ui.LocaleHelper;
import info.guardianproject.securereaderinterface.uiutil.ActivitySwitcher;
import info.guardianproject.securereaderinterface.uiutil.UIHelpers;

public class LockScreenActivity extends Activity implements LockScreenCallbacks, OnFocusChangeListener, ICacheWordSubscriber, OnboardingFragmentListener
{
    private static final String LOGTAG = "LockScreenActivity";
	public static final boolean LOGGING = false;
	
	private EditText mEnterPassphrase;
	private EditText mNewPassphrase;
	private EditText mConfirmNewPassphrase;
	private Button mBtnOpen;
	private View mErrorView;
	
	private CacheWordHandler mCacheWord;
	private info.guardianproject.securereaderinterface.LockScreenActivity.SetUiLanguageReceiver mSetUiLanguageReceiver;

	private View mRootView;
	private int mRootViewId;
	private LayoutInflater mInflater;

	private boolean hasBeenWiped = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// If not auto-login, make us non-transparent
		if (App.getSettings().autoLock() != 0) {
			getWindow().setBackgroundDrawableResource(R.drawable.background_news);
			setTheme(R.style.AppTheme);
		}
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = LayoutInflater.from(this);
		mInflater = inflater.cloneInContext(this);
		LayoutInflaterCompat.setFactory(mInflater, new LayoutFactoryWrapper(inflater.getFactory()));
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mCacheWord = new CacheWordHandler(this);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			 getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
			 WindowManager.LayoutParams.FLAG_SECURE);
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mSetUiLanguageReceiver = new SetUiLanguageReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(mSetUiLanguageReceiver, new IntentFilter(App.SET_UI_LANGUAGE_BROADCAST_ACTION));
	}

	@Override
	protected void onPause() {
	    super.onPause();
	    if (mSetUiLanguageReceiver != null)
	    {
	    	LocalBroadcastManager.getInstance(this).unregisterReceiver(mSetUiLanguageReceiver);
	    	mSetUiLanguageReceiver = null;
	    }
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
		App.getInstance().onLockScreenResumed(this);
        mCacheWord.connectToService();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		App.getInstance().onLockScreenPaused(this);
        mCacheWord.disconnectFromService();
	}

	@Override
	public boolean isInternalActivityOpened()
	{
		return false;
	}


	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(LocaleHelper.wrap(newBase, App.getSettings().uiLanguageCode(), App.getSettings().uiRegionCode()));
	}

	@Override
	public void setContentView(int layoutResID) 
	{
		if (layoutResID != mRootViewId) {
			mRootViewId = layoutResID;
			mRootView = getLayoutInflater().inflate(layoutResID, null);
			super.setContentView(mRootView);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if ((keyCode == KeyEvent.KEYCODE_BACK))
		{
			// Back from lock screen means quit app. So send a kill signal to
			// any open activity and finish!	
			LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void createCreatePassphraseView()
	{
		setContentView(R.layout.lock_screen_create_passphrase);

		mNewPassphrase = (EditText) findViewById(R.id.editNewPassphrase);
		mConfirmNewPassphrase = (EditText) findViewById(R.id.editConfirmNewPassphrase);

		// Passphrase is not set, so allow the user to create one!
		//
		Button btnCreate = (Button) findViewById(R.id.btnCreate);
		btnCreate.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Disallow empty fields (user just pressing "create")
				if (mNewPassphrase.getText().length() == 0 && mConfirmNewPassphrase.getText().length() == 0)
					return;

				// Compare the two text fields!
				if (!mNewPassphrase.getText().toString().equals(mConfirmNewPassphrase.getText().toString()))
				{
					Toast.makeText(LockScreenActivity.this, getString(R.string.lock_screen_passphrases_not_matching), Toast.LENGTH_SHORT).show();
					mNewPassphrase.setText("");
					mConfirmNewPassphrase.setText("");
					mNewPassphrase.requestFocus();
					return; // Try again...
				}

				// Store
				try {
                    mCacheWord.setPassphrase(mNewPassphrase.getText().toString().toCharArray());
                } catch (GeneralSecurityException e) {
                	if (LOGGING)
                		Log.e(LOGTAG, "Cacheword initialization failed: " + e.getMessage());
                }
			}
		});
	}

	private void createLockView()
	{
		setContentView(R.layout.lock_screen_return);

		View root = findViewById(R.id.llRoot);
		root.setOnFocusChangeListener(this);

		mErrorView = root.findViewById(R.id.tvError);
		mErrorView.setVisibility(View.GONE);
		
		mEnterPassphrase = (EditText) findViewById(R.id.editEnterPassphrase);
		mEnterPassphrase.setTypeface(Typeface.DEFAULT);
		mEnterPassphrase.setTransformationMethod(new PasswordTransformationMethod());

		mBtnOpen = (Button) findViewById(R.id.btnOpen);
		mBtnOpen.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (TextUtils.isEmpty(mEnterPassphrase.getText()))
					return;

				// If we have been wiped, just return here. Otherwise the user would just enter the app, since all settings
				// have been reset and we now by default have no lock enabled!
				if (hasBeenWiped) {
					mEnterPassphrase.setText("");
					mErrorView.setVisibility(View.VISIBLE);
					return;
				}

				if (App.getSettings().useKillPassphrase() && mEnterPassphrase.getText().toString().equals(App.getSettings().killPassphrase()))
				{
					// Kill password entered, wipe!
					hasBeenWiped = true;
					App.getInstance().wipe(LockScreenActivity.this, Settings.PanicAction.WipeData, false);
					mEnterPassphrase.setText("");
					mErrorView.setVisibility(View.VISIBLE);
        			LocalBroadcastManager.getInstance(LockScreenActivity.this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
					//finish();
					return; // Try again...
				}

				// Check passphrase
			    try {
                    mCacheWord.setPassphrase(mEnterPassphrase.getText().toString().toCharArray());
                } catch (GeneralSecurityException e) {
                	if (LOGGING)
                		Log.e(LOGTAG, "Cacheword pass verification failed: " + e.getMessage());
                    int failedAttempts = App.getSettings().currentNumberOfPasswordAttempts();
                    failedAttempts++;
                    App.getSettings().setCurrentNumberOfPasswordAttempts(failedAttempts);
					Settings.PanicAction action = App.getSettings().wrongPasswordAction();
                    if (action != Settings.PanicAction.Nothing && failedAttempts == App.getSettings().numberOfPasswordAttempts())
                    {
                        // Ooops, to many attempts! Wipe the data...
						hasBeenWiped = true;
                        App.getInstance().wipe(LockScreenActivity.this, action, false);
            			LocalBroadcastManager.getInstance(LockScreenActivity.this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
                        //finish();
                    }

                    mEnterPassphrase.setText("");
					mErrorView.setVisibility(View.VISIBLE);
                    return; // Try again...
                }
                
				App.getSettings().setCurrentNumberOfPasswordAttempts(0);
				UIHelpers.hideSoftKeyboard(LockScreenActivity.this);

			}
		});

		mEnterPassphrase.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_GO)
				{
					InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

					Handler threadHandler = new Handler();

					imm.hideSoftInputFromWindow(v.getWindowToken(), 0, new ResultReceiver(threadHandler)
					{
						@Override
						protected void onReceiveResult(int resultCode, Bundle resultData)
						{
							super.onReceiveResult(resultCode, resultData);
							mBtnOpen.performClick();
						}
					});
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		if (hasFocus && !(v instanceof EditText))
		{
			LockScreenActivity.hideSoftKeyboard(this);
		}
	}

	public static void hideSoftKeyboard(Activity activity)
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (activity.getCurrentFocus() != null) {
			inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
		}
	}

	@SuppressLint("NewApi")
	protected void onUiLanguageChanged()
	{
		Intent intentThis = getIntent();
		intentThis.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		finish();
		overridePendingTransition(0, 0);
		startActivity(intentThis);
		overridePendingTransition(0, 0);
	}

	@Override
    public void onCacheWordUninitialized() {
		// If we are wiping, do nothig here!
		if (!App.getInstance().isWiping()) {
			showNextOnboardingView();
		}
    }

    @Override
    public void onCacheWordLocked() {
		if (App.getSettings().autoLock() == 0) {
			// No lock, use our stored PW
			try {
				mCacheWord.setPassphrase(getCWPassword().toCharArray());
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
		} else {
			createLockView();
		}
    }

    @Override
    public void onCacheWordOpened() {
        App.getSettings().setCurrentNumberOfPasswordAttempts(0);

        Intent intent = getIntent().getParcelableExtra("originalIntent");
        if (intent == null)
        	intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        if (mRootView != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.putExtra(AppActivity.EXTRA_USE_ROTATION_ANIMATION, true);
			final Intent finalIntent = intent;
			mRootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			ActivitySwitcher.animationOut(mRootView,
					getWindowManager(),
					new ActivitySwitcher.AnimationFinishedListener() {
						@Override
						public void onAnimationFinished() {
							startActivity(finalIntent);
							finish();
							overridePendingTransition(0, 0);
						}
					});
		} else {
			startActivity(intent);
			finish();
			overridePendingTransition(0, 0);
		}
    }
      
	@Override
	public Object getSystemService(@NonNull String name)
	{
		if (LAYOUT_INFLATER_SERVICE.equals(name))
		{
			if (mInflater != null)
				return mInflater;
		}
		return super.getSystemService(name);
	}

	private final class SetUiLanguageReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			new Handler().post(new Runnable()
			{

				@Override
				public void run()
				{
					onUiLanguageChanged();
				}
			});
		}
	}

	public void onUnlocked() {
		App.getSettings().setCurrentNumberOfPasswordAttempts(0);

		Intent intent = getIntent().getParcelableExtra("originalIntent");
		if (intent == null)
			intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		if (mRootView != null) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.putExtra(AppActivity.EXTRA_USE_ROTATION_ANIMATION, true);
			final Intent finalIntent = intent;
			mRootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			ActivitySwitcher.animationOut(mRootView,
					getWindowManager(),
					new ActivitySwitcher.AnimationFinishedListener() {
						@Override
						public void onAnimationFinished() {
							startActivity(finalIntent);
							finish();
							overridePendingTransition(0, 0);
						}
					});
		} else {
			startActivity(intent);
			finish();
			overridePendingTransition(0, 0);
		}
	}
	
	private void showNextOnboardingView() {
		TypedArray onboarding_screens = getResources().obtainTypedArray(R.array.onboarding_screems);
		if (App.getSettings().getOnboardingStage() < onboarding_screens.length()) {
			int layoutId = onboarding_screens.getResourceId(App.getSettings().getOnboardingStage(), 0);
			setContentView(layoutId);
		} else {
			if (!BuildConfig.UI_ENABLE_CREATE_PASSPHRASE) {
				if (mRootView != null)
					mRootView.setAlpha(0.5f); // Fade it out a bit. TODO - show progress spinner?
				App.getSettings().setAutolock(0);    //TODO - add a setting for this!
				try {
					mCacheWord.setPassphrase(getCWPassword().toCharArray());
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
				}
			} else {
				createCreatePassphraseView();
			}
		}
		onboarding_screens.recycle();
	}

	@Override
	public void onNextPressed() {
		App.getSettings().setOnboardingStage(App.getSettings().getOnboardingStage() + 1);
		showNextOnboardingView();
	}

	static String getCWPassword() {
		String passphrase = App.getSettings().launchPassphrase();
		if (TextUtils.isEmpty(passphrase)) {
			passphrase = UUID.randomUUID().toString();
			App.getSettings().setLaunchPassphrase(passphrase);
		}
		return passphrase;
	}
}
