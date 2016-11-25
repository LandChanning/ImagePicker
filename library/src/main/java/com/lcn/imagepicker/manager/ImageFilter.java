package com.lcn.imagepicker.manager;

import com.lcn.imagepicker.model.ImageItem;

/**
 * Created by JC on 2016/9/22.
 * 图片筛选接口回调
 */

public class ImageFilter{
    public boolean filter(ImageItem imageItem) {
        return true;
    }
}
