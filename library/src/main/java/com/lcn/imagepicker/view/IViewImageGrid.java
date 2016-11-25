package com.lcn.imagepicker.view;

import com.lcn.imagepicker.model.ImageFolder;

import java.util.List;

/**
 * Created by JC on 2016/5/13.
 */
public interface IViewImageGrid {

    void showLoading();

    void hideLoading();

    void showError(String errorMsg);

    void initFolderPopup(List<ImageFolder> imageFolders);

    void renderImageData(ImageFolder imageFolder);

}
