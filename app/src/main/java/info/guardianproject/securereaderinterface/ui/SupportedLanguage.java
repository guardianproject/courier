package info.guardianproject.securereaderinterface.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import info.guardianproject.securereader.Settings;
import info.guardianproject.securereaderinterface.App;
import info.guardianproject.securereaderinterface.BuildConfig;
import info.guardianproject.securereaderinterface.R;

public class SupportedLanguage {
    public static SupportedLanguage[] allLanguages = new SupportedLanguage[] {
            new SupportedLanguage("en", R.string.settings_language_english_nt),
            new SupportedLanguage("es", R.string.settings_language_spanish_nt),
            new SupportedLanguage("es_US", R.string.settings_language_spanish_us_nt)
    };
    private static Map<String, SupportedLanguage> languageMapAll = new HashMap<>();
    private static Map<String, SupportedLanguage> languageMapSupported = new HashMap<>();

    static {
        for (SupportedLanguage language : allLanguages) {
            languageMapAll.put(language.getCode(), language);
        }

        if (BuildConfig.UI_LANGUAGES != null) {
            for (String code : BuildConfig.UI_LANGUAGES) {
                if ("*".equals(code)) {
                    languageMapSupported.clear();
                    languageMapSupported.putAll(languageMapAll);
                    break;
                } else {
                    SupportedLanguage language = languageMapAll.get(code);
                    if (language != null) {
                        languageMapSupported.put(language.getCode(), language);
                    }
                }
            }
        }
    }

    public static SupportedLanguage[] supportedLanguages() {
        return languageMapSupported.values().toArray(new SupportedLanguage[0]);
    }

    public static boolean isSupportedLanguageCode(String code) {
        return languageMapSupported.get(code) != null;
    }

    public static String getDefaultSupportedLanguage() {
        if (languageMapSupported.size() > 0) {
            return (languageMapSupported.values().toArray(new SupportedLanguage[0]))[0].getCode();
        }
        return null;
    }

    private final String mCode;
    private final int mIdDisplayName;

    public SupportedLanguage(String code, int idDisplayName)
    {
        mCode = code;
        mIdDisplayName = idDisplayName;
    }

    public String getCode() { return mCode; }

    public int getIdDisplayName()
    {
        return mIdDisplayName;
    }

    @Override
    public String toString()
    {
        return App.getInstance().getString(getIdDisplayName());
    }
}
