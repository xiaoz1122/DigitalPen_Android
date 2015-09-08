package com.smart.pen.core.symbol;
/**
 * 
 * @author Xiaoz
 * @date 2015年6月11日 下午4:07:35
 *
 * Description
 */
public enum ConnectState {
	/**没有找到设备**/
	NOTHING,
	/**正在连接**/
	CONNECTING,
	/**连接成功**/
	CONNECTED,
	/**连接错误**/
	CONNECT_FAIL,
	/**正在断开**/
	DISCONNECTING,
	/**已断开**/
	DISCONNECTED,
	/**开始发现服务**/
	SERVICES_START,
	/**服务准备完成**/
	SERVICES_READY,
	/**发现服务失败**/
	SERVICES_FAIL,
	/**笔准备完成**/
	PEN_READY,
	/**笔初始化完成**/
	PEN_INIT_COMPLETE
}
