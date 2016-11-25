package com.lcn.imagepicker.manager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by Jc on 2015/9/15.
 * Lru LIFO Image Loader
 */
public class MyImageLoader {

    private static MyImageLoader mInstance;
    /**
     * ivImage memory lru cache
     */
    private LruCache<String, Bitmap> mLruCache;
    private static final int maxMemoryCache = 4 * 1024 * 1024;
    /**
     * thread pool
     */
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 1;
    /**
     * task queue
     */
    private LinkedList<Runnable> mTaskQueue;
    /**
     * queue scheduling type
     */
    private Type mType = Type.LIFO;
    /**
     * polling thread
     */
    private Thread mPollingThread;
    private Semaphore mSemaphorePollingThread;

    private Handler mPollingThreadHandler;
    /**
     * to synchronize mPollingThreadHandler
     */
    private Semaphore mSemaphorePollingThreadHandler = new Semaphore(0);

    private Handler mUIHandler;
    /**
     * is need refresh cache for same path
     */
    private boolean mNeedRefresh;


    public enum Type {
        FIFO, LIFO
    }

    private MyImageLoader(int threadCount, Type type){
        init(threadCount, type);
    }

    /**
     * init
     *
     * @param threadCount thread count
     * @param type scheduling type
     */
    private void init(int threadCount, Type type) {
        // init polling thread
        mPollingThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPollingThreadHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        // execute a task from thread pool
                        mThreadPool.execute(getTask());
                        try {
                            // make sure the count of concurrent threads not more than setting
                            // threadCount,and LIFO queue useful
                            mSemaphorePollingThread.acquire();
                        } catch (InterruptedException e) {
                            // do noting
                        }
                    }
                };
                // release semaphore to make sure the mPollingThreadHandler is not null in other thread
                mSemaphorePollingThreadHandler.release();
                Looper.loop();
            }
        };
        mPollingThread.start();

        // init Lru cache, 1/8 of max available memory
        int maxAvailableMemory = (int) Runtime.getRuntime().maxMemory();
        mLruCache = new LruCache<String, Bitmap>(maxAvailableMemory / 8 > maxMemoryCache ? maxMemoryCache : maxAvailableMemory / 8){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        // init thread pool and task queue
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mSemaphorePollingThread = new Semaphore(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;

    }

    public static MyImageLoader getInstance(){
        return getInstance(DEFAULT_THREAD_COUNT, Type.LIFO);
    }

    public static MyImageLoader getInstance(int threadCount, Type type){

        if(mInstance == null){
            synchronized(MyImageLoader.class) {
                if(mInstance == null) {
                    mInstance = new MyImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    /**
     * @param path ivImage local absolute path
     * @param imageView ivImage view
     * @param needRefresh if set true, the bitmap in LruCache will refresh before display
     */
    public void loadImage(@NonNull final String path, @NonNull final ImageView imageView, boolean needRefresh){
        imageView.setTag(path);
        mNeedRefresh = needRefresh;

        if(mUIHandler == null) {
            mUIHandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    // set bitmap for imageView
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    // compare path and getTag
                    if(imageView.getTag().toString().equals(path)){
                        imageView.setImageBitmap(bm);
                    }
                }
            };
        }

        // get bitmap from lru cache
        Bitmap bm = getBitmapFromLruCache(path);
        if(bm != null) {
            refreshBitmap(path, imageView, bm);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    // compress ivImage and load
                    // 1. get display size
                    ImageSize imageSize = getImageViewSize(imageView);
                    // 2. compress ivImage
                    Bitmap bm = decodeSampleBitmapFromPath(path, imageSize.width, imageSize.height);
                    if (bm != null) {
                        // 3. add bitmap to LruCache
                        addBitmapToLruCache(path, bm);
                        // 4. send message to refresh UI
                        refreshBitmap(path, imageView, bm);
                    }

                    mSemaphorePollingThread.release();

                }
            });
        }
    }

    /**
     * clear LruCache
     */
    public void clearLruCache(){
        if(mLruCache != null && mLruCache.size() > 0) {
            mLruCache.evictAll();
        }
//        mLruCache = null;

    }

    public void removeFromLruCache(String imagePath) {

    }

    /**
     * send message to refresh UI
     * @param path ivImage absolute path
     */
    private void refreshBitmap(String path, ImageView imageView, Bitmap bm) {
        if(bm != null) {
            Message msg = Message.obtain();
            ImageBeanHolder holder = new ImageBeanHolder();
            holder.bitmap = bm;
            holder.imageView = imageView;
            holder.path = path;
            msg.obj = holder;
            mUIHandler.sendMessage(msg);
        }
    }

    /**
     * add a task to queue
     * Because mPollingThreadHandler init in polling thread,
     * call the method of this handler maybe produce 'java.lang.NullPointerException',
     * So, introduce Semaphore here to make sure mPollingThreadHandler is not null.
     * Multiply threads acquire semaphore may reduce deadlock at the same time, and operate TaskQueue here,
     * So, use synchronized method
     */
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        if(mPollingThreadHandler == null) {
            try {
                mSemaphorePollingThreadHandler.acquire();
            } catch (InterruptedException e) {
                // do noting
            }
        }
        mPollingThreadHandler.sendEmptyMessage(0);
    }

    /**
     * get a task from queue
     */
    private Runnable getTask() {
        if(mType == Type.LIFO){
            return mTaskQueue.removeLast();
        } else {
            return mTaskQueue.removeFirst();
        }
    }

    private void addBitmapToLruCache(String path, Bitmap bm) {
        if(getBitmapFromLruCache(path) == null && bm != null) {
            mLruCache.put(path, bm);
        }
    }

    private Bitmap getBitmapFromLruCache(String path) {
        if(mNeedRefresh && mLruCache.get(path) != null) {
            mLruCache.remove(path);
        }
        return mLruCache.get(path);
    }

    /**
     * get appropriate width and height to compress ivImage
     */
    private ImageSize getImageViewSize(ImageView imageView) {

        ImageSize imageSize = new ImageSize();

        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();

        int width = imageView.getWidth();
        if(width <= 0) {
            width = lp.width;
        }
        if(width <= 0) {
            width = getFieldValue(imageView, "mMaxWidth");
        }
        if(width <= 0) {
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();
        if(height <= 0) {
            height = lp.height;
        }
        if(height <= 0) {
            height = getFieldValue(imageView, "mMaxHeight");
        }
        if(height <= 0) {
            height = displayMetrics.heightPixels;
        }

        imageSize.width = width;
        imageSize.height = height;

        return imageSize;
    }

    /**
     * get integer field value by reflect
     * @param obj object
     * @param fieldName field name
     */
    private int getFieldValue(Object obj, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(obj);
            if(fieldValue > 0 && fieldValue < Integer.MAX_VALUE){
                value = fieldValue;
            }
        } catch (Exception e) {
            // do nothing
        }
        return value;
    }

    /**
     * get compressed bitmap from absolute path
     * @param path ivImage absolute path
     * @param width display width
     * @param height display height
     * @return compressed Bitmap
     */
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        Bitmap bm = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            options.inSampleSize = calculateInSampleSize(options, width, height);
            options.inJustDecodeBounds = false;

            bm = BitmapFactory.decodeFile(path, options);
        } catch (Throwable oom) {
            // catch OutOfMemoryError
            mLruCache.evictAll();
        }
        return bm;
    }

    /**
     * calculate inSampleSize
     * @param options BitmapFactory.Options
     * @param reqWidth display request width
     * @param reqHeight display request height
     * @return inSampleSize
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        int width = options.outWidth;
        int height = options.outHeight;
        if(width > reqWidth || height > reqHeight) {
            int widthRatio = Math.round(width * 1.0f / reqWidth);
            int heightRatio = Math.round(height * 1.0f / reqHeight);
            inSampleSize = Math.max(widthRatio, heightRatio);
        }

        return inSampleSize;
    }

    private class ImageBeanHolder{
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

    private class ImageSize{
        int width;
        int height;
    }
}
