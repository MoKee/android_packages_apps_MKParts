/**
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

package org.mokee.mkparts.fingerprint;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

public class FingerprintShortcutUtils {

    public static void restoreState(Context context) {
        final PackageManager pm = context.getPackageManager();
        final FingerprintManager fm = (FingerprintManager) context
                .getSystemService(Context.FINGERPRINT_SERVICE);

        final ComponentName cmp = new ComponentName(context,
                FingerprintShortcutSettings.class);

        final int state = fm.isHardwareDetected()
                ? COMPONENT_ENABLED_STATE_ENABLED
                : COMPONENT_ENABLED_STATE_DISABLED;

        pm.setComponentEnabledSetting(cmp, state, 0);
    }

}
