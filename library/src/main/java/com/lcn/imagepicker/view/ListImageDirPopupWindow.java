package com.lcn.imagepicker.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lcn.imagepicker.R;
import com.lcn.imagepicker.model.ImageFolder;

import java.util.List;

/**
 * Created by Jc on 2015/9/17.
 *
 */
public class ListImageDirPopupWindow extends PopupWindow {

    private Activity mContext;
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<ImageFolder> imageFolders;
    private int mSelectPosition = 0;

    private OnDirSelectedListener mListener;

    public ListImageDirPopupWindow(Activity context, List<ImageFolder> imageFolders) {
        mContext = context;
        this.imageFolders = imageFolders;
        // calculate width and height
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_take_photo, null);

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView();

        initEvent();
    }

    public void setOnDirSelectedListener(OnDirSelectedListener mListener) {
        this.mListener = mListener;
    }

    /**
     * calculate width and height
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);

        mWidth = outMetrics.widthPixels;
        mHeight = (int)(outMetrics.heightPixels * 0.7);
    }

    private void initView() {
        mListView = (ListView) mConvertView.findViewById(R.id.lv_folder_list);
        mListView.setAdapter(new ListDirAdapter(mContext, imageFolders));
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    mListener.onSelect(position);
                    mSelectPosition = position;
                    dismiss();
                }
            }
        });
    }

    private class ListDirAdapter extends ArrayAdapter<ImageFolder> {

        private LayoutInflater mInflater;

        public ListDirAdapter(Context context, List<ImageFolder> imageFolders) {
            super(context, 0, imageFolders);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_popup_take_photo, parent, false);
                holder.mImg = (ImageView) convertView.findViewById(R.id.iv_item_folder_image);
                holder.mDirName = (TextView) convertView.findViewById(R.id.tv_item_folder_name);
                holder.mDirCount = (TextView) convertView.findViewById(R.id.tv_item_folder_image_count);
                holder.mChoose = (ImageView) convertView.findViewById(R.id.iv_item_folder_choose);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ImageFolder imageFolder = getItem(position);
            // reset image
            holder.mImg.setImageResource(R.drawable.image_fail);
            if(mSelectPosition == position){
                holder.mChoose.setVisibility(View.VISIBLE);
            } else {
                holder.mChoose.setVisibility(View.GONE);
            }
            // load data
            Glide.with(mContext)
                    .load(Uri.parse("file://" + imageFolder.cover.path))
                    .placeholder(R.drawable.image_fail)
                    .error(R.drawable.image_fail)
                    .centerCrop()
                    .into(holder.mImg);
//            MyImageLoader.getInstance().loadImage(bean.getFirstImgPath(), holder.mImg, false);
            holder.mDirName.setText(imageFolder.name);
            holder.mDirCount.setText(imageFolder.images.size() + "张");
            return convertView;
        }

        private class ViewHolder{
            ImageView mImg;
            TextView mDirName;
            TextView mDirCount;
            ImageView mChoose;
        }
    }

    public interface OnDirSelectedListener {
        void onSelect(int position);
    }
}
