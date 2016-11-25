package com.lcn.imagepicker.presenter;

import android.content.ContentResolver;
import android.support.annotation.NonNull;

import com.lcn.imagepicker.manager.ImageFolderFactory;
import com.lcn.imagepicker.model.ImageFolder;
import com.lcn.imagepicker.view.IViewImageGrid;

import java.util.List;

import rx.Subscriber;
import rx.Subscription;

/**
 * Created by JC on 2016/5/13.
 *
 */
public class ImageGirdPresenter {

    private IViewImageGrid iViewImageGrid;

    private List<ImageFolder> imageFolders;

    private Subscription subscription;

    public ImageGirdPresenter(@NonNull IViewImageGrid iViewImageGrid) {
        this.iViewImageGrid = iViewImageGrid;
    }

    public void init(ContentResolver contentResolver) {
        subscription = ImageFolderFactory.create(contentResolver)
                .subscribe(new Subscriber<List<ImageFolder>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        iViewImageGrid.showError(e.getMessage());
                    }

                    @Override
                    public void onNext(List<ImageFolder> imageFolders) {
                        ImageGirdPresenter.this.imageFolders = imageFolders;
                        iViewImageGrid.initFolderPopup(imageFolders);
                        refreshImageData(0);
                    }
                });
    }

    public void refreshImageData(int folderPosition) {
        iViewImageGrid.renderImageData(imageFolders.get(folderPosition));
    }

    public void destroy() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
    }
}
