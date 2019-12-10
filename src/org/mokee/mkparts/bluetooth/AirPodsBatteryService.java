/*
 * Copyright (C) 2019 The MoKee Open Source Project
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

package org.mokee.mkparts.bluetooth;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AirPodsBatteryService extends Service {

    private static final String TAG = "AirPodsBatteryService";

    private static final int DATA_LENGTH_BATTERY = 25;

    private static final long REPORT_DELAY_MS = 500;

    private static final int FLAG_RIGHTLEFT = 1 << 7;

    private static final int MASK_CHARGING_RIGHT = 1 << 4;
    private static final int MASK_CHARGING_LEFT = 1 << 5;
    private static final int MASK_CHARGING_CASE = 1 << 6;

    private BluetoothAdapter mAdapter;
    private BluetoothA2dp mA2dp;
    private BluetoothLeScanner mScanner;

    private BluetoothDevice mCurrentDevice;

    private String mBestLeAddress = null;
    private int mBestLeRssi = -128;

    private final BroadcastReceiver mA2dpStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            handleA2dpStateChanged(state, device);
        }
    };

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> scanResults) {
            for (ScanResult result : scanResults) {
                handleScanResult(result);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handleScanResult(result);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mA2dpStateReceiver, filter);
        openA2dpProxy();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        stopScan();
        closeA2dpProxy();
        unregisterReceiver(mA2dpStateReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        return START_STICKY;
    }

    private void openA2dpProxy() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Log.w(TAG, "BluetoothAdapter is null, ignored");
            return;
        }

        mAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                mA2dp = (BluetoothA2dp) proxy;
                handleA2dpServiceConnected();
            }

            @Override
            public void onServiceDisconnected(int profile) {
                mA2dp = null;
            }
        }, BluetoothProfile.A2DP);
    }

    private void closeA2dpProxy() {
        if (mAdapter == null || mA2dp == null) {
            return;
        }

        mAdapter.closeProfileProxy(BluetoothProfile.A2DP, mA2dp);
    }

    private void handleA2dpServiceConnected() {
        final List<BluetoothDevice> devices = mA2dp.getConnectedDevices();
        if (!devices.isEmpty()) {
            // TODO: Detect if it's AirPods
            mCurrentDevice = devices.get(0);
            startScan();
        }
    }

    private void handleA2dpStateChanged(int state, BluetoothDevice device) {
        if (state == BluetoothProfile.STATE_CONNECTED) {
            // TODO: Detect if it's AirPods
            mCurrentDevice = device;
            startScan();
        } else if (state == BluetoothProfile.STATE_DISCONNECTING ||
                state == BluetoothProfile.STATE_DISCONNECTED) {
            stopScan();
            mCurrentDevice = null;
        }
    }

    private void startScan() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Log.w(TAG, "BluetoothAdapter is null, ignored");
            return;
        }

        mScanner = mAdapter.getBluetoothLeScanner();
        if (mScanner == null) {
            Log.w(TAG, "BluetoothLeScanner is null, ignored");
            return;
        }

        final List<ScanFilter> filters = new ArrayList<>();
        final byte[] mask = new byte[2 + DATA_LENGTH_BATTERY];
        mask[0] = AirPodsContants.MANUFACTURER_MAGIC;
        mask[1] = DATA_LENGTH_BATTERY;
        filters.add(new ScanFilter.Builder()
                .setManufacturerData(AirPodsContants.MANUFACTURER_ID, mask, mask)
                .build());

        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(REPORT_DELAY_MS)
                .build();

        mScanner.startScan(filters, settings, mScanCallback);
        Log.v(TAG, "startScan");
    }

    private void stopScan() {
        if (mScanner == null) {
            return;
        }
        mScanner.stopScan(mScanCallback);
        mScanner = null;
        mBestLeAddress = null;
        mBestLeRssi = -128;
        Log.v(TAG, "stopScan");
    }

    private void handleScanResult(ScanResult result) {
        final ScanRecord record = result.getScanRecord();
        if (record == null) {
            return;
        }

        final byte[] data = record.getManufacturerSpecificData(AirPodsContants.MANUFACTURER_ID);
        if (data == null || data.length < 2 || data.length != data[1] + 2) {
            return;
        }

        if (data[1] != DATA_LENGTH_BATTERY) {
            return;
        }

        final String address = result.getDevice().getAddress();
        final int rssi = result.getRssi();

        if (mBestLeAddress == null) {
            mBestLeAddress = address;
            mBestLeRssi = rssi;
            Log.d(TAG, "First result from " + address + ", rssi=" + rssi);
        } else if (!mBestLeAddress.equals(address)) {
            if (rssi > mBestLeRssi) {
                mBestLeAddress = address;
                mBestLeRssi = rssi;
                Log.d(TAG, "Better result from " + address + ", rssi=" + rssi);
            } else {
                return;
            }
        }

        // TODO: Find the best RSSI

        final int flags = data[5];
        final int earpiece = data[6];
        final int charger = data[7];

        final boolean rightLeft = ((flags & FLAG_RIGHTLEFT) != 0);
        final int battLeft = (rightLeft ? earpiece : (earpiece >> 4)) & 0x0f;
        final int battRight = (rightLeft ? (earpiece >> 4) : earpiece) & 0x0f;
        final int battCase = charger & 0x0f;

        final boolean chagLeft = (charger & (rightLeft ? MASK_CHARGING_RIGHT : MASK_CHARGING_LEFT)) != 0;
        final boolean chagRight = (charger & (rightLeft ? MASK_CHARGING_LEFT : MASK_CHARGING_RIGHT)) != 0;
        final boolean chagCase = (charger & MASK_CHARGING_CASE) != 0;

        int displayLevel = Math.min(battLeft, battRight);
        if (displayLevel == 15) {
            displayLevel = BluetoothDevice.BATTERY_LEVEL_UNKNOWN;
        } else {
            if (displayLevel > 0) {
                displayLevel = displayLevel - 1; // [0, 9]
            }
        }

        final Object[] arguments = new Object[] {
            1, // NumberOfIndicators
            BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV_BATTERY_LEVEL, // IndicatorType
            displayLevel // IndicatorValue
        };

        broadcastVendorSpecificEventIntent(
                BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_IPHONEACCEV,
                BluetoothAssignedNumbers.APPLE,
                BluetoothHeadset.AT_CMD_TYPE_SET,
                arguments,
                mCurrentDevice);
    }

    private void broadcastVendorSpecificEventIntent(String command, int companyId, int commandType,
            Object[] arguments, BluetoothDevice device) {
        final Intent intent = new Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD, command);
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, commandType);
        // assert: all elements of args are Serializable
        intent.putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, arguments);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "."
                + Integer.toString(companyId));
        sendBroadcastAsUser(intent, UserHandle.ALL, Manifest.permission.BLUETOOTH);
    }

}
