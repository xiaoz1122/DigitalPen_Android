package com.smart.pen.core.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;


/**
 * 
 * @author Xiaoz
 * @date 2015年10月15日 下午2:38:17
 *
 * Description
 */
public class DrawView extends View{
    private Bitmap mBitmap;
	
	public DrawView(Context context) {
        super(context);

    }
	
	@Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mBitmap == null)return;
        
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }
	
	public void init(int width,int height){
    	if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); //设置位图的宽高
	}
	
	public Bitmap getBitmap(){
		return mBitmap;
	}
}
