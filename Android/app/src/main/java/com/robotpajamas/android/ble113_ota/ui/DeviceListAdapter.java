package com.robotpajamas.android.ble113_ota.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.robotpajamas.android.ble113_ota.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class DeviceListAdapter extends BaseAdapter {

    private final LayoutInflater mLayoutInflater;
    private List<BluetoothDevice> mDevices;
    private Comparator<BluetoothDevice> mComparator = (lhs, rhs) -> lhs.getAddress().compareTo(rhs.getAddress());

    public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> deviceList) {
        mLayoutInflater = LayoutInflater.from(context);
        mDevices = deviceList;
    }

    public void setItems(List<BluetoothDevice> devices) {
        mDevices = devices;
        Collections.sort(mDevices, mComparator);
        notifyDataSetChanged();
    }

    public List<BluetoothDevice> getItems() {
        return mDevices;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DeviceItemHolder holder;

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_device_list, parent, false);
            holder = new DeviceItemHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (DeviceItemHolder) convertView.getTag();
        }

        BluetoothDevice device = getItem(position);
        String name = device.getName();
        if (name == null || name.isEmpty()) {
            name = "[No advertised name]";
        }

        holder.name.setText(name);
        holder.macAddress.setText(device.getAddress());

        return convertView;
    }

    public static class DeviceItemHolder {
        @InjectView(R.id.device_name)
        TextView name;

        @InjectView(R.id.device_rssi)
        TextView rssi;

        @InjectView(R.id.device_mac_address)
        TextView macAddress;

        public DeviceItemHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
