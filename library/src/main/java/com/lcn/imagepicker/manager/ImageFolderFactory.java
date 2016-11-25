package com.lcn.imagepicker.manager;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import com.lcn.imagepicker.model.ImageFolder;
import com.lcn.imagepicker.model.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by JC on 2016/5/13.
 * 图片数据model制造工厂
 */
public class ImageFolderFactory {

    private final static String[] IMAGE_PROJECTION = {     //查询图片需要的数据列
            MediaStore.Images.Media.DISPLAY_NAME,   //图片的显示名称  aaa.jpg
            MediaStore.Images.Media.DATA,           //图片的真实路径  /storage/emulated/0/pp/downloader/wallpaper/aaa.jpg
            MediaStore.Images.Media.SIZE,           //图片的大小，long型  132492
            MediaStore.Images.Media.WIDTH,          //图片的宽度，int型  1920
            MediaStore.Images.Media.HEIGHT,         //图片的高度，int型  1080
            MediaStore.Images.Media.MIME_TYPE,      //图片的类型     image/jpeg
            MediaStore.Images.Media.DATE_ADDED};    //图片被添加的时间，long型  1450518608

    static ImageFilter imageFilter;

    public static Observable<List<ImageFolder>> create(final ContentResolver contentResolver) {
        final List<ImageFolder> imageFolders = new ArrayList<>();

        final List<ImageItem> allImageItems = new ArrayList<>();

        return buildObservable(contentResolver)
                .filter(new Func1<ImageItem, Boolean>() {
                    @Override
                    public Boolean call(ImageItem imageItem) {
                        File file = new File(imageItem.path);
                        return file.exists();
                    }
                })
                .filter(new Func1<ImageItem, Boolean>() {
                    @Override
                    public Boolean call(ImageItem imageItem) {
                        if(!allImageItems.contains(imageItem)) {
                            allImageItems.add(imageItem);
                            return true;
                        }
                        return false;
                    }
                })
                .filter(new Func1<ImageItem, Boolean>() {
                    @Override
                    public Boolean call(ImageItem imageItem) {
                        ImageFolder imageFolder = buildImageFolder(imageItem);
                        return !filterNewFilter(imageItem, imageFolder, imageFolders);
                    }
                })
                .filter(new Func1<ImageItem, Boolean>() {
                    @Override
                    public Boolean call(ImageItem imageItem) {
                        ImageFolder imageFolder = buildImageFolder(imageItem);
                        return !filterOldFolder(imageItem, imageFolder, imageFolders);
                    }
                })
                .count()
                .map(new Func1<Integer, List<ImageFolder>>() {
                    @Override
                    public List<ImageFolder> call(Integer integer) {
                        return imageFolders;
                    }
                })
                .map(new Func1<List<ImageFolder>, List<ImageFolder>>() {
                    @Override
                    public List<ImageFolder> call(List<ImageFolder> imageFolders) {
                        Collections.sort(imageFolders, new Comparator<ImageFolder>() {
                            @Override
                            public int compare(ImageFolder lhs, ImageFolder rhs) {
                                return rhs.images.size() - lhs.images.size();
                            }
                        });
                        return imageFolders;
                    }
                })
                .doOnNext(new Action1<List<ImageFolder>>() {
                    @Override
                    public void call(List<ImageFolder> imageFolders) {
                        addAllImgFolder(allImageItems, imageFolders);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private static Observable<ImageItem> buildObservable(final ContentResolver contentResolver) {
        return Observable.create(new Observable.OnSubscribe<ImageItem>() {
            @Override
            public void call(Subscriber<? super ImageItem> subscriber) {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    subscriber.onError(new Throwable("SD卡不可用"));
                }
                if (!subscriber.isUnsubscribed()) {
                    Cursor cursor = null;
                    try {
                        Uri imgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        cursor = contentResolver.query(imgUri,
                                IMAGE_PROJECTION,
                                null,
                                null,
                                IMAGE_PROJECTION[6] + " DESC");
                        if (cursor != null) {
                            while (cursor.moveToNext()) {
                                //查询数据
                                ImageItem imageItem = buildImageItem(cursor);
                                if(imageFilter != null) {
                                    if(imageFilter.filter(imageItem)) {
                                        subscriber.onNext(imageItem);
                                    }
                                } else {
                                    subscriber.onNext(imageItem);
                                }
                            }
                        }
                        subscriber.onCompleted();
                    } catch (Exception e) {
                        subscriber.onError(e);
                    } finally {
                        if (cursor != null) cursor.close();
                    }

                }
            }
        });
    }

    private static ImageItem buildImageItem(Cursor cursor) {
        ImageItem imageItem = new ImageItem();
        imageItem.name = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
        imageItem.path = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
        imageItem.size = cursor.getLong(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[2]));
        imageItem.width = cursor.getInt(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[3]));
        imageItem.height = cursor.getInt(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[4]));
        imageItem.mimeType = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[5]));
        imageItem.addTime = cursor.getLong(cursor.getColumnIndexOrThrow(IMAGE_PROJECTION[6]));
        return imageItem;
    }

    @NonNull
    private static ImageFolder buildImageFolder(ImageItem imageItem) {
        File imageFile = new File(imageItem.path);
        File imageParentFile = imageFile.getParentFile();
        ImageFolder imageFolder = new ImageFolder();
        imageFolder.name = imageParentFile.getName();
        imageFolder.path = imageParentFile.getAbsolutePath();
        return imageFolder;
    }

    private static Boolean filterNewFilter(ImageItem imageItem, ImageFolder imageFolder, List<ImageFolder> imageFolders) {
        if (!imageFolders.contains(imageFolder)) {
            ArrayList<ImageItem> images = new ArrayList<>();
            images.add(imageItem);
            imageFolder.cover = imageItem;
            imageFolder.images = images;
            imageFolders.add(imageFolder);
            return true;
        }
        return false;
    }

    private static Boolean filterOldFolder(ImageItem imageItem, ImageFolder imageFolder, List<ImageFolder> imageFolders) {
        if (imageFolders.contains(imageFolder)) {
            imageFolders.get(imageFolders.indexOf(imageFolder)).images.add(imageItem);
            return true;
        }
        return false;
    }

    private static void addAllImgFolder(List<ImageItem> allImageItems, List<ImageFolder> imageFolders) {
        //构造所有图片的集合
        ImageFolder allImagesFolder = new ImageFolder();
        allImagesFolder.name = "所有图片";
        allImagesFolder.path = "/";
        allImagesFolder.cover = allImageItems.get(0);
        allImagesFolder.images = allImageItems;
        imageFolders.add(0, allImagesFolder);  //确保第一条是所有图片
    }

}
