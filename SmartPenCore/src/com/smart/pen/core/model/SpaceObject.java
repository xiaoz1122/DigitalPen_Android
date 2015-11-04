package com.smart.pen.core.model;
/**
 * 二维空间对象，用于坐标旋转
 * @author Xiaoz
 * @date 2015年10月16日 下午4:39:03
 *
 * Description
 */
public class SpaceObject {
	public short x;
	public short y;
	public short width;
	public short height;
	
	public SpaceObject(short x,short y,short w,short h){
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
	}
	
	/**
	 * 设置角度 -90/0/90/180
	 * @param rotate
	 */
	public void setRotate(int rotate){
		if(rotate != 0){
			int cw = width / 2;
			int ch = height / 2;
			short tmpX = x;
			short tmpY = y;
			
			if(rotate == -90){
				y = (short) (ch - cw + x);
			}
		}
	}
}
