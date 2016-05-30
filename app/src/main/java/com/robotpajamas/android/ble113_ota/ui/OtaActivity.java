package com.robotpajamas.android.ble113_ota.ui;

import android.app.Activity;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.robotpajamas.android.ble113_ota.R;
import com.robotpajamas.android.ble113_ota.peripherals.BluegigaPeripheral;
import com.robotpajamas.android.ble113_ota.utils.DataChunker;
import com.robotpajamas.blueteeth.BlueteethManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;


public class OtaActivity extends Activity {

    @Bind(R.id.firmware_version)
    TextView mFirmwareVersion;

    @Bind(R.id.spinner_files)
    Spinner mFileDropdownSpinner;

    @Bind(R.id.progressbar)
    ProgressBar mProgressBar;

    @Bind(R.id.button_upload)
    Button mUpdateButton;

    private final File DOWNLOAD_DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    private IntentFilter mIntentFilter;
    private ArrayAdapter<String> mFileDropdownAdapter;
    private DataChunker mDataChunker;
    private boolean mFirmwareUpdate;
    private BluegigaPeripheral mBluegigaPeripheral;

    @OnClick(R.id.button_upload)
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
        ButterKnife.bind(this);

        String macAddress = getIntent().getStringExtra(getString(R.string.extra_mac_address));
        mBluegigaPeripheral = new BluegigaPeripheral(BlueteethManager.with(this).getPeripheral(macAddress));

        // Prepare file dropdown
        mFileDropdownAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        mFileDropdownAdapter.add(getResources().getResourceName(R.raw.ota_0_1_0));
        mFileDropdownAdapter.add(getResources().getResourceName(R.raw.ota_0_1_1));
        mFileDropdownSpinner.setAdapter(mFileDropdownAdapter);
    }

    // Take a file and get a byte array from it
    private byte[] getFileBytes() {
        String selectedFile = (String) mFileDropdownSpinner.getSelectedItem();
        if (selectedFile == null || selectedFile.isEmpty()) {
            Timber.d("No file selected in dropdown...");
            Toast.makeText(getApplicationContext(), R.string.no_file_selected, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), R.string.io_exception, Toast.LENGTH_SHORT).show();
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
