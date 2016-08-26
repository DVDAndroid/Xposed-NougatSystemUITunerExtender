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

import android.content.res.XResources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.dvd.android.xposed.nougatsystemuitunerextender.XposedMod.SYSUI_PKG_NAME;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * This reverts commit <a href="https://github.com/android/platform_frameworks_base/commit/72195a011128d1c516149763c4c156539a470ed5">72195a011128d1c516149763c4c156539a470ed5</a>
 * and <a href="https://github.com/android/platform_frameworks_base/commit/52c66d74d13f26e2aea578f6f4308250cb958f67">52c66d74d13f26e2aea578f6f4308250cb958f67</a>
 */

public class TunerHook {

    private static final String TUNER_FRAGMENT_CLASS = "com.android.systemui.tuner.TunerFragment";
    private static final String NAV_BAR_TUNER_FRAGMENT = "com.android.systemui.tuner.NavBarTuner";
    private static final String COLOR_APPEARANCE_TUNER_FRAGMENT = "com.android.systemui.tuner.ColorAndAppearanceFragment";

    private static String navBarString;
    private static String colorAppearanceString;

    public static void hookPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> tunerFragmentClass = findClass(TUNER_FRAGMENT_CLASS, lpparam.classLoader);

        findAndHookMethod(tunerFragmentClass, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                PreferenceFragment fragment = (PreferenceFragment) param.thisObject;
                PreferenceScreen screen = fragment.getPreferenceScreen();

                ///

                Preference navBar = new Preference(fragment.getContext());
                navBar.setKey("nav_bar");
                navBar.setTitle(navBarString);
                navBar.setFragment(NAV_BAR_TUNER_FRAGMENT);

                screen.addPreference(navBar);

                ///

                Preference colorTransform = new Preference(fragment.getContext());
                navBar.setKey("color_transform");
                navBar.setTitle(colorAppearanceString);
                navBar.setFragment(COLOR_APPEARANCE_TUNER_FRAGMENT);

                screen.addPreference(colorTransform);
            }
        });
    }

    public static void hookRes(XC_InitPackageResources.InitPackageResourcesParam resParam) {
        XResources r = resParam.res;

        navBarString = r.getString(r.getIdentifier("nav_bar", "string", SYSUI_PKG_NAME));
        colorAppearanceString = r.getString(r.getIdentifier("color_and_appearance", "string", SYSUI_PKG_NAME));
    }
}
