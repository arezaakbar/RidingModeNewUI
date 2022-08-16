package com.tugasakhir.ridingmode;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SelectDeviceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device);

        RecyclerView recyclerView = findViewById(R.id.deviceList);

        //Inisialisasi Bluetooth Adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Fetching list adapter dari cache
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<Object> deviceList = new ArrayList<>();
        for (BluetoothDevice device: pairedDevices){
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress();
            DeviceInfoModel deviceInfoModel = new DeviceInfoModel(deviceName, deviceHardwareAddress);
            deviceList.add(deviceInfoModel);
        }

        //Setting Up Recycle View
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Koneksi ke data source
        ListAdapter listAdapter = new ListAdapter(this,deviceList);
        recyclerView.setAdapter(listAdapter);

    }

}