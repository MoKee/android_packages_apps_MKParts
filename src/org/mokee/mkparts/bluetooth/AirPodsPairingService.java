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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AirPodsPairingService extends Service {

    private static final String TAG = "AirPodsPairingService";

    private static final int DATA_LENGTH_PAIRING = 15;

    private BluetoothAdapter mAdapter;
    private BluetoothLeScanner mScanner;

    private BluetoothDevice mDeviceFound = null;

    private final BroadcastReceiver mAdapterStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            handleAdapterStateChanged(state);
        }
    };

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            handleScanResult(callbackType, result);
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
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mAdapterStateReceiver, filter);
        startScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        stopScan();
        unregisterReceiver(mAdapterStateReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        return START_STICKY;
    }

    private void handleAdapterStateChanged(int state) {
        if (state == BluetoothAdapter.STATE_ON) {
            startScan();
        } else if (state == BluetoothAdapter.STATE_TURNING_OFF ||
                state == BluetoothAdapter.STATE_OFF) {
            // No need to stopScan, will be stopped once turned off
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

        final byte[] data = new byte[2 + DATA_LENGTH_PAIRING];
        data[0] = AirPodsContants.MANUFACTURER_MAGIC;
        data[1] = DATA_LENGTH_PAIRING;

        final byte[] mask = new byte[2 + DATA_LENGTH_PAIRING];
        mask[0] = -1;
        mask[1] = -1;

        filters.add(new ScanFilter.Builder()
                .setManufacturerData(AirPodsContants.MANUFACTURER_ID, data, mask)
                .build());

        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH |
                            ScanSettings.CALLBACK_TYPE_MATCH_LOST)
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
        Log.v(TAG, "stopScan");
    }

    private void handleScanResult(int callbackType, ScanResult result) {
        final ScanRecord record = result.getScanRecord();
        if (record == null) {
            return;
        }

        final byte[] data = record.getManufacturerSpecificData(AirPodsContants.MANUFACTURER_ID);

        final byte[] address = new byte[6];
        System.arraycopy(data, 5, address, 0, 6);
        final BluetoothDevice device = mAdapter.getRemoteDevice(address);

        if (callbackType == ScanSettings.CALLBACK_TYPE_FIRST_MATCH) {
            if (!device.equals(mDeviceFound)) {
                if (mDeviceFound != null) {
                    handleDeviceLost(mDeviceFound);
                }
                mDeviceFound = device;
                handleDeviceFound(device);
            }
        } else if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST) {
            if (device.equals(mDeviceFound)) {
                mDeviceFound = null;
                handleDeviceLost(device);
            }
        }
    }

    private void handleDeviceFound(BluetoothDevice device) {
        Log.v(TAG, "device found: " + device);
    }

    private void handleDeviceLost(BluetoothDevice device) {
        Log.v(TAG, "device lost: " + device);
    }

}
