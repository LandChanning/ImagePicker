package com.lcn.imagepicker.manager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.lcn.imagepicker.R;
import com.lcn.imagepicker.model.ImageFolder;
import com.lcn.imagepicker.view.PickImgActivity;
import com.lcn.imagepicker.ImagePicker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by JC on 2016/5/11.
 *
 */
public class ImageGridAdapter extends RecyclerView.Adapter {

    private static final int TYPE_CAMERA = 1;
    private static final int TYPE_IMAGE = 2;

    private Activity activity;
    private LayoutInflater inflater;
    private ImageFolder imageFolder;
    private Uri tempFileUri;
    private ImagePicker imagePicker;

    private OnSelectedChangeListener onSelectedChangeListener;
//    private OnImg2CropClickListener onImg2CropClickListener;
    private OnImgClickListener onImgClickListener;

    public ImageGridAdapter(@NonNull Activity activity,
                            @NonNull ImageFolder imageFolder,
                            Uri tempFileUri,
                            ImagePicker imagePicker) {
        this.activity = activity;
        inflater = LayoutInflater.from(activity);
        this.imageFolder = imageFolder;
        this.tempFileUri = tempFileUri;
        this.imagePicker = imagePicker;
    }

    public void changeData(@NonNull ImageFolder imageFolder,
                           Uri tempFileUri, ImagePicker imagePicker) {
        this.imageFolder = imageFolder;
        this.tempFileUri = tempFileUri;
        this.imagePicker = imagePicker;
        notifyDataSetChanged();
    }

    public void clearSelectImg() {
        ArrayList<String> selectedImages = imagePicker.getSelectedImages();
        if(selectedImages.size() > 0) {
            selectedImages.clear();
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(imagePicker.isShowCapture() && position == 0) {
            return TYPE_CAMERA;
        } else {
            return TYPE_IMAGE;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_grid_view, parent, false);
        RecyclerView.ViewHolder viewHolder;
        switch (viewType) {
            case TYPE_CAMERA:
                viewHolder = new VHCamera(view, activity, tempFileUri, imagePicker.isCrop());
                break;
            case TYPE_IMAGE:
                viewHolder = new VHImage(view, imagePicker.getPickMode(), imagePicker.getPickCount(),
                        onSelectedChangeListener, onImgClickListener,
                        imagePicker.isShowCapture(), imagePicker.isCrop());
                break;
            default:
                throw new RuntimeException("view type worry");

        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        VHImage vhImage;
        if(holder.getClass() == VHImage.class) {
            vhImage = (VHImage) holder;
        } else {
            return;
        }

        // get image path
        final String imagePath = getImagePath(position);

        // reset status
        vhImage.ivImage.setImageResource(R.drawable.image_fail);
        if(imagePicker.getSelectedImages().contains(imagePath)) {
            vhImage.selectBtn.setImageResource(R.drawable.selected);
            vhImage.mask.setVisibility(View.VISIBLE);
        } else {
            vhImage.selectBtn.setImageResource(R.drawable.not_selected);
            vhImage.mask.setVisibility(View.GONE);
        }

        // load image
        Glide.with(activity)
                .load(Uri.parse("file://" + imagePath))
                .placeholder(R.drawable.image_fail)
                .error(R.drawable.image_fail)
                .centerCrop()
                .into(vhImage.ivImage);
    }

    private String getImagePath(int adapterPosition) {
        if(imagePicker.isShowCapture()) {
            return imageFolder.images.get(adapterPosition - 1).path;
        } else {
            return imageFolder.images.get(adapterPosition).path;
        }
    }

    @Override
    public int getItemCount() {
        if(imagePicker.isShowCapture()) {
            return imageFolder.images.size() + 1;
        } else {
            return imageFolder.images.size();
        }
    }

    private static class VHCamera extends RecyclerView.ViewHolder {

        private WeakReference<Activity> activityReference;
        private Uri tempFileUri;
        private ImageView image;
        private boolean crop;

        VHCamera(View itemView, Activity activity, Uri tempFileUri,boolean crop) {
            super(itemView);
            activityReference= new WeakReference<>(activity);
            this.tempFileUri = tempFileUri;
            this.crop = crop;

            image = (ImageView) itemView.findViewById(R.id.iv_imgPicker_image);
            image.setImageResource(R.drawable.take_photo);
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    takeCamera();
                }
            });
        }

