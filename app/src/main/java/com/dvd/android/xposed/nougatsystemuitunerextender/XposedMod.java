package com.dvd.android.xposed.nougatsystemuitunerextender;

import android.app.UiModeManager;
import android.content.res.XResources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private static final String SYSUI_PKG_NAME = "com.android.systemui";

    private static final String TUNER_FRAGMENT_CLASS = "com.android.systemui.tuner.TunerFragment";
    private static final String NIGHT_MODE_FRAGMENT_CLASS = "com.android.systemui.tuner.NightModeFragment";
    private static final String NAV_BAR_TUNER_FRAGMENT = "com.android.systemui.tuner.NavBarTuner";
    private static final String COLOR_APPEARANCE_TUNER_FRAGMENT = "com.android.systemui.tuner.ColorAndAppearanceFragment";

    private String navBarString;
    private String colorAppearanceString;
    private String useDarkThemeString;
    private String nightModeDisclaimer;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(SYSUI_PKG_NAME)) return;

        Class<?> tunerFragmentClass = findClass(TUNER_FRAGMENT_CLASS, lpparam.classLoader);
        Class<?> nightModeFragmentClass = findClass(NIGHT_MODE_FRAGMENT_CLASS, lpparam.classLoader);


        findAndHookMethod(tunerFragmentClass, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                PreferenceFragment fragment = (PreferenceFragment) param.thisObject;
                PreferenceScreen screen = fragment.getPreferenceScreen();

                /**
                 * This reverts commit <a href="https://github.com/android/platform_frameworks_base/commit/72195a011128d1c516149763c4c156539a470ed5">72195a011128d1c516149763c4c156539a470ed5</a>
                 */

                Preference navBar = new Preference(fragment.getContext());
                navBar.setKey("nav_bar");
                navBar.setTitle(navBarString);
                navBar.setFragment(NAV_BAR_TUNER_FRAGMENT);

                screen.addPreference(navBar);

                /**
                 * This reverts commit <a href="https://github.com/android/platform_frameworks_base/commit/52c66d74d13f26e2aea578f6f4308250cb958f67">52c66d74d13f26e2aea578f6f4308250cb958f67</a>
                 */

                Preference colorTransform = new Preference(fragment.getContext());
                navBar.setKey("color_transform");
                navBar.setTitle(colorAppearanceString);
                navBar.setFragment(COLOR_APPEARANCE_TUNER_FRAGMENT);

                screen.addPreference(colorTransform);
            }
        });

        /**
         * This and hook below revert <a href="https://github.com/android/platform_frameworks_base/commit/b4a253bc5af3172dee3d4ac8cf1c4a566f695c41>b4a253bc5af3172dee3d4ac8cf1c4a566f695c41</a>
         */

        findAndHookMethod(nightModeFragmentClass, "calculateDisabled", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                SwitchPreference mAdjustTint = (SwitchPreference) getObjectField(param.thisObject, "mAdjustTint");
                SwitchPreference mAdjustBrightness = (SwitchPreference) getObjectField(param.thisObject, "mAdjustBrightness");
                SwitchPreference mDarkTheme = (SwitchPreference) ((PreferenceFragment) param.thisObject).findPreference("dark_theme");

                int enabledCount = (mDarkTheme.isChecked() ? 1 : 0)
                        + (mAdjustTint.isChecked() ? 1 : 0)
                        + (mAdjustBrightness.isChecked() ? 1 : 0);

                if (enabledCount == 1) {
                    if (mDarkTheme.isChecked()) {
                        mDarkTheme.setEnabled(false);
                    } else if (mAdjustTint.isChecked()) {
                        mAdjustTint.setEnabled(false);
                    } else {
                        mAdjustBrightness.setEnabled(false);
                    }
                } else {
                    mAdjustTint.setEnabled(true);
                    mAdjustBrightness.setEnabled(true);
                }

                setObjectField(param.thisObject, "mAdjustTint", mAdjustTint);
                setObjectField(param.thisObject, "mAdjustBrightness", mAdjustBrightness);
                setObjectField(param.thisObject, "mDarkTheme", mDarkTheme);

                return null;
            }
        });

        findAndHookMethod(nightModeFragmentClass, "onCreatePreferences", Bundle.class, String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                final PreferenceFragment fragment = (PreferenceFragment) param.thisObject;
                PreferenceScreen screen = fragment.getPreferenceScreen();

                SwitchPreference darkThemeSwitch = new SwitchPreference(screen.getContext());
                darkThemeSwitch.setKey("dark_theme");
                darkThemeSwitch.setTitle(useDarkThemeString);

                final UiModeManager mUiModeManager = (UiModeManager) getObjectField(param.thisObject, "mUiModeManager");

                darkThemeSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Boolean value = (Boolean) newValue;
                        Class<?> metricsLoggerClass = findClass("com.android.internal.logging.MetricsLogger", lpparam.classLoader);

                        callMethod(metricsLoggerClass, "action", fragment.getContext(), 311, value);

                        callMethod(mUiModeManager, "setNightMode", value ? UiModeManager.MODE_NIGHT_AUTO : UiModeManager.MODE_NIGHT_NO);

                        callMethod(param.thisObject, "postCalculateDisabled");

                        callMethod(param.thisObject, "calculateDisabled");

                        return true;
                    }
                });

                darkThemeSwitch.setChecked(mUiModeManager.getNightMode() == UiModeManager.MODE_NIGHT_AUTO);

                screen.addPreference(darkThemeSwitch);

                Preference disclaimer = new Preference(fragment.getContext());
                disclaimer.setSelectable(false);
                disclaimer.setSummary(nightModeDisclaimer);

                screen.addPreference(disclaimer);
            }
        });
    }


    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
        XResources r = resParam.res;

        navBarString = r.getString(r.getIdentifier("nav_bar", "string", SYSUI_PKG_NAME));
        colorAppearanceString = r.getString(r.getIdentifier("color_and_appearance", "string", SYSUI_PKG_NAME));
        useDarkThemeString = r.getString(r.getIdentifier("use_dark_theme", "string", SYSUI_PKG_NAME));
        nightModeDisclaimer = r.getString(r.getIdentifier("night_mode_disclaimer", "string", SYSUI_PKG_NAME));
    }

}