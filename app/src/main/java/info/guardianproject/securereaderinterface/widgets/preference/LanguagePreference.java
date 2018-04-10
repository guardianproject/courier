package info.guardianproject.securereaderinterface.widgets.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;

import info.guardianproject.securereaderinterface.ui.SupportedLanguage;

/**
 * Created by N-Pex on 2018-03-06.
 */

public class LanguagePreference extends ListPreference {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LanguagePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LanguagePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public LanguagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LanguagePreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        SupportedLanguage[] languages = SupportedLanguage.supportedLanguages();
        CharSequence[] entries = new CharSequence[languages.length];
        CharSequence[] values = new CharSequence[languages.length];
        for (int i = 0; i < languages.length; i++) {
            entries[i] = languages[i].toString();
            values[i] = languages[i].getCode();
        }
        setEntries(entries);
        setEntryValues(values);
    }
}
