package com.smart.pen.core.model;

import android.bluetooth.BluetoothDevice;

/**
 * 设备对象
 * @author Xiaoz
 * @date 2015年6月11日 下午2:25:58
 *
 * Description
 */
public class DeviceObject {
	/**
	 * 设备名字
	 */
	public String name;
	
	/**
	 * 设备地址
	 */
	public String address;
	
	/**
	 * 设备大版本
	 */
	public int verMajor;
	
	/**
	 * 设备小版本
	 */
	public int verMinor;
	
	public DeviceObject(BluetoothDevice device){
		this.name = device.getName();
		this.address = device.getAddress();
		
		//指定规则的名字过滤
		if(name.startsWith("Pen")){
			//只显示最后6位识别码
			name = "Pen"+name.substring(name.length() - 6, name.length());
		}
	}
}
