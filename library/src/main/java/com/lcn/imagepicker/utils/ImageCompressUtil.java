package com.lcn.imagepicker.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lcn.imagepicker.manager.TempManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Jc on 2016/2/25.
 */
public class ImageCompressUtil {

    public static String compressImageFile(Context context, String srcFilePath){
        Bitmap bitmap = compressImageFromFile(srcFilePath);
        File file = TempManager.obtainFile(context);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if(file.exists()){
            compressBmpToFile(bitmap, file);
        }
        if(bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return file.getAbsolutePath();
    }

    private static Bitmap compressImageFromFile(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;//只读边,不读内容
        BitmapFactory.decodeFile(srcPath, newOpts);

        int w = newOpts.outWidth;
        int h = newOpts.outHeight;

        int maxSize = 3000;
        int be = 1;
        if (w >= h && w > maxSize) {
            be = w / maxSize + ((w % maxSize) > 0 ? 1 : 0);
        } else if (w < h && h > maxSize) {
            be = h / maxSize + ((h % maxSize) > 0 ? 1 : 0);
        }

        newOpts.inSampleSize = be;//设置采样率
        newOpts.inPreferredConfig = Bitmap.Config.RGB_565;

        newOpts.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        return bitmap;
    }

    private static boolean compressBmpToFile(Bitmap bmp,File file){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 100;
        bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
        while (baos.toByteArray().length / 1024 > 300) {
            baos.reset();
            if(options > 10) {
                options -= 10;
            } else if(options > 5) {
                options -= 5;
            } else if(options > 2) {
                options -= 2;
            } else if(options >= 0) {
                options -= 1;
            }

            if(options >= 0) {
                bmp.compress(Bitmap.CompressFormat.JPEG, options, baos);
            } else {
                return false;
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
