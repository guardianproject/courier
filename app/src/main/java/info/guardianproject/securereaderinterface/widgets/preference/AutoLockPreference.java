package info.guardianproject.securereaderinterface.widgets.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import info.guardianproject.securereaderinterface.R;

/**
 * Created by N-Pex on 2018-03-06.
 */


public class AutoLockPreference extends SwitchPreference {

    public interface AutoLockPreferenceListener {
        void onViewAutolockOptions(AutoLockPreference autoLockPreference);
    }

    private AutoLockPreferenceListener listener;
    private Switch widget;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AutoLockPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public AutoLockPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public AutoLockPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AutoLockPreference(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        setWidgetLayoutResource(R.layout.settings_switch_widget);
    }

    public AutoLockPreferenceListener getListener() {
        return listener;
    }

    public void setListener(AutoLockPreferenceListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        widget = view.findViewById(R.id.widget_switch);
        widget.setChecked(isChecked());
        widget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (getOnPreferenceClickListener() != null) {
                    getOnPreferenceClickListener().onPreferenceClick(AutoLockPreference.this);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() != null) {
                    getListener().onViewAutolockOptions(AutoLockPreference.this);
                }
            }
        });
    }

    @Override
    protected void onClick() {
    }

    @Override
    public boolean isChecked() {
        return getValue() != 0;
    }

    public int getValue() {
        return getPersistedInt(0);
    }

    public void setValue(int value) {
        persistInt(value);
    }

    @Override
    public CharSequence getSummary() {
        switch (getValue()) {
            case 0:
                return getContext().getString(R.string.pref_security_autolock_summary_never);
            case 1:
                return getContext().getString(R.string.pref_security_autolock_summary_immediately);
            case 300:
                return getContext().getString(R.string.pref_security_autolock_summary_5_minutes);
            case 3600:
                return getContext().getString(R.string.pref_security_autolock_summary_1_hour);
            case 86400:
                return getContext().getString(R.string.pref_security_autolock_summary_1_day);
            default:
                return "";
        }

    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (widget != null) {
            if (restoreValue) {
                widget.setChecked(isChecked());
            } else {
                widget.setChecked(defaultValue != null && (defaultValue instanceof Integer) && (((Integer) defaultValue).intValue() == Integer.valueOf(getKey())));
            }
        }
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        return persistInt(value ? 1 : 0);
    }

    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        return getPersistedInt(defaultReturnValue ? 1 : 0) != 0;
    }
}

/*
public class AutoLockPreference extends ListPreferenceWithSwitch {

    public interface AutoLockPreferenceListener {
        void shouldChangeValueTo(String value);
    }

    private int currentSelection;
    public AutoLockPreferenceListener listener;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AutoLockPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AutoLockPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AutoLockPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoLockPreference(Context context) {
        super(context);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        currentSelection = findIndexOfValue(getValue());
        builder.setSingleChoiceItems(getEntries(), currentSelection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currentSelection = which;
                    }
                });
        builder.setNeutralButton(null, null);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setPositiveButton(R.string.onboarding_next, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setValue(getEntryValues()[currentSelection].toString());
            }
        });
    }

    @Override
    protected boolean persistString(String value) {
        if(value == null) {
            return false;
        } else {
            if (listener != null) {
                listener.shouldChangeValueTo(value);
                return true;
            } else {
                return persistInt(Integer.valueOf(value));
            }
        }
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            int intValue = getPersistedInt(0);
            return String.valueOf(intValue);
        } else {
            return defaultReturnValue;
        }
    }
}
*/
