package com.example.datacollection;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.datacollection.data.ACC;
import com.example.datacollection.data.GYRO;
import com.example.datacollection.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends Activity {


    private ActivityMainBinding binding;
    private Button recordButton;
    private Button predictButton;
    private MySocket mySocket;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkPermission();

        predictButton = findViewById(R.id.predict_btn);
        recordButton = findViewById(R.id.record_btn);

        predictButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, PredictionActivity.class);
            startActivity(intent);
        });

        recordButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, CollectionActivity.class);
            startActivity(intent);
        });

            mySocket = MySocket.getInstance();
//            predictButton.setOnClickListener(v->{
//                 mySocket.sendData("hello");
//            });


    }




    protected void checkPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager. PERMISSION_GRANTED) {
            ActivityCompat. requestPermissions( this, new String[]{Manifest.permission. WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE },
                    1000);
        }
    }
}



