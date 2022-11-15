package com.example.datacollection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MySocket {
    private static MySocket instance;
    public Socket socket;
    private static ExecutorService es = Executors.newSingleThreadExecutor();
    private MySocket (){
//        es.execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    socket =new Socket("192.168.0.155", 8081);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //home
//                    socket =new Socket("192.168.0.155", 8081);
//
//                    lab
//                    socket =new Socket("172.24.195.52", 8082);
//
//                    personal hotspot
                    socket = new Socket("172.20.10.5",8081);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public static MySocket getInstance(){
        if (instance ==null){
            instance = new MySocket();
        }
        return instance;
    }

    private ArrayList<String> data = new ArrayList<>();


    public void sendData(String item){
        data.add(item);
        if(data.size() >= 1){
            List<String> res = new CopyOnWriteArrayList<>(data);
            data.clear();
            es.execute(new Runnable() {
                @Override
                public void run() {
                    try {

                        PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                                socket.getOutputStream())),true);
                        printWriter.println(res.stream().collect(Collectors.joining("\n")));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
//

//        es.execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//
//                    PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
//                            socket.getOutputStream())),true);
//                    printWriter.println(item);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

    }

}
