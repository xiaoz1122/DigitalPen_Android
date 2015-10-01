package com.smart.pen.core.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Xiaoz
 * @date 2015年9月30日 下午2:31:24
 *
 * Description
 */
public class PenDataUtil {
	
	/**笔数据有效长度**/
	public static final int PEN_DATA_VALID_LENGTH = 6;
	

	
	/**笔数据缓存**/
	protected static List<Byte> mDataBuffer = new ArrayList<Byte>();
	
	/**
	 * 清除数据缓存
	 */
	public static void clearDataBuffer(){
		mDataBuffer.clear();
	}
	
	/**
	 * byte转string
	 * @param by
	 * @return
	 */
	public static String toHex(byte by){
		return ""+"0123456789ABCDEF".charAt((by>>4)&0xf)+"0123456789ABCDEF".charAt(by&0xf);
	}
	
	/**
	 * Change the bytes to a short number
	 * @param by  by[0] the low bit,by[1] the higher
	 * @return short number
	 */
	public static short byteToshort(byte[] by){
		short toshort  = (short) (((by[1]&0xff)<<8)|(by[0]&0xff));
		return toshort;
	}

}
