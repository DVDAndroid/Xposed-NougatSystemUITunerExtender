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

package com.dvd.android.xposed.nougatsystemuitunerextender;

import com.dvd.android.xposed.nougatsystemuitunerextender.hook.DarkThemeHook;
import com.dvd.android.xposed.nougatsystemuitunerextender.hook.QuickReplyLockScreenHook;
import com.dvd.android.xposed.nougatsystemuitunerextender.hook.TunerHook;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage, IXposedHookInitPackageResources {

    public static final String SETTINGS_PKG_NAME = "com.android.settings.notification";
    public static final String SYSUI_PKG_NAME = "com.android.systemui";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        switch (lpparam.packageName) {
            case SYSUI_PKG_NAME:
                TunerHook.hookPackage(lpparam);
                DarkThemeHook.hookPackage(lpparam);
                return;
            case SETTINGS_PKG_NAME:
                QuickReplyLockScreenHook.hookPackage(lpparam);
                break;
        }
    }


    @Override
    public void handleInitPackageResources(final XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
        switch (resParam.packageName) {
            case SYSUI_PKG_NAME:
                TunerHook.hookRes(resParam);
                DarkThemeHook.hookRes(resParam);
                break;
            case SETTINGS_PKG_NAME:
                QuickReplyLockScreenHook.hookRes(resParam);
                break;
        }

    }

}