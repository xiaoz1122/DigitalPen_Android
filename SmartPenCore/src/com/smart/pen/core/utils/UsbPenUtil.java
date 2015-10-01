package com.smart.pen.core.utils;

import java.util.ArrayList;
import java.util.List;

import com.smart.pen.core.model.PointObject;
import com.smart.pen.core.symbol.BatteryState;

/**
 * USB数据处理工具
 * @author Xiaoz
 * @date 2015年9月30日 下午2:24:40
 *
 * Description
 */
public class UsbPenUtil extends PenDataUtil{
	public static final String TAG = UsbPenUtil.class.getSimpleName();
	
	
	/**
	 * 获取数码笔点对象
	 * @param device 笔设备，用于根据设备类型过滤数据
	 * @param usbData 笔usb数据
	 * @return
	 */
	public static List<PointObject> getPointList(byte[] usbData){
		List<PointObject> list = new ArrayList<PointObject>();
		if(usbData != null && usbData.length > 0){
			fillPointList(list,usbData);
		}
		return list;
	}
	
	/**
	 * 填充点坐标集合
	 * @param list
	 * @param penData	笔数据
	 */
	private static void fillPointList(List<PointObject> list,byte[] penData){
		byte[] byX = new byte[2];
		byte[] byY = new byte[2];
		
		PointObject item;
		for(int i = 0;i < penData.length;i = i + PEN_DATA_VALID_LENGTH){
			if(isPenData(penData,i)){
				byX[0] = penData[i+2];
				byX[1] = penData[i+3];
				byY[0] = penData[i+4];
				byY[1] = penData[i+5];
				
				item = new PointObject();
				item.originalX = byteToshort(byX);
				item.originalY = byteToshort(byY);
				item.isRoute = isPenRoute(penData,i);
				item.isSw1 = isPenSw1(penData,i);
				item.battery = getBatteryInfo(penData,i);
				list.add(item);
			}
		}
	}

	/**
	 * 检查是否是笔数据
	 * @param data
	 * @param i
	 * @return
	 */
	private static boolean isPenData(byte[] data,int i){
		boolean result = false;
		if(isPenData(data[i],data[i+1])){
			result = true;
		}
		return result;
	}
	
	/***
	 * 检查是否是笔数据
	 * @param b1 笔数据第一个字节
	 * @param b2 笔数据第二个字节
	 * @return
	 */
	private static boolean isPenData(byte b1,byte b2){
		boolean result = false;
		if(toHex(b1).startsWith("4") && toHex(b2).startsWith("0")){
			result = true;
		}
		return result;
	}
	
	/**
	 * 判断是否是书写笔迹
	 * @param data
	 * @param i
	 * @return
	 */
	private static boolean isPenRoute(byte[] data,int i){
		boolean result = false;
		if(isPenData(data,i)){
			String state = toHex(data[i+1]);
			//0001 笔尖按下
			//0010 sw1按下
			//0011 同时按下
			if(state.equals("01") || state.equals("03")){
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * 判断是否按下按键1
	 * @param data
	 * @param i
	 * @return
	 */
	private static boolean isPenSw1(byte[] data,int i){
		boolean result = false;
		if(isPenData(data,i)){
			String state = toHex(data[i+1]);
			//0001 笔尖按下
			//0010 sw1按下
			//0011 同时按下
			if(state.equals("02") || state.equals("03")){
				result = true;
			}
		}
		return result;
	}
	
	private static BatteryState getBatteryInfo(byte[] data,int i){
		BatteryState result = BatteryState.NOTHING;
		if(isPenData(data,i)){
			String state = toHex(data[i]);
			if(state.equals("41")){
				result = BatteryState.LOW;
			}else if(state.equals("42")){
				result = BatteryState.GOOD;
			}
		}
		return result;
	}
}
