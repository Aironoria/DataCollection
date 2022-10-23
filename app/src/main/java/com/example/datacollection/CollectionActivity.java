package com.example.datacollection;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.example.datacollection.data.ACC;
import com.example.datacollection.data.GYRO;

import java.util.ArrayList;
import java.util.List;

public class CollectionActivity extends Activity {

    private SensorManager sensorManager;
    private SensorEventListener accListener;
    private SensorEventListener gyroListener;
    private Sensor acc;
    private Sensor gyro;
    private int count =0;
    private ArrayList<GYRO> gyroData= new ArrayList();
    private ArrayList<ACC> accData = new ArrayList<>();
    List<Sensor> deviceSensors;
    final String TOUCH_DOWN = "touchdown";
    final String CLICK = "click";
    final String TOUCH_UP = "touchup";
    final String IDLE = "idle";

    private Button recordButton;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        this.sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        recordButton =findViewById(R.id.record_btn);
        mTextView = findViewById(R.id.display_text);
        deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);



        recordButton.setOnClickListener(v->{
            //get data
            //save file
            gyro= sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (acc !=null){
                sensorManager.registerListener(accListener,acc,10000);
                sensorManager.registerListener(gyroListener,gyro,10000);

            }else   {
                Log.d("error", "NO ACC");

            }
//            Utils.saveFile("a.csv","hello");
//            int time = 2;
            int time = 2;
            new Thread(){
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(time *1000 +1500 *1);
                        sensorManager.unregisterListener(accListener,acc);
                        sensorManager.unregisterListener(gyroListener,gyro);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTextView.setText("end");
                            }
                        });

                        trim(gyroData,time *100);
                        trim(accData, time *100);

                        saveToFile("10-23-0"+"/"+TOUCH_DOWN);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        });

        this.accListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (count%150 ==0){
                    runOnUiThread(()->{
                        mTextView.setText(String.valueOf(count/150));
                    });
                }
                accData.add(new ACC(event.values[0], event.values[1], event.values[2]));
//                mTextView.setText("[x:" + event.values[0]  ", y:" + event.values[1] + ", z:" + event.values[2] + "]");
                count++;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        this.gyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
//                mTextView.setText("[x:" + event.values[0] +"]");
                gyroData.add(new GYRO(event.values[0], event.values[1], event.values[2]));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

    }

    private void saveToFile(String dir){
//        dir = Utils.getDir(dir);
        String title=Utils.getTime();
        Utils.saveFile(dir +"/"+title+".csv", "ax,ay,az,gx,gy,gz\n");

        StringBuilder res= new StringBuilder();
        for (int i =0; i<accData.size();i++){
            res.append(accData.get(i).toString())
                    .append(gyroData.get(i).toString())
                    .append("\n");
        }

        Utils.saveFile(dir +"/"+title+".csv", res.toString());
//        Utils.saveFile(dir + "/" +"acc.csv", accData.stream()
//                .map(Object::toString)
//                .collect(Collectors.joining("\n")));
        accData.clear();
        gyroData.clear();
        count=0;
    }

    private void trim(ArrayList list, int length){
        while (list.size() > length){
            list.remove(0);
        }
    }



    String getLabel(String movement, String finger, String surface, String support){
        return movement +"_" + surface+"_" + finger +"_" + support;

    }

}