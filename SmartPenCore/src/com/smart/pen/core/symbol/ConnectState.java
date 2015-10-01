package com.smart.pen.core.symbol;
/**
 * 连接状态
 * @author Xiaoz
 * @date 2015年6月11日 下午4:07:35
 *
 * Description
 */
public enum ConnectState {
	/**没有找到设备**/
	NOTHING(0),
	/**正在连接**/
	CONNECTING(1),
	/**连接成功**/
	CONNECTED(2),
	/**连接错误**/
	CONNECT_FAIL(3),
	/**正在断开**/
	DISCONNECTING(4),
	/**已断开**/
	DISCONNECTED(5),
	/**开始发现服务**/
	SERVICES_START(6),
	/**服务准备完成**/
	SERVICES_READY(7),
	/**发现服务失败**/
	SERVICES_FAIL(8),
	/**笔准备完成**/
	PEN_READY(9),
	/**笔初始化完成**/
	PEN_INIT_COMPLETE(10),
	/**连接错误，需要访问权限**/
	CONNECT_FAIL_PERMISSION(11);
	
	private final int value;

    private ConnectState(int value) {
        this.value = value;
    }

    public final int getValue() {
        return value;
    }
    
    /**
     * int转换为ConnectState，如果溢出那么输出NOTHING
     * @param value 需要转换的int值
     * @return
     */
    public static ConnectState toConnectState(int value){
    	if(value >= 0 && value < ConnectState.values().length){
    		return ConnectState.values()[value];
    	}else{
    		return NOTHING;
    	}
    }
}
