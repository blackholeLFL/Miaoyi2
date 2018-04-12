package edu.whut.liufeilin.miaoyi;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.whut.liufeilin.miaoyi.api.TransApi;
import edu.whut.liufeilin.miaoyi.view.OcrView;


/**
 * Created by blackhole on 2018/3/27.
 */



public class FloatService extends Service{
    public static final String TAG = "FloatService";
    private MyBinder mBinder = new MyBinder();
    LinearLayout toucherLayout;
    ConstraintLayout OcrLayout;
    WindowManager.LayoutParams params;
    WindowManager.LayoutParams params1;
    WindowManager windowManager;
    TextView textView;
    ShotUtils shotUtils=MainActivity.shotUtils;
    String result;
    String language = "eng_sim";

    ThreadPoolExecutor threadPoolExecutor;
    TessBaseAPI mTess;
    String sdPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Android/data/" + MainActivity.PACKAGE_NAME + "/files";
    OcrView ocrView;

    String picPath = sdPath + "/" + "temp.png";
    Bitmap bmp = null;
    double scale = 1.0;
    int ScreenHeight = MainActivity.ScreenHeight;
    int ScreenWidth = MainActivity.ScreenWidth;
    //状态栏高度.
    int statusBarHeight = -1;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle data = msg.getData();
            String result = data.getString("result");
            textView.setText(result);

        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() executed");
        initTessBaseData();
        createToucher();
        threadPoolExecutor = new ThreadPoolExecutor(3, 6, 2, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(128));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (textView != null) {
            windowManager.removeView(toucherLayout);
        }
        if(ocrView!=null){
            windowManager.removeView(OcrLayout);
        }
        super.onDestroy();
        Log.d(TAG, "onDestroy() executed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class MyBinder extends Binder {
        FloatService getService() {
            return FloatService.this;
        }

    }

    public void initTessBaseData() {
    /*初始化Tess*/
        mTess = new TessBaseAPI();
        String datapath = sdPath + "/test/";
        Log.d("datapath",datapath);
//        String language = "chi_sim";
        File dir = new File(datapath + "tessdata/");
        if (!dir.exists())
            dir.mkdirs();
        mTess.init(datapath, language);
    }




    public String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
/*
    public InputStream Bitmap2InputStream(Bitmap bm) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        return is;
    }
*/
    public Bitmap FixImageOrientation(Bitmap bitmap) throws IOException {
        //检验图片地址是否正确

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        //图片旋转角度
        int rotate = 90;

        // 获取当前图片的宽和高
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix mtx = new Matrix();
        // 使用Matrix对图片进行处理

        mtx.preRotate(rotate);
        // 旋转图片
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);


