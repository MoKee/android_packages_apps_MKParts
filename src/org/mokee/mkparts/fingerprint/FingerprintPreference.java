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
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;

import org.mokee.mkparts.R;

class FingerprintPreference extends Preference {

    private static final int RESET_HIGHLIGHT_DELAY_MS = 500;

    private final Drawable mHighlightDrawable;

    private Fingerprint mFingerprint;
    private View mView;

    public FingerprintPreference(Context context) {
        super(context);
        mHighlightDrawable = context.getDrawable(R.drawable.preference_highlight);
        setPersistent(false);
    }

    public void setFingerprint(Fingerprint item) {
        mFingerprint = item;
    }

    public Fingerprint getFingerprint() {
        return mFingerprint;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mView = view.itemView;
    }

    public void highlight() {
        final int centerX = mView.getWidth() / 2;
        final int centerY = mView.getHeight() / 2;
        mHighlightDrawable.setHotspot(centerX, centerY);

        mView.setBackground(mHighlightDrawable);
        mView.setPressed(true);
        mView.setPressed(false);

        mView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mView.setBackground(null);
            }
        }, RESET_HIGHLIGHT_DELAY_MS);
    }

}
