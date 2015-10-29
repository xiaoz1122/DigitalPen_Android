package com.smart.pen.core.views;

import com.smart.pen.core.views.MultipleCanvasView.PenModel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;


/**
 * 笔书写view
 * @author Xiaoz
 * @date 2015年10月15日 下午2:38:17
 *
 * Description
 */
public class PenDrawView extends View{
	private Path mPath = new Path();
    private Bitmap mBitmap;
    private Canvas mCanvas;//画布
    private PenModel mPenModel = PenModel.None;
    private int mPenWeight = 1;
    private int mDownMoveNum = 0;	//笔移动数量

    private int mLastX;//上一次记录点的坐标
    private int mLastY;
	
	public PenDrawView(Context context) {
        super(context);
        
    }
	
	@SuppressLint("DrawAllocation")
	@Override
    public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
        if(mBitmap == null)return;
        
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }
	
	/**
     * 根据坐标绘制
     * @param x
     * @param y
     * @param isRoute 是否在写
     */
    public void drawLine(int x, int y, boolean isRoute,Paint paint){
        //是否准备写 笔尖是否接触
        if(isRoute){
            //是否是move
            if(mLastX != 0 && mLastY != 0){
            	double speed = Math.sqrt(Math.pow(mLastX-x,2) + Math.pow(mLastY-y,2));
            	//按下移动距离大于一定距离才开始计算weight
            	if(mDownMoveNum > 3 * mPenWeight && mPenModel != PenModel.None){
            		//根据速度计算笔迹粗/细
                	int fix = (int)(speed / 10);
                	float weight = mPenWeight - fix;
                	
                	//如果距离小于计算后的weight，那么不处理
                	if(speed <= weight)return;
                	if(weight < 1)weight = 1;
                	paint.setStrokeWidth(weight);
            	}else if(speed <= mPenWeight){
            		//如果距离小于weight，那么不处理
            		return;
            	}
            	
            	if(mPenModel == PenModel.Pen){
            		mCanvas.drawLine(mLastX, mLastY, x, y, paint);
            	}else{
            		mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
	                mCanvas.drawPath(mPath, paint);
            	}
            	mDownMoveNum++;
                invalidate();
            }else{
                mDownMoveNum = 0;
            	paint.setStrokeWidth(mPenWeight);
            	
            	if(mPenModel == PenModel.Pen){
	            	mCanvas.drawPoint(x, y, paint);
	          	}else{
	                mPath.reset();
	                mPath.moveTo(x,y);
	                mCanvas.drawPath(mPath, paint);
	          	}
            }
            
            mLastX = x;
            mLastY = y;
        }else{
            mPath.reset();
            //没在写
            mLastX = 0;
            mLastY = 0;
        }
    }
	
    public void init(int width,int height){
    	if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
        this.mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        
        this.mCanvas = new Canvas();
        this.mCanvas.setBitmap(mBitmap);
	}
	
    /**
     * 设置笔模式
     * @param model
     */
    public void setPenModel(PenModel model){
    	this.mPenModel = model;
    }
    
    /**
     * 设置笔画宽度
     * @param weight
     */
    public void setPenWeight(int weight){
    	this.mPenWeight = weight;
    }
}
