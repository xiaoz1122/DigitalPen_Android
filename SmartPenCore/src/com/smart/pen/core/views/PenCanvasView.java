package com.smart.pen.core.views;

import java.util.ArrayList;

import com.smart.pen.core.views.ShapeView.ShapeModel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 
 * @author Xiaoz
 * @date 2015年10月8日 下午9:49:05
 *
 * Description
 */
public class PenCanvasView extends FrameLayout{

	private int mWidth;//画布宽高
    private int mHeight;
    private int mLastX;//上一次记录点的坐标
    private int mLastY;

    private Path mPath;
    private Paint mPenPaint;//声明画笔
    private Paint mErasePaint;//声明橡皮
    private Canvas mCanvas;//画布
    private Bitmap mBitmap;//位图

    private PenModel mPenModel = PenModel.WaterPen;
    private int mPenWeight = 3;
    private int mPenColor = Color.BLACK;
    private boolean mIsRubber = false;
    
    private ArrayList<ShapeView> mShapeViews = new ArrayList<ShapeView>();
    private ShapeModel mAddShape = ShapeModel.None;
    private int mAddStartX = -1,mAddStartY = -1;

    private CanvasManageInterface mCanvasManageInterface;

    public PenCanvasView(Context context,CanvasManageInterface canvasManage) {
        super(context);
        this.mCanvasManageInterface = canvasManage;
        initCanvasInfo();
    }
    public PenCanvasView(Context context, AttributeSet attrs){
        super(context, attrs);
        try{
        	this.mCanvasManageInterface = (CanvasManageInterface)context;
        } catch (ClassCastException e) {
			e.printStackTrace();
		}
        initCanvasInfo();
    }

