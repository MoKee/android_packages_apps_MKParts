/*
 * Copyright (C) 2017 The LineageOS Project
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

package org.mokee.mokeeparts.notificationlight;

import android.content.Context;
import android.os.UserHandle;
import android.util.AttributeSet;

import mokee.providers.MoKeeSettings;

public class BatteryBrightnessZenPreference extends BrightnessPreference {
    private static String TAG = "BatteryBrightnessZenPreference";

    private Context mContext;

    public BatteryBrightnessZenPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected int getBrightnessSetting() {
        return MoKeeSettings.System.getIntForUser(mContext.getContentResolver(),
                MoKeeSettings.System.BATTERY_LIGHT_BRIGHTNESS_LEVEL_ZEN,
                LIGHT_BRIGHTNESS_MAXIMUM, UserHandle.USER_CURRENT);
    }

    @Override
    protected void setBrightnessSetting(int brightness) {
        MoKeeSettings.System.putIntForUser(mContext.getContentResolver(),
                MoKeeSettings.System.BATTERY_LIGHT_BRIGHTNESS_LEVEL_ZEN,
                brightness, UserHandle.USER_CURRENT);
    }
}