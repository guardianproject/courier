package info.guardianproject.securereaderinterface;

import info.guardianproject.securereader.ModeSettings;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.ui.SupportedLanguage;

import android.content.Context;

public class SettingsUI extends Settings
{
	private static final int CURRENT_SETTINGS_UI_VERSION = 2;
	private static final String KEY_SETTINGS_UI_VERSION = "settings_ui_version";

	public static final String KEY_ONBOARDING_STAGE = "onboarding_stage";

	public static String KEY_CONTENT_FONT_SIZE_ADJUSTMENT;
	public static String KEY_BLOCK_SCREENSHOTS;
	public static String KEY_SHOW_PANIC_BUTTON;
	public static String KEY_HAS_SHOWN_OPTIMIZATION_INFO;

	public SettingsUI(Context context)
	{
		super(context);

		KEY_CONTENT_FONT_SIZE_ADJUSTMENT = context.getString(R.string.pref_key_content_font_size_adjustment);
		KEY_BLOCK_SCREENSHOTS = context.getString(R.string.pref_key_security_block_screenshots);
		KEY_SHOW_PANIC_BUTTON = context.getString(R.string.pref_key_security_show_panic_button);
		KEY_HAS_SHOWN_OPTIMIZATION_INFO = context.getString(R.string.pref_key_has_shown_optimization_info);

		int fileVersion = mPrefs.getInt(KEY_SETTINGS_UI_VERSION, 0);

		initializeIfNeeded();

		if (fileVersion == 0) {
			float adjustment = getContentFontSizeAdjustment();
			adjustment /= 8; // range now -1 -> 1
			adjustment *= 100;
			adjustment = Math.max(-50, Math.min(100, adjustment));
			setContentFontSizeAdjustment((int)adjustment);
		}
		if (fileVersion < 2) {
			// Flag was negated in version 2
			setBlockScreenshots(!mPrefs.getBoolean("enable_screenshots", false));
		}
	}

	private void initializeIfNeeded() {
		if (!mPrefs.contains(KEY_SETTINGS_UI_VERSION)) {
			initializeUISettings();
		}
	}

	private void initializeUISettings() {
		mPrefs.edit()
				.putInt(KEY_CONTENT_FONT_SIZE_ADJUSTMENT, context.getResources().getInteger(R.integer.pref_default_content_font_size_adjustment))
				.putBoolean(KEY_BLOCK_SCREENSHOTS, context.getResources().getBoolean(R.bool.pref_default_security_block_screenshots))
				.putBoolean(KEY_SHOW_PANIC_BUTTON, context.getResources().getBoolean(R.bool.pref_default_security_show_panic_button))
				.putBoolean(KEY_HAS_SHOWN_OPTIMIZATION_INFO, false)

				.putInt(KEY_SETTINGS_UI_VERSION, CURRENT_SETTINGS_UI_VERSION)
				.apply();
	}

	@Override
	protected void onResetOptimizedMode(ModeSettings modeOptimized) {
		super.onResetOptimizedMode(modeOptimized);

		// Auto detect some settings here
		App.getInstance().detectDeviceCapsForOptimizedMode(modeOptimized);
	}

	/**
	 * @return Gets whether screen captures are enabled
	 * 
	 */
	public boolean blockScreenshots()
	{
		return mPrefs.getBoolean(KEY_BLOCK_SCREENSHOTS, context.getResources().getBoolean(R.bool.pref_default_security_block_screenshots));
	}

	/**
	 * @return Sets whether screen captures are enabled
	 * 
	 */
	public void setBlockScreenshots(boolean block)
	{
		mPrefs.edit().putBoolean(KEY_BLOCK_SCREENSHOTS, block).apply();
	}

	/**
	 * If we have made content text larger or smaller store the adjustment here
	 * (we don't store the absolute size here since we might change the default
	 * font and therefore need to change the default font size as well).
	 *
	 * @return adjustment we have made to default font size (in sp units)
	 *
	 */
	public int getContentFontSizeAdjustment()
	{
		return mPrefs.getInt(KEY_CONTENT_FONT_SIZE_ADJUSTMENT, 0);
	}

	/**
	 * Set the adjustment for default font size in sp units (positive means
	 * larger, negative means smaller)
	 *
	 */
	public void setContentFontSizeAdjustment(int adjustment)
	{
		mPrefs.edit().putInt(KEY_CONTENT_FONT_SIZE_ADJUSTMENT, adjustment).apply();
	}


	public boolean showPanicButton()
	{
		return mPrefs.getBoolean(KEY_SHOW_PANIC_BUTTON, context.getResources().getBoolean(R.bool.pref_default_security_show_panic_button));
	}
	public void setShowPanicButton(boolean show)
	{
		mPrefs.edit().putBoolean(KEY_SHOW_PANIC_BUTTON, show).apply();
	}

	/**
	 * @return The onboarding stage we are on (if any)
	 * 
	 */
	public int getOnboardingStage()
	{
		return mPrefs.getInt(KEY_ONBOARDING_STAGE, 0);
	}

	/**
	 * @return Sets onboarding stage
	 * 
	 */
	public void setOnboardingStage(int value)
	{
		mPrefs.edit().putInt(KEY_ONBOARDING_STAGE, value).apply();
	}

	@Override
	public void resetSettings()
	{
		super.resetSettings();
		initializeIfNeeded();
	}

	public String uiLanguageCode() {
		String lang = uiLanguage();
		if (SupportedLanguage.isSupportedLanguageCode(lang)) {
			if (lang.equalsIgnoreCase("es_US")) {
				return "es";
			}
			return lang;
		}
		return "en";
	}

	public String uiRegionCode() {
		String lang = uiLanguage();
		if (SupportedLanguage.isSupportedLanguageCode(lang)) {
			if (lang.equalsIgnoreCase("es_US")) {
				return "US";
			}
		}
		return null;
	}

	public boolean hasShownOptimizationInfo()
	{
		return mPrefs.getBoolean(KEY_HAS_SHOWN_OPTIMIZATION_INFO, false);
	}

	public void setHasShownOptimizationInfo(boolean flag)
	{
		mPrefs.edit().putBoolean(KEY_HAS_SHOWN_OPTIMIZATION_INFO, flag).apply();
	}
}
