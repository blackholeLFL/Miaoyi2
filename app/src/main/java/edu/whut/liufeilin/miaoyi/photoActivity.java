package edu.whut.liufeilin.miaoyi;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static edu.whut.liufeilin.miaoyi.MainActivity.shotUtils;


public class photoActivity extends AppCompatActivity {
    ImageView imageView;
    FloatService floatService = MainActivity.getMainActivity().floatService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        imageView = findViewById(R.id.img_view);
        Bundle bundle=getIntent().getExtras();
        String imagePath = bundle.getString("path");
        int code=bundle.getInt("code");//2相册 1相机
        Bitmap temp;
        try {
            temp = floatService.FixImageOrientation(imagePath,code);
            imageView.setImageBitmap(temp);
            if (Build.VERSION.SDK_INT >= 21) {
                View decorView = getWindow().getDecorView();
                int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(option);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //SystemClock.sleep(300);
        //floatService.createOcrView();
/*
        shotUtils.startScreenShot(new ShotUtils.ShotListener() {
            @Override
            public void OnSuccess(final Bitmap bitmap) {
                Log.d("OnSuccess","执行");
//                Toast.makeText(FloatService.this,"截图成功", Toast.LENGTH_SHORT).show();
                try{
                    floatService.saveMyBitmap(bitmap);
                    floatService.bmp = bitmap;
                    floatService.ocrView.SelectRect();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
*/

    }
}
