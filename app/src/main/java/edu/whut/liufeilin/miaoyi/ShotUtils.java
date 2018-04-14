package edu.whut.liufeilin.miaoyi;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;

/**
 * Created by Bert on 2017/4/14.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ShotUtils {

    public static final int REQUEST_MEDIA_PROJECTION = 3 ;
    private Context mContext;
    private ImageReader mImageReader;
    private static Intent mResultData = null;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;


    public ShotUtils(Context mContext) {
        super();
        this.mContext = mContext;
//        createImageReader();
    }

    public void setData(Intent mResultData){
        this.mResultData = mResultData;
    }

    public void init(Activity mActivity){
        Log.d("MainActivity","执行init");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后才允许使用屏幕截图
            Toast.makeText(mContext,"系统版本不支持",Toast.LENGTH_SHORT).show();
            return;
        }
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Log.d("init","执行startActivityForResult");
        mActivity.startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void createImageReader() {
        mImageReader = ImageReader.newInstance(getScreenWidth(), getScreenHeight(), PixelFormat.RGBA_8888, 1);
    }

    public void startScreenShot(final ShotListener mShotListener) {
        Log.d("startScreenShot","执行");
//        startVirtual();
//        startCapture(mShotListener);
        createImageReader();
        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            public void run() {
                startVirtual();
            }
        }, 5);

        handler1.postDelayed(new Runnable() {
            public void run() {

                startCapture(mShotListener);
            }
        }, 300);

    }

    public void startVirtual() {
        Log.d("startVirtual","执行");
        if (mMediaProjection != null) {
            virtualDisplay();
        } else {
            setUpMediaProjection();
            virtualDisplay();
        }
    }


    private void virtualDisplay() {
        Log.d("virtualDisplay","执行");

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror", getScreenWidth(), getScreenHeight(), getScreenDpi(), DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    public void setUpMediaProjection() {
        Log.d("setUpMediaProjection","执行");

        if (mResultData == null) {
            Log.d("setUpMediaProjection","mResultData == null");
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            mContext.startActivity(intent);
        } else {
            Log.d("setUpMediaProjection","mResultData != null");
            if(mMediaProjection == null){
                Log.d("setUpMediaProjection","mMediaProjection == null");
                mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, mResultData);
            }

        }
    }

    private MediaProjectionManager getMediaProjectionManager() {
        Log.d("getMediaProjection","执行");

        return (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public void startCapture(ShotListener mShotListener) {
        Log.d("startCapture","执行");
        Image image = mImageReader.acquireNextImage();
        if (image == null) {
            startScreenShot(mShotListener);
        } else {
            Bitmap bitmap = covetBitmap(image);
            if(bitmap != null){
                mShotListener.OnSuccess(bitmap);
                image.close();
                stopVirtual();
            }else{

            }
        }
    }

    private Bitmap covetBitmap(Image image){
        int width = image.getWidth();
        Log.d("covetBitmap width",String.valueOf(width));
        int height = image.getHeight();
        Log.d("covetBitmap height",String.valueOf(height));

        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        //每个像素的间距
        int pixelStride = planes[0].getPixelStride();
        //总的间距
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
        image.close();
        return bitmap;
    }

    public void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;

        tearDownMediaProjection();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    /**
     *获得屏幕宽高
     */
    private int getScreenWidth() {
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);

        int width = wm.getDefaultDisplay().getWidth();
        Log.d("getScreenWidth",String.valueOf(width));
        return width;
    }

    /**
     *  获得屏幕宽高
     */
    private int getScreenHeight() {
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight();
        Log.d("getScreenHeight",String.valueOf(height));
        return height;
    }

    /**
     * 获得DPI
     */

    private int getScreenDpi() {
        WindowManager wm = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.densityDpi;
    }


    public interface ShotListener {

        void OnSuccess(Bitmap bitmap);
    }
}


