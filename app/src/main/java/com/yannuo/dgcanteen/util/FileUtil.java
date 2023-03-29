package com.yannuo.dgcanteen.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {

    //    把位图数据保存到指定路径的图片文件
    public static void saveImage(String path, Bitmap bitmap) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(path);
//            把位图文件压缩到文件输出流中
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (fileOutputStream != null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //    从指定路径的图片文件中读取位图数据
    public static Bitmap openImage(String path) {
        Bitmap bitmap = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(path);
            bitmap =  BitmapFactory.decodeStream(fileInputStream);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (fileInputStream != null){
                try {
                    fileInputStream.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

}
