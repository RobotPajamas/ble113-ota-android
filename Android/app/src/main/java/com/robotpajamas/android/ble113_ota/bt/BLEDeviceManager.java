package com.robotpajamas.android.ble113_ota.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

public class BLEDeviceManager {

    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private boolean mConnected;

    private static BLEDeviceManager singleton = null;

    public static BLEDeviceManager getInstance() {
        if (singleton == null) {
            synchronized (BLEDeviceManager.class) {
                if (singleton == null) {
                    singleton = new BLEDeviceManager();
                }
            }
        }
        return singleton;
    }

    public void reset() {
        mConnected = false;
        mBluetoothDevice = null;
        mBluetoothGatt = null;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        mBluetoothDevice = bluetoothDevice;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        mBluetoothGatt = bluetoothGatt;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    public void setIsConnected(boolean connected) {
        mConnected = connected;
    }

    public boolean isConnected() {
        return mConnected;
    }
}