        private void takeCamera() {
            Activity activity = activityReference.get();
            if(activity == null) return;
            int reqCode;
            if(crop) {
                reqCode = PickImgActivity.REQ_CODE_TAKE_CAMERA_CROP;
            } else {
                reqCode = PickImgActivity.REQ_CODE_TAKE_CAMERA_NO_CROP;
            }

            Intent iTakePhotoCrop = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            iTakePhotoCrop.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri);
            activity.startActivityForResult(iTakePhotoCrop, reqCode);
        }

    }

    private static class VHImage extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView ivImage;
        ImageView selectBtn;
        View mask;

        int pickMode;
        int pickCount;
        boolean showCapture;
        boolean crop;

        WeakReference<OnSelectedChangeListener> refOnSelectedChangeListener;
        WeakReference<OnImgClickListener> refOnImgClickListener;

        VHImage(View itemView, int pickMode, int pickCount,
                       OnSelectedChangeListener onSelectedChangeListener,
                       OnImgClickListener onImgClickListener,
                       boolean showCapture, boolean crop) {
            super(itemView);
            this.pickMode = pickMode;
            this.pickCount = pickCount;
            this.showCapture = showCapture;
            this.crop = crop;
            refOnSelectedChangeListener= new WeakReference<>(onSelectedChangeListener);
            refOnImgClickListener= new WeakReference<>(onImgClickListener);

            ivImage = (ImageView) itemView.findViewById(R.id.iv_imgPicker_image);
            selectBtn = (ImageView) itemView.findViewById(R.id.iv_imgPicker_select_btn);
            mask = itemView.findViewById(R.id.iv_imgPicker_mask);

            if(pickMode == ImagePicker.MULTIPLY_MODE) {
                selectBtn.setVisibility(View.VISIBLE);
            } else {
                selectBtn.setVisibility(View.GONE);
            }

            selectBtn.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if(id == R.id.iv_imgPicker_select_btn) {
                onSelectBtnClick();
            } else if(id == R.id.layout_item_imgPicker) {
                onItemClick();
            }
        }

        private void onItemClick() {
            OnImgClickListener listener = refOnImgClickListener.get();
            if(listener != null) {
                listener.onImgClick(getImagePosition());
            }
        }

        private void onSelectBtnClick() {

            OnSelectedChangeListener listener = refOnSelectedChangeListener.get();
            if(listener == null) return;
            boolean changeMask = listener.onSelectedChange(getImagePosition());
            if(!changeMask) return;

            if(mask.getVisibility() == View.VISIBLE) {
                selectBtn.setImageResource(R.drawable.not_selected);
                mask.setVisibility(View.GONE);
            } else {
                selectBtn.setImageResource(R.drawable.selected);
                mask.setVisibility(View.VISIBLE);
            }
        }

        private int getImagePosition() {
            if(showCapture) {
                return getAdapterPosition() - 1;
            } else {
                return getAdapterPosition();
            }
        }
    }

    public void setOnSelectedChangeListener(OnSelectedChangeListener onSelectedChangeListener){
        this.onSelectedChangeListener = onSelectedChangeListener;
    }

    public void setOnImgClickListener(OnImgClickListener onImgClickListener){
        this.onImgClickListener = onImgClickListener;
    }

    public interface OnSelectedChangeListener{
        boolean onSelectedChange(int imagePosition);
    }

    public interface OnImgClickListener{
        void onImgClick(int imagePosition);
    }
}
