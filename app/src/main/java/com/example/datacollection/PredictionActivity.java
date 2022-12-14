package com.example.datacollection;

import android.app.Activity;
import android.content.Context;
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

public class PredictionActivity extends Activity {
    private SensorManager sensorManager;
    private Button predictionButton;
    private TextView textView ;


    private ArrayList<GYRO> gyroData= new ArrayList();
    private ArrayList<ACC> accData = new ArrayList<>();
    private int windowSize = 65;
    private int sliding = 20;

    private Module model;
    String predictedResult;
    boolean useGyro = true;
    String lastState = "nothing";
    int inputChannel = 6;

    final String TOUCH_DOWN = "touchdown";
    final String CLICK = "click";
    final String TOUCH_UP = "touchup";
    final String IDLE = "nothing";
    final String SWIPE_UP="swipe_up";
    final String SWIPE_DOWN="swipe_down";
    final String SWIPE_LEFT="swipe_left";
    final String SWIPE_RIGHT="swipe_right";
    final String PINCH="pinch";
    final String SPREAD="spread";
    final String ZOOM_IN="zoom_in";
    final String ZOOM_OUT="zoom_out";
//    String[]  labels = {"click", "nothing",PINCH,SPREAD,SWIPE_DOWN,SWIPE_LEFT,SWIPE_RIGHT,SWIPE_UP,"down","up"};
//    String[]  labels = {SWIPE_DOWN,SWIPE_UP,"click",SPREAD,SWIPE_RIGHT,PINCH,SWIPE_LEFT,"nothing", "nothing","up"};
    //String[] labels ={"scroll_down","click","scroll_up","spread","swipe_right","pinch","swipe_left","touchdown","nothing","touchup"};
    String[] labels ={"scroll_down","swipe_down","swipe_up","click","scroll_up","spread","swipe_right","pinch","swipe_left","touchdown","nothing","touchup"};
    private int[] label_continous_count = new int[]{0,0,0,0,0,0,0,0,0,0,0,0};
    int a =0;
    private int[] threshold = new int[]{a,a,a,a,a,a,a,a,a,a,a,a};
    StringBuilder predictedResultString = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);
        predictionButton = findViewById(R.id.predict_btn);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        textView = findViewById(R.id.display_text);
        textView.setKeepScreenOn(true);
        predictionButton.setOnClickListener(view -> {


            new Thread(){
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(0*1000);
                        registerSensorListener();
//                        Thread.sleep(20 *1000);
//                        unregisterSensorListener();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        });
        try {
            model = LiteModuleLoader.load(assetFilePath(this, "12-04_len65.ptl"));
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
        float[] inputArray = integrateForRNN();
        Log.d("FFT TIME", String.valueOf(System.currentTimeMillis() - startTime));
        int sequence_len =5;
        startTime = System.currentTimeMillis();
//        Tensor input =Tensor.fromBlob(inputArray, new long[]{1,inputChannel,7,7});
        Tensor input =Tensor.fromBlob(inputArray, new long[]{1,windowSize/sequence_len,inputChannel * sequence_len});
        System.out.println();
        final Tensor outputTensor = model.forward(IValue.from(input)).toTensor();
        // getting tensor content as java array of floats
        final float[] scores = outputTensor.getDataAsFloatArray();
//        Log.d("OUTPUT", Arrays.toString(scores));
        // searching for the index with maximum score
        int[] sortedIndices = sortAndReturnIndices(scores);
        int maxScoreIdx= sortedIndices[0];
        predictedResult = labels[maxScoreIdx];
//
        if (scores[maxScoreIdx] > 0.9)
            predictedResult = labels[maxScoreIdx];
        else
            predictedResult = lastState;


//        for (int i=0; i< scores.length;i++){
//            predictedResultString.append(String.format("%-14s",labels[sortedIndices[i]]) )
//                    .append(":")
//                    .append(String.format("%.4f",scores[sortedIndices[i]]))
//                    .append(",");
//        }
//        predictedResultString.append("\n");

//
//        if (predictedResult.equals(CLICK) | predictedResult.equals(TOUCH_DOWN) | predictedResult.equals(TOUCH_UP)){
//
//        }else{
//            if (predictedResult.equals(IDLE)){
//
//            }
//            else if (!lastState.equals(IDLE)){
//                predictedResult = lastState;
//            }
//        }


        if (!predictedResult.equals(IDLE)){
            if (predictedResult.equals(TOUCH_UP)& lastState.equals(CLICK))
                predictedResult = lastState;
            else if (!lastState.equals(IDLE)){
                predictedResult = lastState;
            }
        }

//        if (predictedResult.equals(lastState))
//            label_continous_count[maxScoreIdx]++;
//        else
//            Arrays.fill(label_continous_count, 0);



       if (label_continous_count[maxScoreIdx] >=threshold[maxScoreIdx])
           runOnUiThread(()->{
               textView.setText(predictedResult);
           });
        lastState = predictedResult;
        Log.d("PREDICT TIME", String.valueOf(System.currentTimeMillis() - startTime));
    }



    private void registerSensorListener(){
        clearData();
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),10000);
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),10000);

    }

    private void unregisterSensorListener(){
        sensorManager.unregisterListener(sensorListener);
        String title=Utils.getTime();
        Utils.saveFile( "predictedResult/"+title+".csv",   predictedResultString.toString());
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

    private float[] integrateForRNN(){
        float[] res = new float[windowSize *inputChannel];
        double[] mean ={0.88370824, -1.0719419, 9.571041, -0.0018323545, -0.0061315685, -0.0150832655};
        double[]std ={0.32794556, 0.38917893, 0.35336846, 0.099675156, 0.117989756, 0.06230596};

        for (int i =0; i < windowSize; i++){
            ACC accItem = accData.get(i);
            GYRO gyroItem = gyroData.get(i);

            res[i*6 +0] =(float)  ((accItem.getX() -mean[0]) /std[0]);
            res[i*6 +1] =(float)  ((accItem.getY() -mean[1]) /std[1]);
            res[i*6 +2] =(float)  ((accItem.getZ() -mean[2]) /std[2]);
            res[i*6 +3] =(float)((gyroItem.getGx() -mean[3]) /std[3]);
            res[i*6 +4] =(float)((gyroItem.getGy() -mean[4]) /std[4]);
            res[i*6 +5] =(float)((gyroItem.getGz() -mean[5]) /std[5]);
        }
        return res;
    }

    private void clearData(){
        Utils.trim(accData,0);
        Utils.trim(gyroData,0);
        predictedResultString.setLength(0);
        runOnUiThread(()->{
            textView.setText("Predicting");
        });
    }

    private float[] integrateArray(){

        float[] res = new float[windowSize *inputChannel];
        double[] mean ={0.88370824, -1.0719419, 9.571041, -0.0018323545, -0.0061315685, -0.0150832655};
        double[]std ={0.32794556, 0.38917893, 0.35336846, 0.099675156, 0.117989756, 0.06230596};

        for (int i =0; i < windowSize; i++){
            ACC accItem = accData.get(i);
            GYRO gyroItem = gyroData.get(i);

            res[ windowSize *0 + i] =(float)  ((accItem.getX() -mean[0]) /std[0]);
            res[ windowSize *1 + i] =(float)  ((accItem.getY() -mean[1]) /std[1]);
            res[ windowSize *2 + i] =(float)  ((accItem.getZ() -mean[2]) /std[2]);
            res[ windowSize *3 + i] =(float)((gyroItem.getGx() -mean[3]) /std[3]);
            res[ windowSize *4 + i] =(float)((gyroItem.getGy() -mean[4]) /std[4]);
            res[ windowSize *5 + i] =(float)((gyroItem.getGz() -mean[5]) /std[5]);
        }
        return res;
    }
    private  int[] sortAndReturnIndices(float[] array){
        int[] res = new int[array.length];
        for (int i =0; i< res.length;i++){
            res[i]= i;
        }

        for(int i =0; i< res.length;i++) {
            for (int j = res.length-1; j >i; j--) {
                if (array[res[j]] > array[res[j - 1]]) {
                    int temp = res[j];
                    res[j] = res[j - 1];
                    res[j - 1] = temp;
                }
            }
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