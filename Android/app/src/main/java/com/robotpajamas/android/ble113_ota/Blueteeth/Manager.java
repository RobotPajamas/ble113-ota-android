package com.robotpajamas.android.ble113_ota.Blueteeth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Manager {

    private List<BLEDevice> mScannedDevices = new ArrayList<>();
    private Queue<ScanCallback> mScanCallbackQueue = new LinkedList<>();
    private static Manager singleton = null;

    public static Manager getInstance() {
        if (singleton == null) {
            synchronized (Manager.class) {
                if (singleton == null) {
                    singleton = new Manager();
                }
            }
        }
        return singleton;
    }

    public void scanForDevices(ScanCallback callback) {
        mScannedDevices.clear();
        mScanCallbackQueue.add(callback);
    }

    protected void scanComplete() {
        ScanCallback callback = mScanCallbackQueue.remove();
        callback.call(mScannedDevices);
    }

    public void addDevice(BLEDevice device) {
        mScannedDevices.add(device);
    }

    public void reset() {
        mScannedDevices.clear();
        mConnected = false;
        mBluetoothDevice = null;
        mBluetoothGatt = null;
    }

}

