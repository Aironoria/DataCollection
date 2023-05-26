package com.example.datacollection;
//classic blue tooth
//https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-data

//ble
//https://github.com/itanbp/android-ble-peripheral-central/blob/master/app/src/main/java/itan/com/bluetoothle/PeripheralRoleActivity.java
//https://proandroiddev.com/android-bluetooth-low-energy-building-chat-app-with-ble-d2700956715b
//https://qiita.com/poruruba/items/f74c447dd61be26b4ac2

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.example.datacollection.data.ACC;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@SuppressLint("MissingPermission")
public class BluetoothServer {
    //macbook  5C:E9:1E:BB:A8:3C

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothDevice macbook;

    private BluetoothGattService service;
    private BluetoothGattCharacteristic accCharacteristic;
    private BluetoothGattCharacteristic gyroCharacteristic;

    private UUID serviceUUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private UUID accCharacteristicUUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private UUID gyroCharacteristicUUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");

    private Context parentContext;

    public BluetoothServer(Context context) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        parentContext = context;
        if (bluetoothAdapter != null) {
            prepareBle();
            startAdvertising();

        } else {
            Log.e("BluetoothServer", "bluetoothAdapter is null");
        }


    }

    private void startAdvertising() {
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTimeout(0)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(serviceUUID))
                .setIncludeDeviceName(true)
                .build();

        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        bluetoothLeAdvertiser.startAdvertising(advertiseSettings,advertiseData,scanResponseData, new AdvertiseCallback(){
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d("BluetoothServer", "advertise start success");
            }
            @Override
            public void onStartFailure(int errorCode) {
                Log.d("BluetoothServer", "advertise start failure");
            }
        });
    }

    private void prepareBle() {
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (bluetoothLeAdvertiser == null) {
            Log.e("BluetoothServer", "bluetoothLeAdvertiser is null");
            return;
        }
        //        if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }

        bluetoothGattServer = bluetoothManager.openGattServer(parentContext, gattServerCallback);
        service = new BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        accCharacteristic = new BluetoothGattCharacteristic(accCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"),
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        accCharacteristic.addDescriptor(descriptor);
        service.addCharacteristic(accCharacteristic);
        bluetoothGattServer.addService(service);


    }

    public Boolean sendAccData(ACC acc) {
        if (bluetoothGattServer == null) {
            Log.e("BluetoothServer", "bluetoothGattServer is null");
            return false;
        }
        if (accCharacteristic == null) {
            Log.e("BluetoothServer", "accCharacteristic is null");
            return false;
        }

        if (macbook == null) {
            Log.e("BluetoothServer", "macbook is null");
            return false;
        }

        ByteBuffer bb = ByteBuffer.allocate(24);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putDouble(acc.getX());
        bb.putDouble(acc.getY());
        bb.putDouble(acc.getZ());

        accCharacteristic.setValue(bb.array());

//        accCharacteristic.setValue("hello".getBytes(StandardCharsets.UTF_8));
        bluetoothGattServer.notifyCharacteristicChanged(macbook, accCharacteristic, false);
        return true;
    }

    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status,
                                            int newState) {

            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                macbook = device;
            }
            Log.d("BluetoothServer", "onConnectionStateChange: " + status + " -> " + newState);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d("BluetoothServer", "onCharacteristicReadRequest: " + characteristic.getUuid());
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }


        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            bluetoothGattServer.sendResponse(device, 0, BluetoothGatt.GATT_SUCCESS,
                    0, null);
        }
    };

}
