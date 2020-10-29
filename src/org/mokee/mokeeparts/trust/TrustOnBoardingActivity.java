/*
 * Copyright (C) 2018 The LineageOS Project
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
package org.mokee.mokeeparts.trust;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import mokee.providers.MoKeeSettings;
import mokee.trust.TrustInterface;

import org.mokee.mokeeparts.R;

public class TrustOnBoardingActivity extends AppCompatActivity {
    private ImageView mImage;

    private TrustInterface mInterface;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_trust_onboarding);

        Button learnMore = findViewById(R.id.trust_onboarding_learn_more);
        Button dismiss = findViewById(R.id.trust_onboarding_done);
        mImage = findViewById(R.id.trust_onboarding_image);

        learnMore.setOnClickListener(v -> openTrustSettings());
        dismiss.setOnClickListener(v -> onDismissClick());

        mInterface = TrustInterface.getInstance(this);

        new Handler().postDelayed(this::showAnimation, 800);
    }

    private void showAnimation() {
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) mImage.getDrawable();
        if (drawable != null) {
            drawable.start();
        }
    }

    private void openTrustSettings() {
        setOnboardingCompleted();
        startActivity(new Intent("org.mokee.mokeeparts.TRUST_INTERFACE"));
        finish();
    }

    private void onDismissClick() {
        setOnboardingCompleted();
        finish();
    }

    private void setOnboardingCompleted() {
        MoKeeSettings.System.putInt(getContentResolver(),
                MoKeeSettings.System.TRUST_INTERFACE_HINTED, 1);
        // Run security check test now that the user is aware of what Trust is
        mInterface.runTest();
    }
}