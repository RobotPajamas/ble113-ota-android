package com.robotpajamas.android.ble113_ota.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.robotpajamas.android.ble113_ota.R;
import com.robotpajamas.android.ble113_ota.bt.BLEService;
import com.robotpajamas.android.ble113_ota.utils.DataChunker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import timber.log.Timber;


public class OtaActivity extends ActionBarActivity {

    /* OTA Service */
    private static final UUID OTA_SERVICE = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0");
    private static final UUID OTA_CONTROL_CHAR = UUID.fromString("f7bf3564-fb6d-4e53-88a4-5e37e0326063");
    private static final UUID OTA_DATA_CHAR = UUID.fromString("984227f3-34fc-4045-a5d0-2c581f81a153");

    // Using standard 16bit UUIDs, transformed into the correct 128-bit UUID
    private static final UUID DEVICE_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_NAME = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_MODEL = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_HARDWARE = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_FIRMWARE = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_MANUFACTURER_SOFTWARE = UUID.fromString("00002A28-0000-1000-8000-00805f9b34fb");

    private final File DOWNLOAD_DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private IntentFilter mIntentFilter;
    private ArrayAdapter<String> mFileDropdownAdapter;
    private DataChunker mDataChunker;
    private boolean mFirmwareUpdate;
    private BLEService mBLEService;

    @InjectView(R.id.firmware_version)
    TextView mFirmwareVersion;

    @InjectView(R.id.frag_ota_dropdown)
    Spinner mFileDropdownSpinner;

    @InjectView(R.id.frag_ota_progressbar)
    ProgressBar mProgressBar;

    @InjectView(R.id.frag_ota_button_upload)
    Button mUpdateButton;

    @OnClick(R.id.frag_ota_button_upload)
    void onUploadClicked() {
        // Get the byte array from the file
        byte[] fileData = getFileBytes();
        if (fileData == null)
            return;

        // Try to upload the firmware to the selected device
        mDataChunker = new DataChunker(fileData);
        updateFirmware();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);
        ButterKnife.inject(this);

        // Prepare file dropdown
        mFileDropdownAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        mFileDropdownSpinner.setAdapter(mFileDropdownAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        reloadFileDropdown();

        IntentFilter filter = new IntentFilter(getIntentFilter());
        LocalBroadcastManager.getInstance(this).registerReceiver(mBLEReceiver, filter);

        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        unbindService(mConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBLEReceiver);
    }

    // Look in the public downloads directory, only for files ending in .ota
    private void reloadFileDropdown() {
        File files[] = DOWNLOAD_DIRECTORY.listFiles((dir, filename) -> filename.toLowerCase().endsWith(".ota"));

        if (files == null) {
            Timber.i("No files in downloads directory");
            return;
        }

        // If there are files, add them to the dropdown
        for (File file : files) {
            mFileDropdownAdapter.add(file.getName());
        }
    }

    // Take a file and get a byte array from it
    private byte[] getFileBytes() {
        String selectedFile = (String) mFileDropdownSpinner.getSelectedItem();
        if (selectedFile == null || selectedFile.isEmpty()) {
            Timber.d("No file selected in dropdown...");
            Crouton.makeText(this, R.string.no_file_selected, Style.INFO).show();
            return null;
        }

        // Get file's internal data
        File file = new File(DOWNLOAD_DIRECTORY, selectedFile);
        byte[] bytes = new byte[(int) file.length()];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            int result = buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            Crouton.makeText(this, R.string.file_not_found, Style.ALERT).show();
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            Crouton.makeText(this, R.string.io_exception, Style.ALERT).show();
            e.printStackTrace();
            return null;
        }

        return bytes;
    }

    // Methods to extract sensor data and update the UI
    private void updateFirmware() {

        mProgressBar.setMax(mDataChunker.getTotalChunks());

        mFirmwareUpdate = true;
        writeNextFirmwarePacket(); // Kick off first packet
    }

    private void writeNextFirmwarePacket() {
        if (mDataChunker.hasNext()) {
            byte[] nextFrame = mDataChunker.next();
            mProgressBar.setProgress(mDataChunker.getCurrentChunk());

            // Send data to device
            mBLEService.writeCharacteristic(OTA_SERVICE, OTA_DATA_CHAR, nextFrame);
        } else {
            // Reset the chunker, just in case
            mProgressBar.setProgress(mDataChunker.getTotalChunks());
            mDataChunker.reset();
            mFirmwareUpdate = false;

            // Send DFU reset to device - this will cause the device to reset
            mBLEService.writeCharacteristic(OTA_SERVICE, OTA_CONTROL_CHAR, new byte[]{0x03});

        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            BLEService.BLEBinder b = (BLEService.BLEBinder) binder;
            mBLEService = b.getService();
            if (!mBLEService.initialize()) {
                Timber.e("BLE Service did not initialize correctly. Exiting...");
                finish();
            } else {
                // Need to discover services, otherwise nothing actually works!
                mBLEService.discoverServices();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBLEService = null;
        }
    };


    private IntentFilter getIntentFilter() {
        if (mIntentFilter == null) {
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
            mIntentFilter.addAction(BLEService.ACTION_GATT_CONNECTION_STATE_ERROR);
            mIntentFilter.addAction(BLEService.ACTION_DATA_WRITE);
            mIntentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
            mIntentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        }
        return mIntentFilter;
    }


    private final BroadcastReceiver mBLEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BLEService.ACTION_GATT_DISCONNECTED:
                    // Go back to scan screen on disconnection.
                    finish();
                    break;
                case BLEService.ACTION_GATT_SERVICES_DISCOVERED:
                    // Get the device's firmware
                    mBLEService.readCharacteristic(DEVICE_SERVICE, DEVICE_MANUFACTURER_FIRMWARE);
                    break;

                case BLEService.ACTION_DATA_AVAILABLE:
                    // Populate the device firmware
                    byte[] data = intent.getByteArrayExtra(BLEService.DATA_AVAILABLE);
                    mFirmwareVersion.setText(new String(data));
                    break;

                case BLEService.ACTION_DATA_WRITE:
                    if (mFirmwareUpdate) {
                        // Inject a small delay, so we don't overload the BLE113 firmware writing (15ms)
                        // BLE113 can handle faster writes, but it's less reliable, so be safe and a bit slower with the delay
                        // Also, large uploads tend to stall the BLE113 without the delay
                        // TODO: Figure out why... This is so hacky and hurts my soul
                        try {
                            Thread.sleep(15);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        writeNextFirmwarePacket();
                    }
                    break;
            }
        }
    };

}
