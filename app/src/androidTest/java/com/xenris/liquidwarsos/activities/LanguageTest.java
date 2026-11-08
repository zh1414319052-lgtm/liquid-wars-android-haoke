package com.xenris.liquidwarsos.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.xenris.liquidwarsos.R;
import com.xenris.liquidwarsos.Util;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class LanguageTest {
    @Test
    public void usesOnlyTheFirstSystemLanguage() {
        for (String locales : new String[]{"zh", "zh-CN", "zh-SG", "zh-Hant-TW,en-US", "zh-HK,en"}) {
            assertEquals(Locale.SIMPLIFIED_CHINESE, LocalizedActivity.uiLocale(LocaleList.forLanguageTags(locales)));
        }
        for (String locales : new String[]{"en", "en-US,zh-CN", "en-GB", "ja-JP,zh-TW", "de-DE", ""}) {
            assertEquals(Locale.ENGLISH, LocalizedActivity.uiLocale(LocaleList.forLanguageTags(locales)));
        }
    }

    @Test
    public void resourcesFollowInjectedLanguageAfterRecreation() {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context chinese = contextFor(base, "zh-Hant-TW,en-US");
        assertEquals("液态战争重制版", chinese.getString(R.string.app_name));
        assertEquals("白色椭圆", Util.getMapName(chinese, 2));
        assertEquals("时间到，蓝队获胜！", chinese.getString(R.string.timeout_other_win, Util.teamToNameString(chinese, 1)));
        assertTrue(chinese.getString(R.string.instructions_controls).contains("五个触点"));

        Context english = contextFor(base, "ja-JP,zh-CN");
        assertEquals("Liquid Wars OS", english.getString(R.string.app_name));
        assertEquals("White Ovals", Util.getMapName(english, 2));
        assertEquals("Time is up. Blue wins!", english.getString(R.string.timeout_other_win, Util.teamToNameString(english, 1)));

        try (ActivityScenario<MainMenuActivity> scenario = ActivityScenario.launch(MainMenuActivity.class)) {
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals(
                    LocalizedActivity.uiLocale(android.content.res.Resources.getSystem().getConfiguration().getLocales()).getLanguage(),
                    activity.getResources().getConfiguration().getLocales().get(0).getLanguage()));
        }
    }

    @Test
    public void launcherLabelHandlesChineseVariantsAndEnglishFirst() {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String locales : new String[]{"zh-CN", "zh-Hant-HK", "zh-TW,en-US"}) {
            assertEquals(locales, "液态战争重制版", labelFor(base, locales));
        }
        assertEquals("Liquid Wars OS", labelFor(base, "en-US,zh-CN"));
    }

    private static String labelFor(Context base, String locales) {
        Configuration configuration = new Configuration();
        configuration.setLocales(LocaleList.forLanguageTags(locales));
        return base.createConfigurationContext(configuration).getString(R.string.app_name);
    }

    private static Context contextFor(Context base, String locales) {
        Configuration configuration = new Configuration();
        configuration.setLocale(LocalizedActivity.uiLocale(LocaleList.forLanguageTags(locales)));
        return base.createConfigurationContext(configuration);
    }
}
