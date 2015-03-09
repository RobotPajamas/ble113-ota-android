package com.robotpajamas.android.ble113_ota.ui;

import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.robotpajamas.android.ble113_ota.Blueteeth.BlueteethDevice;
import com.robotpajamas.android.ble113_ota.Blueteeth.BlueteethManager;
import com.robotpajamas.android.ble113_ota.Blueteeth.BlueteethUtils;
import com.robotpajamas.android.ble113_ota.R;
import com.robotpajamas.android.ble113_ota.utils.DataChunker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    //    private BLEService mBLEService;
    BlueteethDevice mDevice;

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
//        updateFirmware();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);
        ButterKnife.inject(this);


        mDevice = BlueteethManager.getInstance().connectedDevice;
        mDevice.discoverServices(() -> BlueteethUtils.readData(DEVICE_MANUFACTURER_FIRMWARE, DEVICE_SERVICE, mDevice, data -> mFirmwareVersion.setText(new String(data))));

        // Prepare file dropdown
        mFileDropdownAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        mFileDropdownSpinner.setAdapter(mFileDropdownAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        reloadFileDropdown();
    }

    @Override
    public void onPause() {
        super.onPause();
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

//    // Methods to extract sensor data and update the UI
//    private void updateFirmware() {
//
//        mProgressBar.setMax(mDataChunker.getTotalChunks());
//
//        mFirmwareUpdate = true;
//        writeNextFirmwarePacket(); // Kick off first packet
//    }

//    private void writeNextFirmwarePacket() {
//        if (mDataChunker.hasNext()) {
//            byte[] nextFrame = mDataChunker.next();
//            mProgressBar.setProgress(mDataChunker.getCurrentChunk());
//
//            // Send data to device
////            mBLEService.writeCharacteristic(OTA_SERVICE, OTA_DATA_CHAR, nextFrame);
//        } else {
//            // Reset the chunker, just in case
//            mProgressBar.setProgress(mDataChunker.getTotalChunks());
//            mDataChunker.reset();
//            mFirmwareUpdate = false;
//
//            // Send DFU reset to device - this will cause the device to reset
////            mBLEService.writeCharacteristic(OTA_SERVICE, OTA_CONTROL_CHAR, new byte[]{0x03});
//
//        }
//    }
}
