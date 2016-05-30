package com.robotpajamas.android.ble113_ota.peripherals;

import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.BlueteethUtils;
import com.robotpajamas.blueteeth.listeners.OnBondingChangedListener;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicReadListener;
import com.robotpajamas.blueteeth.listeners.OnCharacteristicWriteListener;
import com.robotpajamas.blueteeth.listeners.OnConnectionChangedListener;

import java.lang.reflect.Array;
import java.util.UUID;

public class BluegigaPeripheral extends BaseBluetoothPeripheral {

    /* OTA Service */
    private static final UUID SERVICE_OTA = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0");
    private static final UUID CHARACTERISTIC_CONTROL = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063");
    private static final UUID CHARACTERISTIC_DATA = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153");

    public BluegigaPeripheral(BlueteethDevice device) {
        super(device);
    }

    public void writeCounter(byte value, OnCharacteristicWriteListener writeListener) {
        byte[] data = new byte[]{value};
        BlueteethUtils.writeData(data, CHARACTERISTIC_WRITE, SERVICE_TEST, mPeripheral, writeListener);
    }

    public void writeNoResponseCounter(byte value) {
        byte[] data = new byte[]{value};
        BlueteethUtils.writeData(data, CHARACTERISTIC_WRITE_NO_RESPONSE, SERVICE_TEST, mPeripheral, null);
    }

    public void readCounter(OnCharacteristicReadListener readListener) {
        BlueteethUtils.read(CHARACTERISTIC_READ, SERVICE_TEST, mPeripheral, readListener);
    }

    public void toggleNotification(boolean isEnabled, OnCharacteristicReadListener readListener) {
        if (isEnabled) {
            mPeripheral.addNotification(CHARACTERISTIC_NOTIFY, SERVICE_TEST, readListener);
        } else {
//            mPeripheral.removeNotifications(CHARACTERISTIC_NOTIFY, SERVICE_TEST);
        }
    }

    public void writeEcho(byte[] dataToWrite, OnCharacteristicWriteListener writeListener) {
        BlueteethUtils.writeData(dataToWrite, CHARACTERISTIC_WRITE_ECHO, SERVICE_TEST, mPeripheral, writeListener);
        mPeripheral.writeCharacteristic(dataToWrite, CHARACTERISTIC_WRITE_ECHO, SERVICE_TEST, writeListener);
    }

//    public void writeNoResponseEcho(byte[] dataToWrite) {
//        mPeripheral.writeCharacteristic(dataToWrite, CHARACTERISTIC_WRITE_ECHO, SERVICE_TEST, null);
//    }

    public void readEcho(OnCharacteristicReadListener readListener) {
        BlueteethUtils.read(CHARACTERISTIC_READ_ECHO, SERVICE_TEST, mPeripheral, readListener);
    }

//    public void notifyEcho()
//    public void indicateEcho()

}

