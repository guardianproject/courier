package info.guardianproject.securereaderinterface.widgets.preference;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.Arrays;

import info.guardianproject.securereaderinterface.R;
import info.guardianproject.securereaderinterface.ui.SupportedLanguage;

/**
 * Created by N-Pex on 2018-03-06.
 */

public class ListPreferenceWithSwitch extends ListPreference {
    private int uncheckedIndex = -1; //When this index is selected, the switch is shown as unchecked
    private String uncheckedValue = null;
    private CharSequence[] summaries;
    private String uncheckedValueSummary = null;
    private Switch widget;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreferenceWithSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreferenceWithSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public ListPreferenceWithSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ListPreferenceWithSwitch(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ListPreferenceWithSwitch);
            if (a != null) {
                uncheckedIndex = a.getInteger(R.styleable.ListPreferenceWithSwitch_uncheckedIndex, -1);
                uncheckedValue = a.getString(R.styleable.ListPreferenceWithSwitch_uncheckedValue);
                summaries = a.getTextArray(R.styleable.ListPreferenceWithSwitch_summaries);
                uncheckedValueSummary = a.getString(R.styleable.ListPreferenceWithSwitch_uncheckedValueSummary);
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
                if (checked) {
                    if (!isChecked()) {
                        ListPreferenceWithSwitch.super.onClick();
                    }
                } else {
                    if (uncheckedIndex != -1) {
                        String value = getEntryValues()[uncheckedIndex].toString();
                        if (callChangeListener(value)) {
                            setValue(value);
                        }
                    } else if (uncheckedValue != null) {
                        if (callChangeListener(uncheckedValue)) {
                            setValue(uncheckedValue);
                        }
                    }
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ListPreferenceWithSwitch.super.onClick();
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        widget.setChecked(isChecked());
    }

    boolean isChecked() {
        if (uncheckedIndex != -1 && Arrays.asList(getEntryValues()).indexOf(getValue()) == uncheckedIndex) {
            return false;
        } else if (uncheckedValue != null && uncheckedValue.equalsIgnoreCase(getValue())) {
            return false;
        }
        return true;
    }

    @Override
    public CharSequence getSummary() {
        if (uncheckedIndex == -1 && uncheckedValue != null && uncheckedValue.equalsIgnoreCase(getValue())) {
            return uncheckedValueSummary;
        }
        if (summaries == null) {
            return "";
        }
        int index = findIndexOfValue(getValue());
        if (index < 0 || index >= summaries.length) {
            return "";
        }
        return summaries[index];
    }
}
