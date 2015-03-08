package com.robotpajamas.android.ble113_ota.ui;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.widget.ListView;

import com.robotpajamas.android.ble113_ota.R;
import com.robotpajamas.android.ble113_ota.Blueteeth.BLEService;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import timber.log.Timber;


public class MainActivity extends ActionBarActivity {

    private static final int REQ_BLUETOOTH_ENABLE = 1000;

    private BLEService mBLEService;
    private boolean mBound = false;

    private IntentFilter mIntentFilter;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();
    private DeviceListAdapter mDeviceListAdapter;

    private ProgressDialog mProgressDialog;

    @InjectView(R.id.swipeContainer)
    SwipeRefreshLayout mSwipeLayout;

    @InjectView(R.id.listView)
    ListView mDeviceListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // If BLE support isn't there, quit the app
        checkBluetoothSupport();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(this.getString(R.string.progress_connecting));

        mDeviceListAdapter = new DeviceListAdapter(this, mDeviceList);
        mDeviceListAdapter.setItems(mDeviceList);
        mDeviceListView.setAdapter(mDeviceListAdapter);

        mDeviceListView.setOnItemClickListener((parent, view1, position, id) -> {
            mProgressDialog.show();
            mBLEService.connect(mDeviceListAdapter.getItem(position));
        });
    }

    private void checkBluetoothSupport() {
        // Check for BLE support - also checked from Android manifest.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            exitApp("No BLE Support...");
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            exitApp("No BLE Support...");
        }

        //noinspection ConstantConditions
        if (!btAdapter.isEnabled()) {
            enableBluetooth();
        } else {
            connectToBLEService();
        }
    }

    private void exitApp(String reason) {
        // Something failed, exit the app and send a toast as to why
        Timber.e(reason);
        Crouton.makeText(this, reason, Style.ALERT).show();
        finish();
    }

    private void enableBluetooth() {
        // Ask user to enable bluetooth if it is currently disabled
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_BLUETOOTH_ENABLE);
    }

    private void connectToBLEService() {
        // Bind this service as early as possible
        Intent intent = new Intent(this, BLEService.class);
        mBound = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(getIntentFilter());
        LocalBroadcastManager.getInstance(this).registerReceiver(mBLEReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBLEReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBound) {
            unbindService(mConnection);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_BLUETOOTH_ENABLE) {
            // Re-check BLE support
            checkBluetoothSupport();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            BLEService.BLEBinder b = (BLEService.BLEBinder) binder;
            mBLEService = b.getService();
            if (!mBLEService.initialize()) {
                exitApp("Could not initialize Bluetooth...");
            } else {
                // Setup the swipe down to scan for devices, and kick off an initial scan
                mSwipeLayout.setOnRefreshListener(mBLEService::beginScan);
                mBLEService.beginScan();
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
            mIntentFilter.addAction(BLEService.ACTION_BEGIN_SCAN);
            mIntentFilter.addAction(BLEService.ACTION_END_SCAN);
            mIntentFilter.addAction(BLEService.ACTION_DEVICE_DISCOVERED);
            mIntentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
            mIntentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
            mIntentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
            mIntentFilter.addAction(BLEService.ACTION_GATT_CONNECTION_STATE_ERROR);
        }
        return mIntentFilter;
    }


    private final BroadcastReceiver mBLEReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BLEService.ACTION_DEVICE_DISCOVERED:
                    // Add discovered devices to the list
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BLEService.DISCOVERED_DEVICE);
                    mDeviceList.add(bluetoothDevice);
                    mDeviceListAdapter.setItems(mDeviceList);
                    break;

                case BLEService.ACTION_BEGIN_SCAN:
                    // Make list not clickable during scan
                    mDeviceListView.setEnabled(false);
                    mDeviceList.clear();
                    break;

                case BLEService.ACTION_END_SCAN:
                    // Re-enable UI
                    mDeviceListView.setEnabled(true);
                    mSwipeLayout.setRefreshing(false);
                    break;

                case BLEService.ACTION_GATT_CONNECTED:
                    // Click to connect, then show it's connected
                    Timber.d("Connected. Dismiss progress dialog...");
                    mProgressDialog.dismiss();

                    // Start the OTA activity
                    final Intent connectedIntent = new Intent(MainActivity.this, OtaActivity.class);
                    startActivity(connectedIntent);
                    break;

                case BLEService.ACTION_GATT_CONNECTION_STATE_ERROR:
                    Timber.d("Connection Error. Dismiss progress dialog...");
                    mProgressDialog.dismiss();
                    break;
            }
        }
    };
}