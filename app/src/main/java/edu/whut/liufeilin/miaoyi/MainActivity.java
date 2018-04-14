package edu.whut.liufeilin.miaoyi;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;



public class MainActivity extends Activity {
    private static Context mainContext;
    public static final String APP_ID = "20180318000137277";
    public static final String SECURITY_KEY = "tYo5nggG_R95EtuJvX0i";
    private static MainActivity mainActivity;
    //    String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
    ImageButton btn_openCamera;
    ImageButton btn_openAlbum;

    EditText txt;
    String wait_trans;//识别出的等待翻译的字符串
    FloatService floatService;
    DisplayMetrics dm;
    ThreadPoolExecutor threadPoolExecutor;//线程池
    String filename;
    String sdPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Android/data/" + MainActivity.PACKAGE_NAME + "/files";

    String chinese = "chi_sim.traineddata";
    String english = "eng_sim.traineddata";
    String Language = english;

    public static ShotUtils shotUtils;
    public static int ScreenHeight;
    public static int ScreenWidth;
    public static String PACKAGE_NAME = "edu.whut.liufeilin.miaoyi";
    FloatService.MyBinder myBinder;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (FloatService.MyBinder) service;
            floatService = myBinder.getService();
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    final  Context context = this;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            wait_trans = floatService.GetTextFromRect();
            //txtget.setText(wait_trans);

        }
    };

    private void bindServiceConnection() {
        Intent intent = new Intent(MainActivity.this, FloatService.class);
        startService(intent);
        bindService(intent, connection, this.BIND_AUTO_CREATE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //context = MainActivity.this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        Log.i("onCreate", "执行");

        filename = sdPath + "/test/tessdata/" + Language;
        //手机中不存在训练文件，则在sd卡中写入对应的文件
        //应用首次运行，将训练文件拷贝到sd卡中
        SharedPreferences sp = getSharedPreferences("ocr_test", Context.MODE_PRIVATE);
        int int_runtimes = sp.getInt("run", 0);
        if (int_runtimes == 0) {
            try {
                copyBigDataToSD(filename);
                sp.edit().putInt("run", 1).commit();
                Log.d("success", "载入完成");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("error", "报错");
            }
        }
        mainActivity = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(MainActivity.this)) {
                Toast.makeText(MainActivity.this, "已开启Toucher", Toast.LENGTH_SHORT).show();
            } else {
                //若没有权限，提示获取.
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + PACKAGE_NAME));
                Toast.makeText(MainActivity.this, "需要取得权限以使用悬浮窗", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }
        }

        dm = getResources().getDisplayMetrics();
        ScreenHeight = dm.heightPixels;
        ScreenWidth = dm.widthPixels;
        shotUtils = new ShotUtils(getApplicationContext());
        shotUtils.init(MainActivity.this);

        floatService = new FloatService();
        bindServiceConnection();

        btn_openCamera = findViewById(R.id.btn_openCamera);
        btn_openAlbum = findViewById(R.id.btn_openAlbum);
        txt = findViewById(R.id.txt);
        threadPoolExecutor = new ThreadPoolExecutor(3, 6, 2, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(128));

        btn_openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // handler.sendEmptyMessage(1);
                getImage("camera");
                //context1getImageFromCamera(context1);
            }
        });

        btn_openAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    getImage("album");
                }
            }
        });

/*        btn_findText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatService.createOcrView();
                floatService.ocrView.SelectRect();
            }
        });
       btn_getTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtget.setText("识别中，请稍等。");
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(0);
                    }
                };
                threadPoolExecutor.execute(r);

            }
        });

       btn_trans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TransApi api = new TransApi(APP_ID, SECURITY_KEY);
                        String display;
                        display = api.getTransResult(wait_trans, "auto", "en");
                        setText(txtget,display);
                    }
                }).start();

            }
        });
        */
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        Log.i("onCreate", "执行完毕");
    }


    public static MainActivity getMainActivity() {
        return mainActivity;
    }


