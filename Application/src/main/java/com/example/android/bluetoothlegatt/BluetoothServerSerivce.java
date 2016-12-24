package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by chthang on 23/12/2016.
 */

public class BluetoothServerSerivce extends Service{

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        BluetoothServerSerivce getService() {
            return BluetoothServerSerivce.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
