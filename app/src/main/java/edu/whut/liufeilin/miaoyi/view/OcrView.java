package edu.whut.liufeilin.miaoyi.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by blackhole on 2018/3/3.
 */

public class OcrView extends View {
    //    声明Paint对象
    private Paint mPaint = null;
    private int StrokeWidth = 5;
    private boolean IsUsed = false;
    public Rect rect = new Rect(0, 0, 0, 0);//手动绘制矩形

    public OcrView(Context context) {
        super(context);
        //构建对象
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        IsUsed = false;
    }

    public OcrView(Context context, AttributeSet attrs){
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        IsUsed = false;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("OcrView","onDraw");

        //设置无锯齿
        mPaint.setAntiAlias(true);
//        canvas.drawARGB(25, 255, 227, 0);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(StrokeWidth);
        // mPaint.setColor(Color.GREEN);
        mPaint.setAlpha(100);
        // 绘制绿色实心矩形
        // canvas.drawRect(100, 200, 400, 200 + 400, mPaint);
        mPaint.setColor(Color.RED);
        canvas.drawRect(rect, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        Log.d("OcrView","onTouchEvent "+String.valueOf(IsUsed));
//        if (!IsUsed) return false;
        Log.d("OcrView IsUsed",String.valueOf(IsUsed));
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("OcrView","ACTION_DOWN");
                if(IsUsed){
                    rect.right += StrokeWidth;
                    rect.bottom += StrokeWidth;
                    invalidate(rect);
                    rect.left = x;
                    rect.top = y;
                    rect.right = rect.left;
                    rect.bottom = rect.top;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                Log.d("OcrView","ACTION_MOVE");
                if(IsUsed){
                    Rect old = new Rect(rect.left, rect.top, rect.right + StrokeWidth, rect.bottom + StrokeWidth);
                    rect.right = x;
                    rect.bottom = y;
                    old.union(x, y);
                    invalidate(old);
                }
                break;

            case MotionEvent.ACTION_UP:
                Log.d("OcrView","ACTION_UP");
                if(IsUsed)
                    IsUsed = false;
                break;
            default:
                break;
        }
        Log.d("OcrView","最后return"+String.valueOf(IsUsed));
        return true;  //未启用时不处理触摸信息
//         return  true;//处理了触摸信息，消息不再传递
    }
/*
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);

    }
*/
    public void SelectRect() {
        IsUsed = true;
        Log.d("OcrView","执行"+String.valueOf(IsUsed));

    }
}



