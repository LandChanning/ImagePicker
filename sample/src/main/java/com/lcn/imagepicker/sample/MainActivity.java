package com.lcn.imagepicker.sample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioGroup;

import com.bumptech.glide.Glide;
import com.lcn.imagepicker.ImagePicker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSION_CODE = 1;
    private static final int REQ_IMAGE_PICKER_CODE = 2;
    private GridView gridView;
    private ImagePicker imagePicker;
    private RadioGroup rg;
    private EditText etPickCount;
    private CheckBox cbCrop;
    private CheckBox cbShowCapture;
    private CheckBox cbCompress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gridView = (GridView) findViewById(R.id.grid_view);
        rg = (RadioGroup)findViewById(R.id.rg);
        etPickCount = (EditText)findViewById(R.id.et_pick_count);
        cbCrop = (CheckBox)findViewById(R.id.cb_crop);
        cbShowCapture = (CheckBox)findViewById(R.id.cb_show_capture);
        cbCompress = (CheckBox)findViewById(R.id.cb_compress);
    }

    protected boolean requestPermission(Context context, String[] permissions, int reqCode) {
        boolean flag = false;
        if (Build.VERSION.SDK_INT >= 23) {
            for (String permission : permissions) {
                int checkPermission = ContextCompat.checkSelfPermission(context, permission);
                if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                requestPermissions(permissions, reqCode);
            }
        }
        return flag;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_PERMISSION_CODE:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pick();
                }
                break;
        }
    }

    public void onClick(View view) {
        if(!requestPermission(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISSION_CODE)) {
            pick();
        }
    }

    private void pick() {
        imagePicker = new ImagePicker.Builder()
                .setPickMode(rg.getCheckedRadioButtonId() == R.id.rg_single_pick ? ImagePicker.SINGLE_MODE : ImagePicker.MULTIPLY_MODE)
                .setPickCount(Integer.valueOf(etPickCount.getText().toString()))
                .setCompress(cbCompress.isChecked())
                .setCrop(cbCrop.isChecked())
                .setShowCapture(cbShowCapture.isChecked())
                .start(this, REQ_IMAGE_PICKER_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode != RESULT_OK) return;
        if(requestCode != REQ_IMAGE_PICKER_CODE) return;
        ArrayList<String> imgs = data.getStringArrayListExtra(ImagePicker.RESULT_DATA);
        gridView.setAdapter(new MyAdapter(this, R.layout.item, imgs));
    }

    private static class MyAdapter extends ArrayAdapter<String> {

        List<String> data;

        public MyAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            data = objects;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.item, parent, false);
            Glide.with(getContext())
                    .load(data.get(position))
                    .placeholder(R.drawable.image_fail)
                    .error(R.drawable.image_fail)
                    .centerCrop()
                    .into(imageView);
            return imageView;
        }
    }

    @Override
    protected void onDestroy() {
        imagePicker.clear(getApplicationContext());
        super.onDestroy();
    }
}
