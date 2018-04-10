package info.guardianproject.securereaderinterface.widgets.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import info.guardianproject.securereaderinterface.R;

/**
 * Created by N-Pex on 2018-03-02.
 */

public class GroupSwitchPreference extends SwitchPreference {
    private String groupKey;
    private boolean useRadioButtons;
    private boolean useIntValues;
    private boolean forwardAllClicksToWidget;
    private CompoundButton widget;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GroupSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public GroupSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public GroupSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public GroupSwitchPreference(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.GroupSwitchPreference);
            if (a != null) {
                groupKey = a.getString(R.styleable.GroupSwitchPreference_groupKey);
                useRadioButtons = a.getBoolean(R.styleable.GroupSwitchPreference_useRadioButtons, false);
                useIntValues = a.getBoolean(R.styleable.GroupSwitchPreference_useIntValues, false);
                forwardAllClicksToWidget = a.getBoolean(R.styleable.GroupSwitchPreference_forwardAllClicksToWidget, false);
                a.recycle();
            }
        }
        if (useRadioButtons) {
            setWidgetLayoutResource(R.layout.settings_radio_widget);
        } else {
            setWidgetLayoutResource(R.layout.settings_switch_widget);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (useRadioButtons) {
            widget = view.findViewById(R.id.widget_radiobutton);
        } else {
            widget = view.findViewById(R.id.widget_switch);
        }
        widget.setChecked(isChecked());
        widget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                GroupSwitchPreference.this.setChecked(checked);
                if (checked && getOnPreferenceChangeListener() != null) {
                    getOnPreferenceChangeListener().onPreferenceChange(GroupSwitchPreference.this, checked);
                }

            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (forwardAllClicksToWidget) {
                    GroupSwitchPreference.this.setChecked(!GroupSwitchPreference.this.isChecked());
                    if (GroupSwitchPreference.this.isChecked() && getOnPreferenceChangeListener() != null) {
                        getOnPreferenceChangeListener().onPreferenceChange(GroupSwitchPreference.this, GroupSwitchPreference.this.isChecked());
                    }
                } else if (getOnPreferenceClickListener() != null) {
                    getOnPreferenceClickListener().onPreferenceClick(GroupSwitchPreference.this);
                }
            }
        });
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            setChecked(isChecked());
        } else {
            setChecked(defaultValue != null && (defaultValue instanceof String) && ((String)defaultValue).equalsIgnoreCase(getKey()));
        }
    }

    @Override
    public boolean isChecked() {
        if (groupKey != null) {
            if (useIntValues) {
                int currentValue = getSharedPreferences().getInt(groupKey, 0);
                return currentValue == Integer.valueOf(getKey());
            } else {
                String currentValue = getSharedPreferences().getString(groupKey, null);
                return currentValue != null && currentValue.equalsIgnoreCase(getKey());
            }
        }
        return false;
    }

    @Override
    public void setChecked(boolean checked) {
        if (widget != null) {
            widget.setChecked(checked);
        }
        if (checked && groupKey != null) {
            if (useIntValues) {
                getEditor().putInt(groupKey, Integer.valueOf(getKey())).apply();
            } else {
                getEditor().putString(groupKey, getKey()).apply();
            }
        }
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }
}
