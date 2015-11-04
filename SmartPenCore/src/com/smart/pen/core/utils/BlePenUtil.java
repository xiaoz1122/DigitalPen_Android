package com.smart.pen.core.utils;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;

import com.smart.pen.core.model.DeviceObject;
import com.smart.pen.core.model.PointObject;
import com.smart.pen.core.symbol.BatteryState;
import com.smart.pen.core.symbol.DeviceVersion;

/**
 * 蓝牙数据处理工具
 * @author Xiaoz
 * @date 2015年6月17日 上午10:45:20
 *
 * Description
 */
public class BlePenUtil extends PenDataUtil{
	public static final String TAG = BlePenUtil.class.getSimpleName();
	
	/**获取Characteristic返回的值**/
	@SuppressLint("NewApi")
	public static String getCharacteristicValue(BluetoothGattCharacteristic characteristic){
		final byte[] data = characteristic.getValue();
		if (data != null && data.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(data.length);
			for (byte byteChar : data)
				stringBuilder.append(String.format("%02X ", byteChar));
			
			return stringBuilder.toString();
		}else{
			return null;
		}
	}
	
	/**
	 * 获取数码笔点对象
	 * @param device 笔设备，用于根据设备类型过滤数据
	 * @param bleData 笔蓝牙数据
	 * @return
	 */
	public static List<PointObject> getPointList(DeviceObject device,byte[] bleData){
		List<PointObject> list = new ArrayList<PointObject>();
		bleData = filterBleData(device,bleData);
		if(bleData.length > 0){
			for(int i = 0;i < bleData.length;i++){
				mDataBuffer.add(bleData[i]);
			}

			byte[] penData = getValidPenData(mDataBuffer);
			if(penData != null){
				fillPointList(list,penData);
			}
		}
		return list;
	}
	
	/**
	 * 获取有效笔数据。获取后删除buffer中的相应数据
	 * @param buffer
	 * @return
	 */
	private static byte[] getValidPenData(List<Byte> buffer){
		byte[] result = null;
		//buffer存2组以上数据再开始吐坐标数据，防止丢包造成数据混乱
		if(buffer.size() >= PEN_DATA_VALID_LENGTH * 2){
			//对齐数据，检查2组数据
			//把多余的丢掉
			if(alignPenData(buffer,2)){
				int index = 0;
				int residue = buffer.size() % PEN_DATA_VALID_LENGTH;
				
				result = new byte[buffer.size() - residue];
				while(buffer.size() > residue){
					result[index] = buffer.remove(0);
					index++;
				}
			}
		}
		return result;
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
				
				//打印笔数据
//				String value = "";
//				for(int n = 0;n < PEN_DATA_VALID_LENGTH;n++){
//					value += toHex(penData[i+n])+" ";
//				}
//				Log.v(TAG, "PenData:"+value);
			}
		}
	}
	
	/**
	 * 过滤数码笔蓝牙数据
	 * @param device 笔设备，用于根据设备类型过滤数据
	 * @param data
	 * @return
	 */
	private static byte[] filterBleData(DeviceObject device,byte[] bleData){
		byte[] result;
		if(device != null && device.verMajor == DeviceVersion.XN680T){
			if(bleData[0] >= 0x80){
				result = new byte[0];
			}else{
				int length = bleData[0];
				result = new byte[length];
				System.arraycopy(bleData, 1, result, 0, length);
			}
		}else{
			result = bleData;
		}
		
		//打印ble数据
//		String value = "";
//		for(int i = 0;i < bleData.length;i++){
//			value += toHex(bleData[i])+" ";
//		}
//		Log.v(TAG, "BLE Data:"+value);
		//LogUtil.addLog("BLE Data:"+value);
		
		return result;
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
		if(toHex(b1).startsWith("8") && toHex(b2).startsWith("8")){
			result = true;
		}
		return result;
	}
	
	/**
	 * 对齐笔数据
	 * @param buffer
	 * @param checkNum
	 * @return flase 对齐不成功，没有有效数据
	 */
	private static boolean alignPenData(List<Byte> buffer,int checkNum){
		int length = PEN_DATA_VALID_LENGTH * checkNum;
		List<Byte> data = buffer.subList(0, length);
		
		int index = -1;
		int num = 0;
		for(int i = 0;i < length - PEN_DATA_VALID_LENGTH;i++){
			for(int n = 0;n < checkNum;n++){
				int seek = i + PEN_DATA_VALID_LENGTH * n;
				if(data.size() > seek + 1){
					byte b1 = data.get(seek);
					byte b2 = data.get(seek + 1);
					if(isPenData(b1,b2))num++;
				}
			}
			if(num >= checkNum){
				index = i;
				break;
			}
		}
		
		boolean result = false;
		if(index >= 0){
			result = true;
			if(index > 0){
				int newLength = buffer.size() - index;
				while(buffer.size() > newLength){
					buffer.remove(0);
				}
			}
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
			if(state.equals("81") || state.equals("83")){
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
			if(state.equals("82") || state.equals("83")){
				result = true;
			}
		}
		return result;
	}
	
	private static BatteryState getBatteryInfo(byte[] data,int i){
		BatteryState result = BatteryState.NOTHING;
		if(isPenData(data,i)){
			String state = toHex(data[i]);
			if(state.equals("81")){
				result = BatteryState.LOW;
			}else if(state.equals("82")){
				result = BatteryState.GOOD;
			}
		}
		return result;
	}
}
