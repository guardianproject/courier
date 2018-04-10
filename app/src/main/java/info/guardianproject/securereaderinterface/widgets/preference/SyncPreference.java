package info.guardianproject.securereaderinterface.widgets.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import info.guardianproject.securereaderinterface.R;

/**
 * Created by N-Pex on 2018-03-02.
 */

public class SyncPreference extends MultiSelectListPreference {

    private CheckBox checkBoxWidget;
    private AlertDialog currentDialog;
    private CharSequence[] summaries;
    private int color;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SyncPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SyncPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public SyncPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SyncPreference(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
        color = Color.WHITE;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SyncPreference);
            if (a != null) {
                summaries = a.getTextArray(R.styleable.SyncPreference_summaries);
                color = a.getColor(R.styleable.SyncPreference_screenColor, color);
                a.recycle();
            }
        }
    }

    public int getColor() {
        return color;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        checkBoxWidget = view.findViewById(R.id.widget_checkbox);
        if (checkBoxWidget != null) {
            checkBoxWidget.setChecked(getValues().size() > 0);
            checkBoxWidget.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (!checked) {
                        setValues(new HashSet<String>());
                    } else {
                        if (getOnPreferenceClickListener() != null) {
                            getOnPreferenceClickListener().onPreferenceClick(SyncPreference.this);
                        }
                    }
                }
            });
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getOnPreferenceClickListener() != null) {
                    getOnPreferenceClickListener().onPreferenceClick(SyncPreference.this);
                }
            }
        });
        updateSummary();
        view.invalidate();
    }

    public void setSummaries(int idResArray) {
        summaries = getContext().getResources().getStringArray(idResArray);
    }

    public void setSummaries(CharSequence[] summaries) {
        this.summaries = summaries;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        updateSummary();
    }

    @Override
    public void setValues(Set<String> values) {
        super.setValues(values);
        updateSummary();
        if (checkBoxWidget != null) {
            checkBoxWidget.setChecked(values.size() > 0);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        if (getOnPreferenceClickListener() != null) {
            getOnPreferenceClickListener().onPreferenceClick(this);
        }
    }

    private void updateSummary() {
        setSummary(getCurrentSummary());
    }

    public CharSequence getCurrentSummary() {
        Set<String> values = getValues();
        if (values == null || values.size() == 0) {
            return null;
        }
        final List<CharSequence> entryValues = Arrays.asList(getEntryValues());

        List<String> sortedValues = new ArrayList<String>(values);
        Collections.sort(sortedValues, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                int i1 = entryValues.indexOf(s1);
                int i2 = entryValues.indexOf(s2);
                if (i1 == i2) {
                    return 0;
                }
                return i1 < i2 ? -1 : 1;
            }
        });

        // Don't show the first one in the summary
        if (sortedValues.size() > 1 && sortedValues.get(0).equalsIgnoreCase(entryValues.get(0).toString())) {
            sortedValues.remove(0);
        }

        ArrayList<CharSequence> display = new ArrayList<>(sortedValues.size());
        for (String value : sortedValues) {
            int idx = entryValues.indexOf(value);
            if (summaries != null) {
                display.add(summaries[idx]);
            } else {
                display.add(getEntries()[idx]);
            }
        }

        if (display.size() == 1) {
            return display.get(0);
        } else if (display.size() == 2) {
            return getContext().getString(R.string.pref_summary_x_and_y, display.get(0), display.get(1));
        } else if (sortedValues.size() == 3) {
            return getContext().getString(R.string.pref_summary_x_and_y_and_z, display.get(0), display.get(1), display.get(2));
        } else {
            // TODO - can we select this many?
            return null;
        }
    }
}