/*    private void setText(final TextView text, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }*/

    public void getPermisson() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(MainActivity.this)) {
                try {
                    floatService.windowManager.addView(floatService.OcrLayout, floatService.params1);
                } catch (Exception e) {
                    floatService.windowManager.removeView(floatService.OcrLayout);
                    floatService.windowManager.addView(floatService.OcrLayout, floatService.params1);
                }

            } else {
                //若没有权限，提示获取.
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                Toast.makeText(MainActivity.this, "需要取得权限以使用悬浮窗", Toast.LENGTH_SHORT).show();
                startActivityForResult(intent, 2);
            }
        } else {
            try {
                floatService.windowManager.addView(floatService.OcrLayout, floatService.params1);
            } catch (Exception e) {
                floatService.windowManager.removeView(floatService.OcrLayout);
                floatService.windowManager.addView(floatService.OcrLayout, floatService.params1);
            }
        }

    }


    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = floatService.getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = floatService.getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = floatService.getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        try {//
            Intent it = new Intent(MainActivity.this, photoActivity.class);
            Bundle bundle = new Bundle(); //该类用作携带数据
            bundle.putString("path", imagePath);
            bundle.putInt("code",2);
            it.putExtras(bundle); //为Intent追加额外的数据
            startActivity(it);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = floatService.getImagePath(uri, null);
        Intent it = new Intent(MainActivity.this, photoActivity.class);
        Bundle bundle = new Bundle(); //该类用作携带数据
        bundle.putString("path", imagePath);
        bundle.putInt("code",2);
        it.putExtras(bundle); //为Intent追加额外的数据
        startActivity(it);
    }


    private void copyBigDataToSD(String strFileName) throws IOException {
        //首次运行程序，载入训练文件到sd卡
        InputStream myInput;
        getExternalFilesDir(null).getAbsolutePath();
        File dir = new File(sdPath + File.separator + "test/tessdata/");
        dir.mkdirs();
        File filea = new File(sdPath + File.separator + "test/tessdata/" + Language);

        filea.createNewFile();

        OutputStream myOutput = new FileOutputStream(strFileName);
        myInput = this.getAssets().open(Language);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();

    }


    protected void getImage(String source) {
    /*
    得到高质量照片
     */
        Intent getImage;
        if (source.equalsIgnoreCase("camera")) {
            getImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri uri;
            if (Build.VERSION.SDK_INT >= 24) {
                File g = new File(floatService.picPath);//测试错误
                try {
                    g.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                uri = FileProvider.getUriForFile(this, "blackhole", g);
            } else {
                uri = Uri.fromFile(new File(floatService.picPath));
            }
            getImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(getImage, 1);  //相机
        } else {
            getImage = new Intent(Intent.ACTION_GET_CONTENT);
            getImage.setType("image/*");
            startActivityForResult(getImage, 2);  //相册
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImage("album");
                } else {
                    Toast.makeText(this, "You denied the permisson", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                Log.d("onRequest", "2");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(this)) {
                        // SYSTEM_ALERT_WINDOW permission not granted...
                        Toast.makeText(MainActivity.this, "not granted", Toast.LENGTH_SHORT);
                    }
                }
                break;
            default:
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MainActivity",String.valueOf(requestCode));

        if (resultCode == Activity.RESULT_OK && requestCode == 2) {  //从相册选取图片
            if (Build.VERSION.SDK_INT >= 19) {
                handleImageOnKitKat(data);
            } else {
                handleImageBeforeKitKat(data);
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == 1) {   //从相机选取图片
            //FileInputStream fis = null;
            try {
                //fis = new FileInputStream(floatService.picPath);
                Intent it = new Intent(MainActivity.this, photoActivity.class);
                Bundle bundle = new Bundle(); //该类用作携带数据
                bundle.putString("path", floatService.picPath);
                bundle.putInt("code",1);
                it.putExtras(bundle); //为Intent追加额外的数据
                startActivity(it);
/*              floatService.bmp = null;
                floatService.bmp = floatService.FixImageOrientation(floatService.picPath, "camera");
                ImageView im_camera = findViewById(R.id.img_camera);
                im_camera.setImageBitmap(floatService.bmp);*/
            } catch (Exception e) {
                e.printStackTrace();
            } /*finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
        } else if(requestCode == ShotUtils.REQUEST_MEDIA_PROJECTION){
            Log.d("MainActivity","requestCode == ShotUtils.REQUEST_MEDIA_PROJECTION");

            floatService.shotUtils.setData(data);
        }else {
            // Toast.makeText(this,"没有拍到照片",Toast.LENGTH_SHORT).show();
        }
    }


    public static Bitmap getScaleBitmap(Context ctx, String filePath) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        Bitmap bmp ;

        int bmpWidth = opt.outWidth;
        int bmpHeight = opt.outHeight;

        WindowManager windowManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int screenWidth = display.getWidth();
        int screenHeight = display.getHeight();

        opt.inSampleSize = 2;//尺寸缩小2/1
        if (bmpWidth > bmpHeight) {
            if (bmpWidth > screenWidth)
                opt.inSampleSize = bmpWidth / screenWidth;
        } else {
            if (bmpHeight > screenHeight)
                opt.inSampleSize = bmpHeight / screenHeight;
        }
        opt.inJustDecodeBounds = false;

        bmp = BitmapFactory.decodeFile(filePath, opt);
        return bmp;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}