        return bitmap;

    }


    public Bitmap FixImageOrientation(String imagePath, String option) throws IOException {
        //检验图片地址是否正确
        if (imagePath == null || imagePath.equals(""))
            return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (option.equalsIgnoreCase("camera")) {
            options.inSampleSize = 2;    //调整图片为原来的1/2
        } else if (option.equalsIgnoreCase("album")) {
            options.inSampleSize = 1;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        //图片旋转角度
        int rotate = 0;

        ExifInterface exif = new ExifInterface(imagePath);
        //先获取当前图像的方向，判断是否需要旋转
        int imageOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//        Log.i(Tag, "Current image orientation is " + imageOrientation);

        switch (imageOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            default:
                break;
        }
        // 获取当前图片的宽和高
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix mtx = new Matrix();
        // 使用Matrix对图片进行处理
        if (option.equalsIgnoreCase("album")) {
            mtx.postScale(0.75f, 0.75f);
        }
        mtx.preRotate(rotate);
        // 旋转图片
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);


        return bitmap;

    }

    public void createOcrView(){
        Log.d("createOcrView","已执行");
        params1 = new WindowManager.LayoutParams();
//        params1.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            params1.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else{
            params1.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        params1.format = PixelFormat.RGBA_8888;
//        params1.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            params1.width=ScreenHeight;
            params1.height=ScreenWidth;
        }else{
            params1.width = ScreenWidth;
            params1.height = ScreenHeight;
        }
        params1.gravity = Gravity.LEFT | Gravity.TOP;
        params1.x = 0;
        params1.y = 0;
        ocrView=(OcrView)OcrLayout.findViewById(R.id.ocrView);
        ocrView.rect=new Rect(0, 0, 0, 0);
        MainActivity.getMainActivity().getPermisson();


        ocrView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        ocrView.setOnClickListener(new View.OnClickListener() {
            long[] hints = new long[2];
            @Override
            public void onClick(View view) {
                Log.d("ocrView","onClick");
                System.arraycopy(hints,1,hints,0,hints.length -1);
                hints[hints.length -1] = SystemClock.uptimeMillis();
                if (SystemClock.uptimeMillis() - hints[0] < 700) {
                    textView.setText("识别中，请稍等。");
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            result = Screen_GetTextFromRect();
                            TransApi api = new TransApi(MainActivity.APP_ID, MainActivity.SECURITY_KEY);
                            result = result+"\n"+api.getTransResult(result, "auto", "zh");
                            result = "结果为："+result;
                            Message msg = new Message();
                            Bundle data = new Bundle();
                            data.putString("result",result);
                            msg.setData(data);
                            handler.sendMessage(msg);
                        }
                    };
                    threadPoolExecutor.execute(r);
                    windowManager.removeView(OcrLayout);
                }
            }
        });

    }

    private void createToucher()
    {
        //赋值WindowManager&LayoutParam.
        params = new WindowManager.LayoutParams();
        windowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else{
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        //设置效果为背景透明.

        params.format = PixelFormat.RGBA_8888;

        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        //设置窗口初始停靠位置.
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        //设置悬浮窗口长宽数据.
        params.width = 500;
        params.height = 500;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局.
        toucherLayout = (LinearLayout) inflater.inflate(R.layout.toucherlayout,null);
        OcrLayout = (ConstraintLayout)inflater.inflate(R.layout.ocrview, null);
        //添加toucherlayout
        windowManager.addView(toucherLayout,params);
        Log.d("toucherLayout","addView执行完毕");

        //修改param属性 长宽改为全屏 不可触摸 不接受事件 直到悬浮窗被双击 改为最上层


        Log.i(TAG,"toucherlayout-->left:" + toucherLayout.getLeft());
        Log.i(TAG,"toucherlayout-->right:" + toucherLayout.getRight());
        Log.i(TAG,"toucherlayout-->top:" + toucherLayout.getTop());
        Log.i(TAG,"toucherlayout-->bottom:" + toucherLayout.getBottom());

        //主动计算出当前View的宽高信息.
        toucherLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        //用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height","dimen","android");
        if (resourceId > 0)
        {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        Log.i(TAG,"状态栏高度为:" + statusBarHeight);

        //浮动窗口按钮.
        textView = (TextView) toucherLayout.findViewById(R.id.text);
        LinearLayout linearLayout = (LinearLayout) toucherLayout.findViewById(R.id.layout);
        Button button = (Button)toucherLayout.findViewById(R.id.button);
        //长按：截图并开始选取区域
        //双击：文字识别并显示翻译结果
        linearLayout.setOnClickListener(new View.OnClickListener() {
            long[] hints = new long[2];
            @Override
            public void onClick(View v) {
                Log.i(TAG,"点击了");
                System.arraycopy(hints,1,hints,0,hints.length -1);
                hints[hints.length -1] = SystemClock.uptimeMillis();
                if (SystemClock.uptimeMillis() - hints[0] >= 700) {
                    Log.i(TAG,"要执行");
//                    Toast.makeText(FloatService.this,"连续点击两次以退出", Toast.LENGTH_SHORT).show();
                }else{
                    Log.d("startScreenShot","执行");
                    createOcrView();
                    shotUtils.startScreenShot(new ShotUtils.ShotListener() {
                        @Override
                        public void OnSuccess(final Bitmap bitmap) {
                            Log.d("OnSuccess","执行");
//                            saveMyBitmap(bitmap);
                            Toast.makeText(FloatService.this,"截图成功", Toast.LENGTH_SHORT).show();

                            try{
//                                bmp=FixImageOrientation(picPath,"album");
                                /*
                                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                                    Log.d("屏幕方向","横屏");
                                    bmp=FixImageOrientation(bitmap);
                                }
                                else
                                    */
                                bmp = bitmap;
                                ocrView.SelectRect();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            /*
                            textView.setText("识别中，请稍等。");
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    handler.sendEmptyMessage(0);
                                }
                            };
                            threadPoolExecutor.execute(r);
                            */
                        }
                    });
                }
            }
        });

        /*
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d("悬浮窗","长按");
                textView.setText("识别中，请稍等。");
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(0);
                    }
                };
                threadPoolExecutor.execute(r);
                return false;
            }
        });
        */

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                params.x = (int) event.getRawX() - 250;
                params.y = (int) event.getRawY() - 50 - statusBarHeight;
                windowManager.updateViewLayout(toucherLayout,params);
                return false;
            }
        });



    }


    public void saveMyBitmap(Bitmap bitmap){
        File f = new File(picPath);
        try {
            f.createNewFile();
        }catch (Exception e){
            Log.d("saveMyBitmap","error1");
            e.printStackTrace();
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (Exception e) {
            Log.d("saveMyBitmap","error2");
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
        } catch (IOException e) {
            Log.d("saveMyBitmap","error2");
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            Log.d("saveMyBitmap","error2");
            e.printStackTrace();
        }
    }

    public String Screen_GetTextFromRect(){
        mTess.clear();
        if (bmp == null) return null;
        mTess.setImage(bmp);
        Log.d("GetTextFromRect","left:"+ocrView.rect.left+" top:"+ocrView.rect.top+" width:"+ocrView.rect.width()+" height:"+ocrView.rect.height());
        mTess.setRectangle(ocrView.rect.left, ocrView.rect.top, ocrView.rect.width(), ocrView.rect.height());

        String result;
        if(ocrView.rect.width()<=0||ocrView.rect.height()<=0){
            result = "";
        }else{
            result = mTess.getUTF8Text();
        }
        return result;
    }

    public String GetTextFromRect() {
        mTess.clear();
        if (bmp == null) return null;
        mTess.setImage(bmp);
//                mTess.setRectangle(0,0,1220,600);
        scale = (double) bmp.getWidth() / (double) ScreenWidth;
        Log.d("GetTextFromRect","left:"+ocrView.rect.left+" top:"+ocrView.rect.top+" width:"+ocrView.rect.width()+" height:"+ocrView.rect.height());
        if (scale > 1)
            mTess.setRectangle((int) (scale * ocrView.rect.left), (int) (scale * ocrView.rect.top), (int) (scale * ocrView.rect.width()), (int) (scale * ocrView.rect.height()));
        else
            mTess.setRectangle(ocrView.rect.left, ocrView.rect.top, ocrView.rect.width(), ocrView.rect.height());

//        Log.i(TAG,"hhhhhhhhhhhhhh "+ocrView.rect.left+"\\"+ocrView.rect.top+"\\"+ocrView.rect.width()+"\\"+ocrView.rect.height());
//        Log.i(TAG,"hhhhhhhhhhhhhh "+"\\"+scale+(scale*ocrView.rect.left)+"\\"+(int)(scale*ocrView.rect.top)+
//                "\\"+(int)(scale*ocrView.rect.width())+"\\"+(int)(scale*ocrView.rect.height()));
//        Log.i(TAG,""+bmp.getWidth()+"]]"+ bmp.getHeight());
//        Log.i(TAG,""+ScreenWidth+"]]"+ ScreenHeight);
        String result;
        if(ocrView.rect.width()<=0||ocrView.rect.height()<=0){
            result = "";
        }else{
            result = mTess.getUTF8Text();
        }
        return result;
    }
}
