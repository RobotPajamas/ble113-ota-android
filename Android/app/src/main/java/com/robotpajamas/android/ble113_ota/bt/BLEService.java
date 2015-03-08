package com.robotpajamas.android.ble113_ota.bt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.util.UUID;

import timber.log.Timber;

public class BLEService extends Service {

    public static final String ACTION_BEGIN_SCAN = ".BLEService.ACTION_BEGIN_SCAN";
    public static final String ACTION_END_SCAN = ".BLEService.ACTION_END_SCAN";
    public static final String ACTION_DEVICE_DISCOVERED = ".BLEService.ACTION_DEVICE_DISCOVERED";
    public static final String ACTION_GATT_CONNECTED = ".BLEService.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = ".BLEService.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_CONNECTION_STATE_ERROR = ".BLEService.ACTION_GATT_CONNECTION_STATE_ERROR";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = ".BLEService.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = ".BLEService.ACTION_DATA_AVAILABLE";
    public static final String ACTION_DATA_WRITE = ".BLEService.ACTION_DATA_WRITE";
    public static final String ACTION_READ_REMOTE_RSSI = ".BLEService.ACTION_READ_REMOTE_RSSI";
    public static final String ACTION_DESCRIPTOR_WRITE = ".BLEService.ACTION_DESCRIPTOR_WRITE";

    public static final String DISCOVERED_DEVICE = ".BLEService.DISCOVERED_DEVICE";
    public static final String DEVICE = ".BLEService.DEVICE";
    public static final String DEVICE_ADDRESS = ".BLEService.DEVICE_ADDRESS";
    public static final String RSSI = ".BLEService.RSSI";
    public static final String UUID_CHARACTERISTIC = ".BLEService.UUID_CHARACTERISTIC";
    public static final String UUID_DESCRIPTOR = ".BLEService.UUID_DESCRIPTOR";
    public static final String GATT_STATUS = ".BLEService.GATT_STATUS";
    public static final String SCAN_RECORD = ".BLEService.SCAN_RECORD";
    public static final String DATA_AVAILABLE = ".BLEService.DATA_AVAILABLE";

    private final IBinder mBLEBinder = new BLEBinder();
    private BluetoothManager mBLEManager;
    private BluetoothAdapter mBLEAdapter;
    private Handler mHandler = new Handler();
    private BluetoothGatt mConnectedGatt;


    @Override
    public IBinder onBind(Intent intent) {
        return mBLEBinder;
    }

    public class BLEBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return Service.START_NOT_STICKY;
    }

    private BluetoothAdapter.LeScanCallback mBLEScanCallback = (device, rssi, scanRecord) -> {
        BLEDeviceManager.getInstance().setBluetoothDevice(device);

        Intent broadcastIntent = new Intent(ACTION_DEVICE_DISCOVERED);
        broadcastIntent.putExtra(DISCOVERED_DEVICE, device);
        broadcastIntent.putExtra(RSSI, rssi);
        broadcastIntent.putExtra(SCAN_RECORD, scanRecord);

        LocalBroadcastManager.getInstance(BLEService.this).sendBroadcast(broadcastIntent);
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        LocalBroadcastManager.getInstance(BLEService.this)
                                .sendBroadcast(new Intent(ACTION_GATT_CONNECTED));
                        break;

                    case BluetoothProfile.STATE_DISCONNECTED:
                        LocalBroadcastManager.getInstance(BLEService.this)
                                .sendBroadcast(new Intent(ACTION_GATT_DISCONNECTED));
                        break;
                }
            } else {
                gatt.disconnect();
                LocalBroadcastManager.getInstance(BLEService.this)
                        .sendBroadcast(new Intent(ACTION_GATT_CONNECTION_STATE_ERROR));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BLEDeviceManager.getInstance().setBluetoothGatt(gatt);
                LocalBroadcastManager.getInstance(BLEService.this)
                        .sendBroadcast(new Intent(ACTION_GATT_SERVICES_DISCOVERED));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Intent broadcastIntent = new Intent(ACTION_DATA_AVAILABLE);
            broadcastIntent.putExtra(DATA_AVAILABLE, characteristic.getValue());

            LocalBroadcastManager.getInstance(BLEService.this)
                    .sendBroadcast(broadcastIntent);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            LocalBroadcastManager.getInstance(BLEService.this)
                    .sendBroadcast(new Intent(ACTION_DATA_WRITE));
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            // TODO: Add RSSI code here
        }
    };

    public boolean initialize() {
        Timber.d("Initializing BluetoothManager");
        if (mBLEManager == null) {
            mBLEManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBLEManager == null) {
                Timber.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        Timber.d("Initializing BLEAdapter");
        mBLEAdapter = mBLEManager.getAdapter();
        if (mBLEAdapter == null) {
            Timber.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if (!mBLEAdapter.isEnabled()) {
            Timber.e("Bluetooth is not enabled.");
            return false;
        }

        return true;
    }

    public void beginScan() {
        final int kScanDuration = 3000;
        mHandler.postDelayed(this::endScan, kScanDuration);

        LocalBroadcastManager.getInstance(BLEService.this).sendBroadcast(new Intent(ACTION_BEGIN_SCAN));
        mBLEAdapter.startLeScan(mBLEScanCallback);
    }

    public void endScan() {
        mBLEAdapter.stopLeScan(mBLEScanCallback);
        LocalBroadcastManager.getInstance(BLEService.this).sendBroadcast(new Intent(ACTION_END_SCAN));
    }

    // TODO: Add provisions for multiple simultaneous connections
    public void connect(BluetoothDevice device) {
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
        mConnectedGatt = device.connectGatt(this, false, mGattCallback);

    }

    // TODO: Add provisions for multiple simultaneous connections
    public void disconnect() {
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
        BLEDeviceManager.getInstance().reset();
    }

    public void discoverServices() {
        if (mConnectedGatt != null) {
            mConnectedGatt.discoverServices();
        }
    }

    public void readCharacteristic(UUID serviceUUID, UUID characteristicUUID) {
        if (mConnectedGatt == null) {
            return;
        }

        BluetoothGattCharacteristic characteristic = mConnectedGatt.getService(serviceUUID)
                .getCharacteristic(characteristicUUID);

        mConnectedGatt.readCharacteristic(characteristic);
    }

    public void writeCharacteristic(UUID serviceUUID, UUID characteristicUUID, byte[] writeArray) {
        if (mConnectedGatt == null) {
            return;
        }

        BluetoothGattCharacteristic characteristic = mConnectedGatt.getService(serviceUUID)
                .getCharacteristic(characteristicUUID);

        characteristic.setValue(writeArray);
        mConnectedGatt.writeCharacteristic(characteristic);
    }
}
