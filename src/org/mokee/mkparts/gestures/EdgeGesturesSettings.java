/*
 * Copyright (C) 2018 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mokee.mkparts.gestures;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import org.mokee.mkparts.R;
import org.mokee.mkparts.SettingsPreferenceFragment;
import org.mokee.mkparts.widget.SecureSettingSeekBarPreference;

public class EdgeGesturesSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String EDGE_GESTURES_ENABLED = "edge_gestures_enabled";
    public static final String EDGE_GESTURES_SCREEN_PERCENT = "edge_gestures_back_screen_percent";

    private String previousTitle;

    private SwitchPreference enabledPreference;
    private SecureSettingSeekBarPreference screenPercentPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.edge_gestures);

        enabledPreference = (SwitchPreference) findPreference(EDGE_GESTURES_ENABLED);
        /*enabledPreference.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.Secure.EDGE_GESTURES_ENABLED, 0) == 1));*/
        enabledPreference.setOnPreferenceChangeListener(this);

        screenPercentPreference = (SecureSettingSeekBarPreference) findPreference(EDGE_GESTURES_SCREEN_PERCENT);
        int percent = Settings.Secure.getIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_BACK_SCREEN_PERCENT, 60, UserHandle.USER_CURRENT);
        screenPercentPreference.setValue(percent);
        screenPercentPreference.setIntervalValue(5);
    }

    @Override
    public void onStart() {
        super.onStart();

        ActionBar actionBar = getActivity().getActionBar();
        previousTitle = actionBar.getTitle().toString();
        actionBar.setTitle(R.string.edge_gestures_title);
    }

    @Override
    public void onStop() {
        super.onStop();

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setTitle(previousTitle);
    }

    /*@Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.FH_SETTINGS;
    }*/

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == enabledPreference) {
            int enabled = ((boolean) newValue) ? 1 : 0;
            //Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_ENABLED, enabled, UserHandle.USER_CURRENT);

            if (enabled == 1) {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.NAVIGATION_BAR_VISIBLE,
                        0);
            } else {
                if (hasNavbarByDefault(getPrefContext())) {
                    Settings.Secure.putInt(getContentResolver(),
                            Settings.Secure.NAVIGATION_BAR_VISIBLE,
                            1);
                }
            }
            return true;
        } /*else if (preference == hapticFeedbackDurationPreference) {
            int hapticFeedbackValue = Integer.valueOf((String) newValue);
            Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_FEEDBACK_DURATION, hapticFeedbackValue, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == longPressDurationPreference) {
            int longPressValue = Integer.valueOf((String) newValue);
            Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.EDGE_GESTURES_LONG_PRESS_DURATION, longPressValue, UserHandle.USER_CURRENT);
            return true;
        }*/

        return false;
    }
    
    public static boolean hasNavbarByDefault(Context context) {
        boolean needsNav = (Boolean)getValue(context, "config_showNavigationBar", "bool", "android");
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            needsNav = false;
        } else if ("0".equals(navBarOverride)) {
            needsNav = true;
        }
        return needsNav;
    }

    public static Resources getResourcesForPackage(Context ctx, String pkg) {
        try {
            Resources res = ctx.getPackageManager()
                    .getResourcesForApplication(pkg);
            return res;
        } catch (Exception e) {
            return ctx.getResources();
        }
    }
    
    public static Object getValue(Context context, String resName, String resType, String pkg) {
        Resources res = getResourcesForPackage(context, pkg);
        int id = res.getIdentifier(resName, resType, pkg);
        return Boolean.valueOf(res.getBoolean(id));
    }
    
}
