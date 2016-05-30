package com.robotpajamas.android.ble113_ota.peripherals;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.robotpajamas.android.ble113_ota.listeners.OnFirmwarePacketUploadedListener;
import com.robotpajamas.android.ble113_ota.listeners.OnFirmwareUpdateCompleteListener;
import com.robotpajamas.blueteeth.BlueteethDevice;
import com.robotpajamas.blueteeth.BlueteethUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import timber.log.Timber;

public class BluegigaPeripheral extends BaseBluetoothPeripheral {

    /* OTA Service */
    private static final UUID SERVICE_OTA = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0");
//    private static final UUID CHARACTERISTIC_CONTROL_ACK = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063");
//    private static final UUID CHARACTERISTIC_DATA_NO_ACK = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153");

    private static final UUID CHARACTERISTIC_CONTROL_NO_ACK = UUID.fromString("01737572-6573-686a-6f73-68692e636f6d");
    private static final UUID CHARACTERISTIC_DATA_ACK = UUID.fromString("00737572-6573-686a-6f73-68692e636f6d");

    private static final int PACKET_SIZE = 16;

    Handler mHandler = new Handler(Looper.getMainLooper());

    private BufferedSource mOtaSource = new Buffer();
    private OnFirmwarePacketUploadedListener mOnFirmwarePacketUploadedListener;
    private OnFirmwareUpdateCompleteListener mOnFirmwareUpdateCompleteListener;

    public BluegigaPeripheral(BlueteethDevice device) {
        super(device);
    }

    public int updateFirmware(File otaFile,
                              @NonNull OnFirmwarePacketUploadedListener onFirmwarePacketUploadedListener,
                              @NonNull OnFirmwareUpdateCompleteListener onFirmwareUpdateCompleteListener) {
        Timber.d("Starting firmware update");

        mOnFirmwarePacketUploadedListener = onFirmwarePacketUploadedListener;
        mOnFirmwareUpdateCompleteListener = onFirmwareUpdateCompleteListener;

        int totalNumberOfPackets = (int) (otaFile.length() / 16);

        try {
            mOtaSource = Okio.buffer(Okio.source(otaFile));
            BlueteethUtils.writeData(mOtaSource.readByteArray(PACKET_SIZE), CHARACTERISTIC_DATA_ACK, SERVICE_OTA, mPeripheral, response -> {
                mOnFirmwarePacketUploadedListener.call();
                mHandler.postDelayed(uploadNextPacket, 50);
            });
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        return totalNumberOfPackets;
    }

    private Runnable uploadNextPacket = new Runnable() {
        @Override
        public void run() {
            try {
                byte[] data;
                boolean sendReset = mOtaSource.exhausted();
                if (sendReset) {
                    data = new byte[]{0x03};
                    mPeripheral.writeCharacteristic(data, CHARACTERISTIC_CONTROL_NO_ACK, SERVICE_OTA, response -> mOnFirmwareUpdateCompleteListener.call());
                } else {
                    data = mOtaSource.readByteArray(PACKET_SIZE);
                    mPeripheral.writeCharacteristic(data, CHARACTERISTIC_DATA_ACK, SERVICE_OTA, response -> {
                        mOnFirmwarePacketUploadedListener.call();
                        mHandler.postDelayed(uploadNextPacket, 50);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

}

