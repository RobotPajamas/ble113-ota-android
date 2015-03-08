package com.robotpajamas.android.ble113_ota.Blueteeth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * Created by sureshjoshi on 15-03-08.
 */
public class BLEDevice {
    BluetoothDevice mDevice;
    BluetoothGatt mGatt;

    BLEDevice(BluetoothDevice device) {
        mDevice = device;
    }
}
