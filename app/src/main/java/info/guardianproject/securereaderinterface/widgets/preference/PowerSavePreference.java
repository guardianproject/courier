package info.guardianproject.securereaderinterface.widgets.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import info.guardianproject.securereaderinterface.R;

/**
 * Created by N-Pex on 2018-03-02.
 */

public class PowerSavePreference extends SwitchPreference {
    private CompoundButton widget;
    private AlertDialog currentDialog;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PowerSavePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public PowerSavePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public PowerSavePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PowerSavePreference(Context context) {
        super(context);
        init(null);
    }

    private void init(AttributeSet attrs) {
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
                if (checked) {
                    showDialog();
                }
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setChecked(true);
                showDialog();
            }
        });
        updateSummary();
    }


    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        updateSummary();
    }

    @Override
    public void setChecked(boolean checked) {
        if (widget != null) {
            widget.setChecked(checked);
        }
        super.setChecked(checked);
    }

    protected void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setView(R.layout.settings_seekbar);
        builder.setTitle(getTitle());
        builder.setPositiveButton(R.string.pref_done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int button) {
                SeekBar seekBar = currentDialog.findViewById(R.id.seekbar);
                setPercentage(seekBar.getProgress());
            }
        });
        currentDialog = builder.create();
        currentDialog.setTitle(getContext().getString(R.string.pref_sleep_if_below, getPercentage()));
        currentDialog.show();

        SeekBar seekBar = currentDialog.findViewById(R.id.seekbar);
        if (seekBar != null) {
            seekBar.setProgress(getPercentage());
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int newValue, boolean setByUser) {
                    currentDialog.setTitle(getContext().getString(R.string.pref_sleep_if_below, newValue));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }

    private int getPercentage() {
        int defaultValue = 20;
        if (getSharedPreferences() != null) {
            String keyPercentage = getContext().getString(R.string.pref_key_save_power_percentage);
            return getSharedPreferences().getInt(keyPercentage, defaultValue);
        }
        return defaultValue;
    }

    private void setPercentage(int percentage) {
        if (getSharedPreferences() != null) {
            String keyPercentage = getContext().getString(R.string.pref_key_save_power_percentage);
            getEditor().putInt(keyPercentage, percentage).apply();
            updateSummary();
        }
    }

    private void updateSummary() {
        setTitle(getContext().getString(R.string.pref_sleep_if_below, getPercentage()));
    }
}
