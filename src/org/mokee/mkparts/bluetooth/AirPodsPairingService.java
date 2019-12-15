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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import org.mokee.mkparts.R;

public class AirPodsPairingService extends Service {

    private static final String TAG = "AirPodsPairingService";

    private static final int DATA_LENGTH_PAIRING = 15;

    private static final String NOTIFICATION_CHANNEL_PAIRING = "pairing";

    private static final String ACTION_CONFIRM = "org.mokee.mkparts.bluetooth.pairing.CONFIRM";

    private NotificationManager mNotificationManager;

    private BluetoothAdapter mAdapter;
    private BluetoothLeScanner mScanner;

    private BluetoothDevice mDeviceFound = null;

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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                handleDeviceDiscovered(device);
            }
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

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        final NotificationChannel serviceChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_PAIRING,
                getString(R.string.bluetooth_channel_pairing),
                NotificationManager.IMPORTANCE_HIGH);
        mNotificationManager.createNotificationChannel(serviceChannel);

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        startScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        final String action = intent.getAction();
        if (ACTION_CONFIRM.equals(action)) {
            handleConfirm();
        }
        return START_STICKY;
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
        data[0] = AirPodsConstants.MANUFACTURER_MAGIC;
        data[1] = DATA_LENGTH_PAIRING;

        final byte[] mask = new byte[2 + DATA_LENGTH_PAIRING];
        mask[0] = -1;
        mask[1] = -1;

        filters.add(new ScanFilter.Builder()
                .setManufacturerData(AirPodsConstants.MANUFACTURER_ID, data, mask)
                .build());

        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(2)
                .build();

        mScanner.startScan(filters, settings, mScanCallback);
        Log.v(TAG, "startScan");
    }

    private void handleScanResult(ScanResult result) {
        final ScanRecord record = result.getScanRecord();
        if (record == null) {
            return;
        }

        final byte[] data = record.getManufacturerSpecificData(AirPodsConstants.MANUFACTURER_ID);

        final byte[] address = new byte[6];
        System.arraycopy(data, 5, address, 0, 6);
        final BluetoothDevice device = mAdapter.getRemoteDevice(address);

        if (!device.equals(mDeviceFound)) {
            mDeviceFound = device;
            handleDeviceFound();
        }
    }

    private Notification.Builder createNotification() {
        return new Notification.Builder(this, NOTIFICATION_CHANNEL_PAIRING)
                .setCategory(Notification.CATEGORY_STATUS)
                .setSmallIcon(R.drawable.ic_settings_bluetooth);
    }

    private void handleDeviceFound() {
        Log.v(TAG, "Device found: " + mDeviceFound + " - " + mDeviceFound.getName());
        if (mDeviceFound.getBondState() == BluetoothDevice.BOND_NONE) {
            if (mDeviceFound.getName() != null) {
                handleDeviceResolved();
            } else {
                startDiscovery();
            }
        }
    }

    private void startDiscovery() {
        Log.v(TAG, "Start discovery...");
        mAdapter.startDiscovery();
    }

    private void cancelDiscovery() {
        if (mAdapter == null) {
            return;
        }
        mAdapter.cancelDiscovery();
        Log.v(TAG, "Cancel discovery");
    }

    private void handleDeviceDiscovered(BluetoothDevice device) {
        if (mDeviceFound == null) {
            cancelDiscovery();
            return;
        }

        if (!device.equals(mDeviceFound)) {
            Log.v(TAG, "Not my device: " + device + " - " + device.getName());
            return;
        }

        Log.v(TAG, "Found: " + device + " - " + device.getName());
        cancelDiscovery();

        mDeviceFound = device;
        handleDeviceResolved();
    }

    private void handleDeviceResolved() {
        final PendingIntent pendingIntent = PendingIntent.getService(this, 1,
                new Intent(this, AirPodsPairingService.class)
                        .setAction(ACTION_CONFIRM),
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationManager.notify(mDeviceFound.getAddress().hashCode(), createNotification()
                .setContentTitle(mDeviceFound.getName())
                .setContentText(getString(R.string.bluetooth_found_message))
                .addAction(new Notification.Action.Builder(null,
                        getString(R.string.bluetooth_found_confirm),
                        pendingIntent)
                        .build())
                .build());
    }

    private void handleConfirm() {
        if (mDeviceFound == null) {
            return;
        }

        mNotificationManager.cancel(mDeviceFound.getAddress().hashCode());

        mDeviceFound.createBond();
    }

}
