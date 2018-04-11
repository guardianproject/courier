package info.guardianproject.securereaderinterface;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import info.guardianproject.cacheword.CacheWordHandler;
import info.guardianproject.cacheword.ICacheWordSubscriber;
import info.guardianproject.cacheword.PassphraseSecrets;
import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.widgets.DottedProgressView;
import info.guardianproject.securereaderinterface.widgets.NestedViewPager;
import info.guardianproject.securereaderinterface.widgets.preference.AutoLockPreference;
import info.guardianproject.securereaderinterface.widgets.preference.GroupSwitchPreference;
import info.guardianproject.securereaderinterface.widgets.preference.ListPreferenceWithSwitch;
import info.guardianproject.securereaderinterface.widgets.preference.SyncPreference;

public class SettingsActivity extends AppActivity implements ICacheWordSubscriber {
	private static final boolean LOGGING = false;
	private static final String LOGTAG = "Settings";

	SettingsUI mSettings;

	private boolean mIsBeingRecreated;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mSettings = App.getSettings();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		setMenuIdentifier(R.menu.activity_settings);
		setDisplayHomeAsUp(true);

		AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
		((AppBarLayout) mToolbar.getParent()).setExpanded(true, true);
		lp.setScrollFlags(0);
		mToolbar.setLayoutParams(lp);

		getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
				Fragment f = getFragmentManager().findFragmentById(R.id.settings_fragment);
				if (f != null && f instanceof PreferenceFragment) {
					PreferenceFragment pf = (PreferenceFragment)f;
					if (f instanceof SyncPreferenceFragment) {
						setActionBarColor(((SyncPreferenceFragment)f).getColor(), false);
						setActionBarTitle(null);
					} else {
						if (pf.getPreferenceScreen() != null) {
							setActionBarTitle(pf.getPreferenceScreen().getTitle());
						}

						// Restore to colorPrimary
						int color = ContextCompat.getColor(SettingsActivity.this, R.color.claret);
						TypedArray a = SettingsActivity.this.getTheme().obtainStyledAttributes(new int[] { R.attr.colorPrimary });
						if (a != null) {
							color = a.getColor(0, color);
							a.recycle();
						}
						setActionBarColor(color, true);
					}
				}
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			if (getFragmentManager().getBackStackEntryCount() > 0) {
				getFragmentManager().popBackStack();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (getFragmentManager().getBackStackEntryCount() > 0) {
			getFragmentManager().popBackStack();
			return;
		}
		super.onBackPressed();
	}

	@Override
	public void recreateNowOrOnResume() {
		mIsBeingRecreated = true;
		super.recreateNowOrOnResume();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		super.onSharedPreferenceChanged(sharedPreferences, key);
		if (key.equals(SettingsUI.KEY_PROXY_TYPE)) { // TODO || key.equals(ModeSettings.KEY_SYNC_MODE)) {
			updateLeftSideMenu();
		}
	}

	@Override
	public void onCacheWordLocked() {
	}

	@Override
	public void onCacheWordOpened() {
	}

	@Override
	public void onCacheWordUninitialized() {
	}

	void promptForNewPassphrase(final int setTimeoutToThisValue) {
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_change_passphrase, null, false);

