/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2012 The MoKee Open Source Project
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

package org.mokee.mkparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import org.mokee.mkparts.contributors.ContributorsCloudFragment;
import org.mokee.mkparts.fingerprint.FingerprintShortcutUtils;
/*
import org.mokee.mkparts.gestures.TouchscreenGestureSettings;
import org.mokee.mkparts.input.ButtonSettings;
*/

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    /*
    private static final String ONE_TIME_TUNABLE_RESTORE = "hardware_tunable_restored";
    */

    @Override
    public void onReceive(Context ctx, Intent intent) {
        /*
        if (!hasRestoredTunable(ctx)) {
            /* Restore the hardware tunable values */
            /*
            ButtonSettings.restoreKeyDisabler(ctx);
            setRestoredTunable(ctx);
        }

        TouchscreenGestureSettings.restoreTouchscreenGestureStates(ctx);
        */

        // Extract the contributors database
        ContributorsCloudFragment.extractContributorsCloudDatabase(ctx);

        FingerprintShortcutUtils.restoreState(ctx);
    }

    /*
    private boolean hasRestoredTunable(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(ONE_TIME_TUNABLE_RESTORE, false);
    }

    private void setRestoredTunable(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(ONE_TIME_TUNABLE_RESTORE, true).apply();
    }
    */
}
