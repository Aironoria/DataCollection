package com.example.datacollection;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.datacollection.data.ACC;
import com.example.datacollection.data.GYRO;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RealtimeActivity extends Activity {

    private SensorManager sensorManager;
    private SensorEventListener accListener;
    private SensorEventListener gyroListener;
    private Sensor acc;
    private Sensor gyro;
    private int count =0;
    private ArrayList<GYRO> gyroData= new ArrayList();
    private ArrayList<ACC> accData = new ArrayList<>();
    List<Sensor> deviceSensors;

    private Button startButton;
    private Button endButton;
    private ToggleButton toggleButton;

    private BluetoothServer bluetoothServer;

    @Override
    //圆通 swipe left 比较怪
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime);

        this.sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        toggleButton = findViewById(R.id.toggleButton);
        deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        toggleButton.setKeepScreenOn(true);

//        {sensorManager.unregisterListener(accListener,acc);
//                        sensorManager.unregisterListener(gyroListener,gyro);}

        bluetoothServer = new BluetoothServer(this);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    gyro= sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                    acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    count=0;
                    if (acc !=null){
                        sensorManager.registerListener(accListener,acc,10000);
                        sensorManager.registerListener(gyroListener,gyro,10000);

                    }else   {
                        Log.d("error", "NO ACC");

                    }
                } else {
                    sensorManager.unregisterListener(accListener,acc);
                    sensorManager.unregisterListener(gyroListener,gyro);
                }
            }
        });


        this.accListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                ACC acc  = new ACC(event.values[0], event.values[1], event.values[2]);
                bluetoothServer.sendAccData(acc);

//                accData.add(new ACC(event.values[0], event.values[1], event.values[2]));
//                bluetoothServer.sendAccData(new ACC(1,2,3));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        this.gyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
//                gyroData.add(new GYRO(event.values[0], event.values[1], event.values[2]));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

//        mySocket = MySocket.getInstance();
    }


    protected BluetoothAdapter getBluetoothAdapter() {

        BluetoothAdapter bluetoothAdapter;
        BluetoothManager bluetoothService = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));

        if (bluetoothService != null) {

            bluetoothAdapter = bluetoothService.getAdapter();

            // Is Bluetooth supported on this device?
            if (bluetoothAdapter != null) {

                // Is Bluetooth turned on?
                if (bluetoothAdapter.isEnabled()) {
                    /*
                    all the other Bluetooth initial checks already verified in MainActivity
                     */
                    return bluetoothAdapter;
                }
            }
        }

        return null;
    }



}