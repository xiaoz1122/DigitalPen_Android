package com.smart.pen.core.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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
	private Path mPath = new Path();
	private Rect mRect = new Rect();
    private Paint mPaint;

    private boolean mIsFill = false;

    public ShapeView(Context context,ShapeModel model){
        super(context);
        this.mMode = model;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if(mPaint != null){
        	switch(mMode){
        	case Line:
        		canvas.drawPath(mPath, mPaint);
        		break;
        	case Rect:
        		canvas.drawRect(mRect, mPaint);
        		break;
        	case Circle:
        		canvas.drawPath(mPath, mPaint);
        		return;
        	}
        }
    }
    
    public void setIsFill(boolean isFill){
        this.mIsFill = isFill;
    }

    public void setPaint(Paint point){
        this.mPaint = point;
        mPaint.setStyle(mIsFill? Paint.Style.FILL: Paint.Style.STROKE);
    }
    
    /**
     * 释放资源
     */
    public void release(){
    	mPath.reset();
    	mMode = ShapeModel.None;
    }

    /**
     * 设置图形大小
     * @param w
     * @param h
     */
    public void setSize(int l,int t,int r,int b){
    	int left,top,right,bottom;
    	if(mMode == ShapeModel.Line){
    		left = l;
    		top = t;
    		right = r;
    		bottom = b;
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
		mPath.reset();
    	switch (mMode){
    	case Line:
    		mPath.moveTo(left,top); 
    		mPath.lineTo(right, bottom);
    		break;
		case Rect:
			mRect.set(left, top, right, bottom);
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
        	mPath.addCircle(left,top,h / 2, Path.Direction.CW);
        }else{
        	mPath.addCircle(left,top,w / 2, Path.Direction.CW);
        }
    }
    
    public enum ShapeModel {
        None,
        Line,
        Rect,
        Circle
    }
}
