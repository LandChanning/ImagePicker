package com.lcn.imagepicker.manager;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by Jc on 2015/9/25.
 * temp file manager
 */
public class  TempManager {

    private static final String TEMP_DIR_NAME = "ImagePickerTemp";
    private static final String TEMP_FILE_EXTENSION = "jpg";

    public static File obtainFile(Context context) {
        File tempDir = context.getExternalFilesDir(TEMP_DIR_NAME);
        if(tempDir != null) {
            return new File(tempDir, System.currentTimeMillis() + "." + TEMP_FILE_EXTENSION);
        } else {
            return null;
        }
    }

    public static void clearTemp(Context context) {
        File tempDir = context.getExternalFilesDir(TEMP_DIR_NAME);
        if(tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            for (File file : files) {
                if (file.exists()) file.delete();
            }
        }
    }
}