		final EditText editEnterPassphrase = (EditText) contentView.findViewById(R.id.editEnterPassphrase);
		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);

		// If we have an auto-generated PW, populate field and hide it
		if (App.getSettings().autoLock() == 0) {
			editEnterPassphrase.setText(App.getSettings().launchPassphrase());
			editEnterPassphrase.setVisibility(View.GONE);
		}

		Builder alert = new AlertDialog.Builder(this)
				.setTitle(App.getSettings().autoLock() == 0 ? R.string.settings_security_create_passphrase : R.string.settings_security_change_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0) {
							dialog.dismiss();
							promptForNewPassphrase(setTimeoutToThisValue);
							return; // Try again...
						}

						if (!(editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()))) {
							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_not_matching), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForNewPassphrase(setTimeoutToThisValue);
							return; // Try again...
						}

						CacheWordHandler cwh = new CacheWordHandler(SettingsActivity.this);

						char[] passwd = editEnterPassphrase.getText().toString().toCharArray();
						PassphraseSecrets secrets;
						try {
							secrets = PassphraseSecrets.fetchSecrets(SettingsActivity.this, passwd);
							cwh.changePassphrase(secrets, editNewPassphrase.getText().toString().toCharArray());
							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_changed), Toast.LENGTH_LONG).show();
							if (setTimeoutToThisValue != App.getSettings().autoLock()) {
								App.getSettings().setAutolock(setTimeoutToThisValue);
							}
						} catch (Exception e) {
							// Invalid password or the secret key has been
							if (LOGGING)
								Log.e(LOGTAG, e.getMessage());

							Toast.makeText(SettingsActivity.this, getString(R.string.change_passphrase_incorrect), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForNewPassphrase(setTimeoutToThisValue);
							return; // Try again...
						}

						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.show();
	}

	private void setTimeoutToNever() {
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_change_passphrase, null, false);

		final EditText editEnterPassphrase = (EditText) contentView.findViewById(R.id.editEnterPassphrase);
		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);
		editNewPassphrase.setVisibility(View.GONE);
		editConfirmNewPassphrase.setVisibility(View.GONE);
		Builder alert = new AlertDialog.Builder(this)
				.setTitle(R.string.settings_security_enter_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						CacheWordHandler cwh = new CacheWordHandler(SettingsActivity.this);

						char[] passwd = editEnterPassphrase.getText().toString().toCharArray();
						PassphraseSecrets secrets;
						try {
							secrets = PassphraseSecrets.fetchSecrets(SettingsActivity.this, passwd);
							cwh.changePassphrase(secrets, LockScreenActivity.getCWPassword().toCharArray());
							App.getSettings().setAutolock(0);
						} catch (Exception e) {
							// Invalid password or the secret key has been
							if (LOGGING)
								Log.e(LOGTAG, e.getMessage());
							dialog.dismiss();
							setTimeoutToNever();
							return; // Try again...
						}
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.show();
	}

	/**
	 * Lets the user input a kill passphrase
	 *
	 * @param setToOnIfSuccessful If true, update the settings if we manage to set the
	 *                            passphrase.
	 */
	void promptForKillPassphrase(final boolean setToOnIfSuccessful) {
		View contentView = LayoutInflater.from(this).inflate(R.layout.settings_set_kill_passphrase, null, false);

		final EditText editNewPassphrase = (EditText) contentView.findViewById(R.id.editNewPassphrase);
		final EditText editConfirmNewPassphrase = (EditText) contentView.findViewById(R.id.editConfirmNewPassphrase);

		Builder alert = new AlertDialog.Builder(this)
				.setTitle(R.string.settings_security_set_kill_passphrase)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (editNewPassphrase.getText().length() == 0 && editConfirmNewPassphrase.getText().length() == 0) {
							dialog.dismiss();
							promptForKillPassphrase(setToOnIfSuccessful);
							return; // Try again...
						}

						// Check old
						boolean matching = (editNewPassphrase.getText().toString().equals(editConfirmNewPassphrase.getText().toString()));
						boolean sameAsPassphrase = false;
						CacheWordHandler cwh = new CacheWordHandler(SettingsActivity.this);
						try {
							cwh.setPassphrase(editNewPassphrase.getText().toString().toCharArray());
							sameAsPassphrase = true;
						} catch (GeneralSecurityException e) {
							if (LOGGING)
								Log.e(LOGTAG, "Cacheword initialization failed: " + e.getMessage());
						}
						if (!matching || sameAsPassphrase) {
							editNewPassphrase.setText("");
							editConfirmNewPassphrase.setText("");
							editNewPassphrase.requestFocus();
							if (!matching)
								Toast.makeText(SettingsActivity.this, getString(R.string.lock_screen_passphrases_not_matching), Toast.LENGTH_LONG).show();
							else
								Toast.makeText(SettingsActivity.this, getString(R.string.settings_security_kill_passphrase_same_as_login), Toast.LENGTH_LONG).show();
							dialog.dismiss();
							promptForKillPassphrase(setToOnIfSuccessful);
							return; // Try again...
						}

						// Store
						App.getSettings().setKillPassphrase(editNewPassphrase.getText().toString());
						if (setToOnIfSuccessful)
							updateUseKillPassphrase();
						dialog.dismiss();
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.setView(contentView);
		AlertDialog dialog = alert.create();
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (setToOnIfSuccessful)
					updateUseKillPassphrase();
			}
		});
		dialog.show();
	}

	private void updateUseKillPassphrase() {
		if (!TextUtils.isEmpty(mSettings.killPassphrase())) {
			mSettings.setUseKillPassphrase(true);
		} else {
			mSettings.setUseKillPassphrase(false);
		}
	}

	public static class PreferenceFragmentBase extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener, AutoLockPreference.AutoLockPreferenceListener {

		@Override
		public void addPreferencesFromResource(int preferencesResId) {
			super.addPreferencesFromResource(preferencesResId);
			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
				Preference p = getPreferenceScreen().getPreference(i);
				if (p instanceof PreferenceGroup) {
					PreferenceGroup group = (PreferenceGroup)p;
					for (int j = 0; j < group.getPreferenceCount(); j++) {
						Preference gp = group.getPreference(j);
						if (gp instanceof GroupSwitchPreference) {
							GroupSwitchPreference gsp = (GroupSwitchPreference)gp;
							gsp.setOnPreferenceClickListener(this);
							gsp.setOnPreferenceChangeListener(this);
						} else if (gp instanceof AutoLockPreference) {
							gp.setOnPreferenceClickListener(this);
							((AutoLockPreference)gp).setListener(this);
						}
					}
				}
			}
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (preference instanceof GroupSwitchPreference) {
				GroupSwitchPreference gsp = (GroupSwitchPreference) preference;
				if (!TextUtils.isEmpty(gsp.getGroupKey())) {
					selectPrefInGroup(gsp);
				}
			}
			return true;
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
			if (preference.getFragment() != null) {
				String fragmentName = preference.getFragment();
				Fragment f = Fragment.instantiate(getActivity(), fragmentName);
				getFragmentManager().beginTransaction().add(R.id.settings_fragment,
						f).addToBackStack(fragmentName).commit();
				return true;
			} else if (preference.getKey().equalsIgnoreCase(getString(R.string.pref_autolock_reset))) {
				// Reset the auto lock password
				((SettingsActivity)getActivity()).promptForNewPassphrase(App.getSettings().autoLock());
			}
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (preference instanceof GroupSwitchPreference) {
/*				GroupSwitchPreference gsp = (GroupSwitchPreference)preference;
				if (!TextUtils.isEmpty(gsp.getGroupKey())) {
					selectPrefInGroup(gsp);
				}*/
				if (preference.getFragment() != null) {
					String fragmentName = preference.getFragment();
					Fragment f = Fragment.instantiate(getActivity(), fragmentName);
					getFragmentManager().beginTransaction().add(R.id.settings_fragment,
							f).addToBackStack(fragmentName).commit();
					return true;
				}
				return true;
			} else if (preference instanceof AutoLockPreference) {
				AutoLockPreference alp = (AutoLockPreference)preference;
				if (alp.getValue() != 0) {
					((SettingsActivity)getActivity()).setTimeoutToNever();
				} else {
					((SettingsActivity)getActivity()).promptForNewPassphrase(1);
				}
				return true;
			}
			return false;
		}

		private void selectPrefInGroup(GroupSwitchPreference pref) {
			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
				Preference p = getPreferenceScreen().getPreference(i);
				if (p instanceof PreferenceGroup) {
					PreferenceGroup group = (PreferenceGroup)p;
					for (int j = 0; j < group.getPreferenceCount(); j++) {
						Preference gp = group.getPreference(j);
						if (gp instanceof GroupSwitchPreference) {
							GroupSwitchPreference gsp = (GroupSwitchPreference)gp;
							if (pref.getGroupKey().equalsIgnoreCase(gsp.getGroupKey())) {
								gsp.setChecked(gp == pref);
							}
						}
					}
				}
			}
		}

		@Override
		public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			final ListView lv = (ListView) view.findViewById(android.R.id.list);
			if (lv != null) {
				ViewCompat.setNestedScrollingEnabled(lv, true);
			}
		}

		@Override
		public void onViewAutolockOptions(AutoLockPreference autoLockPreference) {
			if (autoLockPreference.getValue() == 0) {
				// Not enabled. Ask for password to enable it!
				((SettingsActivity)getActivity()).promptForNewPassphrase(1);
			} else {
				String fragmentName = AutoLockPreferenceFragment.class.getName();
				Fragment f = Fragment.instantiate(getActivity(), fragmentName, null);
				getFragmentManager().beginTransaction().replace(R.id.settings_fragment,
						f).addToBackStack(fragmentName).commit();
			}
		}
	}

	public static class GeneralPreferenceFragment extends PreferenceFragmentBase {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_general);
		}
	}

	public static class ModePreferenceFragment extends PreferenceFragmentBase implements SharedPreferences.OnSharedPreferenceChangeListener {
		protected Settings.Mode mode;
		protected int resIdTitle = 0;
		protected int resIdTitleCustomized = 0;
		protected int resIdReset = 0;
		protected Preference resetPreference;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager().setSharedPreferencesName(App.getSettings().getModeFilename(mode));
			getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
			addPreferencesFromResource(R.xml.pref_mode);
			updateUI();
		}

		@Override
		public void onDestroy() {
			getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
			super.onDestroy();
		}

		protected void setTitle(int idString) {
			getPreferenceScreen().setTitle(idString);
			if (isVisible()) {
				((SettingsActivity) getActivity()).setActionBarTitle(getPreferenceScreen().getTitle());
			}
		}

		@Override
		public void addPreferencesFromResource(int preferencesResId) {
			super.addPreferencesFromResource(preferencesResId);
			resetPreference = findPreference("reset");
			resetPreference.setTitle(resIdReset);
			findPreference(getString(R.string.pref_key_sync_over_wifi)).setOnPreferenceClickListener(this);
			findPreference(getString(R.string.pref_key_sync_over_data)).setOnPreferenceClickListener(this);
		}

		protected void setShowReset(boolean show) {
			if (show) {
				if (getPreferenceScreen().findPreference("reset") == null) {
					getPreferenceScreen().addPreference(resetPreference);
				}
			} else {
				getPreferenceScreen().removePreference(resetPreference);
			}
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
			boolean ret = super.onPreferenceTreeClick(preferenceScreen, preference);
			if (preference.getKey().equalsIgnoreCase("reset")) {
				App.getSettings().resetModeSettings(mode);
				getPreferenceScreen().removeAll();
				addPreferencesFromResource(R.xml.pref_mode);
			}
			updateUI();
			return ret;
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
		    if (preference instanceof SyncPreference) {
		    	String fragmentName = SyncPreferenceFragment.class.getName();
		    	Bundle arguments = new Bundle();
		    	arguments.putString(SyncPreferenceFragment.ARG_PREF_FILE, App.getSettings().getModeFilename(mode));
		    	arguments.putString(SyncPreferenceFragment.ARG_PREF_KEY, preference.getKey());
		    	arguments.putString(SyncPreferenceFragment.ARG_TITLE, ((SyncPreference)preference).getScreenTitle());
		    	arguments.putInt(SyncPreferenceFragment.ARG_COLOR, ((SyncPreference)preference).getScreenColor());
				Fragment f = Fragment.instantiate(getActivity(), fragmentName, arguments);
				getFragmentManager().beginTransaction().replace(R.id.settings_fragment,
						f).addToBackStack(fragmentName).commit();
		        return true;
            }
			return super.onPreferenceClick(preference);
		}

		private void updateUI() {
			if (App.getSettings().hasChangedModeSettings(mode)) {
				setTitle(resIdTitleCustomized);
				setShowReset(true);
			} else {
				setTitle(resIdTitle);
				setShowReset(false);
			}
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			updateUI();
		}

        @Override
        public void onResume() {
            super.onResume();
			getPreferenceScreen().removeAll();
			addPreferencesFromResource(R.xml.pref_mode);
			updateUI();
        }
    }

	public static class OptimizedModePreferenceFragment extends ModePreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			mode = Settings.Mode.Optimized;
			resIdTitle = R.string.pref_mode_optimized_title;
			resIdTitleCustomized = R.string.pref_mode_optimized_title_customized;
			resIdReset = R.string.pref_reset_optimized_defaults;
			super.onCreate(savedInstanceState);
		}
	}

	public static class EverythingModePreferenceFragment extends ModePreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			mode = Settings.Mode.Everything;
			resIdTitle = R.string.pref_mode_everything_title;
			resIdTitleCustomized = R.string.pref_mode_everything_title_customized;
			resIdReset = R.string.pref_reset_everything_defaults;
			super.onCreate(savedInstanceState);
		}
	}

	public static class AboutFragment extends Fragment {
		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.settings_about, container, false);
		}
	}

	public static class TermsFragment extends Fragment {
		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.settings_terms, container, false);
		}
	}

	public static class SyncPreferenceFragment extends PreferenceFragment {

		public static final String ARG_PREF_FILE = "pref_file";
		public static final String ARG_PREF_KEY = "pref_key";
		public static final String ARG_TITLE = "title";
		public static final String ARG_COLOR = "color";
		private SyncPreference syncPreference;
		private TextView summary;

		@Override
		public void onCreate(@Nullable Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager().setSharedPreferencesName(getArguments().getString(ARG_PREF_FILE));
			syncPreference = new SyncPreference(getActivity());
			syncPreference.setKey(getArguments().getString(ARG_PREF_KEY));
			syncPreference.setEntries(R.array.pref_key_sync_entries);
			syncPreference.setEntryValues(R.array.pref_key_sync_values);
			syncPreference.setSummaries(R.array.pref_key_sync_entries_summary);
			syncPreference.setValues(getPreferenceManager().getSharedPreferences().getStringSet(syncPreference.getKey(), null));
		}

		public int getColor() {
			return getArguments().getInt(ARG_COLOR, Color.WHITE);
		}

		void updateSummary() {
			if (summary != null && syncPreference != null) {
				summary.setText(syncPreference.getCurrentSummary());
			}
		}

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

			Context context = container.getContext();

			View view = inflater.inflate(R.layout.settings_sync, container, false);

			int color = getArguments().getInt(ARG_COLOR, Color.WHITE);
			view.setBackgroundColor(color);

			TextView title = view.findViewById(R.id.title);
			title.setText(getArguments().getString(ARG_TITLE, ""));

			summary = view.findViewById(R.id.text);
			updateSummary();

			DottedProgressView indicator = view.findViewById(R.id.screenshotsIndicator);

			final NestedViewPager viewPager = view.findViewById(R.id.screenshots);
			viewPager.setViewPagerIndicator(indicator);
			viewPager.setOffscreenPageLimit(10);
			//viewPager.setPageMargin(container.getWidth() / 10);
			viewPager.setPageTransformer(false, new ViewPager.PageTransformer() {
				private static final float MIN_FADE = 0.2f;

				public void transformPage(View view, float position) {


					ImageView imageView = (ImageView)view;
					float w = imageView.getDrawable().getIntrinsicWidth();
					float h = imageView.getDrawable().getIntrinsicHeight();
					float aspect = w / h;

					int availableWidth = view.getWidth();
					int availableHeight = view.getHeight();

					int pageWidth = (int)(availableHeight * aspect);

					int containerWidth = viewPager.getWidth();

					if (position < -1) {
						view.setAlpha(MIN_FADE);
					} else if (position < 0) {
						view.setAlpha(1 + position * (1 - MIN_FADE));
						view.setTranslationX(-containerWidth * position + pageWidth * 1.1f * position);
						ViewCompat.setTranslationZ(view, position);
						float scaleFactor = 1.0f - 0.2f * Math.abs(position);
						view.setScaleX(scaleFactor);
						view.setScaleY(scaleFactor);
					} else if (position == 0) {
						view.setAlpha(1);
						view.setTranslationX(0);
						view.setScaleX(1.0f);
						ViewCompat.setTranslationZ(view, 0);
						view.setScaleY(1.0f);
					} else if (position <= 1) {
						ViewCompat.setTranslationZ(view, -position);
						view.setAlpha(1 - position * (1 - MIN_FADE));
						view.setTranslationX(-containerWidth * position + pageWidth * 1.1f * position);
						float scaleFactor = 1.0f - 0.2f * Math.abs(position);
						view.setScaleX(scaleFactor);
						view.setScaleY(scaleFactor);
					} else {
						view.setAlpha(MIN_FADE);
					}
				}
			});
			viewPager.setAdapter(new ScreenShotPagerAdapter(viewPager));

			boolean syncMedia = false;
			boolean syncFullText = false;
			Set<String> values = syncPreference.getValues();
			for (String value : values) {
				if (value.equalsIgnoreCase(getString(R.string.pref_key_sync_media))) {
					syncMedia = true;
				}
				if (value.equalsIgnoreCase(getString(R.string.pref_key_sync_full_text))) {
					syncFullText = true;
				}
			}
			if (syncMedia && syncFullText) {
				viewPager.setCurrentItem(2);
			} else if (syncFullText) {
				viewPager.setCurrentItem(1);
			}

			viewPager.postDelayed(new Runnable() {
				@Override
				public void run() {
					// force transform with a 1 pixel nudge
					viewPager.beginFakeDrag();
					viewPager.fakeDragBy(1.0f);
					viewPager.endFakeDrag();
				}
			}, 10);

			return view;
		}

		@Override
		public void onAttach(Context context) {
			super.onAttach(context);
			if (context instanceof SettingsActivity) {
				((SettingsActivity) context).setActionBarTitle(getArguments().getString(ARG_TITLE, ""));
			}
		}

		private class ScreenShotPagerAdapter extends PagerAdapter {
			private int[] images;
			private String[][] settings;
			private ViewPager viewPager;

			public ScreenShotPagerAdapter(ViewPager viewPager) {
				super();
				this.viewPager = viewPager;
				images = new int[]{
						R.mipmap.story_preview,
						R.mipmap.story_full_text_no_media,
						R.mipmap.story_full_text_media
				};
				settings = new String[][] {
						new String[] { getString(R.string.pref_key_sync_summaries) },
						new String[] { getString(R.string.pref_key_sync_summaries), getString(R.string.pref_key_sync_full_text) },
						new String[] { getString(R.string.pref_key_sync_summaries), getString(R.string.pref_key_sync_full_text), getString(R.string.pref_key_sync_media)}
				};
			}

			@Override
			public void setPrimaryItem(ViewGroup container, int position,
									   Object object) {
				super.setPrimaryItem(container, position, object);
				Set<String> values = new HashSet<String>(Arrays.asList(settings[position]));
				syncPreference.setValues(values); // Will not persist, this preference is detached!
				getPreferenceManager().getSharedPreferences().edit().putStringSet(syncPreference.getKey(), values).apply();
				updateSummary();
			}

			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				return arg0.equals(arg1);
			}

			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				View view = LayoutInflater.from(container.getContext())
						.inflate(R.layout.settings_screenshot_item, container, false);
				ImageView imageView = view.findViewById(R.id.image);
				imageView.setImageResource(images[position]);
				container.addView(view);
				return view;
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
				View imageView = (View) object;
				container.removeView(imageView);
			}

			@Override
			public int getCount() {
				return images.length;
			}
		}
	}

	public static class AutoLockPreferenceFragment extends PreferenceFragmentBase implements SharedPreferences.OnSharedPreferenceChangeListener {

		private PreferenceGroup limitPreferencesGroup;
		private ArrayList<Preference> limitPreferences = new ArrayList<>();

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
			addPreferencesFromResource(R.xml.pref_auto_lock);
		}

		@Override
		public void onDestroy() {
			getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
			super.onDestroy();
		}

		@Override
		public void addPreferencesFromResource(int preferencesResId) {
			super.addPreferencesFromResource(preferencesResId);
			limitPreferences.clear();
			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
				Preference p = getPreferenceScreen().getPreference(i);
				if (p instanceof PreferenceGroup) {
					PreferenceGroup group = (PreferenceGroup)p;
					for (int j = 0; j < group.getPreferenceCount(); j++) {
						Preference gp = group.getPreference(j);
						if (gp instanceof ListPreferenceWithSwitch) {
							ListPreferenceWithSwitch gsp = (ListPreferenceWithSwitch) gp;
						} else if (gp instanceof GroupSwitchPreference) {
							GroupSwitchPreference groupSwitchPreference = (GroupSwitchPreference)gp;
							if (groupSwitchPreference.getGroupKey().equalsIgnoreCase(getString(R.string.pref_key_wrong_password_limit))) {
								limitPreferencesGroup = group;
								limitPreferences.add(groupSwitchPreference);
							}
						}
						gp.setOnPreferenceChangeListener(this);
					}
				}
			}
			boolean show = App.getSettings().wrongPasswordAction() != Settings.PanicAction.Nothing;
			updateShowLimitPreferences(show);
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean ret = super.onPreferenceChange(preference, newValue);
			if (preference.getKey().equalsIgnoreCase(Settings.KEY_WRONG_PASSWORD_ACTION)) {
				updateShowLimitPreferences(newValue instanceof String && !((String) newValue).equalsIgnoreCase(getString(R.string.pref_key_wrong_password_action_nothing)));
			} else if (preference.getKey().equalsIgnoreCase(Settings.KEY_USE_KILL_PASSPHRASE)) {
			    if (newValue instanceof Boolean && (Boolean) newValue) {
                    ((SettingsActivity) getActivity()).promptForKillPassphrase(true);
                    return false;
                }
			}
			return ret;
		}

		private void updateShowLimitPreferences(boolean show) {
			if (limitPreferencesGroup != null) {
				// First remove all
				for (Preference p : limitPreferences) {
					limitPreferencesGroup.removePreference(p);
				}
				if (show) {
					for (Preference p : limitPreferences) {
						limitPreferencesGroup.addPreference(p);
					}
				}
			}
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equalsIgnoreCase(Settings.KEY_USE_KILL_PASSPHRASE)) {
				// Update
				getPreferenceScreen().removeAll();
				addPreferencesFromResource(R.xml.pref_auto_lock);
			}
		}
	}
}
