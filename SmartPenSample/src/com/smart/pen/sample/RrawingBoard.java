package com.smart.pen.sample;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class RrawingBoard extends View{
	 private int mov_x;//声明起点坐标
	 private int mov_y;
	 private int last_x;//上一次记录点的坐标
	 private int last_y;
	 private Paint paint;//声明画笔
	 private Canvas canvas;//画布
	 private Bitmap bitmap;//位图
	 private int width;//画布宽高
	 private int height;
	 
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
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); //设置位图的宽高
		paint = new Paint(Paint.DITHER_FLAG);//创建一个画笔
		canvas = new Canvas();
		canvas.setBitmap(bitmap);
		paint.setStyle(Style.STROKE);//设置非填充
		paint.setStrokeWidth(1);//笔宽像素
		paint.setColor(Color.BLACK);
		paint.setAntiAlias(true);//锯齿不显示
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
	//  super.onDraw(canvas);
	  canvas.drawBitmap(bitmap,0,0,null);
	 }
	 //触摸事件
	 @Override
	 public boolean onTouchEvent(MotionEvent event) {
		 if (event.getAction()==MotionEvent.ACTION_MOVE) {//如果拖动
			 canvas.drawLine(mov_x, mov_y, event.getX(), event.getY(), paint);//画线
			 invalidate();
		 }
		 if (event.getAction()==MotionEvent.ACTION_DOWN) {//如果点击
			 mov_x=(int) event.getX();
			 mov_y=(int) event.getY();
			 canvas.drawPoint(mov_x, mov_y, paint);//画点
			 invalidate();
		 }
		 mov_x=(int) event.getX();
		 mov_y=(int) event.getY();
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
				 canvas.drawLine(last_x, last_y, x, y, paint);
				 Log.i("aa", last_x+" , "+last_y+" , "+x+", "+y+", "+isRoute+" Line");
				 last_x = x;
				 last_y = y;
			 }else{
				 canvas.drawPoint(x, y, paint);
				 last_x = x;
				 last_y = y;
				 Log.i("aa", last_x+" , "+last_y+" , "+x+", "+y+", "+isRoute+" paint");
			 }
		 }else{
			 //没在写
			 last_x = 0;
			 last_y = 0;
			 Log.i("aa", last_x+" , "+last_y+" , "+x+", "+y+", "+isRoute+" noWrite");
		 }
		 
		 invalidate();
	 }
	 
	 
}