    public PenCanvasView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        try{
        	this.mCanvasManageInterface = (CanvasManageInterface)context;
        } catch (ClassCastException e) {
			e.printStackTrace();
		}
        initCanvasInfo();
    }
    
    private void initCanvasInfo(){
    	this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
    	
    	if(mCanvasManageInterface != null){
            this.mWidth = mCanvasManageInterface.getPenCanvasWidth();
            this.mHeight = mCanvasManageInterface.getPenCanvasHeight();
            this.mPenWeight = mCanvasManageInterface.getPenWeight();
            this.mPenColor = mCanvasManageInterface.getPenColor();
            this.mIsRubber = mCanvasManageInterface.getIsRubber();
            initObjects();
    	}
    }

    private void initObjects(){
    	if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888); //设置位图的宽高

        mPath = new Path();

        mPenPaint = new Paint(Paint.DITHER_FLAG);//创建一个画笔
        mPenPaint.setStyle(Paint.Style.STROKE);//设置非填充
        mPenPaint.setStrokeWidth(mPenWeight);//笔宽像素
        mPenPaint.setStrokeCap(Paint.Cap.ROUND);
        mPenPaint.setStrokeJoin(Paint.Join.ROUND);
        mPenPaint.setColor(mPenColor);
        mPenPaint.setAntiAlias(true);//锯齿不显示

        mErasePaint = new Paint();//创建一个橡皮
        mErasePaint.setStyle(Paint.Style.FILL);
        mErasePaint.setColor(Color.TRANSPARENT);
        mErasePaint.setStrokeWidth(mPenWeight);  
        mErasePaint.setAlpha(0);     
        mErasePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN)); 

        mCanvas = new Canvas();
        mCanvas.setBitmap(mBitmap);
    }

    /**
     * 设置画布尺寸
     * @param w
     * @param h
     */
    public void setSize(int w,int h){
        mWidth = w;
        mHeight = h;
        initObjects();
    }
    
    public void setPenModel(PenModel value){
    	this.mPenModel = value;
    }

    /**
     * 设置笔粗细
     * @param value
     */
    public void setPenWeight(int value){
        mPenWeight = value;
        mPenPaint.setStrokeWidth(mPenWeight);
        mErasePaint.setStrokeWidth(mPenWeight);
    }

    /**
     * 设置笔颜色
     * @param value
     */
    public void setPenColor(int value){
        mPenColor = value;
        mPenPaint.setColor(mPenColor);
    }

    public void setIsRubber(boolean value){
        mIsRubber = value;
    }
    
    public void setAddShape(ShapeModel shape){
    	this.mAddShape = shape;
    }

    public void cleanContext(){
        if(mBitmap != null && !mBitmap.isRecycled())mBitmap.recycle();
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas.setBitmap(mBitmap);
        invalidate();
    }

    /**
     * 获取书写窗体宽度
     * @return
     */
    public int getWindowWidth(){
        return mWidth;
    }

    /**
     * 获取书写窗体高度
     * @return
     */
    public int getWindowHeight(){
        return mHeight;
    }


    //画位图
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(mBitmap, 0, 0, null);
    }
    
    //触摸事件
    @SuppressLint("ClickableViewAccessibility")
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
    	if(mAddShape != ShapeModel.None){
    		if(mAddStartX < 0 && mAddStartY < 0){
    			mAddStartX = x;
    			mAddStartY = y;
    			
    			ShapeView view = new ShapeView(getContext(),mAddShape);
    			view.setLeft(x);
    			view.setTop(y);
    			mShapeViews.add(view);
    			this.addView(view);
    		}else{
    			mShapeViews.get(mShapeViews.size() - 1).setSize(x - mAddStartX, y - mAddStartY);
    		}
    	}else{
            boolean isRoute = (event.getAction()==MotionEvent.ACTION_MOVE || event.getAction()==MotionEvent.ACTION_DOWN);
            drawLine(x,y,isRoute);
    	}
        return true;
    }

    /**
     * 根据坐标绘制
     * @param x
     * @param y
     * @param isRoute 是否在写
     */
    public void drawLine(int x, int y, boolean isRoute){
        Paint point = mIsRubber?mErasePaint:mPenPaint;
        //是否准备写 笔尖是否接触
        if(isRoute){
            //是否是move
            if(mLastX != 0 && mLastY != 0){
            	double speed = Math.sqrt(Math.pow(mLastX-x,2) + Math.pow(mLastY-y,2));
                if(speed > mPenWeight){
                	if(mPenModel != PenModel.None && !mIsRubber){
                		//根据速度计算笔迹粗/细
	                	int fix = (int)(speed / 10);
	                	int weight = mPenWeight - fix;
	                	if(weight < 1)weight = 1;
	                	point.setStrokeWidth(weight);
                	}
	            	if(mPenModel == PenModel.Pen){
	            		mCanvas.drawLine(mLastX, mLastY, x, y, point);
	            	}else{
	            		mPath.quadTo(mLastX, mLastY, (mLastX + x) / 2, (mLastY + y) / 2);
		                mCanvas.drawPath(mPath, point);
	            	}
	                mLastX = x;
	                mLastY = y;
                }
            }else{
            	point.setStrokeWidth(mPenWeight);

            	if(mPenModel == PenModel.Pen){
                    mCanvas.drawPoint(x, y, point);
            	}else{
	                mCanvas.drawPath(mPath, point);
	                mPath.reset();
	                mPath.moveTo(x,y);
            	}
                mLastX = x;
                mLastY = y;
            }
        }else{
            mPath.reset();

            //没在写
            mLastX = 0;
            mLastY = 0;
        }

        invalidate();
    }

    /**
     * 笔模式
     * @author Xiaoz
     * @date 2015年10月8日 下午9:55:03
     *
     */
    public enum PenModel {
    	None,
    	/**
    	 * 钢笔
    	 */
    	Pen,
    	/**
    	 * 水笔
    	 */
    	WaterPen
    }
    
    public interface CanvasManageInterface {
    	/**
    	 * 获取笔模式
    	 * @return
    	 */
    	PenModel getPenModel();
    	
        /**
         * 获取笔的粗细
         * @return
         */
        int getPenWeight();

        /**
         * 获取笔颜色
         * @return
         */
        int getPenColor();

        /**
         * 获取画布宽度
         * @return
         */
        int getPenCanvasWidth();

        /**
         * 获取画布高度
         * @return
         */
        int getPenCanvasHeight();

        /**
         * 获取是否是橡皮擦状态
         * @return
         */
        boolean getIsRubber();
    }
}
