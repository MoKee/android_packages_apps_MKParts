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

import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.SparseArray;

import mokee.fingerprint.FingerprintShortcutManager;
import mokee.fingerprint.FingerprintShortcutManager.Target;

import org.mokee.mkparts.R;
import org.mokee.mkparts.SettingsPreferenceFragment;

import static android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_LOCKOUT;

public class FingerprintShortcutSettings extends SettingsPreferenceFragment {

    private static final long LOCKOUT_DURATION = 30000;

    private FingerprintManager mFingerprintManager;
    private CancellationSignal mFingerprintCancel;
    private boolean mInFingerprintLockout = false;

    private FingerprintShortcutManager mFpShortcutManager;

    private int mUserId = UserHandle.myUserId();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mFingerprintLockoutReset = new Runnable() {
        @Override
        public void run() {
            mInFingerprintLockout = false;
            retryFingerprint();
        }
    };

    private final AuthenticationCallback mAuthCallback = new SimpleAuthenticationCallback() {
        @Override
        public void onAuthenticationSucceeded(AuthenticationResult result) {
            mFingerprintCancel = null;
            final int fingerId = result.getFingerprint().getFingerId();
            highlightFingerprintItem(fingerId);
            retryFingerprint();
        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            mFingerprintCancel = null;
            if (errorCode == FINGERPRINT_ERROR_LOCKOUT) {
                mInFingerprintLockout = true;
                if (!mHandler.hasCallbacks(mFingerprintLockoutReset)) {
                    mHandler.postDelayed(mFingerprintLockoutReset,
                            LOCKOUT_DURATION);
                }
            }
        }
    };

    private static String genKey(int id) {
        return String.format("finger_%d", id);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context ctx = getContext();
        mFingerprintManager = (FingerprintManager) ctx.getSystemService(
                Context.FINGERPRINT_SERVICE);
        mFpShortcutManager = new FingerprintShortcutManager(ctx);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopFingerprint();
    }

    private void updatePreferences() {
        createPreferenceHierarchy();
        retryFingerprint();
    }

    private void retryFingerprint() {
        if (!mInFingerprintLockout) {
            mFingerprintCancel = new CancellationSignal();
            mFingerprintManager.authenticate(null, mFingerprintCancel,
                    0, mAuthCallback, mHandler, mUserId);
        }
    }

    private void stopFingerprint() {
        if (mFingerprintCancel != null && !mFingerprintCancel.isCanceled()) {
            mFingerprintCancel.cancel();
        }
        mFingerprintCancel = null;
    }

    private void createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.fingerprint_shortcut_settings);
        root = getPreferenceScreen();
        addFingerprintItemPreferences(root);
        setPreferenceScreen(root);
    }

    private void addFingerprintItemPreferences(PreferenceGroup root) {
        root.removeAll();

        final String notSpecified = getContext().getString(
                R.string.fingerprint_shortcut_not_specified);

        final SparseArray<Target> shortcuts = mFpShortcutManager.getShortcuts();

        for (Fingerprint item : mFingerprintManager.getEnrolledFingerprints(mUserId)) {
            final FingerprintPreference pref = new FingerprintPreference(root.getContext());

            final int fingerId = item.getFingerId();
            final Target target = shortcuts.get(fingerId);

            pref.setKey(genKey(fingerId));
            pref.setTitle(item.getName());
            pref.setSummary(target != null ? target.getLabel() : notSpecified);
            pref.setFingerprint(item);
            // pref.setIcon(R.drawable.ic_fingerprint_24dp);
            root.addPreference(pref);
        }
    }

    private void highlightFingerprintItem(int fpId) {
        final FingerprintPreference pref
                = (FingerprintPreference) findPreference(genKey(fpId));
        pref.highlight();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        return true;
    }

}
