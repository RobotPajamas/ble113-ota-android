package com.robotpajamas.android.ble113_ota.Blueteeth;

import java.util.List;

public interface ScanCallback {
    public void call(List<BLEDevice> bleDevices);
}
