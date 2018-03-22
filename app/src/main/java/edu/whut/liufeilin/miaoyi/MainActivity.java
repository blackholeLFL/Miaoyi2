package edu.whut.liufeilin.miaoyi;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
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

import edu.whut.liufeilin.miaoyi.api.TransApi;
import edu.whut.liufeilin.miaoyi.view.OcrView;


public class MainActivity extends Activity {
    //private static Context context;
    private static final String APP_ID = "20180318000137277";
    private static final String SECURITY_KEY = "tYo5nggG_R95EtuJvX0i";
    String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
    Button btn_openCamera;
    Button btn_findText;
    Button btn_getTxt;
    Button btn_openAlbum;
    Button btn_trans;
    OcrView ocrView;
    TextView txtget;
    String wait_trans;//识别出的等待翻译的字符串
    double scale = 1.0;
    Bitmap bmp = null;
    TessBaseAPI mTess;
    String sdPath;
    String picPath;
    String TXT_get = null;
    ThreadPoolExecutor threadPoolExecutor;//线程池
    String filename;
    int ScreenHeight;
    int ScreenWidth;
    public static int REQUST_ORIGINAL = 101;//原图标志

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    final Context context = this;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            if(msg.what==0)
            wait_trans = GetTextFromRect();
            txtget.setText(wait_trans);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //context = MainActivity.this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_openCamera = findViewById(R.id.btn_openCamera);
        btn_findText = findViewById(R.id.btn_findtext);
        btn_getTxt = findViewById(R.id.btn_gettext);
        btn_openAlbum = findViewById(R.id.btn_openAlbum);
        btn_trans = findViewById(R.id.btn_trans);

        ocrView = new OcrView(this);

        txtget = findViewById(R.id.txt_get);
        threadPoolExecutor = new ThreadPoolExecutor(3, 6, 2, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(128));
        sdPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "Android/data/" + getPackageName() + "/files";
        picPath = sdPath + "/" + "temp.png";
        filename = sdPath + "/test/tessdata/chi_sim.traineddata";

        btn_openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // handler.sendEmptyMessage(1);
                txtget.setText("");
                getImage("camera");
                //context1getImageFromCamera(context1);
            }
        });
        btn_openAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtget.setText("");
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    getImage("album");
                }
            }
        });
        btn_findText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ocrView.SelectRect();
            }
        });
        btn_getTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtget.setText("识别中，请稍等。");
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        TXT_get = GetTextFromRect();
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
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        addContentView(ocrView, p);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                initTessBaseData();
            }
        };
        threadPoolExecutor.execute(runnable);
        // Example of a call to a native method
        // TextView tv = (TextView) findViewById(R.id.sample_text);
        // tv.setText(stringFromJNI());
        WindowManager wm = this.getWindowManager();
        ScreenHeight = wm.getDefaultDisplay().getHeight();
        ScreenWidth = wm.getDefaultDisplay().getWidth();
    }


    private void setText(final TextView text, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }


    private void initTessBaseData() {
    /*初始化Tess*/
        mTess = new TessBaseAPI();
        String datapath = sdPath + "/test/";
        String language = "chi_sim";
        File dir = new File(datapath + "tessdata/");
        if (!dir.exists())
            dir.mkdirs();
        mTess.init(datapath, language);
    }


    private void copyBigDataToSD(String strFileName) throws IOException {
        //首次运行程序，载入训练文件到sd卡
        InputStream myInput;
        getExternalFilesDir(null).getAbsolutePath();
        File dir = new File(sdPath + File.separator + "test/tessdata/");
        dir.mkdirs();
        File filea = new File(sdPath + File.separator + "test/tessdata/chi_sim.traineddata");

        filea.createNewFile();

        OutputStream myOutput = new FileOutputStream(strFileName);
        myInput = this.getAssets().open("chi_sim.traineddata");
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


    public boolean isFileExist(String fileName, String path) {
        /*
        判断sd卡上文件是否存在
        无效 原因--未知..
         */
        File file = new File(sdCardRoot + path + File.separator + fileName);
        boolean f = file.exists();
        return file.exists();
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
            default:
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
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }
        try {
            bmp = FixImageOrientation(imagePath, "album");
            ImageView im_camera = findViewById(R.id.img_camera);
            im_camera.setImageBitmap(bmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        try {
            bmp = FixImageOrientation(imagePath, "album");
            ImageView im_camera = findViewById(R.id.img_camera);
            im_camera.setImageBitmap(bmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String getImagePath(Uri uri, String selection) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == 2) {  //从相册选取图片
            if (Build.VERSION.SDK_INT >= 19) {
                handleImageOnKitKat(data);
            } else {
                handleImageBeforeKitKat(data);
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == 1) {   //从相机选取图片
            FileInputStream fis = null;
            try {
//                Log.e("sdpath2",picPath);
                fis = new FileInputStream(picPath);
                bmp = null;
                bmp = FixImageOrientation(picPath, "camera");
                ImageView im_camera = findViewById(R.id.img_camera);
                im_camera.setImageBitmap(bmp);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Toast.makeText(this,"没有拍到照片",Toast.LENGTH_SHORT).show();
        }
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
                File g = new File(picPath);//测试错误
                try {
                    g.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                uri = FileProvider.getUriForFile(this, "blackhole", g);
            } else {
                uri = Uri.fromFile(new File(picPath));
            }
            getImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(getImage, 1);  //相机
        } else {
            getImage = new Intent(Intent.ACTION_GET_CONTENT);
            getImage.setType("image/*");
            startActivityForResult(getImage, 2);  //相册
        }


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


    public String GetTextFromRect() {
        mTess.clear();
        if (bmp == null) return null;
        mTess.setImage(bmp);
//                mTess.setRectangle(0,0,1220,600);
        scale = (double) bmp.getWidth() / (double) ScreenWidth;
        if (scale > 1)
            mTess.setRectangle((int) (scale * ocrView.rect.left), (int) (scale * ocrView.rect.top), (int) (scale * ocrView.rect.width()), (int) (scale * ocrView.rect.height()));
        else
            mTess.setRectangle(ocrView.rect.left, ocrView.rect.top, ocrView.rect.width(), ocrView.rect.height());

//        Log.i(TAG,"hhhhhhhhhhhhhh "+ocrView.rect.left+"\\"+ocrView.rect.top+"\\"+ocrView.rect.width()+"\\"+ocrView.rect.height());
//        Log.i(TAG,"hhhhhhhhhhhhhh "+"\\"+scale+(scale*ocrView.rect.left)+"\\"+(int)(scale*ocrView.rect.top)+
//                "\\"+(int)(scale*ocrView.rect.width())+"\\"+(int)(scale*ocrView.rect.height()));
//        Log.i(TAG,""+bmp.getWidth()+"]]"+ bmp.getHeight());
//        Log.i(TAG,""+ScreenWidth+"]]"+ ScreenHeight);

        String result = mTess.getUTF8Text();
        return result;
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
