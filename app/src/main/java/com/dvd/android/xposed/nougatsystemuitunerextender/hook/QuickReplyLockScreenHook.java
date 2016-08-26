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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.dvd.android.xposed.nougatsystemuitunerextender.XposedMod;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

/**
 * This reverts commit <a href="https://github.com/android/platform_packages_apps_settings/commit/38e4e5dca62fabaf20ea3c72a23a8e784d4aa186">38e4e5dca62fabaf20ea3c72a23a8e784d4aa186</a>
 * <p/>
 * Non-reverted commit <a href="https://github.com/android/platform_packages_apps_settings/commit/059b0fa060b8805c6c6199ccd9a4e13e64ad6995">059b0fa060b8805c6c6199ccd9a4e13e64ad6995</a>
 */
public class QuickReplyLockScreenHook {

    private static final String REDACTION_INTERSTITIAL_FRAGMENT_CLASS = "com.android.settings.notification.RedactionInterstitial.RedactionInterstitialFragment";
    private static final String SETUP_REDACTION_INTERSTITIAL_FRAGMENT_CLASS = "com.android.settings.notification.SetupRedactionInterstitial.RedactionInterstitialFragment";
    private static final String RESTRICTED_LOCK_UTILS_CLASS = "com.android.settingslib.RestrictedLockUtils";
    private static final String CONFIGURE_NOTIFICATION_SETTINGS_CLASS = "com.android.settings.notification.ConfigureNotificationSettings";
    private static final String NOTIFICATION_LOCKSCREEN_PREFERENCE_CLASS = "com.android.settings.notification.NotificationLockscreenPreference";
    private static final String RESTRICTED_CHECK_BOX_CLASS = "com.android.settings.RestrictedCheckBox";
    private static final String UTILS_CLASS = "com.android.settings.Utils";
    private static final String RESTRICTED_ITEM_CLASS = "com.android.settings.RestrictedListPreference.RestrictedItem";

    private static int redactionInterstitialLayout;

    public static void hookRes(XC_InitPackageResources.InitPackageResourcesParam resParam) {
        redactionInterstitialLayout = resParam.res.getIdentifier("redaction_interstitial", "layout", XposedMod.SETTINGS_PKG_NAME);
    }

    public static void hookPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        final Class<?> RedactionInterstitial = findClass(REDACTION_INTERSTITIAL_FRAGMENT_CLASS, lpparam.classLoader);
        final Class<?> SetupRedactionInterstitial = findClass(SETUP_REDACTION_INTERSTITIAL_FRAGMENT_CLASS, lpparam.classLoader);
        final Class<?> RestrictedLockUtils = findClass(RESTRICTED_LOCK_UTILS_CLASS, lpparam.classLoader);
        final Class<?> ConfigureNotificationSettings = findClass(CONFIGURE_NOTIFICATION_SETTINGS_CLASS, lpparam.classLoader);
        final Class<?> NotificationLockscreenPreference = findClass(NOTIFICATION_LOCKSCREEN_PREFERENCE_CLASS, lpparam.classLoader);
        final Class<?> RestrictedCheckBox = findClass(RESTRICTED_CHECK_BOX_CLASS, lpparam.classLoader);
        final Class<?> Utils = findClass(UTILS_CLASS, lpparam.classLoader);
        final Class<?> RestrictedItem = findClass(RESTRICTED_ITEM_CLASS, lpparam.classLoader);

