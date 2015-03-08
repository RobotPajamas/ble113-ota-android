package com.robotpajamas.android.ble113_ota.Blueteeth;

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

import java.util.UUID;

import timber.log.Timber;

public class BLEService extends Service {

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

    private BluetoothAdapter.LeScanCallback mBLEScanCallback = (device, rssi, scanRecord) -> Manager.getInstance().addDevice(new BLEDevice(device));

    private BluetoothAdapter.LeScanCallback mBLEScanCompleteCallback = (device, rssi, scanRecord) -> Manager.getInstance().scanComplete();

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
//                        LocalBroadcastManager.getInstance(BLEService.this)
//                                .sendBroadcast(new Intent(ACTION_GATT_CONNECTED));
                        break;

                    case BluetoothProfile.STATE_DISCONNECTED:
//                        LocalBroadcastManager.getInstance(BLEService.this)
//                                .sendBroadcast(new Intent(ACTION_GATT_DISCONNECTED));
                        break;
                }
            } else {
                gatt.disconnect();
//                LocalBroadcastManager.getInstance(BLEService.this)
//                        .sendBroadcast(new Intent(ACTION_GATT_CONNECTION_STATE_ERROR));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Manager.getInstance().setBluetoothGatt(gatt);
//                LocalBroadcastManager.getInstance(BLEService.this)
//                        .sendBroadcast(new Intent(ACTION_GATT_SERVICES_DISCOVERED));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

//            Intent broadcastIntent = new Intent(ACTION_DATA_AVAILABLE);
//            broadcastIntent.putExtra(DATA_AVAILABLE, characteristic.getValue());
//
//            LocalBroadcastManager.getInstance(BLEService.this)
//                    .sendBroadcast(broadcastIntent);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

//            LocalBroadcastManager.getInstance(BLEService.this)
//                    .sendBroadcast(new Intent(ACTION_DATA_WRITE));
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

    public void beginScan(int scanDuration, ScanCallback callback) {
        mHandler.postDelayed(() -> endScan(),scanDuration);
        mBLEAdapter.startLeScan(mBLEScanCallback);
    }

    public void endScan() {
        mBLEAdapter.stopLeScan(mBLEScanCompleteCallback);
    }

    public void connect(BluetoothDevice device) {
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
        mConnectedGatt = device.connectGatt(this, false, mGattCallback);

    }

    public void disconnect() {
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
        Manager.getInstance().reset();
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

    public void writeCharacteristic(byte[] data, UUID serviceUUID, UUID characteristicUUID) {
        if (mConnectedGatt == null) {
            return;
        }

        BluetoothGattCharacteristic characteristic = mConnectedGatt.getService(serviceUUID)
                .getCharacteristic(characteristicUUID);

        characteristic.setValue(data);
        mConnectedGatt.writeCharacteristic(characteristic);
    }
}
