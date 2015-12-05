package com.smart.pen.core.views;

import java.util.ArrayList;

import com.smart.pen.core.views.ShapeView.ShapeModel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;

/**
 * 笔画布视图
 * @author Xiaoz
 * @date 2015年10月8日 下午9:49:05
 *
 * Description
 */
public class MultipleCanvasView extends FrameLayout implements OnLongClickListener{
	public static final String TAG = MultipleCanvasView.class.getSimpleName();

	private int mWidth;//画布宽高
    private int mHeight;

    private Paint mPenPaint;//声明画笔
    private Paint mErasePaint;//声明橡皮
    private PenDrawView mPenDrawView;

    private PenModel mPenModel = PenModel.None;
    private int mPenWeight = 3;
    private int mPenColor = Color.BLACK;
    private int mBgColor = Color.WHITE;
    private boolean mIsRubber = false;
    private boolean mIsEditPhoto = false;
    
    private ShapeModel mInsertShape = ShapeModel.None;
    private ShapeView mInsertShapeTmp;
    private boolean mInsertShapeIsFill = false;
    private int mInsertStartX = -1,mInsertStartY = -1;
    private ArrayList<ShapeView> mShapeList = new ArrayList<ShapeView>();
    private ArrayList<PhotoView> mPhotoList = new ArrayList<PhotoView>();

    private CanvasManageInterface mCanvasManageInterface;

    public MultipleCanvasView(Context context,CanvasManageInterface canvasManage) {
        super(context);
        this.mCanvasManageInterface = canvasManage;
        initCanvasInfo();
    }
    public MultipleCanvasView(Context context, AttributeSet attrs){
        super(context, attrs);
        try{
        	this.mCanvasManageInterface = (CanvasManageInterface)context;
        } catch (ClassCastException e) {
			e.printStackTrace();
		}
        initCanvasInfo();
    }

