package com.smart.pen.core.symbol;
/**
 * 
 * @author Xiaoz
 * @date 2015年6月12日 上午11:50:14
 *
 * Description
 */
public class Keys {

	/**笔连接完成，可以开始操作**/
	public static final int MSG_PEN_READY = 1000;
	
	/**笔初始化完成**/
	public static final int MSG_PEN_INIT_COMPLETE = 1010;
	
	/**发现设备**/
	public static final int MSG_DISCOVERY_DEVICE = 1001;
	
	/**扫描完成**/
	public static final int MSG_DISCOVERY_END = 1002;
	
	/**连接成功**/
	public static final int MSG_CONNECTED = 1003;
	
	/**断开成功**/
	public static final int MSG_DISCONNECTED = 1004;
	
	/**服务发现完成**/
	public static final int MSG_SERVICES_READY = 1005;
	
	/**服务发现失败**/
	public static final int MSG_SERVICES_FAIL = 1006;
	
	/**连接发生错误**/
	public static final int MSG_CONNECT_FAIL = 1007;
	
	/**输出笔坐标集合**/
	public static final int MSG_OUT_POINT = 1020;
	
	/**输出笔坐标定位信息**/
	public static final int MSG_OUT_FIXED_INFO = 1021;
	
	/**后台ble服务key**/
	public static final String APP_PEN_SERVICE_NAME = "com.smart.pen.core.services.SmartPenService";

	/**后台发送笔迹广播包**/
	public final static String ACTION_SERVICE_SEND_POINT = "com.smart.pen.core.services.Send_Point";
	
	/**笔坐标信息key**/
	public static final String KEY_PEN_POINT = "pen_point";

	/**默认场景**/
	public static final String DEFAULT_SETTING_KEY = "SMART_PEN_DEFAULT_SETTING_KEY";
	
	public static final String DEFAULT_SCENE_KEY = "DEFAULT_SCENE_KEY";
	public static final String DEFAULT_SCENE_WIDTH_KEY = "DEFAULT_SCENE_WIDTH_KEY";
	public static final String DEFAULT_SCENE_HEIGHT_KEY = "DEFAULT_SCENE_HEIGHT_KEY";
	public static final String DEFAULT_SCENE_OFFSET_X_KEY = "DEFAULT_SCENE_OFFSET_X_KEY";
	public static final String DEFAULT_SCENE_OFFSET_Y_KEY = "DEFAULT_SCENE_OFFSET_Y_KEY";
	/**最后一次连接上的设备**/
	public static final String DEFAULT_LAST_DEVICE_KEY = "DEFAULT_LAST_DEVICE_KEY";
}
