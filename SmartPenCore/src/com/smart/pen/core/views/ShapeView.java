package com.smart.pen.core.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
    private Paint mErasePaint;
    private Paint mPaint;
    private Bitmap mBitmap;
    private Canvas mCanvas;

    private boolean mIsFill = false;

    public ShapeView(Context context,ShapeModel model){
        super(context);
        this.mMode = model;
        this.mCanvas = new Canvas();
       
        mErasePaint = new Paint();
        mErasePaint.setStyle(Paint.Style.FILL);
        mErasePaint.setColor(Color.TRANSPARENT);
        mErasePaint.setStrokeWidth(1);  
        mErasePaint.setAlpha(0);     
        mErasePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN)); 
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mBitmap == null)return;
        
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    /**
     * 释放资源
     */
    public void release(){
    	if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
    	mBitmap = null;
    }
    
    /**
     * 设置绘制区域
     * @param w
     * @param h
     */
    public void setDrawSize(int w,int h){
    	recycleBitmap();
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
    }
    
    public void setIsFill(boolean isFill){
        this.mIsFill = isFill;
    }

    public void setPaint(Paint point){
        this.mPaint = point;
        mPaint.setStyle(mIsFill? Paint.Style.FILL: Paint.Style.STROKE);
    }

    /**
     * 设置图形大小
     * @param w
     * @param h
     */
    public void setSize(int l,int t,int r,int b){
    	clearView();
    	int left,top,right,bottom;
    	if(mMode == ShapeModel.Line){
    		left = l;
    		top = t;
    		right = r;
    		if(b > t){
    			bottom = b;
    		}else{
    			bottom = t;
    		}
    	}else{
        	if(r > l){
        		left = l;
        		right  = r;
        	}else{
        		left = r;
        		right = l;
        	}
	    	if(b > t){
	    		top = t;
	    		bottom = b;
	    	}else{
	    		top = b;
	    		bottom = t;
	    	}
    	}
        initShape(left,top,right,bottom);
		invalidate();
    }
    
    private void initShape(int left,int top,int right,int bottom){
    	switch (mMode){
    	case Line:
    		mCanvas.drawLine(left,top,right,bottom,mPaint);
    		break;
		case Rect:
			mCanvas.drawRect(left,top,right,bottom,mPaint);
     		break;
		case Circle:
			drawCircle(left,top,right,bottom);
			break;
		default:

			break;
    	}
    }
    
    private void drawCircle(int left,int top,int right,int bottom){
    	int w = right - left;
    	int h = bottom - top;
        if(w > h){
            mCanvas.drawCircle(left,top,h / 2,mPaint);
        }else{
            mCanvas.drawCircle(left,top,w / 2,mPaint);
        }
    }
    
    private void clearView(){
    	if(mBitmap != null){
    		mCanvas.drawRect(0,0,mBitmap.getWidth(),mBitmap.getHeight(),mErasePaint);
    	}
    }
    
    private void recycleBitmap(){
        if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
        mBitmap = null;
    }
    
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void setViewBackground(Drawable drawable){
    	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
    		this.setBackground(drawable);
    	}else{
    		this.setBackgroundDrawable(drawable);
    	}
    }

    public enum ShapeModel {
        None,
        Line,
        Rect,
        Circle
    }
}
