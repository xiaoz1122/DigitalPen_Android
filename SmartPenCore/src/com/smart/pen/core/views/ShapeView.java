package com.smart.pen.core.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * 图形view
 * @author Xiaoz
 * @date 2015年10月9日 上午12:56:38
 *
 * Description
 */
public class ShapeView extends View {

    private ShapeModel mMode = ShapeModel.None;
    private Paint mPaint;
    private Canvas mCanvas;
    private Bitmap mBitmap;

    private boolean mIsFill = false;

    public ShapeView(Context context,ShapeModel model){
        super(context);
        this.mMode = model;
        this.mCanvas = new Canvas();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    public void setIsFill(boolean isFill){
        this.mIsFill = isFill;
    }

    public void setPaint(Paint point){
        this.mPaint = point;
    }

    public void setSize(int w,int h){
        recycleBitmap();
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);

        mPaint.setStyle(mIsFill? Paint.Style.FILL: Paint.Style.STROKE);
        switch (mMode){
            case Line:
                mCanvas.drawLine(0,0,w,h,mPaint);
                break;
            case Rectangle:
                mCanvas.drawRect(0,0,w,h,mPaint);
                break;
            case Round:
                drawCircle(w,h);
                break;
            default:
            	
            	break;
        }
        invalidate();
    }

    private void drawCircle(int w,int h){
        if(w > h){
            mCanvas.drawCircle(0,0,h / 2,mPaint);
        }else{
            mCanvas.drawCircle(0,0,w / 2,mPaint);
        }
    }

    private void recycleBitmap(){
        if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
        mBitmap = null;
    }

    public enum ShapeModel {
        None,
        Line,
        Rectangle,
        Round
    }
}
