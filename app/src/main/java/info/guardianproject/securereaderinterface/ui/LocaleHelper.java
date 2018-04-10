package info.guardianproject.securereaderinterface.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.os.LocaleListCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;

import java.util.Locale;

public class LocaleHelper extends ContextThemeWrapper {

    public LocaleHelper(Context base) {
        super(base, base.getTheme());
    }

    @SuppressWarnings("deprecation")
    public static ContextWrapper wrap(Context context, String language, String region) {
        if (!language.equals("")) {
            Locale locale = null;
            if (Build.VERSION.SDK_INT >= 21) {
                Locale.Builder builder = new Locale.Builder().setLanguage(language);
                if (!TextUtils.isEmpty(region)) {
                    builder.setRegion(region);
                }
                locale = builder.build();
            } else {
                if (!TextUtils.isEmpty(region)) {
                    locale = new Locale(language, region);
                } else {
                    locale = new Locale(language);
                }
            }
            Locale.setDefault(locale);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Configuration config = new Configuration();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setSystemLocale(config, locale);
                } else {
                    setSystemLocaleLegacy(config, locale);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        config.setLayoutDirection(locale);
                    }
                }
                context = context.createConfigurationContext(config);
            } else {
                Configuration config = context.getResources().getConfiguration();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setSystemLocale(config, locale);
                } else {
                    setSystemLocaleLegacy(config, locale);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        config.setLayoutDirection(locale);
                    }
                }
                context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            }
        }
        return new LocaleHelper(context);
    }

    @SuppressWarnings("deprecation")
    public static Locale getSystemLocaleLegacy(Configuration config){
        return config.locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static Locale getSystemLocale(Configuration config){
        return config.getLocales().get(0);
    }

    @SuppressWarnings("deprecation")
    public static void setSystemLocaleLegacy(Configuration config, Locale locale){
        config.locale = locale;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public static void setSystemLocale(Configuration config, Locale locale){
        config.setLocale(locale);
    }
}