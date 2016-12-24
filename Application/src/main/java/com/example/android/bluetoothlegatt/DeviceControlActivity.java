/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private final static int REQUEST_BT_PAIRING = 2;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_CHOOSEN_DEVICE = "CHOOSEN_DEVICE";
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothDevice device;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive with action: " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "BLE Connected");
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG, "BLE DisConnected");
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "onReceive - Action Data Available");
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                //Echo back received data, with something inserted
//                final byte[] rxBytes = bluetoothGattCharacteristicHM_10.getValue();
//                final byte[] insertSomething = {(byte)'\n'};
//                byte[] txBytes = new byte[insertSomething.length + rxBytes.length];
//                System.arraycopy(insertSomething, 0, txBytes, 0, insertSomething.length);
//                System.arraycopy(rxBytes, 0, txBytes, insertSomething.length, rxBytes.length);

//                if(bluetoothGattCharacteristicHM_10 != null){
//                    bluetoothGattCharacteristicHM_10.setValue(txBytes);
//                    mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
//                    mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10,true);
//                }

            }

            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);
            if (bondState == BluetoothDevice.BOND_BONDED ||
                    bondState == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "Bond_Bonded or Bond_none");
            }

            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action))
            {
                Log.d(TAG, "Request Pair");
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);

                if (type == BluetoothDevice.PAIRING_VARIANT_PIN) {
                   Log.d(TAG, "Paired");
                }
                else {
                    Log.d(TAG, "UnPaired");
                }
            }
        }
    };



    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.button_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        device = intent.getParcelableExtra(EXTRAS_CHOOSEN_DEVICE);
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //startBluetoothPairing(device);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        Log.d(TAG, "Display data");
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        return intentFilter;
    }

    public void onClickWrite(View v){
        if(mBluetoothLeService != null) {
            mBluetoothLeService.writeCustomCharacteristic("02");
        }
    }
    public void onClickWrite00(View v){
        if(mBluetoothLeService != null) {
            mBluetoothLeService.writeCustomCharacteristic("00");
        }
    }

    public void onClickRead(View v){
        mBluetoothLeService.showListServiceOfDevice();
        if(mBluetoothLeService != null) {
            //mBluetoothLeService.readCustomCharacteristic();
        }
    }

    private void startBluetoothPairing(BluetoothDevice device) {
        Intent pairingIntent = new Intent(BluetoothDevice.ACTION_PAIRING_REQUEST);
        pairingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION);
        pairingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivityForResult(pairingIntent, REQUEST_BT_PAIRING);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // this is called before user clicks Cancel or Pair in the dialog
        if (requestCode == REQUEST_BT_PAIRING) {
            if (resultCode == Activity.RESULT_OK) {  // 0 != -1
                Log.d("XXX", "Let#s pair!!!!"); // NOT CALLED
            }

            return;
        }
    }
}
