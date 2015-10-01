package com.smart.pen.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class RrawingBoard extends View{
	private int width;//画布宽高
    private int height;
    private int last_x;//上一次记录点的坐标
    private int last_y;

    private Paint mPaint;//声明画笔
    private Canvas mCanvas;//画布
    private Bitmap mBitmap;//位图

    private int mPenWeight = 2;
    private int mPenColor = Color.BLACK;

    public RrawingBoard(Context context) {
        super(context);
    }
    public RrawingBoard(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public RrawingBoard(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    private void init(){
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); //设置位图的宽高
        mPaint = new Paint(Paint.DITHER_FLAG);//创建一个画笔
        mCanvas = new Canvas();
        mCanvas.setBitmap(mBitmap);
        mPaint.setStyle(Paint.Style.STROKE);//设置非填充
        mPaint.setStrokeWidth(mPenWeight);//笔宽像素
        mPaint.setColor(mPenColor);
        mPaint.setAntiAlias(true);//锯齿不显示
    }

    /**
     * 设置画布尺寸
     * @param w
     * @param h
     */
    public void setSize(int w,int h){
        width = w;
        height = h;
        init();
    }

    /**
     * 设置笔粗细
     * @param value
     */
    public void setPenWeight(int value){
        mPenWeight = value;
    }

    /**
     * 设置笔颜色
     * @param value
     */
    public void setPenColor(int value){
        mPenColor = value;
    }

    /**
     * 获取书写窗体宽度
     * @return
     */
    public int getWindowWidth(){
        return width;
    }

    /**
     * 获取书写窗体高度
     * @return
     */
    public int getWindowHeight(){
        return height;
    }


    //画位图
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, null);
        //super.onDraw(canvas);
    }
    //触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isRoute = (event.getAction()==MotionEvent.ACTION_MOVE || event.getAction()==MotionEvent.ACTION_DOWN);
        int x = (int)event.getX();
        int y = (int)event.getY();
        draw(x,y,isRoute);
        return true;
    }

    /**
     * 根据坐标绘制
     * @param x
     * @param y
     * @param isRoute 是否在写
     */
    public void draw(int x, int y, boolean isRoute){
        //是否准备写 笔尖是否接触
        if(isRoute){
            //是否是move
            if(last_x != 0 && last_y != 0){
                mCanvas.drawLine(last_x, last_y, x, y, mPaint);
            }else{
                mCanvas.drawPoint(x, y, mPaint);
            }
            last_x = x;
            last_y = y;
        }else{
            //没在写
            last_x = 0;
            last_y = 0;
        }

        invalidate();
    }
	 
	 
}