        findAndHookMethod(SetupRedactionInterstitial, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                LayoutInflater inflater = (LayoutInflater) param.args[0];
                ViewGroup container = (ViewGroup) param.args[1];
                LinearLayout v = (LinearLayout) inflater.inflate(redactionInterstitialLayout, container, false);

                Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                Object mRemoteInputCheckbox = XposedHelpers.newInstance(RestrictedCheckBox, context);

                v.addView((View) mRemoteInputCheckbox);
                return null;
            }
        });

        findAndHookMethod(RedactionInterstitial, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                LayoutInflater inflater = (LayoutInflater) param.args[0];
                ViewGroup container = (ViewGroup) param.args[1];
                LinearLayout v = (LinearLayout) inflater.inflate(redactionInterstitialLayout, container, false);

                Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                Object mRemoteInputCheckbox = XposedHelpers.newInstance(RestrictedCheckBox, context);

                setAdditionalInstanceField(param.thisObject, "mRemoteInputCheckbox", mRemoteInputCheckbox);

                v.addView((View) mRemoteInputCheckbox);
                return null;
            }
        });

        findAndHookMethod(RedactionInterstitial, "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Object mRemoteInputCheckbox = getObjectField(param.thisObject, "mRemoteInputCheckbox");

                callMethod(mRemoteInputCheckbox, "setOnCheckedChangeListener", new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                        ContentResolver contentResolver = (ContentResolver) callMethod(param.thisObject, "getContentResolver");
                        Object mUserId = getObjectField(param.thisObject, "mUserId");

                        callMethod(Settings.Secure.class, "putIntForUser", contentResolver, b ? 0 : 1, mUserId);
                    }
                });

            }
        });

        findAndHookMethod(RedactionInterstitial, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                Object mRemoteInputCheckbox = getObjectField(param.thisObject, "mRemoteInputCheckbox");

                Activity activity = (Activity) callMethod(param.thisObject, "getActivity");
                Object result = callMethod(RestrictedLockUtils, "checkIfKeyguardFeaturesDisabled", activity, 64, getObjectField(param.thisObject, "mUserId"));

                callMethod(mRemoteInputCheckbox, "setDisabledByAdmin", result);
            }
        });

        findAndHookMethod(RedactionInterstitial, "loadFromSettings", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                ContentResolver contentResolver = (ContentResolver) callMethod(param.thisObject, "getContentResolver");

                Object mUserId = getObjectField(param.thisObject, "mUserId");
                Object mRemoteInputCheckbox = getObjectField(param.thisObject, "mRemoteInputCheckbox");
                Object mRadioGroup = getObjectField(param.thisObject, "mRadioGroup");

                boolean allowRemoteInput = ((int) callMethod(Settings.Secure.class, "getIntForUser", contentResolver, "lock_screen_allow_remote_input", 0, mUserId)) != 0;

                callMethod(mRemoteInputCheckbox, "setChecked", !allowRemoteInput);

                // updateRemoteInputCheckboxVisibility()
                int id = (int) callMethod(getObjectField(param.thisObject, "mShowAllButton"), "getId");

                boolean visible = ((int) callMethod(mRadioGroup, "getCheckedRadioButtonId")) == id;
                boolean isManagedProfile = (boolean) callMethod(Utils, "isManagedProfile", callMethod(UserManager.class, "get", callMethod(param.thisObject, "getPrefContext")), mUserId);

                callMethod(mRemoteInputCheckbox, "setVisibility", (visible && !isManagedProfile) ? View.VISIBLE : View.INVISIBLE);
            }
        });

        findAndHookMethod(RedactionInterstitial, "onCheckedChanged", RadioGroup.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                // updateRemoteInputCheckboxVisibility()

                Object mRemoteInputCheckbox = getObjectField(param.thisObject, "mRemoteInputCheckbox");
                Object mUserId = getObjectField(param.thisObject, "mUserId");
                Object mRadioGroup = getObjectField(param.thisObject, "mRadioGroup");

                Class<?> Utils = findClass("com.android.settings.Utils", lpparam.classLoader);
                int id = (int) callMethod(getObjectField(param.thisObject, "mShowAllButton"), "getId");

                boolean visible = ((int) callMethod(mRadioGroup, "getCheckedRadioButtonId")) == id;
                boolean isManagedProfile = (boolean) callMethod(Utils, "isManagedProfile", callMethod(UserManager.class, "get", callMethod(param.thisObject, "getPrefContext")), mUserId);

                callMethod(mRemoteInputCheckbox, "setVisibility", (visible && !isManagedProfile) ? View.VISIBLE : View.INVISIBLE);
            }
        });

        //////////////////////////////////////////////

        findAndHookMethod(ConfigureNotificationSettings, "initLockscreenNotifications", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                PreferenceScreen screen = (PreferenceScreen) callMethod(param.thisObject, "getPreferenceScreen");
                callMethod(screen, "removePreference", "lock_screen_notifications");

                Object mLockscreen = newInstance(NotificationLockscreenPreference, screen.getContext(), null);

                callMethod(mLockscreen, "setKey", "lock_screen_notifications");

                Context mContext = (Context) getObjectField(param.thisObject, "mContext");
                Object myUserId = callMethod(UserHandle.class, "myUserId");

                Object utils = callMethod(RestrictedLockUtils, "checkIfKeyguardFeaturesDisabled", mContext, DevicePolicyManager.KEYGUARD_DISABLE_REMOTE_INPUT, myUserId);
                callMethod(mLockscreen, "setRemoteInputRestricted", utils);

                setAdditionalInstanceField(param.thisObject, "mLockscreen1", mLockscreen);
                screen.addPreference((Preference) mLockscreen);
            }
        });

        findAndHookMethod(ConfigureNotificationSettings, "initLockscreenNotificationsForProfile", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                PreferenceScreen screen = (PreferenceScreen) callMethod(param.thisObject, "getPreferenceScreen");
                callMethod(screen, "removePreference", "lock_screen_notifications_profile");

                Object mLockscreenProfile = newInstance(NotificationLockscreenPreference, screen.getContext(), null);
                Object mProfileChallengeUserId = getObjectField(param.thisObject, "mProfileChallengeUserId");

                callMethod(mLockscreenProfile, "setKey", "lock_screen_notifications_profile");
                callMethod(mLockscreenProfile, "setUserId", mProfileChallengeUserId);

                Context mContext = (Context) getObjectField(param.thisObject, "mContext");

                Object utils = callMethod(RestrictedLockUtils, "checkIfKeyguardFeaturesDisabled", mContext, DevicePolicyManager.KEYGUARD_DISABLE_REMOTE_INPUT, mProfileChallengeUserId);
                callMethod(mLockscreenProfile, "setRemoteInputRestricted", utils);

                callMethod(mLockscreenProfile, "setRemoteInputCheckBoxEnabled", false);
            }
        });

        findAndHookMethod(ConfigureNotificationSettings, "setRestrictedIfNotificationFeaturesDisabled", CharSequence.class, CharSequence.class, int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

                Context mContext = (Context) getObjectField(param.thisObject, "mContext");
                Object keyguardNotificationFeatures = param.args[2];
                Object myUserId = callMethod(UserHandle.class, "myUserId");
                Object mLockscreen = getObjectField(param.thisObject, "mLockscreen1");

                Object admin = callMethod(RestrictedLockUtils, "checkIfKeyguardFeaturesDisabled", mContext, keyguardNotificationFeatures, myUserId);

                if (admin != null) {
                    Object item = newInstance(RestrictedItem, param.args[0], param.args[1], admin);


                    callMethod(mLockscreen, "addRestrictedItem", item);
                }

                int mProfileChallengeUserId = (int) getObjectField(param.thisObject, "mProfileChallengeUserId");

                if (mProfileChallengeUserId != -10000) {
                    Object profileAdmin = callMethod(RestrictedLockUtils, "checkIfKeyguardFeaturesDisabled", mContext, keyguardNotificationFeatures, mProfileChallengeUserId);
                    if (profileAdmin != null) {
                        Object item = newInstance(RestrictedItem, param.args[0], param.args[1], admin);

                        callMethod(mLockscreen, "addRestrictedItem", item);
                    }
                }

                return null;
            }
        });
    }

}
