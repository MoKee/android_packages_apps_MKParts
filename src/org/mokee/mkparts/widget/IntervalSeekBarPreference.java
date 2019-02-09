/*
 ** Copyright 2013, The ChameleonOS Open Source Project
 ** Copyright 2016, The OmniROM Project
 ** Copyright 2019, The MoKee Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
package org.mokee.mkparts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.settingslib.RestrictedPreference;

import org.mokee.mkparts.R;

public class IntervalSeekBarPreference extends RestrictedPreference implements OnSeekBarChangeListener {

    private final String TAG = getClass().getName();

    private static final int DEFAULT_VALUE = 50;

    private int mMaxValue = 100;
    private int mMinValue = 0;
    private int mInterval = 1;
    private int mCurrentValue;
    private String mUnits = "";

    private SeekBar mSeekBar;
    private TextView mStatusText;

    public IntervalSeekBarPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPreference(context, attrs, defStyleAttr, defStyleRes);
    }

    public IntervalSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IntervalSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                R.attr.preferenceScreenStyle,
                com.android.internal.R.attr.seekBarPreferenceStyle));
    }

    public IntervalSeekBarPreference(Context context) {
        this(context, null);
    }

    private void initPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setValuesFromXml(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_interval_seek_bar);
    }

    private void setValuesFromXml(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.ProgressBar, defStyleAttr, defStyleRes);
        mMaxValue = a.getInt(com.android.internal.R.styleable.ProgressBar_max, mMaxValue);
        mMinValue = a.getInt(com.android.internal.R.styleable.ProgressBar_min, mMinValue);

        final TypedArray attributes = context.obtainStyledAttributes(attrs,
                R.styleable.IntervalSeekBarPreference);

        TypedValue unitsAttr =
                attributes.peekValue(R.styleable.IntervalSeekBarPreference_units);
        CharSequence data = null;
        if (unitsAttr != null && unitsAttr.type == TypedValue.TYPE_STRING) {
            if (unitsAttr.resourceId != 0) {
                data = context.getText(unitsAttr.resourceId);
            } else {
                data = unitsAttr.string;
            }
        }
        mUnits = (data == null) ? "" : data.toString();

        TypedValue intervalAttr =
                attributes.peekValue(R.styleable.IntervalSeekBarPreference_interval);
        if (intervalAttr != null && intervalAttr.type == TypedValue.TYPE_INT_DEC) {
            mInterval = intervalAttr.data;
        }

        attributes.recycle();
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);
        this.setShouldDisableView(true);
        if (mSeekBar != null) {
            mSeekBar.setEnabled(!disableDependent);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mSeekBar = (SeekBar) holder.findViewById(R.id.seekbar);
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mCurrentValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setEnabled(isEnabled());

        mStatusText = (TextView) holder.findViewById(R.id.seekBarPrefValue);
        mStatusText.setText(String.valueOf(mCurrentValue));
        mStatusText.setMinimumWidth(30);

        TextView units = (TextView) holder.findViewById(R.id.seekBarPrefUnits);
        units.setText(mUnits);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = progress + mMinValue;

        if (newValue > mMaxValue) {
            newValue = mMaxValue;
        } else if (newValue < mMinValue) {
            newValue = mMinValue;
        } else if (mInterval != 1 && newValue % mInterval != 0) {
            newValue = Math.round(((float) newValue) / mInterval) * mInterval;
        }

        // change rejected, revert to the previous value
        if (!callChangeListener(newValue)) {
            seekBar.setProgress(mCurrentValue - mMinValue);
            return;
        }

        // change accepted, store it
        mCurrentValue = newValue;
        mStatusText.setText(String.valueOf(newValue));
        persistInt(newValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray ta, int index) {
        int defaultValue = ta.getInt(index, DEFAULT_VALUE);
        return defaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue);
        } else {
            int temp = 0;
            try {
                temp = (Integer) defaultValue;
            } catch (Exception ex) {
                Log.e(TAG, "Invalid default value: " + defaultValue.toString());
            }
            persistInt(temp);
            mCurrentValue = temp;
        }
    }

    public void setValue(int value) {
        mCurrentValue = value;
    }

    public void setMaxValue(int value) {
        mMaxValue = value;
        if (mSeekBar != null) {
            mSeekBar.setMax(mMaxValue - mMinValue);
        }
    }

    public void setMinValue(int value) {
        mMinValue = value;
        if (mSeekBar != null) {
            mSeekBar.setMax(mMaxValue - mMinValue);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mSeekBar != null) {
            mSeekBar.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

}