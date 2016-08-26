/*
 * Copyright 2016 dvdandroid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dvd.android.xposed.nougatsystemuitunerextender.hook;

import android.app.UiModeManager;
import android.content.res.XResources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.dvd.android.xposed.nougatsystemuitunerextender.XposedMod.SYSUI_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

/**
 * This reverts commit <a href="https://github.com/android/platform_frameworks_base/commit/b4a253bc5af3172dee3d4ac8cf1c4a566f695c41>b4a253bc5af3172dee3d4ac8cf1c4a566f695c41</a>
 */
public class DarkThemeHook {

    private static final String NIGHT_MODE_FRAGMENT_CLASS = "com.android.systemui.tuner.NightModeFragment";

    private static String useDarkThemeString;
    private static String nightModeDisclaimer;

    public static void hookPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> nightModeFragmentClass = findClass(NIGHT_MODE_FRAGMENT_CLASS, lpparam.classLoader);

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
                setAdditionalInstanceField(param.thisObject, "mDarkTheme", mDarkTheme);

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

    public static void hookRes(XC_InitPackageResources.InitPackageResourcesParam resParam) {
        XResources r = resParam.res;

        useDarkThemeString = r.getString(r.getIdentifier("use_dark_theme", "string", SYSUI_PKG_NAME));
        nightModeDisclaimer = r.getString(r.getIdentifier("night_mode_disclaimer", "string", SYSUI_PKG_NAME));
    }
}
