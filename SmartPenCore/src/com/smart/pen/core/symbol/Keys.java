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
	
	/**发现设备**/
	public static final int MSG_DISCOVERY_DEVICE = 1001;
	
	/**扫描完成**/
	public static final int MSG_DISCOVERY_END = 1002;
	
	/**正在连接**/
	public static final int MSG_CONNECTING = 1003;
	
	/**连接成功**/
	public static final int MSG_CONNECTED = 1004;
	
	/**断开成功**/
	public static final int MSG_DISCONNECTED = 1005;
	
	/**服务发现完成**/
	public static final int MSG_SERVICES_READY = 1006;
	
	/**服务发现失败**/
	public static final int MSG_SERVICES_FAIL = 1007;
	
	/**连接发生错误**/
	public static final int MSG_CONNECT_FAIL = 1008;
	
	/**笔初始化完成**/
	public static final int MSG_PEN_INIT_COMPLETE = 1010;
	
	/**输出笔坐标集合**/
	public static final int MSG_OUT_POINT = 1020;
	
	/**输出笔坐标定位信息**/
	public static final int MSG_OUT_FIXED_INFO = 1021;
	
	/**后台ble服务key**/
	public static final String APP_PEN_SERVICE_NAME = "com.smart.pen.core.services.SmartPenService";
	
	/**后台USB服务key**/
	public static final String APP_USB_SERVICE_NAME = "com.smart.pen.core.services.UsbPenService";
	
	/**广播设置发送广播状态**/
	public final static String ACTION_SERVICE_SETTING_SEND_RECEIVER = "com.smart.pen.core.services.setting.Send_Receiver";
	
	/**广播设置场景类型**/
	public final static String ACTION_SERVICE_SETTING_SCENE_TYPE = "com.smart.pen.core.services.setting.Scene_Type";

	/**发送笔迹广播包**/
	public final static String ACTION_SERVICE_SEND_POINT = "com.smart.pen.core.services.Send_Point";
	
	/**发送ble连接设备广播**/
	public final static String ACTION_SERVICE_BLE_CONNECT = "com.smart.pen.core.services.ble.Connect";
	
	/**发送ble断开设备广播**/
	public final static String ACTION_SERVICE_BLE_DISCONNECT = "com.smart.pen.core.services.ble.Disconnect";
	
	/**发送ble连接状态**/
	public final static String ACTION_SERVICE_BLE_CONNECT_STATE = "com.smart.pen.core.services.ble.Connect_State";
	
	/**发送开始BLE扫描**/
	public final static String ACTION_SERVICE_BLE_SCAN = "com.smart.pen.core.services.ble.Scan";
	
	/**发送ble 发现到的设备**/
	public final static String ACTION_SERVICE_BLE_DISCOVERY_DEVICE = "com.smart.pen.core.services.ble.Discovery_Device";
	
	public final static String ACTION_SERVICE_USB_CONNECT = "com.smart.pen.core.services.usb.Connect";
	public final static String ACTION_SERVICE_USB_DISCONNECT = "com.smart.pen.core.services.usb.Disconnect";
	
	/**usb消息广播**/
	public final static String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	
	/**笔坐标信息key**/
	public static final String KEY_PEN_POINT = "pen_point";
	public static final String KEY_DEVICE_ADDRESS = "device_address";
	public static final String KEY_DEVICE_NAME = "device_name";
	public static final String KEY_CONNECT_STATE = "connect_state";
	public static final String KEY_VALUE = "value";
	
	/**录制设置**/
    public static final String RECORD_SETTING_KEY = "SMART_PEN_RECORD_SETTING_KEY";
    /**录制级别**/
    public static final String RECORD_LEVEL_KEY = "RECORD_LEVEL_KEY";

	/**默认场景**/
	public static final String DEFAULT_SETTING_KEY = "SMART_PEN_DEFAULT_SETTING_KEY";
	
	/**纸张场景**/
	public static final String DEFAULT_SCENE_KEY = "DEFAULT_SCENE_KEY";
	/**自定义场景宽**/
	public static final String DEFAULT_SCENE_WIDTH_KEY = "DEFAULT_SCENE_WIDTH_KEY";
	/**自定义场景高**/
	public static final String DEFAULT_SCENE_HEIGHT_KEY = "DEFAULT_SCENE_HEIGHT_KEY";
	/**自定义场景x偏移**/
	public static final String DEFAULT_SCENE_OFFSET_X_KEY = "DEFAULT_SCENE_OFFSET_X_KEY";
	/**自定义场景y偏移**/
	public static final String DEFAULT_SCENE_OFFSET_Y_KEY = "DEFAULT_SCENE_OFFSET_Y_KEY";
	/**最后一次连接上的设备**/
	public static final String DEFAULT_LAST_DEVICE_KEY = "DEFAULT_LAST_DEVICE_KEY";
	
	/**储存是否自动发现设备**/
	public static final String DEFAULT_AUTO_FIND_DEVICE_KEY = "DEFAULT_AUTO_FIND_DEVICE_KEY";
	/**储存自动发现扫描时间**/
	public static final String DEFAULT_AUTO_FIND_SCAN_KEY = "DEFAULT_AUTO_FIND_SCAN_KEY";
	/**储存自动发现间隔**/
	public static final String DEFAULT_AUTO_FIND_GAP_KEY = "DEFAULT_AUTO_FIND_GAP_KEY";
}
