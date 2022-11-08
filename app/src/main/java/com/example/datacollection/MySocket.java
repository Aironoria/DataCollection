package com.example.datacollection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                    socket =new Socket("192.168.0.155", 8081);
//                    socket =new Socket("172.24.195.52", 8081);
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


    public void sendData(String data){
//      new Thread(new Runnable() {
//          @Override
//          public void run() {
//              try {
//                  BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
//                          socket.getOutputStream()));
//                  writer.write(data.replace("\n", " ") +"\n");
//                  writer.flush();
//              } catch (IOException e) {
//                  e.printStackTrace();
//              }
//          }
//      }).start();

       es.execute(new Runnable() {
          @Override
          public void run() {
              try {
                  PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                          socket.getOutputStream())),true);
                  printWriter.println(data.replace("\n", " "));

              } catch (IOException e) {
                  e.printStackTrace();
              }
          }
      });


    }

}
