package com.smart.pen.core.common;

import java.util.HashMap;

import com.smart.pen.core.model.DeviceObject;
import com.smart.pen.core.model.PointObject;
import com.smart.pen.core.symbol.ConnectState;
import com.smart.pen.core.symbol.LocationState;

/**
 * 监听集合
 * @author Xiaoz
 * @date 2015年6月11日 下午2:10:05
 *
 * Description
 */
public class Listeners {
	
	/**
	 * 扫描设备监听
	 */
	public interface OnScanDeviceListener {
		/**
		 * 发现设备
		 * @param device
		 */
		void find(DeviceObject device);
		
		/**
		 * 扫描完成
		 * @param list
		 */
		void complete(HashMap<String,DeviceObject> list);
	}
	
	/**
	 * 连接设备监听
	 */
	public interface OnConnectStateListener{
		/**
		 * 状态更改
		 * @param state
		 */
		void stateChange(String address,ConnectState state);
	}
	
	/**
	 * 笔坐标更改监听
	 */
	public interface OnPointChangeListener{
		/**
		 * 坐标更改
		 * @param point
		 */
		void change(PointObject point);
	}
	
	/**手势监听**/
	public interface OnPenGestureListener{
		
		/**长按**/
		void longClick(PointObject point);
	}
	
	/**
	 * 坐标定点监听
	 *
	 */
	public interface OnFixedPointListener{
		/**
		 * 定位状态
		 * @param first		第一个点
		 * @param second	第二个点
		 * @param state		定位状态
		 */
		void location(PointObject first,PointObject second,LocationState state);
	}
}
