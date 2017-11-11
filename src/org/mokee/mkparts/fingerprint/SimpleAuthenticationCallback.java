/**
 * Copyright (C) 2017 The MoKee Open Source Project
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

import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;

class SimpleAuthenticationCallback extends AuthenticationCallback {

    @Override
    public void onAuthenticationSucceeded(AuthenticationResult result) {
    }

    @Override
    public void onAuthenticationFailed() {
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
    }

}
