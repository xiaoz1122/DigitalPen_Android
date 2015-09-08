package com.smart.pen.core.model;

import com.smart.pen.core.symbol.BatteryState;
import com.smart.pen.core.symbol.SceneType;

/**
 *数码笔点对象 
 * @author Xiaoz
 * @date 2015年6月17日 上午10:48:03
 *
 * Description
 */
public class PointObject {
	private short width;
	private short height;
	
	private SceneType sceneType = SceneType.NOTHING;
	
	/**
	 * 笔迹原始坐标X
	 */
	public short originalX;
	
	/**
	 * 获取笔迹原始坐标Y
	 */
	public short originalY;
	
	/**x轴偏移坐标**/
	private short offsetX;
	/**y轴偏移坐标**/
	private short offsetY;
	
	/**
	 * 标记是否是笔迹
	 */
	public boolean isRoute;
	
	/**
	 * 标记是否按下按键1
	 */
	public boolean isSw1;
	
	/**
	 * 数码笔电量信息<br /> 
	 * -1、0：没状态<br />	
	 * 1：低电量<br />
	 * 2：良好
	 */
	public BatteryState battery = BatteryState.NOTHING;
	
	/**
	 * 设置场景类型
	 * @param type
	 */
	public void setSceneType(SceneType type){
		sceneType = type;
	}
	/**
	 * 获取场景类型
	 * @return
	 */
	public SceneType getSceneType(){
		return sceneType;
	}
	
	/**
	 * 设置自定义场景
	 * @param width
	 * @param height
	 */
	public void setCustomScene(short width,short height){
		setCustomScene(width,height,(short)0,(short)0);
	}
	
	/**
	 * 设置自定义场景
	 * @param width		场景宽度
	 * @param height	场景高度
	 * @param offsetX	场景位于接收器X中心的偏移量
	 * @param offsetY	场景位于接收器Y起点的偏移量
	 */
	public void setCustomScene(short width,short height,short offsetX,short offsetY){
		this.sceneType = SceneType.CUSTOM;
		this.width = width;
		this.height = height;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public short getOffsetX(){
		return offsetX;
	}
	public short getOffsetY(){
		return offsetY;
	}
	
	public short getWidth(){
		return getWidth(sceneType);
	}
	public short getWidth(SceneType type){
		switch(type){
		case A4:
			return 10000;
		case A4_horizontal:
			return 14500;
		case A5:
			return 7000;
		case A5_horizontal:
			return 9500;
		default:
			return width;
		}
	}

	public short getHeight(){
		return getHeight(sceneType);
	}
	public short getHeight(SceneType type){
		switch(sceneType){
		case A4:
			return 14500;
		case A4_horizontal:
			return 10000;
		case A5:
			return 9500;
		case A5_horizontal:
			return 7000;
		default:
			return height;
		}
	}
	

	/**获取场景x值**/
	public short getSceneX(){
		return getSceneX(0);
	}
	
	/**
	 * 获取场景x值
	 * @param showWidth 当前UI显示的宽度，输出的x将根据这个值与纸张的宽度等比缩放
	 * @return
	 */
	public short getSceneX(int showWidth){
		//计算偏移量
		int value = (int)((originalX + offsetX) + ((float)getWidth() / 2f));
		if(value < 0){
			value = 0;
		}else if(value > getWidth()){
			value = getWidth();
		}
		
		if(showWidth > 0){
			//按显示宽度等比缩放
			value = (int)((float)value * ((float)showWidth / (float)getWidth()));
		}
		
		return (short)value;
	}
	
	/**
	 * 获取场景y值
	 * @return
	 */
	public short getSceneY(){
		return getSceneY(0);
	}
	
	/**
	 * 获取场景y值
	 * @param showHeight 当前UI显示的高度，输出的x将根据这个值与纸张的高度等比缩放
	 * @return
	 */
	public short getSceneY(int showHeight){
		//计算偏移量
		int value = originalY - offsetY > getHeight()?getHeight():originalY - offsetY;
		
		if(showHeight > 0){
			//按显示宽度等比缩放
			value = (int)((float)value * ((float)showHeight / (float)getHeight()));
		}
		
		return (short)value;
	}
	
	@Override
	public String toString(){
		return "isRoute:"+ isRoute +",isSw1:"+ isSw1 +",battery:"+ battery +"\nx:"+ originalX +",y:"+originalY
				+"\nsceneType:"+sceneType+"  sceneX:"+ getSceneX() +",sceneY:"+getSceneY();
	}
}
