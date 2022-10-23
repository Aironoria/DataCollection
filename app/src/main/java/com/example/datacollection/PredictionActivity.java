package com.example.datacollection;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.google.android.gms.common.util.ArrayUtils;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class PredictionActivity extends Activity {
    private SensorManager sensorManager;
    private Button predictionButton;
    private TextView textView ;


    private ArrayList<GYRO> gyroData= new ArrayList();
    private ArrayList<ACC> accData = new ArrayList<>();
    private int windowSize = 49;
    private int sliding = 10;

    private Module model;
    String predictedResult;
    boolean useGyro = true;

    int inputChannel = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);
        predictionButton = findViewById(R.id.predict_btn);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        textView = findViewById(R.id.display_text);

        predictionButton.setOnClickListener(view -> { registerSensorListener();});
        try {
            model = LiteModuleLoader.load(assetFilePath(this, "10-12_augmented.ptl"));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        useGyro =false;

        if (useGyro){
            inputChannel =6;
        }else {
            inputChannel=3;
        }

    }

    private void predict(){
        long startTime = System.currentTimeMillis();
        float[] inputArray = fftAndIntegrateArray();
        Log.d("FFT TIME", String.valueOf(System.currentTimeMillis() - startTime));

        startTime = System.currentTimeMillis();
        Tensor input =Tensor.fromBlob(inputArray, new long[]{1,inputChannel,7,7});
        final Tensor outputTensor = model.forward(IValue.from(input)).toTensor();
        // getting tensor content as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();
//        Log.d("OUTPUT", Arrays.toString(scores));
        // searching for the index with maximum score
        float maxScore = -Float.MAX_VALUE;
        int maxScoreIdx = -1;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxScoreIdx = i;
            }
        }
//        String[]  labels = { "click", "down","nothing","up"};
        String[]  labels = { "touch","nontouch"};

        predictedResult = labels[maxScoreIdx];
        Log.d("PREDICT TIME", String.valueOf(System.currentTimeMillis() - startTime));

        runOnUiThread(()->{
            textView.setText(predictedResult);
        });
    }



    private void registerSensorListener(){
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),10000);
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),10000);

    }

    private void unregisterSensorListener(){
        sensorManager.unregisterListener(sensorListener);
    }

    private SensorEventListener sensorListener = new SensorEventListener() {
        private int count =0;
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            switch (sensorEvent.sensor.getType()){
                case   Sensor.TYPE_ACCELEROMETER:
                    accData.add(new ACC(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
                    count++;
                    break;
                case  Sensor.TYPE_GYROSCOPE:
                    gyroData.add(new GYRO(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
                    break;
            }

            if(accData.size() >= windowSize & gyroData.size() >= windowSize & count >= sliding){
                Utils.trim(accData,windowSize);
                Utils.trim(gyroData,windowSize);
                predict();
                count = 0;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    static float [] dft(float[] inreal) {
        int n = inreal.length;
        float[] outreal = new float[n];
        float[] outimag = new float[n];
        float[] res = new float[n];
        for (int k = 0; k < n; k++) {  // For each output element
            double sumreal = 0;
            double sumimag = 0;
            for (int t = 0; t < n; t++) {  // For each input element
                double angle = 2 * Math.PI * t * k / n;
                sumreal +=  inreal[t] * Math.cos(angle) + 0 * Math.sin(angle);
                sumimag += -inreal[t] * Math.sin(angle) + 0 * Math.cos(angle);
            }
            outreal[k] = (float) sumreal;
            outimag[k] = (float) sumimag;
            res[k] = (float) Math.sqrt(Math.pow(sumreal ,2) + Math.pow(sumimag,2));
        }
        return res;
    }

    private float[] fftAndIntegrateArray(){
        float[] res = new float[windowSize *inputChannel];
        float[] accX = new float[windowSize];
        float[] accY = new float[windowSize];
        float[] accZ = new float[windowSize];
        float[] gyroX = new float[windowSize];
        float[] gyroY = new float[windowSize];
        float[] gyroZ = new float[windowSize];

        for( int i =0; i< windowSize; i++){
            ACC accItem = accData.get(i);
            GYRO gyroItem = gyroData.get(i);
            accX[i] = (float) accItem.getX();
            accY[i] = (float) accItem.getY();
            accZ[i] = (float) accItem.getZ();
           if (useGyro){
               gyroX[i] = (float) gyroItem.getGx();
               gyroY[i] = (float) gyroItem.getGy();
               gyroZ[i] = (float) gyroItem.getGz();
           }
        }

        accX = dft(accX);
        accY = dft(accY);
        accZ =dft(accZ);
        if (useGyro){
            gyroX =dft(gyroX);
            gyroY = dft(gyroY);
            gyroZ = dft(gyroZ);
        }

        for (int i =0; i < windowSize; i++){
            ACC accItem = accData.get(i);
            GYRO gyroItem = gyroData.get(i);

            res[ windowSize *0 + i] =accX[i];
            res[ windowSize *1 + i] =accY[i];
            res[ windowSize *2 + i] =accZ[i];
            if (useGyro){
                res[ windowSize *3 + i] =gyroX[i];
                res[ windowSize *4 + i] =gyroY[i];
                res[ windowSize *5 + i] =gyroZ[i];
            }
        }


        return res;
    }


    private float[] integrateArray(){

        float[] res = new float[windowSize *6];

        for (int i =0; i < windowSize; i++){
            ACC accItem = accData.get(i);
            GYRO gyroItem = gyroData.get(i);

            res[ windowSize *0 + i] =(float) accItem.getX();
            res[ windowSize *1 + i] =(float) accItem.getY();
            res[ windowSize *2 + i] =(float) accItem.getZ();
            res[ windowSize *3 + i] =(float) gyroItem.getGx();
            res[ windowSize *4 + i] =(float) gyroItem.getGy();
            res[ windowSize *5 + i] =(float) gyroItem.getGz();
        }
        return res;
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }


}