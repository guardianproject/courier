package info.guardianproject.securereaderinterface.profiler;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.AppActivity;
import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.SettingsActivity;

public class ProfilerWizardActivity extends AppActivity {
	private static final boolean LOGGING = false;
	private static final String LOGTAG = "ProfilerWizard";

	private int currentStep = 0; // Current step in the wizard

	private Button btnNext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentViewNoBase(R.layout.activity_profiler_wizard);

		// Display home as up
		setDisplayHomeAsUp(true);
		setMenuIdentifier(R.menu.activity_profiler_wizard);

		btnNext = findViewById(R.id.btnNext);
		btnNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setStep(currentStep + 1);
			}
		});

		getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
			@Override
			public void onBackStackChanged() {
				Fragment f = getFragmentManager().findFragmentById(R.id.settings_fragment);
				if (f != null && f instanceof PreferenceFragment) {
					PreferenceFragment pf = (PreferenceFragment)f;
					if (pf.getPreferenceScreen() != null) {
						setActionBarTitle(pf.getPreferenceScreen().getTitle());
					}
				}
			}
		});

		setStep(0);
	}

	private void setStep(int step) {
		currentStep = step;
		if (step == 0) {
			btnNext.setText(R.string.onboarding_next);
			getToolbar().setBackgroundColor(ContextCompat.getColor(this, R.color.settings_sync_data));
			String fragmentName = SettingsActivity.SyncPreferenceFragment.class.getName();
			Bundle arguments = new Bundle();
			arguments.putString(SettingsActivity.SyncPreferenceFragment.ARG_PREF_FILE, App.getSettings().getModeFilename(Settings.Mode.Optimized));
			arguments.putString(SettingsActivity.SyncPreferenceFragment.ARG_PREF_KEY, getString(R.string.pref_key_sync_over_data));
			arguments.putString(SettingsActivity.SyncPreferenceFragment.ARG_TITLE, getString(R.string.pref_over_data));
			arguments.putInt(SettingsActivity.SyncPreferenceFragment.ARG_COLOR, ContextCompat.getColor(this, R.color.settings_sync_data));
			Fragment f = Fragment.instantiate(this, fragmentName, arguments);
			getFragmentManager().beginTransaction().replace(R.id.fragment_container,
					f).commit();
		} else if (step == 1) {
			btnNext.setText(R.string.pref_done);
			getToolbar().setBackgroundColor(ContextCompat.getColor(this, R.color.settings_sync_wifi));
			String fragmentName = SettingsActivity.SyncPreferenceFragment.class.getName();
			Bundle arguments = new Bundle();
			arguments.putString(SettingsActivity.SyncPreferenceFragment.ARG_PREF_FILE, App.getSettings().getModeFilename(Settings.Mode.Optimized));
			arguments.putString(SettingsActivity.SyncPreferenceFragment.ARG_PREF_KEY, getString(R.string.pref_key_sync_over_wifi));
			arguments.putString(SettingsActivity.SyncPreferenceFragment.ARG_TITLE, getString(R.string.pref_over_wifi));
			arguments.putInt(SettingsActivity.SyncPreferenceFragment.ARG_COLOR, ContextCompat.getColor(this, R.color.settings_sync_wifi));
			Fragment f = Fragment.instantiate(this, fragmentName, arguments);
			getFragmentManager().beginTransaction().replace(R.id.fragment_container,
					f).commit();
		} else {
			finish();
		}
	}

	@Override
	protected boolean shouldAddDefaultOptionsItems() {
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			if (currentStep > 0) {
				setStep(currentStep - 1);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (currentStep > 0) {
			setStep(currentStep - 1);
			return;
		}
		super.onBackPressed();
	}
}
