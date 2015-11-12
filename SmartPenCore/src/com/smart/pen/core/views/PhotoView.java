package com.smart.pen.core.views;


import com.smart.pen.core.utils.BitmapUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

/**
 * 用于添加在画布里的图片视图，实现拖动、缩放
 * @author Xiaoz
 * @date 2015年10月9日 下午4:00:32
 *
 * Description
 */
public class PhotoView extends View {
	public static final String TAG = PhotoView.class.getSimpleName();
	
    private Bitmap mBitmap;
    private int mX,mY,mDownX,mDownY;
    private int mStartX = 0;
    private int mStartY = 0;
    private float mStartDis = -1;
    private int mStartWidth,mStartHeight;
    private byte[] mBitmapData;
    
    /**是否编辑状态**/
    public boolean isEdit = true;
    
    /**
     * 按下点的数量
     */
    private int mDownPointNum = 0;
    
    public PhotoView(Context context,Bitmap bitmap){
        super(context);
        
        this.setTag(TAG);
        this.mBitmap = bitmap;
        this.mBitmapData = BitmapUtil.bitmap2Bytes(mBitmap, 100);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mBitmap == null)return;
        
        canvas.drawBitmap(mBitmap, mX, mY, null);
    }
    
    @SuppressLint("ClickableViewAccessibility")
	@Override
    public boolean onTouchEvent (MotionEvent event) {
    	if(!isEdit)return false;
    	
    	int action = event.getActionMasked();
        int x = (int) event.getX();
        int y = (int) event.getY();
        
        if(action == MotionEvent.ACTION_DOWN 
        		&& (x < mStartX || y < mStartY 
        		|| x > mStartX + getImageWidth() || y > mStartY + getImageHeight()))
        	return false;
        
        switch(action) {
        case MotionEvent.ACTION_DOWN:
        	mDownX = x;
        	mDownY = y;
        	mDownPointNum = 1;
            break;
        case MotionEvent.ACTION_MOVE:
        	if(mStartDis > 0){
        		float dis = distance(event);
        		if(Math.abs(dis - mStartDis) > 1){
	        		zoomImage(dis / mStartDis);
	        	}
        	}else{
        		moveImage(x,y);
        	}
        	break;
        case MotionEvent.ACTION_UP:
        	mStartX = mX;
        	mStartY = mY;
        	mDownPointNum = 0;
            break;
        case MotionEvent.ACTION_POINTER_UP:
        	mDownPointNum--;
        	if(mDownPointNum < 2){
        		mStartDis = -1;
        		mDownX = x;
            	mDownY = y;
        		resetImage();
        	}
        	break;
        case MotionEvent.ACTION_POINTER_DOWN:
        	mDownPointNum++;
        	if(mStartDis < 0){
        		mStartDis = distance(event);
        		mStartWidth = getImageWidth();
        		mStartHeight = getImageHeight();
        	}
        	break;
        }
        return true;
    }
    
    public int getImageWidth(){
    	if(mBitmap != null){
    		return mBitmap.getWidth();
    	}
    	return 0;
    }
    
    public int getImageHeight(){
    	if(mBitmap != null){
    		return mBitmap.getHeight();
    	}
    	return 0;
    }

    /**
     * 释放Bitmap
     */
    public void releaseBitmap(){
    	if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
    	mBitmap = null;
    }
    
    /**
     * 释放资源
     */
    public void release(){
    	releaseBitmap();
    	mBitmapData = null;
    }
    
    /**  
     *  计算两个手指间的距离
     *  @param event
     *  @return   
     */
    private float distance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        /** 使用勾股定理返回两点之间的距离 */
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 还原图片，图片经过多次缩放后会降低质量，取原始数据做还原处理
     */
    private void resetImage(){
    	if(mBitmapData != null){
    		Bitmap bmp = BitmapFactory.decodeByteArray(mBitmapData, 0, mBitmapData.length);
    		if(bmp != null){
    			int w = mBitmap.getWidth();
    			int h = mBitmap.getHeight();
    			if(!mBitmap.isRecycled())mBitmap.recycle();
    			mBitmap = BitmapUtil.zoomBitmap(bmp, w, h);
    			if(!bmp.isRecycled())bmp.recycle();
    		}
    	}
    }
    
    private void moveImage(int x,int y){
    	int moveX = x - mDownX;
    	int moveY = y - mDownY;
    	
    	mX = mStartX + moveX;
    	mY = mStartY + moveY;
    	invalidate();
    }
    
    private void zoomImage(float scale){
    	int newWidth = (int)(mStartWidth * scale);
    	int newHeight = (int)(mStartHeight * scale);
    	mBitmap = BitmapUtil.zoomBitmap(mBitmap, newWidth, newHeight);
    	invalidate();
    }
}
