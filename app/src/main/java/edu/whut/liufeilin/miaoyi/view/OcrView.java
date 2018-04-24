package edu.whut.liufeilin.miaoyi.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
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
    private int startX,endX,startY,endY,downX,downY;
    private RectF ltVer,rtVer,lbVer,rbVer;
    private int vertexWidth = 30;
    private int BUTTON_EXTRA_WIDTH = 10;
    private int adjustNum = 0;
    private boolean isAdjustMode = false;
    private boolean isMoveMode = false;
    private boolean isDrawMode = true;
    public Rect rect = new Rect(0, 0, 0, 0);//手动绘制矩形

    public OcrView(Context context) {
        super(context);
        //构建对象
        init(context,null);
    }

    public OcrView(Context context, AttributeSet attrs){
        super(context, attrs);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs){
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        IsUsed = false;
        ltVer=new RectF();
        rtVer=new RectF();
        lbVer=new RectF();
        rbVer=new RectF();
        rect = new Rect(0, 0, 0, 0);
    }
    private void reset(){
        ltVer=new RectF();
        rtVer=new RectF();
        lbVer=new RectF();
        rbVer=new RectF();
        rect = new Rect(0, 0, 0, 0);
        startX=startY=endX=endY=downX=downY=0;
        isAdjustMode = false;
        isMoveMode = false;
        isDrawMode = true;
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
                    adjustNum = 0;
                    downX = x;
                    downY = y;
                    if (isAreaContainPoint(ltVer,x, y)) {
                        isMoveMode = false;
                        isAdjustMode = true;
                        adjustNum = 1;
                    } else if (isAreaContainPoint(rtVer,x, y)) {
                        isMoveMode = false;
                        isAdjustMode = true;
                        adjustNum = 2;
                    } else if (isAreaContainPoint(lbVer,x, y)) {
                        isMoveMode = false;
                        isAdjustMode = true;
                        adjustNum = 3;
                    } else if (isAreaContainPoint(rbVer,x, y)) {
                        isMoveMode = false;
                        isAdjustMode = true;
                        adjustNum = 4;
                    } else if (rect.contains(x, y)) {
                        isMoveMode = true;
                        isAdjustMode = false;
                    }else{
                        if(!isDrawMode){
                            break;
                        }
                        rect.right += StrokeWidth;
                        rect.bottom += StrokeWidth;
                        rect.left = x;
                        rect.top = y;
                        startX = x;
                        startY = y;
                        rect.right = rect.left;
                        rect.bottom = rect.top;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                Log.d("OcrView","ACTION_MOVE");
                if(IsUsed){
                    adjustRect(x,y);
                }
                break;

            case MotionEvent.ACTION_UP:
                Log.d("OcrView","ACTION_UP");
                adjustRect(x, y);
                startX = rect.left;
                startY = rect.top;
                endX = rect.right;
                endY = rect.bottom;
                isDrawMode = false;
//                if(IsUsed)
//                    IsUsed = false;
                break;
            default:
                break;
        }
        invalidate();
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
        reset();
        Log.d("OcrView","执行"+String.valueOf(IsUsed));
    }

    public void StopSelect(){
        IsUsed = false;
    }

    private boolean isAreaContainPoint(Rect area,int x,int y){
        Rect newArea=new Rect(area.left-BUTTON_EXTRA_WIDTH,area.top-BUTTON_EXTRA_WIDTH,area.right+BUTTON_EXTRA_WIDTH,area.bottom+BUTTON_EXTRA_WIDTH);
        if (newArea.contains(x,y)){
            return true;
        }
        return false;
    }
    private boolean isAreaContainPoint(RectF area,int x,int y){
        RectF newArea=new RectF(area.left-BUTTON_EXTRA_WIDTH,area.top-BUTTON_EXTRA_WIDTH,area.right+BUTTON_EXTRA_WIDTH,area.bottom+BUTTON_EXTRA_WIDTH);
        if (newArea.contains(x,y)){
            return true;
        }
        return false;
    }


    public void adjustRect(int x,int y){
        if (isAdjustMode){
            int moveMentX = x-downX;
            int moveMentY = y-downY;

            switch (adjustNum){
                case 1:
                    startX=startX+moveMentX;
                    startY=startY+moveMentY;
                    break;
                case 2:
                    endX=endX+moveMentX;
                    startY=startY+moveMentY;
                    break;
                case 3:
                    startX=startX+moveMentX;
                    endY=endY+moveMentY;
                    break;
                case 4:
                    endX=endX+moveMentX;
                    endY=endY+moveMentY;
                    break;
            }
            downX=x;
            downY=y;
        }else if (isMoveMode){
            int moveMentX = x-downX;
            int moveMentY = y-downY;

            startX=startX+moveMentX;
            startY=startY+moveMentY;

            endX=endX+moveMentX;
            endY=endY+moveMentY;

            downX=x;
            downY=y;
        }else if(isDrawMode){
            endX = x;
            endY = y;
        }
        rect.set(Math.min(startX,endX),Math.min(startY,endY),Math.max(startX, endX), Math.max(startY, endY));
        ltVer.set(rect.left - vertexWidth / 2, rect.top - vertexWidth / 2, rect.left + vertexWidth / 2, rect.top + vertexWidth / 2);
        rtVer.set(rect.right - vertexWidth / 2, rect.top - vertexWidth / 2, rect.right + vertexWidth / 2, rect.top + vertexWidth / 2);
        lbVer.set(rect.left - vertexWidth / 2, rect.bottom - vertexWidth / 2, rect.left + vertexWidth / 2, rect.bottom + vertexWidth / 2);
        rbVer.set(rect.right - vertexWidth / 2, rect.bottom - vertexWidth / 2, rect.right + vertexWidth / 2, rect.bottom + vertexWidth / 2);

    }
/*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("onKeyDown","收到按键事件");
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            return false;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d("dispatchKeyEvent","收到按键事件");
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                || event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) {
            if (mOnKeyListener != null) {
                mOnKeyListener.onKey(this, KeyEvent.KEYCODE_BACK, event);
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    OnKeyListener mOnKeyListener = null;

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        mOnKeyListener = l;

        super.setOnKeyListener(l);
    }
*/
}



