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
            new SupportedLanguage("es_US", R.string.settings_language_spanish_us_nt),
            new SupportedLanguage("ar", R.string.settings_language_ar_nt),
            new SupportedLanguage("az", R.string.settings_language_az_nt),
            new SupportedLanguage("br", R.string.settings_language_br_nt),
            new SupportedLanguage("de", R.string.settings_language_de_nt),
            new SupportedLanguage("fa", R.string.settings_language_fa_nt),
            new SupportedLanguage("fr", R.string.settings_language_fr_nt),
            new SupportedLanguage("hi", R.string.settings_language_hi_nt),
            new SupportedLanguage("hu", R.string.settings_language_hu_nt),
            new SupportedLanguage("it", R.string.settings_language_it_nt),
            new SupportedLanguage("ja", R.string.settings_language_ja_nt),
            new SupportedLanguage("nb_NO", R.string.settings_language_nb_NO_nt),
            new SupportedLanguage("nl", R.string.settings_language_nl_nt),
            new SupportedLanguage("pt_BR", R.string.settings_language_pt_BR_nt),
            new SupportedLanguage("ru", R.string.settings_language_ru_nt),
            new SupportedLanguage("ta", R.string.settings_language_ta_nt),
            new SupportedLanguage("te", R.string.settings_language_te_nt),
            new SupportedLanguage("tr", R.string.settings_language_tr_nt),
            new SupportedLanguage("uk", R.string.settings_language_uk_nt),
            new SupportedLanguage("zh", R.string.settings_language_zh_nt),
            new SupportedLanguage("zh_TW", R.string.settings_language_zh_TW_nt)
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
