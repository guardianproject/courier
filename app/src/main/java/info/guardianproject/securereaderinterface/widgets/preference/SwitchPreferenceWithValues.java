package info.guardianproject.securereaderinterface.widgets.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import info.guardianproject.securereaderinterface.R;

/**
 * Created by N-Pex on 2018-03-02.
 */

public class SwitchPreferenceWithValues extends SwitchPreference {
    private String valueOn;
    private String valueOff;
    private CompoundButton widget;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwitchPreferenceWithValues(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public SwitchPreferenceWithValues(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public SwitchPreferenceWithValues(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SwitchPreferenceWithValues(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SwitchPreferenceWithValues);
            if (a != null) {
                valueOn = a.getString(R.styleable.SwitchPreferenceWithValues_valueOn);
                valueOff = a.getString(R.styleable.SwitchPreferenceWithValues_valueOff);
                a.recycle();
            }
        }
        setWidgetLayoutResource(R.layout.settings_switch_widget);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        widget = view.findViewById(R.id.widget_switch);
        widget.setChecked(isChecked());
        widget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                setChecked(checked);
                if (getOnPreferenceClickListener() != null) {
                    getOnPreferenceClickListener().onPreferenceClick(SwitchPreferenceWithValues.this);
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SwitchPreferenceWithValues.this.onClick();
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
        if (valueOff == null) {
            return false;
        }
        return getPersistedString(valueOff).equalsIgnoreCase(valueOn);
    }

    @Override
    public void setChecked(boolean checked) {
        if (widget != null) {
            widget.setChecked(checked);
        }
        getEditor().putString(getKey(), checked ? valueOn : valueOff).apply();
    }
}
