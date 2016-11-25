package com.lcn.imagepicker.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lcn.imagepicker.R;
import com.lcn.imagepicker.manager.ImageGridAdapter;
import com.lcn.imagepicker.ImagePicker;
import com.lcn.imagepicker.manager.TempManager;
import com.lcn.imagepicker.model.ImageFolder;
import com.lcn.imagepicker.presenter.ImageGirdPresenter;
import com.lcn.imagepicker.utils.ImageCompressUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by JC on 2016/5/13.
 */
public class PickImgActivity extends AppCompatActivity implements IViewImageGrid {

    // crop img and others request
    public static final int REQ_CODE_TAKE_CAMERA_CROP = 110;
    public static final int REQ_CODE_TAKE_CAMERA_NO_CROP = 111;
    public static final int REQ_CODE_CROP_IMG = 112;

    // default crop size
    private static final int DEFAULT_CROP_SIZE = 600;

    private static final int MAX_IMAGE_SIZE_TO_CROP = 5000;

    private String mTempImagePath;
    private Uri mTempImageUri;

    private static final int DEFAULT_MAX_SELECT_COUNT = 9;
    
    private ImagePicker imagePicker;

    private RecyclerView rvGrid;
    private TextView mTVComplete;
    private TextView mTVCurrentDirName;
    private RelativeLayout rlBottom;
    private ProgressBar progressBar;

    private ImageGridAdapter adapter;
    private ImageFolder currentFolder;

    private ListImageDirPopupWindow mDirPopupWindow;

