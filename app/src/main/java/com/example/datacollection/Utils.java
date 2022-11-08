package com.example.datacollection;


import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Stack;

public class Utils {


    public static String getDir(String dir){
        String download_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        File file = new File (download_dir,dir);

        if (!file.exists()){
            file.mkdirs();
        }
        String subDir;
        switch (file.list().length){
            case 0 : subDir = "move";break;
            case 1: subDir = "swipe";break;
            case 2 : subDir ="pinch";break;
            default: subDir = String.valueOf(file.list().length);
        }
        return dir +"/" + "click";
//        return dir +"/" + subDir ;
    }

    public static String getTime(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM_dd_HH_mm_ss");// HH:mm:ss
        Date date = new Date(System.currentTimeMillis());
       return simpleDateFormat.format(date);
    }

    public static void saveFile(String title,String content){

        String download_dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        try {
            File file = new File(download_dir +"/"+ title);

            if (!file.exists()) {
                File parent = file.getParentFile();
                if (!parent.exists())
                    parent.mkdirs();
                file.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(file, true);
            outStream.write(content.getBytes());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trim(ArrayList list, int length){
        while (list.size() > length){
            list.remove(0);
        }
    }
    private static SimpleDateFormat  simpleDateFormat= new SimpleDateFormat("HH:mm:ss:SSS");

    public static String getTimeInMillSecond(){
        return simpleDateFormat.format(new Date());
    }

}