    public MultipleCanvasView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        try{
        	this.mCanvasManageInterface = (CanvasManageInterface)context;
        } catch (ClassCastException e) {
			e.printStackTrace();
		}
        initCanvasInfo();
    }

	@Override
	public boolean onLongClick(View v) {
		Log.v(TAG, "onLongClick tag:"+v.getTag());
		
		return false;
	}
    
    private void initCanvasInfo(){
    	
    	if(mCanvasManageInterface != null){
    		this.mPenModel = mCanvasManageInterface.getPenModel();
            this.mWidth = mCanvasManageInterface.getPenCanvasWidth();
            this.mHeight = mCanvasManageInterface.getPenCanvasHeight();
            this.mPenWeight = mCanvasManageInterface.getPenWeight();
            this.mPenColor = mCanvasManageInterface.getPenColor();
            this.mBgColor = mCanvasManageInterface.getBgColor();
            this.mIsRubber = mCanvasManageInterface.getIsRubber();
            initObjects();
    	}
    }

    private void initObjects(){
    	this.removeAllViews();
    	this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
    	this.setBackgroundColor(mBgColor);

        mPenPaint = new Paint(Paint.DITHER_FLAG);//创建一个画笔
        mPenPaint.setStyle(Paint.Style.STROKE);//设置非填充
        mPenPaint.setStrokeWidth(mPenWeight);
        mPenPaint.setStrokeCap(Paint.Cap.ROUND);
//        mPenPaint.setStrokeJoin(Paint.Join.ROUND);
        mPenPaint.setColor(mPenColor);
        mPenPaint.setAntiAlias(true);//锯齿不显示
        //mPenPaint.setDither(true); 
        mPenPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mErasePaint = new Paint();//创建一个橡皮
        mErasePaint.setStyle(Paint.Style.FILL);
        mErasePaint.setColor(Color.TRANSPARENT);
        mErasePaint.setStrokeWidth(mPenWeight);
        mErasePaint.setAlpha(0);     
        mErasePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN)); 
        
        if(mPenDrawView == null){
        	mPenDrawView = new PenDrawView(getContext());
        }
        mPenDrawView.setPenWeight(mPenWeight);
        mPenDrawView.init(mWidth, mHeight);
        this.addView(mPenDrawView);
        
    }
    
    /**
     * 获取插入的图片数量
     * @return
     */
    public int getPhotoCount(){
    	return mPhotoList.size();
    }
    
    /**
     * 获取插入的图形数量
     * @return
     */
    public int getShapeCount(){
    	return mShapeList.size();
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
        mPenDrawView.setPenWeight(mPenWeight);
    }

    /**
     * 设置笔颜色
     * @param value
     */
    public void setPenColor(int value){
        mPenColor = value;
        mPenPaint.setColor(mPenColor);
    }

    /**
     * 设置当前画笔是否是橡皮擦
     * @param value
     */
    public void setIsRubber(boolean value){
        mIsRubber = value;
    }
    
    /**
     * 设置开始插入图形
     * @param shape
     * @param isFill
     */
    public void setInsertShape(ShapeModel shape,boolean isFill){
    	if(shape == ShapeModel.None){
    		stopInsertShape();
    	}else{
	    	this.mInsertShape = shape;
	    	this.mInsertShapeIsFill = isFill;
    	}
    }
    
    /**
     * 停止插图图形
     */
    public void stopInsertShape(){
    	mInsertShape = ShapeModel.None;
    	mInsertStartX = mInsertStartY = -1;
		mInsertShapeTmp = null;
    	mInsertShapeIsFill = false;
    	mPenPaint.setStyle(Paint.Style.STROKE);
    }
    
    /**
     * 设置编辑图片状态
     */
    public void setPhotoEditState(boolean isEdit){
    	mIsEditPhoto = isEdit;
    	for(int i = 0;i<mPhotoList.size();i++){
    		mPhotoList.get(i).isEdit = isEdit;
    	}
    }
    
    /**
     * 清除全部资源
     */
    public void cleanAll(){
    	cleanAllDraw();
    	cleanAllShape();
    	cleanAllPhoto();
    }
    
    /**
     * 清除画布上的所有图形
     */
    public void cleanAllShape(){
    	ShapeView view;
    	while(mShapeList.size() > 0){
    		view = mShapeList.remove(0);
    		this.removeView(view);
    		view.release();
    	}
    }
    
    /**
     * 清除画布上的所有图片
     */
    public void cleanAllPhoto(){
    	PhotoView view;
    	while(mPhotoList.size() > 0){
    		view = mPhotoList.remove(0);
    		this.removeView(view);
    		view.release();
    	}
    }

    /**
     * 清除笔迹内容
     */
    public void cleanAllDraw(){
    	mPenDrawView.init(mWidth, mHeight);
    	mPenDrawView.invalidate();
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
    }
    
    //触摸事件
    @SuppressLint("ClickableViewAccessibility")
	@Override
    public boolean onTouchEvent(MotionEvent event) {
    	if(mIsEditPhoto)return false;
    	
        int x = (int)event.getX();
        int y = (int)event.getY();
        boolean isRoute = (event.getAction()==MotionEvent.ACTION_MOVE || event.getAction()==MotionEvent.ACTION_DOWN);
    	if(mInsertShape != ShapeModel.None){
    		insertShape(x,y,isRoute);
    	}else{
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
		if(mCanvasManageInterface != null)mCanvasManageInterface.penRouteStatus(isRoute);
		
        Paint paint;
        if(mIsRubber){
        	paint = mErasePaint;
        	mPenDrawView.setPenModel(PenModel.None);
        }else{
        	paint = mPenPaint;
        	mPenDrawView.setPenModel(mPenModel);
        }
        mPenDrawView.drawLine(x, y, isRoute, paint);
    }
    
    /**
     * 插入图形
     * @param x
     * @param y
     * @param isRoute
     */
    private void insertShape(int x, int y, boolean isRoute){
    	if(isRoute){
			if(mInsertStartX < 0 && mInsertStartY < 0){
				mInsertStartX = x;
				mInsertStartY = y;
				
				ShapeView view = new ShapeView(getContext(),mInsertShape);
				view.setIsFill(mInsertShapeIsFill);
				view.setPaint(mPenPaint);

		    	int index = getInsertIndex(mPhotoList.size() + mShapeList.size());
				this.addView(view,index);
				this.mShapeList.add(view);
				
				mInsertShapeTmp = view;
			}else{
				if(mInsertShapeTmp != null)
					mInsertShapeTmp.setSize(mInsertStartX,mInsertStartY,x, y);
			}
		}else if(mInsertStartX > 0 && mInsertStartY > 0){
			mInsertStartX = mInsertStartY = -1;
			mInsertShapeTmp = null;
		}
    }
    
    public void insertPhoto(Bitmap bitmap){
    	mIsEditPhoto = true;
    	PhotoView view = new PhotoView(getContext(),bitmap);
    	
    	int index = getInsertIndex(mPhotoList.size());
		this.addView(view,index);
		mPenDrawView.bringToFront();
		this.mPhotoList.add(view);
    }
    
    private int getInsertIndex(int count){
    	int index = count;
		if(index >= this.getChildCount()){
			if(this.getChildCount() > 1){
				index = this.getChildCount() - 2;
			}else{
				index = 0;
			}
		}
		return index;
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
         * 获取笔的粗细,建议以3为起始值
         * @return
         */
        int getPenWeight();

        /**
         * 获取笔颜色
         * @return
         */
        int getPenColor();
        
        /**
         * 获取背景颜色
         * @return
         */
        int getBgColor();

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
        
        /**
         * 输出绘画状态
         * @param isDraw
         */
        void penRouteStatus(boolean isRoute);
    }
}