    private ImageGirdPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_image);
        // 禁止横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (Build.VERSION.SDK_INT >= 23) {
            int checkReadExternalPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkReadExternalPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
            } else {
                init();
            }
        } else {
            init();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("mTempImagePath", mTempImagePath);
        outState.putParcelable("mTempImageUri", mTempImageUri);
        outState.putParcelable(ImagePicker.PARCELED_OBJ, imagePicker);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTempImagePath = savedInstanceState.getString("mTempImagePath");
        mTempImageUri = savedInstanceState.getParcelable("mTempImageUri");
        imagePicker = savedInstanceState.getParcelable(ImagePicker.PARCELED_OBJ);
    }

    private void init() {
        initView();
        initEvent();
        initData();
    }

    private void initView() {

        imagePicker = getIntent().getParcelableExtra(ImagePicker.PARCELED_OBJ);
        
        rvGrid = (RecyclerView) findViewById(R.id.rv_image_picker);
        rvGrid.setLayoutManager(new GridLayoutManager(this, 3));
        mTVCurrentDirName = (TextView) findViewById(R.id.tv_current_dir_name);
        mTVComplete = (TextView) findViewById(R.id.tv_complete);
        rlBottom = (RelativeLayout) findViewById(R.id.rl_bottomBar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar_image_picker);

        if (ImagePicker.MULTIPLY_MODE == imagePicker.getPickMode()) {
            mTVComplete.setVisibility(View.VISIBLE);
        } else {
            mTVComplete.setVisibility(View.GONE);
        }
    }

    private void initData() {
        File tempFile = TempManager.obtainFile(getApplicationContext());
        if(tempFile == null) {
            Toast.makeText(this, R.string.sdcard_invalid, Toast.LENGTH_LONG).show();
        }
        mTempImagePath = tempFile.getAbsolutePath();
        mTempImageUri = Uri.fromFile(tempFile);

        presenter = new ImageGirdPresenter(this);
        presenter.init(getApplication().getContentResolver());
    }

    private void initEvent() {
        mTVCurrentDirName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDirPopupWindow != null) {
                    mDirPopupWindow.showAsDropDown(mTVCurrentDirName, 0, 0);
                    lightOff();
                }
            }
        });

        mTVComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imagePicker.isCrop() && imagePicker.getPickCount() == 1) {
                    crop(imagePicker.getSelectedImages().get(0));
                } else {
                    response2Act();
                }
            }
        });

    }

    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        rvGrid.setVisibility(View.INVISIBLE);
        rlBottom.setVisibility(View.INVISIBLE);
    }

    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
        rvGrid.setVisibility(View.VISIBLE);
        rlBottom.setVisibility(View.VISIBLE);
    }

    @Override
    public void showError(String errorMsg) {
//        Toast.makeText(this, "图片加载失败，请返回重试", Toast.LENGTH_SHORT).show();
        Toast.makeText(this, R.string.pick_image_no_num, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void initFolderPopup(List<ImageFolder> imageFolders) {
        mDirPopupWindow = new ListImageDirPopupWindow(this, imageFolders);
        mDirPopupWindow.setAnimationStyle(R.style.dir_popup_window_anim);
        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        mDirPopupWindow.setOnDirSelectedListener(new ListImageDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelect(int position) {
                presenter.refreshImageData(position);
            }
        });
    }

    @Override
    public void renderImageData(ImageFolder imageFolder) {
        currentFolder = imageFolder;
        if (adapter == null) {
            initAdapter();
        } else {
            adapter.changeData(currentFolder, mTempImageUri, imagePicker);
        }

        mTVCurrentDirName.setText(currentFolder.name);
    }

    private void initAdapter() {
        adapter = new ImageGridAdapter(this, currentFolder, mTempImageUri, imagePicker);
        // set adapter event
        adapter.setOnSelectedChangeListener(new ImageGridAdapter.OnSelectedChangeListener() {
            @Override
            public boolean onSelectedChange(int position) {
                boolean changeMask = false;

                String imagePath = currentFolder.images.get(position).path;

                ArrayList<String> selectedImages = imagePicker.getSelectedImages();
                int selectCount = imagePicker.getPickCount();
                
                if (selectedImages.contains(imagePath)) {
                    selectedImages.remove(imagePath);
                    changeMask = true;
                } else {
                    if (selectedImages.size() < selectCount) {
                        selectedImages.add(imagePath);
                        changeMask = true;
                    }
                }

                if (selectedImages.size() == 0) {
                    mTVComplete.setText("完成");
                } else {
                    mTVComplete.setText("完成 " + selectedImages.size() + "/" + selectCount);
                }

                return changeMask;
            }
        });
        adapter.setOnImgClickListener(new ImageGridAdapter.OnImgClickListener() {
            @Override
            public void onImgClick(int position) {
                //TODO on img click
            }
        });
        rvGrid.setAdapter(adapter);
    }

    private void crop(String imgPath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);

        int cropWidth = imagePicker.getCropWidth();
        int cropHeight = imagePicker.getCropHeight();

        // 如果图片宽高小于裁剪目标值，则直接返回
        if (options.outWidth <= cropWidth && options.outHeight <= cropHeight) {
            respCropImg(imgPath);
            return;
        }

        // 如果图片大于5000px提示图片太大
        if (options.outWidth >= MAX_IMAGE_SIZE_TO_CROP
                || options.outHeight >= MAX_IMAGE_SIZE_TO_CROP) {
            Toast.makeText(PickImgActivity.this, "图片太大，无法裁剪", Toast.LENGTH_SHORT).show();
            return;
        }

        File fileInput = new File(imgPath);
        if (fileInput.isFile()) {
            Uri uriInput = Uri.fromFile(new File(imgPath));
            cropImageUri(uriInput, mTempImageUri, cropWidth, cropHeight);
        }
    }

    private void respCropImg(String path) {
        clearSelectedImg();
        File tempFileCrop = new File(path);
        if (tempFileCrop.exists()) {
            imagePicker.getSelectedImages().add(path);
            response2Act();
        }
    }

    private void clearSelectedImg() {
        if (imagePicker.getSelectedImages().size() > 0) {
            imagePicker.getSelectedImages().clear();
        }
        if (adapter != null) adapter.clearSelectImg();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            // result from camera WITHOUT crop
            case REQ_CODE_TAKE_CAMERA_NO_CROP:
                clearSelectedImg();
                File tempFile = new File(mTempImagePath);
                if (tempFile.exists()) {
                    imagePicker.getSelectedImages().add(mTempImagePath);
                    response2Act();
                } else {
                    Toast.makeText(this, "拍照失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            // result from camera WITH crop
            case REQ_CODE_TAKE_CAMERA_CROP:
                cropImageUri(mTempImageUri, imagePicker.getCropWidth(), imagePicker.getCropHeight());
                break;
            case REQ_CODE_CROP_IMG:
                respCropImg(mTempImagePath);
                break;
        }
    }

    private void cropImageUri(Uri uriOutput, int outputX, int outputY) {
        cropImageUri(uriOutput, uriOutput, outputX, outputY);
    }

    private void cropImageUri(Uri uriInput, Uri uriOutput, int outputX, int outputY) {

        int cropWidth = imagePicker.getCropWidth();
        int cropHeight = imagePicker.getCropHeight();

        Intent intent = new Intent("com.android.camera.action.CROP");

        intent.setDataAndType(uriInput, "image/*");

        intent.putExtra("crop", "true");

        intent.putExtra("aspectX", cropWidth > cropHeight ? cropWidth / cropHeight : 1);

        intent.putExtra("aspectY", cropHeight > cropWidth ? cropHeight / cropWidth : 1);

        intent.putExtra("outputX", outputX);

        intent.putExtra("outputY", outputY);

        intent.putExtra("scale", true);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriOutput);

        intent.putExtra("return-data", false);

        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        intent.putExtra("noFaceDetection", true); // no face detection

        startActivityForResult(intent, REQ_CODE_CROP_IMG);
    }


    private void response2Act() {
        ArrayList<String> selectedImages = imagePicker.getSelectedImages();
        if (selectedImages == null || selectedImages.size() == 0) {
            return;
        }
        showLoading();

        final ArrayList<String> respImgList = new ArrayList<>();
        Observable.from(selectedImages)
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String srcImagePath) {
                        if(imagePicker.isCompress()) {
                            return ImageCompressUtil.compressImageFile(PickImgActivity.this, srcImagePath);
                        } else {
                            return srcImagePath;
                        }
                    }
                })
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        respImgList.add(s);
                        return null;
                    }
                })
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<String>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        hideLoading();
                        Toast.makeText(PickImgActivity.this, "图片处理失败，请重新选择", Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onNext(List<String> strings) {
                        hideLoading();
                        Intent intent = new Intent();
                        intent.putStringArrayListExtra(ImagePicker.RESULT_DATA, respImgList);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    public void onBackClick(View view) {
        finish();
    }
}
