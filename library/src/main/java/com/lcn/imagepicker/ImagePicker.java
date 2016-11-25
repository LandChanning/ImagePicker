package com.lcn.imagepicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.lcn.imagepicker.manager.TempManager;
import com.lcn.imagepicker.view.PickImgActivity;

import java.util.ArrayList;

/**
 * Created by LCN on 2016/11/25.
 * 总控：保存配置参数，启动页面
 */

public class ImagePicker implements Parcelable {

    // request key
    public static final String PARCELED_OBJ = "parceledObj";
    // response key
    public static final String RESULT_DATA = "resultData";

    /** use MULTIPLY_MODE & pickCount=1 instead */
    @Deprecated
    public static final int SINGLE_MODE = 1;
    public static final int MULTIPLY_MODE = 2;

    /**  pick mode, single or multiply */
    private int pickMode;
    
    /**  max pick count for multiply mode */
    private int pickCount;
    
    /** mCrop or not */
    private boolean crop;
    private int cropWidth;
    private int cropHeight;
    
    /**  show capture button or not */
    private boolean showCapture;
    
    /**  compress or not */
    private boolean compress;
    
    /**  selected for result */
    private ArrayList<String> selectedImages;

    private ImagePicker() {
        pickMode = SINGLE_MODE;
        pickCount = 1;
        crop = false;
        cropWidth = 600;
        cropHeight = 600;
        showCapture = false;
        compress = false;
        selectedImages = new ArrayList<>();
    }

    public void clear(Context context) {
        TempManager.clearTemp(context);
        if(selectedImages.size() > 0) {
            selectedImages.clear();
        }
        selectedImages = null;
    }

    public int getPickMode() {
        return pickMode;
    }

    public int getPickCount() {
        return pickCount;
    }

    public boolean isCrop() {
        return crop;
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public boolean isShowCapture() {
        return showCapture;
    }

    public boolean isCompress() {
        return compress;
    }

    public ArrayList<String> getSelectedImages() {
        return selectedImages;
    }

    public static class Builder {

        private ImagePicker imagePicker;

        public Builder() {
            imagePicker = new ImagePicker();
        }

        public Builder setPickMode(int pickMode) {
            if(pickMode != SINGLE_MODE && pickMode != MULTIPLY_MODE) {
                throw new IllegalArgumentException("pick mode error, " +
                        "must use ImagePicker.SINGLE_MODE or ImagePicker.MULTIPLY_MODE");
            }
            imagePicker.pickMode = pickMode;
            return this;
        }

        public Builder setPickCount(int pickCount) {
            imagePicker.pickCount = pickCount;
            return this;
        }

        public Builder setCrop(boolean crop) {
            imagePicker.crop = crop;
            return this;
        }

        public Builder setCropWidth(int cropWidth) {
            imagePicker.cropWidth = cropWidth;
            return this;
        }

        public Builder setCropHeight(int cropHeight) {
            imagePicker.cropHeight = cropHeight;
            return this;
        }

        public Builder setShowCapture(boolean showCapture) {
            imagePicker.showCapture = showCapture;
            return this;
        }

        public Builder setCompress(boolean compress) {
            imagePicker.compress = compress;
            return this;
        }

        public ImagePicker start(Activity activity, int reqCode) {
            Intent intent = new Intent(activity, PickImgActivity.class);
            intent.putExtra(PARCELED_OBJ, imagePicker);
            activity.startActivityForResult(intent, reqCode);
            return imagePicker;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.pickMode);
        dest.writeInt(this.pickCount);
        dest.writeByte(this.crop ? (byte) 1 : (byte) 0);
        dest.writeInt(this.cropWidth);
        dest.writeInt(this.cropHeight);
        dest.writeByte(this.showCapture ? (byte) 1 : (byte) 0);
        dest.writeByte(this.compress ? (byte) 1 : (byte) 0);
        dest.writeStringList(this.selectedImages);
    }

    protected ImagePicker(Parcel in) {
        this.pickMode = in.readInt();
        this.pickCount = in.readInt();
        this.crop = in.readByte() != 0;
        this.cropWidth = in.readInt();
        this.cropHeight = in.readInt();
        this.showCapture = in.readByte() != 0;
        this.compress = in.readByte() != 0;
        this.selectedImages = in.createStringArrayList();
    }

    public static final Parcelable.Creator<ImagePicker> CREATOR = new Parcelable.Creator<ImagePicker>() {
        @Override
        public ImagePicker createFromParcel(Parcel source) {
            return new ImagePicker(source);
        }

        @Override
        public ImagePicker[] newArray(int size) {
            return new ImagePicker[size];
        }
    };
}
