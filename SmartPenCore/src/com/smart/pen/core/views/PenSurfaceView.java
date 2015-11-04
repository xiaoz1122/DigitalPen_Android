package com.smart.pen.core.views;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * 笔绘制图形view
 * @author Xiaoz
 * @date 2015年10月28日 下午3:59:40
 *
 * Description
 */
public class PenSurfaceView extends SurfaceView{

    private SurfaceHolder mSurfaceHolder;  
    private Canvas mCanvas;
    private Path mPath = new Path();
    private Rect mInvalidRect = new Rect(); 
    
    private int mLastX;//上一次记录点的坐标
    private int mLastY;
	
	public PenSurfaceView(Context context){  
        super(context);  
        this.setBackgroundColor(Color.TRANSPARENT);
        this.setZOrderOnTop(true);
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
    }
	
	/**
     * 根据坐标绘制
     * @param x
     * @param y
     * @param isRoute 是否在写
     */
    public void drawLine(int x, int y, boolean isRoute,Paint paint){
    	Rect areaToRefresh = null; 
        //是否准备写 笔尖是否接触
        if(isRoute){
            //是否是move
            if(mLastX != 0 && mLastY != 0){
            	double speed = Math.sqrt(Math.pow(mLastX-x,2) + Math.pow(mLastY-y,2));
            	if(speed > 1){
            		areaToRefresh = mInvalidRect;  
                    areaToRefresh.set(mLastX, mLastY, mLastX, mLastY); 
            		
            		mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
            		
            		areaToRefresh.union(mLastX,mLastY,x, y); 
            	}
            }else{
                mPath.reset();
                mPath.moveTo(x, y);
                mInvalidRect.set(x, y, x , y);
            }
            
            mLastX = x;
            mLastY = y;
        }else{

            //没在写
            mLastX = 0;
            mLastY = 0;
        }

        drawCanvas(paint);
        invalidate();
//        if(areaToRefresh != null){
//            invalidate(areaToRefresh);
//        }
    }
	
	private void drawCanvas(Paint paint) {  
        try {  
        	mCanvas = mSurfaceHolder.lockCanvas();  
            if (mCanvas != null) {  
            	mCanvas.drawPath(mPath, paint);  
            }  
        } catch (Exception e) {  
            // TODO: handle exception  
        } finally {  
            if (mCanvas != null)  
            	mSurfaceHolder.unlockCanvasAndPost(mCanvas);  
        }
	}  
}
