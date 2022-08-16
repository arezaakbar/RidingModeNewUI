package com.tugasakhir.ridingmode;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String deviceAddress = null;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static CreateConnectThread createConnectThread;
    public static ConnectedThread connectedThread;

    private final static int CONNECTION_STATUS = 1;
    private final static int READ_MESSAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instansiasi UI
        final TextView textBluetoothStatus = findViewById(R.id.textBluetoothStatus);
        final TextView displayCurrentMode = findViewById(R.id.displayCurrentMode);
        Button connectButton = findViewById(R.id.connectButton);
        Button disconnectButton = findViewById(R.id.disconnectButton);
        ImageButton ecoButton = findViewById(R.id.ecoButton);
        ImageButton sportyButton = findViewById(R.id.sportyButton);
        ImageButton defaultButton = findViewById(R.id.defaultButton);

        //Connect Button
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        //Get informasi Device Address
        deviceAddress = getIntent().getStringExtra("deviceAddress");
        //Ketika Device Address telah ditemukan
        if (deviceAddress != null){
            textBluetoothStatus.setText("Connecting");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            createConnectThread.start();
        }

        //Handler
        handler = new Handler(Looper.getMainLooper()){

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CONNECTION_STATUS:
                        switch (msg.arg1){
                            case 1:
                                textBluetoothStatus.setText("Bluetooth Connected");
                                break;
                            case -1:
                                textBluetoothStatus.setText("Connection Failed");
                                break;
                        }
                        break;

                    //apabila message berisi data dari Arduino
                    case READ_MESSAGE:
                        String statusText = msg.obj.toString().replace("/n","");
                        displayCurrentMode.setText(statusText);
                        break;
                }
            }
        };

        //disconnect
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (createConnectThread != null){
                    createConnectThread.cancel();
                    textBluetoothStatus.setText("Bluetooth Disconnected");
                }
            }
        });

        //Eco Button
        ecoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String androidCmd = "w";
                connectedThread.write(androidCmd);
                displayCurrentMode.setText("ECO MODE TERPILIH");
            }
        });

        //Sporty Button
        sportyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String androidCmd = "s";
                connectedThread.write(androidCmd);
                displayCurrentMode.setText("SPORTY MODE TERPILIH");
            }
        });

        //Default Button
        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String androidCmd = "d";
                connectedThread.write(androidCmd);
                displayCurrentMode.setText("SPORTY MODE TERPILIH");
            }
        });
        ecoButton.setOnClickListener(v -> {
            //log ecoButton
            Log.i("Eco Button State", "Eco Mode Terpilih");

        });

        //Function sportyButton
        sportyButton.setOnClickListener(v -> {
            //log sportyButton
            Log.i("Sporty Button State", "Sporty Mode terpilih");
        });
    }

    /* ============= THREAD 1 ============ */
    public static class CreateConnectThread<connectedThread> extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address){
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp=null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();
            try {
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e){
                Log.e("Error Message",e.toString());
            }
            mmSocket = tmp;
        }

        public void run(){
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                handler.obtainMessage(CONNECTION_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException){
                try {
                    mmSocket.close();
                    handler.obtainMessage(CONNECTION_STATUS, -1,-1).sendToTarget();
                } catch (IOException closeException){}
                return;
            }

            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();

        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e){}
        }
    }

    /*================Thread 2==============*/
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e){}
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes = 0;
            while (true){
                try {
                    buffer[bytes] = (byte) mmInStream.read();
                    String arduinoMsg = null;
                    if (buffer[bytes] == '\n'){
                        arduinoMsg = new String(buffer, 0, bytes);
                        handler.obtainMessage(READ_MESSAGE,arduinoMsg).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String input){
            byte[] bytes = input.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e){}
        }
    }
}